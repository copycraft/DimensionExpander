package com.copicraftDev;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {

    private final long window;
    private final Random rand = new Random();

    // Player / world
    private float playerX = 0f;
    private float playerY = 0f;
    private float velX = 0f;
    private float velY = 0f;

    // Camera
    private float cameraX = 0f;
    private float deadZone = 0.55f; // smaller dead zone

    // Movement tuning
    private final float accel = 6.0f;
    private final float maxSpeed = 2.8f;
    private final float friction = 6.0f;

    // Tunnel sizes
    private float tunnelHeight;
    private final float minTunnelHeight = 0.08f;
    private final float maxTunnelHeight = 1.6f;

    // Mode toggle
    private boolean target2D = false; // desired mode
    private boolean prevH = false;    // H key edge detect

    // smoothing / push
    private final float tunnelSmoothSpeed = 6f;
    private final float pushStrengthBase = 4.5f;

    // Player size
    private final float playerHalfW = 0.06f;
    private final float playerHalfH = 0.06f;

    // Particles
    private static final int PARTICLE_COUNT = 120;
    private final float[] particleX = new float[PARTICLE_COUNT];
    private final float[] particleY = new float[PARTICLE_COUNT];
    private final float[] particleSize = new float[PARTICLE_COUNT];

    // 1D obstacles
    private static final float OBSTACLE_HALF_COLLISION = 0.06f;
    private static final float OBSTACLE_HEIGHT = 0.08f;
    private static final float OBSTACLE_SPAWN_DISTANCE = 2.5f;
    private static final float OBSTACLE_SPAWN_CHANCE_1D = 0.45f;
    private static final float OBSTACLE_SPAWN_CHANCE_2D = 0.03f;

    private static class Obstacle1D {
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
        }

        void render() {
            if (!active) return;
            float yBottom = -OBSTACLE_HEIGHT / 2f;
            float yTop = OBSTACLE_HEIGHT / 2f;

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x - visualHalfW, yBottom);
            GL11.glVertex2f(x + visualHalfW, yBottom);
            GL11.glVertex2f(x + visualHalfW, yTop);
            GL11.glVertex2f(x - visualHalfW, yTop);
            GL11.glEnd();
        }
    }

    private final List<Obstacle1D> obstacles1D = new ArrayList<>();
    private float lastObstacleX = 0f;

    // 2D tunnel obstacles
    private static class TunnelObstacle {
        float x;
        float halfThickness;
        boolean active;

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

    private final List<TunnelObstacle> tunnelObstacles = new ArrayList<>();

    public Game(long window) {
        this.window = window;
        this.tunnelHeight = minTunnelHeight;
        this.cameraX = playerX;

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleX[i] = rand.nextFloat() * 4f - 2f;
            particleY[i] = rand.nextFloat() * 2f - 1f;
            particleSize[i] = 0.002f + rand.nextFloat() * 0.006f;
        }

        System.out.println("Game started");
    }

    public void update(float dt) {
        handleToggleInput();
        handleMovementInput(dt);
        updateTunnel(dt);
        applyVerticalPush(dt);
        integrate(dt);

        spawnObstacles1D();
        spawnTunnelObstaclesIf2D();

        for (Obstacle1D o : obstacles1D) o.update(dt);

        if (isEffectively2D()) {
            checkCollisionTunnelObstacles();
        } else {
            checkObstacleCollision1D();
        }

        updateCamera(dt);
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
        return tunnelHeight > (minTunnelHeight + 0.02f);
    }

    private void handleMovementInput(float dt) {
        float inputX = 0f, inputY = 0f;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) inputX -= 1f;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) inputX += 1f;

        boolean allowVertical = tunnelHeight > (minTunnelHeight + 0.02f);
        if (allowVertical) {
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS ||
                    GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) inputY += 1f;
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS ||
                    GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) inputY -= 1f;
        }

        if (inputX != 0f) velX += inputX * accel * dt;
        else applyFriction(dt);

        if (allowVertical) {
            if (inputY != 0f) velY += inputY * accel * dt;
            else velY *= Math.max(0f, 1f - (friction * 0.5f * dt));
        } else velY *= Math.max(0f, 1f - (friction * 2f * dt));

        velX = clamp(velX, -maxSpeed, maxSpeed);
        velY = clamp(velY, -maxSpeed, maxSpeed);
    }

    private void applyFriction(float dt) {
        if (velX > 0f) { velX -= friction * dt; if (velX < 0f) velX = 0f; }
        else if (velX < 0f) { velX += friction * dt; if (velX > 0f) velX = 0f; }
    }

    private void updateTunnel(float dt) {
        float target = target2D ? maxTunnelHeight : minTunnelHeight;
        float alpha = 1f - (float) Math.exp(-tunnelSmoothSpeed * dt);
        tunnelHeight += (target - tunnelHeight) * alpha;
    }

    private void applyVerticalPush(float dt) {
        float t = (tunnelHeight - minTunnelHeight) / Math.max(0.0001f, (maxTunnelHeight - minTunnelHeight));
        t = clamp(t, 0f, 1f);
        float halfH = tunnelHeight / 2f - playerHalfH - 0.01f;

        if (t < 0.999f) {
            float strength = pushStrengthBase * (1f - t);
            float pushAlpha = 1f - (float) Math.exp(-strength * dt);
            playerY += (0f - playerY) * pushAlpha;
            velY *= Math.max(0f, 1f - (strength * 0.8f * dt));
        }

        // Collision with tunnel edges (gray walls)
        if (playerY < -halfH) { playerY = -halfH; velY = 0f; }
        if (playerY > halfH) { playerY = halfH; velY = 0f; }
    }

    private void integrate(float dt) {
        playerX += velX * dt;
        playerY += velY * dt;
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
            if (t.x > playerX) {
                hasObstacleAhead = true;
                break;
            }
        }

        if (!hasObstacleAhead && rand.nextFloat() < OBSTACLE_SPAWN_CHANCE_2D) {
            float spawnX = playerX + 2f + rand.nextFloat() * 2f;
            float halfThickness = 0.08f + rand.nextFloat() * 0.12f;
            tunnelObstacles.add(new TunnelObstacle(spawnX, halfThickness));
        }
    }

    private void checkObstacleCollision1D() {
        for (Obstacle1D obs : obstacles1D) {
            if (!obs.active) continue;
            if (playerX + playerHalfW > obs.x - OBSTACLE_HALF_COLLISION &&
                    playerX - playerHalfW < obs.x + OBSTACLE_HALF_COLLISION) {
                if (velX > 0f) playerX = obs.x - OBSTACLE_HALF_COLLISION - playerHalfW;
                else if (velX < 0f) playerX = obs.x + OBSTACLE_HALF_COLLISION + playerHalfW;
                velX = 0f;
            }
        }
    }

    private void checkCollisionTunnelObstacles() {
        for (TunnelObstacle to : tunnelObstacles) {
            if (!to.active) continue;
            float half = to.halfThickness;
            if (playerX + playerHalfW > to.x - half &&
                    playerX - playerHalfW < to.x + half) {
                if (velX > 0f) playerX = to.x - half - playerHalfW;
                else if (velX < 0f) playerX = to.x + half + playerHalfW;
                velX = 0f;
            }
        }
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

        drawParticles();
        drawTunnel();
        drawTunnelBorder();

        GL11.glColor3f(0.2f, 0.2f, 0.2f);
        for (Obstacle1D o : obstacles1D) o.render();

        for (TunnelObstacle to : tunnelObstacles) to.render(tunnelHeight);

        drawPlayer();
    }

    private void drawParticles() {
        GL11.glColor3f(0.5f, 0.5f, 0.5f);
        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float x = particleX[i];
            float y = particleY[i];
            float s = particleSize[i];

            float screenLeft = cameraX - 1f;
            float screenRight = cameraX + 1f;
            if (x < screenLeft - 0.1f) x += 4f;
            if (x > screenRight + 0.1f) x -= 4f;
            particleX[i] = x;

            GL11.glVertex2f(x - s, y - s);
            GL11.glVertex2f(x + s, y - s);
            GL11.glVertex2f(x + s, y + s);
            GL11.glVertex2f(x - s, y + s);
        }
        GL11.glEnd();
    }

    private void drawTunnel() {
        float left = cameraX - 1f;
        float right = cameraX + 1f;
        float bottom = -tunnelHeight / 2f;
        float top = tunnelHeight / 2f;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(0f, 0f, 0f);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
    }

    private void drawTunnelBorder() {
        float border = 0.02f;
        float left = cameraX - 1f;
        float right = cameraX + 1f;
        float bottom = -tunnelHeight / 2f;
        float top = tunnelHeight / 2f;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(0.65f, 0.65f, 0.65f);
        // top
        GL11.glVertex2f(left, top);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(right, top + border);
        GL11.glVertex2f(left, top + border);
        // bottom
        GL11.glVertex2f(left, bottom - border);
        GL11.glVertex2f(right, bottom - border);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(left, bottom);
        GL11.glEnd();
    }

    private void drawPlayer() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(1f, 0.6f, 0.2f);
        GL11.glVertex2f(playerX - playerHalfW, playerY - playerHalfH);
        GL11.glVertex2f(playerX + playerHalfW, playerY - playerHalfH);
        GL11.glVertex2f(playerX + playerHalfW, playerY + playerHalfH);
        GL11.glVertex2f(playerX - playerHalfW, playerY + playerHalfH);
        GL11.glEnd();
    }

    private static float clamp(float v, float a, float b) {
        return Math.max(a, Math.min(b, v));
    }
}
