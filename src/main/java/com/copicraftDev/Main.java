package com.copicraftDev;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class Main {

    private long window;
    private int windowWidth = 800;
    private int windowHeight = 600;
    private final int[] fbw = new int[1];
    private final int[] fbh = new int[1];

    private boolean gameStarted = false;
    private Game game = null;

    private final int buttonPixelW = 400;
    private final int buttonPixelH = 150;
    private int buttonPixelX;
    private int buttonPixelY;

    public void run() {
        init();
        loop();
        GLFW.glfwTerminate();
    }

    private void init() {
        if (!GLFW.glfwInit()) throw new IllegalStateException("GLFW init failed");
        window = GLFW.glfwCreateWindow(windowWidth, windowHeight, "Dimension Expander", 0, 0);
        if (window == 0) throw new RuntimeException("Failed to create window");
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GL.createCapabilities();
        int[] ww = new int[1], wh = new int[1];
        GLFW.glfwGetWindowSize(window, ww, wh);
        windowWidth = ww[0];
        windowHeight = wh[0];
        GLFW.glfwGetFramebufferSize(window, fbw, fbh);
        GL11.glViewport(0, 0, fbw[0], fbh[0]);
        computeButtonPosition();
        GLFW.glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            GL11.glViewport(0, 0, w, h);
        });
        GLFW.glfwSetWindowSizeCallback(window, (win, w, h) -> {
            windowWidth = w;
            windowHeight = h;
            computeButtonPosition();
        });
        GL11.glClearColor(0f, 0f, 0f, 1f);
    }

    private void computeButtonPosition() {
        buttonPixelX = (windowWidth - buttonPixelW) / 2;
        buttonPixelY = (windowHeight - buttonPixelH) / 2;
    }

    private void loop() {
        double last = GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();
            GLFW.glfwGetFramebufferSize(window, fbw, fbh);
            GL11.glViewport(0, 0, fbw[0], fbh[0]);
            double now = GLFW.glfwGetTime();
            float dt = (float) Math.max(0.0005, Math.min(0.05, now - last));
            last = now;
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            if (!gameStarted) {
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glLoadIdentity();
                GL11.glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glLoadIdentity();
                drawButtonPixels();
                if (checkButtonClickPixels()) {
                    gameStarted = true;
                }
            } else {
                if (game == null) {
                    game = new Game(window);
                }
                game.update(dt);
            }
            GLFW.glfwSwapBuffers(window);
        }
    }

    private void drawButtonPixels() {
        int x1 = buttonPixelX;
        int y1 = buttonPixelY;
        int x2 = buttonPixelX + buttonPixelW;
        int y2 = buttonPixelY + buttonPixelH;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3f(0.1f, 0.8f, 0.3f);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x1, y2);
        GL11.glEnd();
    }

    private boolean checkButtonClickPixels() {
        double[] mx = new double[1];
        double[] my = new double[1];
        GLFW.glfwGetCursorPos(window, mx, my);
        int mouseX = (int) mx[0];
        int mouseY = (int) my[0];
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            if (mouseX >= buttonPixelX && mouseX <= buttonPixelX + buttonPixelW &&
                    mouseY >= buttonPixelY && mouseY <= buttonPixelY + buttonPixelH) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
