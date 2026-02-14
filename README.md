# Dimension Expander

**Version:** 1.0-SNAPSHOT  
**Author:** copicraftDev  
**License:** [GLWTS (Good Luck With That Shit) Public License](#license)

---

## Overview

**Dimension Expander** is a Java + LWJGL OpenGL game prototype that lets the player **switch between 1D and 2D dimensions** while navigating an endless tunnel. The player can move horizontally in 1D, and both horizontally and vertically in 2D. The game features **smooth camera movement, dynamic obstacles, particle effects, and responsive controls**.  

This project is designed to be a **playable single-player experience** while demonstrating **procedural obstacle generation, 1D/2D dimension expansion mechanics, and smooth input handling**.

---

## Features

- **1D & 2D dimension expansion:** Press `H` to toggle between dimensions.
- **Smooth animations:** Tunnel expands/retracts, obstacles shrink into 1D smoothly.
- **Obstacles:**  
  - 1D obstacles spawn frequently and block the player horizontally.  
  - 2D obstacles spawn rarely and occupy the tunnel width.  
- **Camera deadzone:** Camera only follows player after moving beyond `0.55` units.
- **Particle background:** Subtle gray particles indicate player movement.
- **Infinite horizontal world:** Keep moving right to see new obstacles.
- **Collision detection:** Player cannot pass through active obstacles.
- **Smooth player controls:** Supports **WASD** and **Arrow keys** for movement.

---

## Controls

| Action                    | Key(s)                    |
|----------------------------|---------------------------|
| Move Left                  | `A` / `Left Arrow`        |
| Move Right                 | `D` / `Right Arrow`       |
| Move Up (2D only)          | `W` / `Up Arrow`          |
| Move Down (2D only)        | `S` / `Down Arrow`        |
| Toggle Dimension (1D/2D)  | `H`                        |

---

## How to Play
1. Launch the .jar latest
2. Click the big green button to start playing.
---

## Requirements

- **Java 17+**

---

