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

1. **Launch the game**: Run the `Main` class from your IDE or terminal.  
2. **Click the big green button** on the start screen to begin.  
3. **Move the player** through the tunnel:  
   - In **1D**, only horizontal movement is allowed.  
   - In **2D**, move freely within the tunnel.  
4. **Avoid obstacles**: Hitting obstacles stops movement in that direction.  
5. **Toggle dimensions**: Press `H` to expand/retract the tunnel.  
6. **Observe particle effects**: Particles give a subtle sense of motion.  

---

## Requirements

- **Java 17+**
- **LWJGL 3.3.3+**
- **Gradle 9+** (for building and running)
- Works on **Linux, Windows, and Mac**.

---

## Setup

1. Clone the repository:

```bash
git clone https://github.com/copicraftDev/DimensionExpander.git
cd DimensionExpander
```

Build and run with Gradle:

./gradlew run


On Windows, use gradlew.bat run.

Click the big green button to start playing.

Code Structure

Main.java – Starts the application and displays the menu.

Game.java – Main game logic: player movement, camera, tunnel, obstacles, rendering.

Obstacle1D – 1D obstacles class with smooth shrinking animation.

TunnelObstacle – 2D obstacles class covering the tunnel width.

Particle system – Background movement indicator.
