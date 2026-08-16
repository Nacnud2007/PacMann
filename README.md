# Pac-Mann
This project was made as part of Cornell's CS 2110: Object-Oriented Programming
An enhanced Java Swing implementation of the classic Pac-Man game set in the hilly terrain of Ithaca. Developed for CS 2110, this project models elevation-based navigation using graphs, pathfinding algorithms, and interactive UI controls.  

Features
- Topographical Elevation Modeling: Incorporates Perlin Noise generated terrain where tile height differences affect movement speeds—climbing uphill is slower while traveling downhill is faster.  
PDF
- Dijkstra's Shortest Path AI: Ghost actors navigate the maze using a custom non-backtracking Dijkstra's shortest path algorithm to calculate optimal target trajectories.  
PDF
- 4 Unique Ghost Behaviors:
  - Blinky (Red): Directly pursues Pac-Mann's current position.  
  - Pinky (Pink): Targets 3 spaces ahead of Pac-Mann to ambush him.  
  - Inky (Cyan): Uses vector math targeting relative to both Pac-Mann and Blinky to trap the player.
  - Clyde (Orange): Chases Pac-Mann when far away (distance ≥10), but retreats/roams randomly when close.
- Event-Driven UI: Built with Java Swing, featuring custom component painting, key listeners for directional movement, and observer pattern dialog pop-ups for game-over states.
- PacMannAI Challenge Mode: An autonomous agent mode capable of clearing mazes without user intervention.

Project Architecture
├── a8/           # Core structures (ProbingPacMap, MinPQueue)
├── graph/        # MazeGraph and Pathfinding algorithms
├── model/        # Game rules, state management, and Actor classes (Ghosts, PacMann)
├── ui/           # Custom Swing components (GameBoard, GameFrame)
├── util/         # Map generation, Perlin Noise elevation, and helper utilities
└── tests/        # JUnit unit tests for graph and pathfinding components

---

## Installation & Setup

1. **Prerequisites**: Ensure Java Development Kit (JDK 17 or higher) and JUnit 5 are installed.
2. **Clone & Open**: Open the repository folder directly inside **IntelliJ IDEA** or your preferred Java IDE.
3. **Dependency Copying**: Ensure your completed `ProbingPacMap.java` and `MinPQueue.java` files from A8 are placed inside the `a8/` package directory with `package a8;` at the top.

---

## **Running the Application**

### **Interactive GUI Mode**
To play the game manually, execute the `main()` method inside ui.GraphicalApp
* **Controls**: Use the **Arrow Keys** to dictate Pac-Mann's movement direction[cite: 1].
* **Buttons**: Click **New Game** to generate a fresh randomized maze blueprint, or toggle **Show Graph** / **Show Chase Paths** to visualize backend graph components and ghost targeting lines in real-time[cite: 1].

### **AI Automated Mode**
To run the project using the automated AI controller, run `GraphicalApp` with the program argument:
ai_on

Alternatively, execute batch simulations without GUI rendering to test performance across multiple games:
ui.BatchApp

---

## **Testing**

Unit tests use JUnit 5 to verify graph generation and Dijkstra pathfinding accuracy[cite: 1]. Run the provided test suites via your IDE or build system:
* `tests/graph/MazeGraphTest.java`[cite: 1]
* `tests/graph/PathfindingTest.java`[cite: 1]
