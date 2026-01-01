package com.copicraftDev;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class Game {

    private final long window;

    // World coordinates
    private float playerX = 0f;
    private float playerY = 0f;
    private float velX = 0f;
    private float velY = 0f;

    // Camera
    private float cameraX = 0f;

    // Dead zone for smooth follow
    private final float deadZone = 0.25f; // NDC units

    // Movement tuning
    private final float accel = 8.0f;
    private final float maxSpeed = 3.0f;
    private final float friction = 7.0f;

    // Tunnel sizes
    private float tunnelHeight;
    private final float minTunnelHeight = 0.08f;
    private final float maxTunnelHeight = 1.6f;
    private final float tunnelWidth = 1.6f; // visible width of tunnel (for drawing)

    // Expansion control
    private boolean target2D = false;
    private boolean prevH = false;

    // Smoothing
    private final float tunnelSmoothSpeed = 8f;
    private final float pushStrengthBase = 6f;

    // Player size
    private final float playerHalfW = 0.06f;
    private final float playerHalfH = 0.06f;

    public Game(long window) {
        this.window = window;
        this.tunnelHeight = minTunnelHeight;
        this.cameraX = playerX; // start camera at player
        System.out.println("Game started");
    }

    public void update(float dt) {
        handleToggleInput();
        handleMovementInput(dt);
        updateTunnel(dt);
        applyVerticalPush(dt);
        integrate(dt);
        updateCamera(dt);
        render();
    }

    private void handleToggleInput() {
        boolean hNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_H) == GLFW.GLFW_PRESS;
        if (hNow && !prevH) {
            target2D = !target2D;
        }
        prevH = hNow;
    }

    private void handleMovementInput(float dt) {
        float inputX = 0f;
        float inputY = 0f;

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

        // Apply acceleration
        if (inputX != 0f) velX += inputX * accel * dt;
        else applyFriction(dt);

        if (allowVertical) {
            if (inputY != 0f) velY += inputY * accel * dt;
            else velY *= Math.max(0f, 1f - (friction * 0.5f * dt));
        } else {
            velY *= Math.max(0f, 1f - (friction * 2f * dt));
        }

        // Clamp
        velX = clamp(velX, -maxSpeed, maxSpeed);
        velY = clamp(velY, -maxSpeed, maxSpeed);
    }

    private void applyFriction(float dt) {
        if (velX > 0f) {
            velX -= friction * dt;
            if (velX < 0f) velX = 0f;
        } else if (velX < 0f) {
            velX += friction * dt;
            if (velX > 0f) velX = 0f;
        }
    }

    private void updateTunnel(float dt) {
        float target = target2D ? maxTunnelHeight : minTunnelHeight;
        float alpha = 1f - (float) Math.exp(-tunnelSmoothSpeed * dt);
        tunnelHeight += (target - tunnelHeight) * alpha;
    }

    private void applyVerticalPush(float dt) {
        float halfH = tunnelHeight / 2f - playerHalfH - 0.01f;
        float t = (tunnelHeight - minTunnelHeight) / Math.max(0.0001f, (maxTunnelHeight - minTunnelHeight));
        t = clamp(t, 0f, 1f);

        if (t < 0.999f) {
            float strength = pushStrengthBase * (1f - t);
            float pushAlpha = 1f - (float) Math.exp(-strength * dt);
            playerY += (0f - playerY) * pushAlpha;
            velY *= Math.max(0f, 1f - (strength * 0.8f * dt));
        } else {
            if (playerY < -halfH) playerY = -halfH;
            if (playerY > halfH) playerY = halfH;
        }
    }

    private void integrate(float dt) {
        playerX += velX * dt;
        playerY += velY * dt;
    }

    private void updateCamera(float dt) {
        float leftBound = cameraX - deadZone;
        float rightBound = cameraX + deadZone;

        if (playerX > rightBound) cameraX = playerX - deadZone;
        else if (playerX < leftBound) cameraX = playerX + deadZone;
    }

    private void render() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(cameraX - 1.0, cameraX + 1.0, -1.0, 1.0, -1.0, 1.0);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glClearColor(0.82f, 0.82f, 0.82f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        drawTunnel();
        drawTunnelBorder();
        drawPlayer();
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
