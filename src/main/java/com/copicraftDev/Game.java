package com.copicraftDev;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    private final long window;
    private final Random rand = new Random();
    private float playerX = 0f;
    private float playerY = 0f;
    private float velX = 0f;
    private float velY = 0f;
    private float cameraX = 0f;
    private final float deadZone = 0.55f;
    private static final float ACCEL = 6.0f;
    private static final float MAX_SPEED = 2.8f;
    private static final float FRICTION = 6.0f;
    private float tunnelHeight;
    private static final float MIN_TUNNEL = 0.08f;
    private static final float MAX_TUNNEL = 1.6f;
    private boolean target2D = false;
    private boolean prevH = false;
    private static final float TUNNEL_SMOOTH = 6f;
    private static final float PUSH_BASE = 4.5f;
    private static final float PLAYER_HALF_W = 0.06f;
    private static final float PLAYER_HALF_H = 0.06f;
    private static final int PARTICLE_COUNT = 120;
    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private static final float OBSTACLE_HALF_COLLISION = 0.06f;
    private static final float OBSTACLE_HEIGHT = 0.08f;
    private static final float OBSTACLE_SPAWN_DISTANCE = 2.5f;
    private static final float OBSTACLE_SPAWN_CHANCE_1D = 0.45f;
    private static final float OBSTACLE_SPAWN_CHANCE_2D = 0.03f;
    private final List<Obstacle1D> obstacles1D = new ArrayList<>();
    private float lastObstacleX = 0f;
    private final List<TunnelObstacle> tunnelObstacles = new ArrayList<>();

    public Game(long window) {
        this.window = window;
        this.tunnelHeight = MIN_TUNNEL;
        this.cameraX = playerX;
        this.lastObstacleX = playerX - OBSTACLE_SPAWN_DISTANCE;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float px = rand.nextFloat() * 4f - 2f;
            float py = rand.nextFloat() * 2f - 1f;
            float size = 0.0025f + rand.nextFloat() * 0.0075f;
            float depth = 0.6f + rand.nextFloat() * 1.4f;
            particles[i] = new Particle(px, py, size, depth);
        }
        System.out.println("Game started");
    }

    public void update(float dt) {
        handleToggleInput();
        handleMovementInput(dt);
        updateTunnel(dt);
        applyVerticalPush(dt);
        sweepIntegrateHorizontal(dt);
        integrateVertical(dt);
        spawnObstacles1D();
        spawnTunnelObstaclesIf2D();
        for (Obstacle1D o : obstacles1D) o.update(dt);
        pruneOldObstacles();
        updateCamera(dt);
        for (Particle p : particles) p.update(dt, cameraX);
        render();
    }

    private void handleToggleInput() {
        boolean hNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_H) == GLFW.GLFW_PRESS;
        if (hNow && !prevH) {
            boolean prevTarget2D = target2D;
            target2D = !target2D;
            if (prevTarget2D && !target2D) convertAllTunnelTo1D();
        }
        prevH = hNow;
    }

    private void convertAllTunnelTo1D() {
        float startVisualHalfW = tunnelHeight / 2f;
        float targetHalfW = OBSTACLE_HALF_COLLISION;
        float animSpeed = 8f;
        for (TunnelObstacle to : tunnelObstacles) {
            if (!to.active) continue;
            obstacles1D.add(new Obstacle1D(to.x, startVisualHalfW, targetHalfW, animSpeed));
        }
        tunnelObstacles.clear();
    }

    private boolean isEffectively2D() {
        return tunnelHeight > (MIN_TUNNEL + 0.02f);
    }

    private void handleMovementInput(float dt) {
        float inputX = 0f, inputY = 0f;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) inputX -= 1f;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) inputX += 1f;
        boolean allowVertical = tunnelHeight > (MIN_TUNNEL + 0.02f);
        if (allowVertical) {
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) inputY += 1f;
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) inputY -= 1f;

        }
        if (inputX != 0f) velX += inputX * ACCEL * dt;
        else applyFriction(dt);
        if (allowVertical) {
            if (inputY != 0f) velY += inputY * ACCEL * dt;
            else velY *= Math.max(0f, 1f - (FRICTION * 0.5f * dt));
        } else {
            velY *= Math.max(0f, 1f - (FRICTION * 2f * dt));
        }
        velX = clamp(velX, -MAX_SPEED, MAX_SPEED);
        velY = clamp(velY, -MAX_SPEED, MAX_SPEED);
    }

    private void applyFriction(float dt) {
        if (velX > 0f) { velX -= FRICTION * dt; if (velX < 0f) velX = 0f; }
        else if (velX < 0f) { velX += FRICTION * dt; if (velX > 0f) velX = 0f; }
    }

    private void updateTunnel(float dt) {
        float target = target2D ? MAX_TUNNEL : MIN_TUNNEL;
        float alpha = 1f - (float) Math.exp(-TUNNEL_SMOOTH * dt);
        tunnelHeight += (target - tunnelHeight) * alpha;
    }

    private void applyVerticalPush(float dt) {
        float t = (tunnelHeight - MIN_TUNNEL) / Math.max(0.0001f, (MAX_TUNNEL - MIN_TUNNEL));
        t = clamp(t, 0f, 1f);
        float halfH = tunnelHeight / 2f - PLAYER_HALF_H - 0.01f;
        if (t < 0.999f) {
            float strength = PUSH_BASE * (1f - t);
            float pushAlpha = 1f - (float) Math.exp(-strength * dt);
            playerY += (0f - playerY) * pushAlpha;
            velY *= Math.max(0f, 1f - (strength * 0.8f * dt));
        }
        if (playerY < -halfH) { playerY = -halfH; velY = 0f; }
        if (playerY > halfH)  { playerY = halfH;  velY = 0f; }
    }

    private void sweepIntegrateHorizontal(float dt) {
        float dx = velX * dt;
        if (dx == 0f) return;
        float oldX = playerX;
        float intendedX = playerX + dx;
        float candidateX = intendedX;
        boolean collided = false;
        if (isEffectively2D()) {
            for (TunnelObstacle to : tunnelObstacles) {
                if (!to.active) continue;
                float left = to.x - to.halfThickness;
                float right = to.x + to.halfThickness;
                if (dx > 0f && oldX + PLAYER_HALF_W <= left && intendedX + PLAYER_HALF_W >= left) {
                    float hitX = left - PLAYER_HALF_W;
                    if (hitX < candidateX) { candidateX = hitX; collided = true; }
                } else if (dx < 0f && oldX - PLAYER_HALF_W >= right && intendedX - PLAYER_HALF_W <= right) {
                    float hitX = right + PLAYER_HALF_W;
                    if (hitX > candidateX) { candidateX = hitX; collided = true; }
                }
            }
        } else {
            for (Obstacle1D obs : obstacles1D) {
                if (!obs.active) continue;
                float left = obs.x - OBSTACLE_HALF_COLLISION;
                float right = obs.x + OBSTACLE_HALF_COLLISION;
                if (dx > 0f && oldX + PLAYER_HALF_W <= left && intendedX + PLAYER_HALF_W >= left) {
                    float hitX = left - PLAYER_HALF_W;
                    if (hitX < candidateX) { candidateX = hitX; collided = true; }
                } else if (dx < 0f && oldX - PLAYER_HALF_W >= right && intendedX - PLAYER_HALF_W <= right) {
                    float hitX = right + PLAYER_HALF_W;
                    if (hitX > candidateX) { candidateX = hitX; collided = true; }
                }
            }
        }
        if (collided) {
            playerX = candidateX;
            velX = 0f;
        } else {
            playerX = intendedX;
        }
    }

    private void integrateVertical(float dt) {
        float dy = velY * dt;
        playerY += dy;
        float halfH = tunnelHeight / 2f - PLAYER_HALF_H - 0.01f;
        if (playerY < -halfH) { playerY = -halfH; velY = 0f; }
        if (playerY > halfH)  { playerY = halfH;  velY = 0f; }
    }

    private void spawnObstacles1D() {
        if (isEffectively2D()) return;
        float spawnX = lastObstacleX + OBSTACLE_SPAWN_DISTANCE;
        while (spawnX < playerX + 3f) {
            if (rand.nextFloat() < OBSTACLE_SPAWN_CHANCE_1D) {
                obstacles1D.add(new Obstacle1D(spawnX, OBSTACLE_HALF_COLLISION, OBSTACLE_HALF_COLLISION, 10f));
            }
            spawnX += OBSTACLE_SPAWN_DISTANCE;
        }
        lastObstacleX = spawnX - OBSTACLE_SPAWN_DISTANCE;
    }

    private void spawnTunnelObstaclesIf2D() {
        if (!target2D) return;
        boolean hasObstacleAhead = false;
        for (TunnelObstacle t : tunnelObstacles) {
            if (!t.active) continue;
            if (t.x > playerX - 0.5f && t.x < playerX + 4f) hasObstacleAhead = true;
        }
        if (!hasObstacleAhead && rand.nextFloat() < OBSTACLE_SPAWN_CHANCE_2D) {
            float spawnX = playerX + 2f + rand.nextFloat() * 2f;
            float halfThickness = 0.08f + rand.nextFloat() * 0.12f;
            tunnelObstacles.add(new TunnelObstacle(spawnX, halfThickness));
        }
    }

    private void pruneOldObstacles() {
        float removeBeforeX = cameraX - 6f;
        obstacles1D.removeIf(o -> o.x < removeBeforeX);
        tunnelObstacles.removeIf(t -> t.x < removeBeforeX);
    }

    private void updateCamera(float dt) {
        float leftBound = cameraX - deadZone;
        float rightBound = cameraX + deadZone;
        float targetX = cameraX;
        if (playerX > rightBound) targetX = playerX - deadZone;
        else if (playerX < leftBound) targetX = playerX + deadZone;
        float smoothSpeed = 6f;
        cameraX += (targetX - cameraX) * (1f - (float) Math.exp(-smoothSpeed * dt));
    }

    private void render() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(cameraX - 1.0, cameraX + 1.0, -1.0, 1.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glClearColor(0.82f, 0.82f, 0.82f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        for (Particle p : particles) p.render();
        drawTunnel();
        drawTunnelBorder();
        for (Obstacle1D o : obstacles1D) o.render();
        for (TunnelObstacle to : tunnelObstacles) to.render(tunnelHeight);
        drawPlayer();
    }

    private void drawTunnel() {
        float left = cameraX - 1f;
        float right = cameraX + 1f;
        float bottom = -tunnelHeight / 2f;
        float top = tunnelHeight / 2f;
        GL11.glColor3f(0f, 0f, 0f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
    }

    private void drawTunnelBorder() {
        float left = cameraX - 1f;
        float right = cameraX + 1f;
        float bottom = -tunnelHeight / 2f;
        float top = tunnelHeight / 2f;
        GL11.glLineWidth(2f);
        GL11.glColor3f(0.3f, 0.3f, 0.3f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
    }

    private void drawPlayer() {
        GL11.glColor3f(1f, 0.6f, 0.2f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(playerX - PLAYER_HALF_W, playerY - PLAYER_HALF_H);
        GL11.glVertex2f(playerX + PLAYER_HALF_W, playerY - PLAYER_HALF_H);
        GL11.glVertex2f(playerX + PLAYER_HALF_W, playerY + PLAYER_HALF_H);
        GL11.glVertex2f(playerX - PLAYER_HALF_W, playerY + PLAYER_HALF_H);
        GL11.glEnd();
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static final class Particle {
        float x, y, size, depth, vx, vy, phase;
        Particle(float x, float y, float size, float depth) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.depth = depth;
            this.phase = (float) Math.random() * 6.2831855f;
            this.vx = 0.25f * depth * (0.6f + (float) Math.random() * 0.8f);
            this.vy = ((float) Math.random() - 0.5f) * 0.06f * depth;
        }
        void update(float dt, float camX) {
            phase += dt * (0.4f + depth * 0.6f);
            y += (float) Math.sin(phase) * 0.02f * dt * (1f + depth * 0.5f);
            x += vx * dt;
            y += vy * dt;
            float left = camX - 1f;
            float right = camX + 1f;
            float range = 4f;
            if (x < left - 0.2f) x += range;
            if (x > right + 0.2f) x -= range;
            if (y < -1f) y += 2f;
            if (y > 1f) y -= 2f;
        }
        void render() {
            GL11.glColor3f(0.5f, 0.5f, 0.5f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x - size, y - size);
            GL11.glVertex2f(x + size, y - size);
            GL11.glVertex2f(x + size, y + size);
            GL11.glVertex2f(x - size, y + size);
            GL11.glEnd();
        }
    }

    private static final class Obstacle1D {
        float x;
        float visualHalfW;
        final float targetHalfW;
        final float animSpeed;
        boolean active = true;
        Obstacle1D(float x, float startVisualHalfW, float targetHalfW, float animSpeed) {
            this.x = x;
            this.visualHalfW = startVisualHalfW;
            this.targetHalfW = targetHalfW;
            this.animSpeed = animSpeed;
        }
        void update(float dt) {
            float alpha = 1f - (float) Math.exp(-animSpeed * dt);
            visualHalfW += (targetHalfW - visualHalfW) * alpha;
            if (visualHalfW < 0f) visualHalfW = 0f;
        }
        void render() {
            if (!active) return;
            float yBottom = -OBSTACLE_HEIGHT / 2f;
            float yTop = OBSTACLE_HEIGHT / 2f;
            GL11.glColor3f(0.2f, 0.2f, 0.2f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x - visualHalfW, yBottom);
            GL11.glVertex2f(x + visualHalfW, yBottom);
            GL11.glVertex2f(x + visualHalfW, yTop);
            GL11.glVertex2f(x - visualHalfW, yTop);
            GL11.glEnd();
        }
    }

    private static final class TunnelObstacle {
        float x;
        float halfThickness;
        boolean active = true;
        TunnelObstacle(float x, float halfThickness) {
            this.x = x;
            this.halfThickness = halfThickness;
            this.active = true;
        }
        void render(float tunnelHeight) {
            if (!active) return;
            float h = tunnelHeight / 2f;
            GL11.glColor3f(0.25f, 0.25f, 0.25f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x - halfThickness, -h);
            GL11.glVertex2f(x + halfThickness, -h);
            GL11.glVertex2f(x + halfThickness, h);
            GL11.glVertex2f(x - halfThickness, h);
            GL11.glEnd();
        }
    }
}
