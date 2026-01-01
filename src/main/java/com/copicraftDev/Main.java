package com.copicraftDev;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class Main {

    private long window;
    private int windowWidth = 800;    // logical window coords (what cursor uses)
    private int windowHeight = 600;
    // framebuffer (actual GPU pixels) - used for viewport
    private final int[] fbw = new int[1];
    private final int[] fbh = new int[1];

    private boolean gameStarted = false;
    private Game game = null;

    // Button in pixels (centered)
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

        // initial sizes
        int[] ww = new int[1], wh = new int[1];
        GLFW.glfwGetWindowSize(window, ww, wh);
        windowWidth = ww[0];
        windowHeight = wh[0];
        GLFW.glfwGetFramebufferSize(window, fbw, fbh);
        GL11.glViewport(0, 0, fbw[0], fbh[0]);

        // compute centered button pixel position
        computeButtonPosition();

        // optional: handle window resize to update viewport and button
        GLFW.glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            GL11.glViewport(0, 0, w, h);
            // framebuffer changed (HiDPI), still keep logical window size for mouse coords,
            // but we could query window size if needed.
        });

        // also update on window-size changes (logical size, used for cursor coords)
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
        while (!GLFW.glfwWindowShouldClose(window)) {
            GLFW.glfwPollEvents();

            // Keep viewport in sync (in case dpi/framebuffer changed externally)
            GLFW.glfwGetFramebufferSize(window, fbw, fbh);
            GL11.glViewport(0, 0, fbw[0], fbh[0]);

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            if (!gameStarted) {
                // set simple pixel projection: origin top-left matches GLFW cursor coords
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glLoadIdentity();
                // left=0, right=windowWidth, top=0, bottom=windowHeight => origin top-left
                GL11.glOrtho(0, windowWidth, windowHeight, 0, -1, 1);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glLoadIdentity();

                drawButtonPixels();
                if (checkButtonClickPixels()) {
                    System.out.println("Button clicked!");
                    gameStarted = true;
                }
            } else {
                if (game == null) {
                    game = new Game(window);
                }
                // switch to a logical projection for the game if needed (Game can handle)
                game.update(1f / 60f);
            }

            GLFW.glfwSwapBuffers(window);
        }
    }

    private void drawButtonPixels() {
        // draw using pixel coords (top-left origin)
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
        // Get cursor pos in window coordinates (top-left origin)
        double[] mx = new double[1];
        double[] my = new double[1];
        GLFW.glfwGetCursorPos(window, mx, my);
        int mouseX = (int) mx[0];
        int mouseY = (int) my[0];

        // Debug prints - comment out if noisy
        // System.out.printf("mouseX=%d mouseY=%d windowW=%d windowH=%d fb=%dx%d%n", mouseX, mouseY, windowWidth, windowHeight, fbw[0], fbh[0]);

        // Mouse click down
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            // cursor coords are top-left origin, buttonPixelX/Y uses same top-left origin => direct compare
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
