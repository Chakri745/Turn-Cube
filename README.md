**Cube Puzzle Game**

This project is an interactive cube puzzle game developed using Java Swing. The cube moves on a 4×4 grid, and each time it rolls onto a tile, the color of the bottom face swaps with the tile on the grid. The objective of the game is to collect blue tiles so that all six faces of the cube become blue.

The project also serves as a visualization of different algorithmic problem-solving techniques. Users can manually play the game, request hints, or watch various algorithms automatically solve the puzzle.

**Game Mechanics**

The cube starts on a grid containing several randomly placed blue tiles. When the cube moves to a new square, the bottom face of the cube exchanges color with the tile on that square. By strategically moving across the grid, the player must transfer blue tiles onto the cube faces until every face becomes blue.
Movement can be performed using the arrow keys or by clicking in the direction of movement on the grid.

**Algorithms Implemented**

The project demonstrates multiple algorithmic strategies for solving the puzzle:
Greedy Algorithm – selects the best immediate move using heuristic scoring and sorted move evaluation.
A* Search – uses heuristic-guided search to find an efficient solution path.
Divide and Conquer – solves the puzzle by splitting the task into two subproblems involving different sets of cube faces.
Dynamic Programming – applies memoization and depth-limited search to reuse previously solved states.
Backtracking – explores possible move sequences using iterative deepening and pruning techniques.

**Additional Algorithm Concepts**

Several fundamental algorithmic techniques are also used in the implementation:
Merge Sort for ordering moves based on heuristic scores
Breadth-First Search (BFS) for distance calculation and reachability checks
Graph data structures for grid representation

**Technologies Used:**

Java
Java Swing for graphical user interface
Graph-based state representation
AI search and optimization algorithms

**Purpose of the Project**

The main goal of this project is to demonstrate how different algorithmic approaches can be applied to solve the same problem. By allowing users to compare hints, solutions, and automatic solvers, the project provides an intuitive way to understand the behavior and efficiency of various algorithms in a visual environment.
