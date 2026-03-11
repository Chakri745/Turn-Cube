package game_dp_backtracking;

import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;


enum Direction { // For the movement of the cube
    LEFT("←"), RIGHT("→"), UP("↑"), DOWN("↓");
    
    private final String symbol;
    Direction(String symbol) { this.symbol = symbol; }
    public String getSymbol() { return symbol; }
}

enum SolidType { // For now we use only cube. But scalable
    TETRAHEDRON, CUBE, OCTAHEDRON, ICOSAHEDRON 
}

// ----------------- BASIC CLASSES --------------

class Vector3 { // For the faces of the cube
    float x, y, z; // x--> left/ right , y-> up/down, z-> inside the screen/ outside the screen
    public Vector3(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    } 
}

class GridSquare { // Used for geometric representation not for the logic
    float x, y;
    int nPoints;
    float[] points; 
    Map<Direction, Integer> directions; // store the tile we reach if we go in a particular direction
    
    public GridSquare(float x, float y, float[] points, int nPoints) {
        this.x = x; this.y = y;
        this.points = points;
        this.nPoints = nPoints;
        this.directions = new HashMap<>();
    } 
}

// ----------------- GRAPH COMPONENTS ---------------------

class GraphNode { // Each cell in the grid for the actual logic
    int id;
    GridSquare square;
    Map<Direction, GraphNode> neighbors; // store which cell we will get if we go in that direction
    
    public GraphNode(int id, GridSquare square) {
        this.id = id;
        this.square = square;
        this.neighbors = new HashMap<>();
    }
    
    public void addNeighbor(Direction dir, GraphNode neighbor) { // add neighbour method
        neighbors.put(dir, neighbor); 
    }
    
    public GraphNode getNeighbor(Direction dir) { // get neighbour with direction
        return neighbors.get(dir);
    }  
}

class GridGraph { // The grid panel(4*4)
    List<GraphNode> nodes; // List of all nodes
    Map<Integer, GraphNode> nodeMap; // maps the position with the Node 
    
    public GridGraph() {
        nodes = new ArrayList<>();
        nodeMap = new HashMap<>();
    }
    
    public void addNode(GraphNode node) { // adds the node in both list and 
        nodes.add(node); 
        nodeMap.put(node.id, node);
    }
    
    public GraphNode getNode(int id) {
        return nodeMap.get(id);
    }
    
    public List<GraphNode> getNodes() {
        return nodes;
    }
}

// ------------------------ SOLID -------------------------

class Solid {
    int nVertices, order, nFaces;
    Vector3[] vertices, normals;
    int[] faces;
    float shear, border;
    
    public Solid(int nVertices, float[] vertexData, int order, int nFaces,
                 int[] faces, float[] normalData, float shear, float border) {
        this.nVertices = nVertices;
        this.order = order;
        this.nFaces = nFaces;
        this.faces = faces;
        this.shear = shear;
        this.border = border;
        
        vertices = new Vector3[nVertices]; // Corners of the cube for the gui(8 corners)
        for (int i = 0; i < nVertices; i++) {
            vertices[i] = new Vector3(vertexData[i*3], vertexData[i*3+1], vertexData[i*3+2]);
        }

        normals = new Vector3[nFaces]; // Direction each face points to
        for (int i = 0; i < nFaces; i++) {
            normals[i] = new Vector3(normalData[i*3], normalData[i*3+1], normalData[i*3+2]);
        }
    }
}

class SolidFactory {
    public static Solid createCube() {
        float[] vertices = {
            -0.5f,-0.5f,-0.5f, -0.5f,-0.5f,+0.5f,
            -0.5f,+0.5f,-0.5f, -0.5f,+0.5f,+0.5f,
            +0.5f,-0.5f,-0.5f, +0.5f,-0.5f,+0.5f,
            +0.5f,+0.5f,-0.5f, +0.5f,+0.5f,+0.5f
        };
        int[] faces = {0,1,3,2, 1,5,7,3, 5,4,6,7, 4,0,2,6, 0,4,5,1, 3,7,6,2};
        float[] normals = {-1.0f,0.0f,0.0f, 0.0f,0.0f,+1.0f, +1.0f,0.0f,0.0f, 
                          0.0f,0.0f,-1.0f, 0.0f,-1.0f,0.0f, 0.0f,+1.0f,0.0f};
        return new Solid(8, vertices, 4, 6, faces, normals, 0.3f, 0.5f);
    }
    
    public static Solid getSolid(SolidType type) {
        return createCube();
    }
} 

// ------------- GAME STATE -----------

class GameState {
    SolidType solidType;
    Solid solid;
    int gridWidth, gridHeight;
    GridGraph grid;
    boolean[] blueSquares;
    boolean[] faceColors;
    int currentSquare;
    int moveCount;
    boolean completed;
    int[] faceOrientation;
    
    public GameState(SolidType type, int width, int height) {
        this.solidType = type;
        this.gridWidth = width;
        this.gridHeight = height;
        this.solid = SolidFactory.getSolid(type);
        this.grid = createGrid();
        this.blueSquares = new boolean[grid.nodes.size()];
        this.faceColors = new boolean[solid.nFaces];
        this.currentSquare = 0;
        this.moveCount = 0;
        this.completed = false;
        this.faceOrientation = new int[]{0, 1, 2, 3, 4, 5};
        
        Random rand = new Random(); // Random colored tails 
        int numBlue = Math.min(solid.nFaces, grid.nodes.size() - 1);
        Set<Integer> blueIndices = new HashSet<>();
        while (blueIndices.size() < numBlue) {
            int idx = rand.nextInt(grid.nodes.size());
            if (idx != currentSquare) {
                blueIndices.add(idx);
            }
        }
        for (int idx : blueIndices) {
            blueSquares[idx] = true;
        }
    }
    
    private GridGraph createGrid() {
        GridGraph graph = new GridGraph();
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int id = y * gridWidth + x;
                float[] points = {x-0.5f, y-0.5f, x-0.5f, y+0.5f, x+0.5f, y+0.5f, x+0.5f, y-0.5f};
                GridSquare square = new GridSquare(x, y, points, 4);
                GraphNode node = new GraphNode(id, square);
                graph.addNode(node);
            }
        }
        
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int id = y * gridWidth + x;
                GraphNode node = graph.getNode(id);
                if (x > 0) node.addNeighbor(Direction.LEFT, graph.getNode(id - 1));
                if (x < gridWidth - 1) node.addNeighbor(Direction.RIGHT, graph.getNode(id + 1));
                if (y > 0) node.addNeighbor(Direction.UP, graph.getNode(id - gridWidth));
                if (y < gridHeight - 1) node.addNeighbor(Direction.DOWN, graph.getNode(id + gridWidth));
            }
        }
        
        return graph;
    }
    /*
     *                [5]
                TOP
                 ↑
        -------------------
        |                 |
 [3]    |       CUBE      |    [2]
 LEFT   |                 |    RIGHT
        -------------------
                 ↓
                [4]
               BOTTOM

Front & Back:
        [1] → FRONT  (towards screen)
        [0] → BACK   (into screen)
        
        */
    public boolean makeMove(Direction dir) {
        GraphNode current = grid.getNode(currentSquare);
        GraphNode next = current.getNeighbor(dir);
        
        if (next == null) return false;
        
        int[] newOrientation = new int[6];
        System.arraycopy(faceOrientation, 0, newOrientation, 0, 6);
        
        switch (dir) {
            case UP:
                newOrientation[0] = faceOrientation[5];
                newOrientation[1] = faceOrientation[4];
                newOrientation[2] = faceOrientation[2];
                newOrientation[3] = faceOrientation[3];
                newOrientation[4] = faceOrientation[0];
                newOrientation[5] = faceOrientation[1];
                break;
            case DOWN:
                newOrientation[0] = faceOrientation[4];
                newOrientation[1] = faceOrientation[5];
                newOrientation[2] = faceOrientation[2];
                newOrientation[3] = faceOrientation[3];
                newOrientation[4] = faceOrientation[1];
                newOrientation[5] = faceOrientation[0];
                break;
            case LEFT:
                newOrientation[2] = faceOrientation[5];
                newOrientation[3] = faceOrientation[4];
                newOrientation[4] = faceOrientation[2];
                newOrientation[5] = faceOrientation[3];
                break;
            case RIGHT:
                newOrientation[2] = faceOrientation[4];
                newOrientation[3] = faceOrientation[5];
                newOrientation[4] = faceOrientation[3];
                newOrientation[5] = faceOrientation[2];
                break;
        }
        
        faceOrientation = newOrientation;
        
        int bottomFace = faceOrientation[4];
        boolean tempColor = faceColors[bottomFace];
        faceColors[bottomFace] = blueSquares[next.id];
        blueSquares[next.id] = tempColor;
        
        currentSquare = next.id;
        moveCount++;
        
        // Check if completed
        boolean allBlue = true;
        for (boolean color : faceColors) {
            if (!color) {
                allBlue = false;
                break;
            }
        }
        completed = allBlue;
        
        return true;
    }
    
    public GameState copy() {
        GameState clone = new GameState(solidType, gridWidth, gridHeight);
        clone.currentSquare = currentSquare;
        clone.faceOrientation = faceOrientation.clone();
        clone.faceColors = faceColors.clone();
        clone.blueSquares = blueSquares.clone();
        clone.moveCount = moveCount;
        clone.completed = completed;
        return clone;
    }
    
    public String getStateHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(currentSquare).append("|");
        for (boolean b : faceColors) sb.append(b ? "1" : "0");
        sb.append("|");
        for (int i : faceOrientation) sb.append(i);
        sb.append("|");
        for (boolean b : blueSquares) sb.append(b ? "1" : "0");
        return sb.toString();
    }
}

// ============= SORTING IMPLEMENTATION =============

/**
 * MoveScore class for sorting moves by their heuristic scores
 */
class MoveScore implements Comparable<MoveScore> {
    Direction direction;
    double score;
    
    public MoveScore(Direction direction, double score) {
        this.direction = direction;
        this.score = score;
    }
    
    @Override
    public int compareTo(MoveScore other) {
        return Double.compare(other.score, this.score); // Descending order
    }
}

/**
 * SquareDistance class for sorting squares by distance
 */
class SquareDistance {
    int squareId;
    int distance;
    
    public SquareDistance(int squareId, int distance) {
        this.squareId = squareId;
        this.distance = distance;
    }
}

/**
 * GAME SORTING CLASS - Contains TWO sorting algorithms
 * 1. MERGE SORT - for move prioritization (used by Greedy & D&C)
 * 2. QUICK SORT - for distance-based sorting
 */
class GameSorting {
    
    // ========== SORTING ALGORITHM 1: MERGE SORT ==========
    public static List<MoveScore> mergeSortMoves(List<MoveScore> moves) {
        if (moves.size() <= 1) {
            return moves;
        }
        
        int mid = moves.size() / 2;
        List<MoveScore> left = new ArrayList<>(moves.subList(0, mid));
        List<MoveScore> right = new ArrayList<>(moves.subList(mid, moves.size()));
        
        left = mergeSortMoves(left);
        right = mergeSortMoves(right);
        
        return merge(left, right);
    }
    
    private static List<MoveScore> merge(List<MoveScore> left, List<MoveScore> right) {
        List<MoveScore> result = new ArrayList<>();
        int i = 0, j = 0;
        
        while (i < left.size() && j < right.size()) {
            if (left.get(i).score >= right.get(j).score) {
                result.add(left.get(i++));
            } else {
                result.add(right.get(j++));
            }
        }
        
        while (i < left.size()) result.add(left.get(i++));
        while (j < right.size()) result.add(right.get(j++));
        
        return result;
    }
    
    // ========== SORTING ALGORITHM 2: QUICK SORT ==========
    public static void quickSortDistances(List<SquareDistance> list, int low, int high) {
        if (low < high) {
            int pi = partition(list, low, high);
            quickSortDistances(list, low, pi - 1);
            quickSortDistances(list, pi + 1, high);
        }
    }
    
    private static int partition(List<SquareDistance> list, int low, int high) {
        int pivot = list.get(high).distance;
        int i = low - 1;
        
        for (int j = low; j < high; j++) {
            if (list.get(j).distance <= pivot) {
                i++;
                Collections.swap(list, i, j);
            }
        }
        
        Collections.swap(list, i + 1, high);
        return i + 1;
    }
    
    public static List<Direction> getSortedMovesByScore(GameState state) {
        Direction[] moves = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        List<MoveScore> moveScores = new ArrayList<>();
        
        for (Direction dir : moves) {
            GameState testState = state.copy();
            if (testState.makeMove(dir)) {
                double score = evaluateMoveScore(testState, state);
                moveScores.add(new MoveScore(dir, score));
            }
        }
        
        moveScores = mergeSortMoves(moveScores);
        
        List<Direction> sortedDirections = new ArrayList<>();
        for (MoveScore ms : moveScores) {
            sortedDirections.add(ms.direction);
        }
        
        return sortedDirections;
    }
    
    public static List<Integer> getSortedBlueSquaresByDistance(GameState state) {
        List<SquareDistance> distances = new ArrayList<>();
        
        for (int i = 0; i < state.blueSquares.length; i++) {
            if (state.blueSquares[i] && i != state.currentSquare) {
                int dist = calculateManhattanDistance(state, state.currentSquare, i);
                distances.add(new SquareDistance(i, dist));
            }
        }
        
        if (!distances.isEmpty()) {
            quickSortDistances(distances, 0, distances.size() - 1);
        }
        
        List<Integer> sortedSquares = new ArrayList<>();
        for (SquareDistance sd : distances) {
            sortedSquares.add(sd.squareId);
        }
        
        return sortedSquares;
    }
    
    private static double evaluateMoveScore(GameState newState, GameState oldState) {
        double score = 0;
        
        if (isGoalState(newState)) {
            return 10000;
        }
        
        int blueFaces = 0;
        for (boolean c : newState.faceColors) if (c) blueFaces++;
        score += blueFaces * 100;
        
        int nearestDist = findNearestBlueDistance(newState);
        if (nearestDist >= 0) {
            score += (10.0 / (nearestDist + 1)) * 50;
        }
        
        int oldBottom = oldState.faceOrientation[4];
        int newBottom = newState.faceOrientation[4];
        
        if (!oldState.faceColors[oldBottom] && newState.faceColors[newBottom]) {
            score += 150;
        }
        
        if (oldState.faceColors[oldBottom] && !newState.faceColors[newBottom]) {
            score += 120;
        }
        
        return score;
    }
    
    private static boolean isGoalState(GameState state) {
        for (boolean color : state.faceColors) {
            if (!color) return false;
        }
        return true;
    }
    
    private static int findNearestBlueDistance(GameState state) {
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> distances = new HashMap<>();
        
        queue.offer(state.currentSquare);
        visited.add(state.currentSquare);
        distances.put(state.currentSquare, 0);
        
        while (!queue.isEmpty()) {
            int current = queue.poll();
            int dist = distances.get(current);
            
            if (state.blueSquares[current] && current != state.currentSquare) {
                return dist;
            }
            
            GraphNode node = state.grid.getNode(current);
            Direction[] dirs = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
            for (Direction dir : dirs) {
                GraphNode neighbor = node.getNeighbor(dir);
                if (neighbor != null && !visited.contains(neighbor.id)) {
                    visited.add(neighbor.id);
                    distances.put(neighbor.id, dist + 1);
                    queue.offer(neighbor.id);
                }
            }
        }
        
        return -1;
    }
    
    private static int calculateManhattanDistance(GameState state, int sq1, int sq2) {
        int x1 = sq1 % state.gridWidth;
        int y1 = sq1 / state.gridWidth;
        int x2 = sq2 % state.gridWidth;
        int y2 = sq2 / state.gridWidth;
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
}

// ============= AI SOLVER (GREEDY + A* + BFS) =============

class AISolver {
    private static Direction lastSuggestedMove = null;
    private static int sameMoveCount = 0;
    
    public static Direction findGreedyMove(GameState state) { 
        List<Direction> sortedMoves = GameSorting.getSortedMovesByScore(state);
        
        if (sortedMoves.isEmpty()) {
            return null;
        }
        
        Direction bestMove = sortedMoves.get(0);
        
        if (bestMove == lastSuggestedMove) {
            sameMoveCount++;
            if (sameMoveCount >= 3 && sortedMoves.size() > 1) {
                bestMove = sortedMoves.get(1);
                sameMoveCount = 0;
            }
        } else {
            sameMoveCount = 0;
            lastSuggestedMove = bestMove;
        }
        
        return bestMove;
    }
    
    public static List<Direction> findAStarSolution(GameState currentState) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Map<String, Integer> gScore = new HashMap<>();
        Map<String, AStarNode> cameFrom = new HashMap<>();
        
        String startHash = currentState.getStateHash();
        gScore.put(startHash, 0);
        
        AStarNode startNode = new AStarNode(currentState.copy(), 0, heuristic(currentState));
        openSet.add(startNode);
        
        Direction[] moves = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        int maxIterations = 50000;
        int iterations = 0;
        
        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            AStarNode current = openSet.poll();
            
            if (isGoalState(current.state)) {
                return reconstructPath(cameFrom, current);
            }
            
            for (Direction dir : moves) {
                GameState neighborState = current.state.copy();
                if (neighborState.makeMove(dir)) {
                    String neighborHash = neighborState.getStateHash();
                    int tentativeG = gScore.get(current.state.getStateHash()) + 1;
                    if (!gScore.containsKey(neighborHash) || tentativeG < gScore.get(neighborHash)) {
                        cameFrom.put(neighborHash, new AStarNode(current.state.copy(), 0, 0, dir));
                        gScore.put(neighborHash, tentativeG);
                        int f = tentativeG + heuristic(neighborState);
                        openSet.add(new AStarNode(neighborState.copy(), f, 0, dir));
                    }
                }
            }
        }
        
        return new ArrayList<>();
    }
    
    private static int heuristic(GameState state) {
        int nonBlueFaces = 0;
        for (boolean color : state.faceColors) {
            if (!color) nonBlueFaces++;
        }
        return nonBlueFaces * 2;
    }
    
    private static List<Direction> reconstructPath(Map<String, AStarNode> cameFrom, AStarNode current) {
        List<Direction> path = new ArrayList<>();
        
        while (cameFrom.containsKey(current.state.getStateHash())) {
            AStarNode node = cameFrom.get(current.state.getStateHash());
            path.add(0, node.move);
            current = new AStarNode(node.state.copy(), 0, 0);
        }
        
        return path;
    }
    
    public static List<Direction> findSolution(GameState currentState) {
        List<Direction> solution = findAStarSolution(currentState);
        if (solution.isEmpty()) {
            solution = findOptimalSolution(currentState);
        }
        return solution;
    }
    
    public static List<Direction> findOptimalSolution(GameState currentState) {
        Queue<SearchNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        queue.offer(new SearchNode(currentState.copy(), new ArrayList<>()));
        visited.add(currentState.getStateHash());
        
        Direction[] moves = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        int maxIterations = 100000;
        int iterations = 0;
        
        SearchNode solution = null;
        
        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            SearchNode current = queue.poll();
            
            if (isGoalState(current.state)) {
                solution = current;
                break;
            }
        
            for (Direction dir : moves) {
                GameState next = current.state.copy();
                if (next.makeMove(dir)) {
                    String hash = next.getStateHash();
                    if (!visited.contains(hash)) {
                        visited.add(hash);
                        List<Direction> newPath = new ArrayList<>(current.path);
                        newPath.add(dir);
                        queue.offer(new SearchNode(next, newPath));
                    }
                }
            }
        }
        
        return solution != null ? solution.path : new ArrayList<>();
    }
    
    static boolean isGoalState(GameState state) {
        for (boolean color : state.faceColors) {
            if (!color) return false;
        }
        return true;
    }
    
    static class SearchNode {
        GameState state;
        List<Direction> path;
        SearchNode(GameState state, List<Direction> path) {
            this.state = state;
            this.path = path;
        }
    }
    
    static class AStarNode {
        GameState state;
        int f;
        int g;
        Direction move;
        
        AStarNode(GameState state, int f, int g) {
            this(state, f, g, null);
        }
        
        AStarNode(GameState state, int f, int g, Direction move) {
            this.state = state;
            this.f = f;
            this.g = g;
            this.move = move;
        }
    }
}

// ============= DIVIDE AND CONQUER SOLVER =============

class DivideConquerSolver {
    private static final int MAX_ITERATIONS = 50000;
    
    public static List<Direction> solveDivideAndConquer(GameState initialState) {
        if (isGoalState(initialState)) {
            return new ArrayList<>();
        }
        
        List<Direction> fullSolution = new ArrayList<>();
        GameState currentState = initialState.copy();
        
        Set<Integer> firstHalf = new HashSet<>(Arrays.asList(0, 1, 2));
        List<Direction> solution1 = solveSubproblem(currentState, firstHalf);
        
        for (Direction dir : solution1) {
            currentState.makeMove(dir);
        }
        fullSolution.addAll(solution1);
        
        Set<Integer> secondHalf = new HashSet<>(Arrays.asList(3, 4, 5));
        List<Direction> solution2 = solveSubproblemPreserving(currentState, secondHalf, firstHalf);
        
        fullSolution.addAll(solution2);
        
        return fullSolution;
    }
    
    private static List<Direction> solveSubproblem(GameState state, Set<Integer> targetFaces) {
        Queue<SearchNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>(); 
        
        queue.offer(new SearchNode(state.copy(), new ArrayList<>()));
        visited.add(state.getStateHash());
        
        Direction[] moves = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        int iterations = 0;
        
        while (!queue.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            SearchNode current = queue.poll();
            
            if (isSubproblemSolved(current.state, targetFaces)) {
                return current.path;
            }

            for (Direction dir : moves) {
                GameState next = current.state.copy();
                if (next.makeMove(dir)) {
                    String hash = next.getStateHash();
                    if (!visited.contains(hash)) {
                        visited.add(hash);
                        List<Direction> newPath = new ArrayList<>(current.path);
                        newPath.add(dir);
                        queue.offer(new SearchNode(next, newPath));
                    }
                }
            }
        } 
        return new ArrayList<>();
    }
    
    private static List<Direction> solveSubproblemPreserving(GameState state, 
                                                              Set<Integer> targetFaces, 
                                                              Set<Integer> preserveFaces) {
        Queue<SearchNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>(); 
        queue.offer(new SearchNode(state.copy(), new ArrayList<>()));
        visited.add(state.getStateHash());
        Direction[] moves = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        int iterations = 0;
        while (!queue.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            SearchNode current = queue.poll();
            if (isSubproblemSolved(current.state, targetFaces) && 
                arePreserved(current.state, preserveFaces)) {
                return current.path;
            }
            for (Direction dir : moves) {
                GameState next = current.state.copy();
                if (next.makeMove(dir)) {
                    if (countBlueFaces(next, preserveFaces) >= preserveFaces.size() - 1) {
                        String hash = next.getStateHash();
                        if (!visited.contains(hash)) {
                            visited.add(hash);
                            List<Direction> newPath = new ArrayList<>(current.path);
                            newPath.add(dir);
                            queue.offer(new SearchNode(next, newPath));
                        }
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    public static Direction findDivideConquerHint(GameState state) {
        Set<Integer> firstHalf = new HashSet<>(Arrays.asList(0, 1, 2));
        
        boolean firstHalfDone = isSubproblemSolved(state, firstHalf);
        
        Set<Integer> targetSet = firstHalfDone ? 
            new HashSet<>(Arrays.asList(3, 4, 5)) : firstHalf;
        
        List<Direction> sortedMoves = GameSorting.getSortedMovesByScore(state);
        
        for (Direction dir : sortedMoves) {
            GameState testState = state.copy();
            if (testState.makeMove(dir)) {
                int beforeBlue = countBlueFaces(state, targetSet);
                int afterBlue = countBlueFaces(testState, targetSet);
                
                if (afterBlue > beforeBlue) {
                    return dir;
                }
            }
        }
        
        return sortedMoves.isEmpty() ? null : sortedMoves.get(0);
    }
    
    private static boolean isSubproblemSolved(GameState state, Set<Integer> targetFaces) {
        for (int face : targetFaces) {
            if (!state.faceColors[face]) {
                return false;
            }
        }
        return true;
    }
  
    private static boolean arePreserved(GameState state, Set<Integer> preserveFaces) {
        for (int face : preserveFaces) {
            if (!state.faceColors[face]) {
                return false;
            }
        }
        return true;
    }
    
    static int countBlueFaces(GameState state, Set<Integer> faces) {
        int count = 0;
        for (int face : faces) {
            if (state.faceColors[face]) {
                count++;
            }
        }
        return count;
    }
    
    private static boolean isGoalState(GameState state) {
        for (boolean color : state.faceColors) {
            if (!color) return false;
        }
        return true;
    }
    
    static class SearchNode {
        GameState state;
        List<Direction> path;
        
        SearchNode(GameState state, List<Direction> path) {
            this.state = state;
            this.path = path;
        }
    }
}

class DPSolver {


    private static Map<String, List<Direction>> dpMemo = new HashMap<>();


    private static Set<String> activeStates = new HashSet<>();

    //  CONSTANTS
    private static final int MAX_DEPTH = 40;
    private static final Direction[] ACTIONS = {
        Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT
    };

    public static List<Direction> solveDynamic(GameState initialState) {
        if (isGoalState(initialState)) return new ArrayList<>();

        for (int depthLimit = 1; depthLimit <= MAX_DEPTH; depthLimit++) {
            
            dpMemo = new HashMap<>();
            activeStates = new HashSet<>();

            List<Direction> result = dp(initialState, depthLimit);
            if (result != null) {
                return result; // Optimal solution found at this depth
            }
        }

        return new ArrayList<>(); // No solution within MAX_DEPTH
    }

    
  
    private static List<Direction> dp(GameState state, int d) {


        if (isGoalState(state)) return new ArrayList<>();

       
        if (d == 0) return null;

        String stateHash = state.getStateHash();
        String memoKey   = stateHash + "|" + d;

        // ── MEMOIZATION LOOKUP ─────────────────────────────────────────────

        if (dpMemo.containsKey(memoKey)) {
            return dpMemo.get(memoKey); // O(1) lookup — subproblem already solved
        }

        
        if (activeStates.contains(stateHash)) {
            return null;
        }

        // Mark state as active on the current recursion path
        activeStates.add(stateHash);


        List<Direction> bestSolution = null;

        for (Direction action : ACTIONS) {
            GameState nextState = state.copy();

            if (nextState.makeMove(action)) {
                // ── RECURSIVE SUB-PROBLEM ──────────────────────────────────

                List<Direction> subSolution = dp(nextState, d - 1);

                if (subSolution != null) {
                    int candidateCost = 1 + subSolution.size();
                    int bestCost      = bestSolution == null ? Integer.MAX_VALUE
                                                             : bestSolution.size();

                    // Update best if this action leads to a shorter solution
                    if (candidateCost < bestCost) {
                        bestSolution = new ArrayList<>();
                        bestSolution.add(action);       // add the current move 
                        bestSolution.addAll(subSolution); 
                    }
                }
            }
        }

        // Unmark: state is leaving the active recursion path
        activeStates.remove(stateHash);

        dpMemo.put(memoKey, bestSolution);

        return bestSolution;
    }

 
    public static Direction findDPHint(GameState state) {
        if (isGoalState(state)) return null;

        // Shallow IDDFS: find exact best first move up to depth 12
        for (int d = 1; d <= 12; d++) {
            dpMemo = new HashMap<>();
            activeStates = new HashSet<>();
            List<Direction> solution = dp(state, d);
            if (solution != null && !solution.isEmpty()) {
                return solution.get(0); // First move of optimal shallow solution
            }
        }


        Direction bestDir   = null;
        double    bestValue = Double.MAX_VALUE;

        for (Direction dir : ACTIONS) {
            GameState next = state.copy();
            if (next.makeMove(dir)) {
                double value = dpValueFunction(next);
                if (value < bestValue) {
                    bestValue = value;
                    bestDir   = dir;
                }
            }
        }

        return bestDir;
    }

    private static double dpValueFunction(GameState state) {
        if (isGoalState(state)) return 0.0;

        // Cost 1: non-blue faces (primary DP cost term — moves still needed)
        int nonBlueFaces = 0;
        for (boolean c : state.faceColors) if (!c) nonBlueFaces++;

        // Cost 2: BFS distance to nearest blue square on the grid
        int nearestDist  = bfsNearestBlue(state);
        double proxCost  = (nearestDist >= 0) ? nearestDist * 0.5 : 10.0;

        // Bonus: bottom face already blue → can immediately collect on next step
        int    bottomFace   = state.faceOrientation[4];
        double bottomBonus  = state.faceColors[bottomFace] ? -1.5 : 0.0;

        // Cost 3: blue squares remaining on grid (future collectibles)
        int remainingBlue = 0;
        for (boolean b : state.blueSquares) if (b) remainingBlue++;

        return nonBlueFaces * 3.0 + proxCost + bottomBonus + remainingBlue * 0.3;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /** BFS to find distance to nearest blue square on the grid. */
    private static int bfsNearestBlue(GameState state) {
        Queue<Integer>      queue   = new LinkedList<>();
        Set<Integer>        visited = new HashSet<>();
        Map<Integer,Integer> dist   = new HashMap<>();

        queue.offer(state.currentSquare);
        visited.add(state.currentSquare);
        dist.put(state.currentSquare, 0);

        while (!queue.isEmpty()) {
            int cur = queue.poll();
            int d   = dist.get(cur);

            if (state.blueSquares[cur] && cur != state.currentSquare) return d;

            GraphNode node = state.grid.getNode(cur);
            for (Direction dir : Direction.values()) {
                GraphNode nb = node.getNeighbor(dir);
                if (nb != null && !visited.contains(nb.id)) {
                    visited.add(nb.id);
                    dist.put(nb.id, d + 1);
                    queue.offer(nb.id);
                }
            }
        }
        return -1;
    }

    private static boolean isGoalState(GameState state) {
        for (boolean color : state.faceColors) if (!color) return false;
        return true;
    }
}

class BacktrackingSolver {
	
    private static final int MAX_DEPTH = 35;

    // ==================== AUTO SOLVE ====================

    public static List<Direction> solveBacktracking(GameState initialState) {
        if (isGoalState(initialState)) {
            return new ArrayList<>();
        }

        Direction[] moves = {
            Direction.UP,
            Direction.DOWN,
            Direction.LEFT,
            Direction.RIGHT
        };

        List<Direction> bestPath = null;

        for (Direction dir : moves) {
            GameState next = initialState.copy();

            if (next.makeMove(dir)) {
                Set<String> visited = new HashSet<>();
                List<Direction> path = new ArrayList<>();

                // Mark both initial state and first move state as visited
                visited.add(initialState.getStateHash());
                visited.add(next.getStateHash());

                // Include the first move in the path
                path.add(dir);

                if (backtrack(next, path, visited, 1)) {
                    if (bestPath == null || path.size() < bestPath.size()) {
                        bestPath = new ArrayList<>(path);
                    }
                }
            }
        }

        return bestPath != null ? bestPath : new ArrayList<>();
    }

    // ==================== HINT ====================

    public static Direction findBacktrackingHint(GameState state) {
        Direction[] moves = {
            Direction.UP,
            Direction.DOWN,
            Direction.LEFT,
            Direction.RIGHT
        };

        Direction bestDir = null;
        int bestLength = Integer.MAX_VALUE;

        for (Direction dir : moves) {
            GameState next = state.copy();

            if (next.makeMove(dir)) {
                Set<String> visited = new HashSet<>();
                List<Direction> path = new ArrayList<>();

                // Mark both current and next state to prevent going back
                visited.add(state.getStateHash());
                visited.add(next.getStateHash());
 
                if (backtrack(next, path, visited, 0)) {
                    int totalLength = path.size() + 1;

                    if (totalLength < bestLength) {
                        bestLength = totalLength;
                        bestDir = dir;
                    }
                }
            }
        }

        return bestDir;
    }

    // ==================== CORE BACKTRACKING ====================

    private static boolean backtrack(GameState state,
                                     List<Direction> path,
                                     Set<String> visited,
                                     int depth) {
        // Goal check
        if (isGoalState(state)) {
            return true;
        }
        // Constraint 1: depth limit — branch too long, prune
        if (depth >= MAX_DEPTH) {
            return false;
        }
        // Constraint 2: progress constraint — if remaining faces to collect
        // is more than remaining depth, impossible to solve, prune
        int remainingFaces = countRemainingFaces(state);
        if (remainingFaces > (MAX_DEPTH - depth)) {
            return false;
        }
        // Constraint 3: reachability constraint — if no blue squares exist
        // on the grid but faces still need filling, prune
        if (!hasReachableBlueSquare(state) && remainingFaces > 0) {
            return false;
        }
        Direction[] moves = {
            Direction.UP,
            Direction.DOWN,
            Direction.LEFT,
            Direction.RIGHT
        };

        for (Direction dir : moves) {
            GameState next = state.copy();
            if (next.makeMove(dir)) {
                String hash = next.getStateHash();
                if (!visited.contains(hash)) {
                    // Constraint 4: move usefulness — prune moves that make
                    // no progress toward the goal
                    if (!isUsefulMove(state, next)) {
                        continue;
                    }
                    // All constraints passed — CHOOSE
                    visited.add(hash);
                    path.add(dir);
                    // EXPLORE
                    if (backtrack(next, path, visited, depth + 1)) {
                        return true;
                    }
                    // BACKTRACK — undo
                    path.remove(path.size() - 1);
                    visited.remove(hash);
                }
            }
        }

        return false;
    }

    // ==================== CONSTRAINT HELPERS ====================

    // A move is useful if it collects a blue face OR moves closer to a blue square
    private static boolean isUsefulMove(GameState before, GameState after) {
        // Useful if we gained a blue face
        int facesBefore = countRemainingFaces(before);
        int facesAfter  = countRemainingFaces(after);
        if (facesAfter < facesBefore) {
            return true;
        }
        // Useful if we moved closer to or stayed same distance from blue square
        int distBefore = bfsNearestBlue(before);
        int distAfter  = bfsNearestBlue(after);
        // No blue squares left on grid at all — all collected into faces already
        if (distBefore == -1 && distAfter == -1) {
            return true;
        }
        // Blue squares disappeared after move — we collected one
        if (distAfter == -1) {
            return true;
        }
        // No blue squares before either — nothing to move toward
        if (distBefore == -1) {
            return false;
        }
        // Only keep move if we are getting closer or staying same distance
        return distAfter <= distBefore;
    }

    private static boolean hasReachableBlueSquare(GameState state) {
        for (boolean b : state.blueSquares) {
            if (b) return true;
        }
        return false;
    }

    private static int countRemainingFaces(GameState state) {
        int count = 0;
        for (boolean c : state.faceColors) {
            if (!c) count++;
        }
        return count;
    }

    private static int bfsNearestBlue(GameState state) {
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> dist = new HashMap<>();

        queue.offer(state.currentSquare);
        visited.add(state.currentSquare);
        dist.put(state.currentSquare, 0);

        while (!queue.isEmpty()) {
            int cur = queue.poll();
            int d = dist.get(cur);

            if (state.blueSquares[cur] && cur != state.currentSquare) {
                return d;
            }

            GraphNode node = state.grid.getNode(cur);
            for (Direction dir : Direction.values()) {
                GraphNode nb = node.getNeighbor(dir);
                if (nb != null && !visited.contains(nb.id)) {
                    visited.add(nb.id);
                    dist.put(nb.id, d + 1);
                    queue.offer(nb.id);
                }
            }
        }
        return -1;
    }

    // ==================== GOAL CHECK ====================

    private static boolean isGoalState(GameState state) {
        for (boolean color : state.faceColors) {
            if (!color) return false;
        }
        return true;
    }
}

// ============= GAME PANEL =============

class CubeGamePanel extends JPanel {
    GameState state;
    int gridScale = 90;
    int offsetX = 120;
    int offsetY = 100;
    Direction suggestedMove = null;
    boolean isAutoSolving = false;
    Timer autoSolveTimer;
    List<Direction> autoSolvePath;
    int autoSolveIndex;
    
    JPanel gameCanvas;
    private JLabel statusLabel;
    private JLabel movesLabel;
    private JLabel bottomLabel;
    
    public CubeGamePanel() {
        state = new GameState(SolidType.CUBE, 4, 4);
        
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 250));
        
        JPanel topPanel = createTopStatusPanel();
        add(topPanel, BorderLayout.NORTH);
        
        gameCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintGameContent(g);
            }
            
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(650, 550);
            }
            
            @Override
            public boolean isFocusable() {
                return true;
            }
        };
        gameCanvas.setBackground(new Color(245, 245, 250));
        gameCanvas.setFocusable(true);
        
        setupKeyListeners(gameCanvas);
        
        autoSolveTimer = new Timer(500, e -> {
            if (isAutoSolving && autoSolveIndex < autoSolvePath.size()) {
                Direction move = autoSolvePath.get(autoSolveIndex++);
                state.makeMove(move);
                updateStatusPanel();
                gameCanvas.repaint();
                
                if (autoSolveIndex >= autoSolvePath.size() || state.completed) {
                    stopAutoSolve();
                    if (state.completed) {
                        showCompletionDialog();
                    }
                }
            } else {
                stopAutoSolve();
            }
        });
        
        JPanel controlPanel = createControlPanel();
        
        add(gameCanvas, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);
        
        updateStatusPanel();
        
        SwingUtilities.invokeLater(() -> gameCanvas.requestFocusInWindow());
    }
    
    private JPanel createTopStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(new Color(70, 130, 180));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        panel.setPreferredSize(new Dimension(getWidth(), 80));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 15, 5, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel statusTitle = new JLabel("STATUS:");
        statusTitle.setFont(new Font("Arial", Font.BOLD, 14));
        statusTitle.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(statusTitle, gbc);
        
        statusLabel = new JLabel("Playing");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setForeground(new Color(255, 255, 150));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1;
        panel.add(statusLabel, gbc);
        
        JLabel movesTitle = new JLabel("MOVES:");
        movesTitle.setFont(new Font("Arial", Font.BOLD, 14));
        movesTitle.setForeground(Color.WHITE);
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(movesTitle, gbc);
        
        movesLabel = new JLabel("0");
        movesLabel.setFont(new Font("Arial", Font.BOLD, 16));
        movesLabel.setForeground(Color.WHITE);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1;
        panel.add(movesLabel, gbc);
        
        JLabel bottomTitle = new JLabel("BOTTOM FACE:");
        bottomTitle.setFont(new Font("Arial", Font.BOLD, 14));
        bottomTitle.setForeground(Color.WHITE);
        gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(bottomTitle, gbc);
        
        bottomLabel = new JLabel("Empty");
        bottomLabel.setFont(new Font("Arial", Font.BOLD, 16));
        bottomLabel.setForeground(Color.WHITE);
        gbc.gridx = 5; gbc.gridy = 0; gbc.weightx = 1;
        panel.add(bottomLabel, gbc);
        
        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setOpaque(false);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 6;
        gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(progressPanel, gbc);
        
        JLabel progressLabel = new JLabel("Progress: 0/6 faces blue");
        progressLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        progressLabel.setForeground(Color.WHITE);
        progressPanel.add(progressLabel, BorderLayout.NORTH);
        
        JProgressBar progressBar = new JProgressBar(0, 6);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(100, 200, 100));
        progressBar.setBackground(new Color(200, 200, 200));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        
        panel.putClientProperty("progressLabel", progressLabel);
        panel.putClientProperty("progressBar", progressBar);
        
        return panel;
    }
    
    private void updateStatusPanel() {
        if (state.completed) {
            statusLabel.setText("COMPLETED!");
            statusLabel.setForeground(new Color(100, 255, 100));
        } else {
            statusLabel.setText("Playing");
            statusLabel.setForeground(new Color(255, 255, 150));
        }
        
        movesLabel.setText(String.valueOf(state.moveCount));
        
        int bottomFace = state.faceOrientation[4];
        boolean bottomIsBlue = state.faceColors[bottomFace];
        bottomLabel.setText(bottomIsBlue ? "BLUE" : "EMPTY");
        bottomLabel.setForeground(bottomIsBlue ? new Color(150, 200, 255) : Color.WHITE);
        
        int blueFaces = 0;
        for (boolean color : state.faceColors) {
            if (color) blueFaces++;
        }
        
        JPanel topPanel = (JPanel) getComponent(0);
        JLabel progressLabel = (JLabel) topPanel.getClientProperty("progressLabel");
        JProgressBar progressBar = (JProgressBar) topPanel.getClientProperty("progressBar");
        
        if (progressLabel != null) {
            progressLabel.setText("Progress: " + blueFaces + "/6 faces blue");
        }
        if (progressBar != null) {
            progressBar.setValue(blueFaces);
        }
    }
    
    private void setupKeyListeners(JPanel canvas) {
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (state.completed || isAutoSolving) return;
                
                Direction dir = null;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:  dir = Direction.LEFT; break;
                    case KeyEvent.VK_RIGHT: dir = Direction.RIGHT; break;
                    case KeyEvent.VK_UP:    dir = Direction.UP; break;
                    case KeyEvent.VK_DOWN:  dir = Direction.DOWN; break;
                }
                
                if (dir != null && state.makeMove(dir)) {
                    suggestedMove = null;
                    updateStatusPanel();
                    gameCanvas.repaint();
                    if (state.completed) {
                        showCompletionDialog();
                    }
                }
            }
        });
        
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (state.completed || isAutoSolving) return;
                
                canvas.requestFocusInWindow();
                
                GraphNode current = state.grid.getNode(state.currentSquare);
                int cx = (int)(current.square.x * gridScale) + offsetX;
                int cy = (int)(current.square.y * gridScale) + offsetY;
                
                double angle = Math.atan2(e.getY() - cy, e.getX() - cx);
                Direction dir;
                
                if (Math.abs(angle) > 3*Math.PI/4) dir = Direction.LEFT;
                else if (Math.abs(angle) < Math.PI/4) dir = Direction.RIGHT;
                else if (angle > 0) dir = Direction.DOWN;
                else dir = Direction.UP;
                
                if (state.makeMove(dir)) {
                    suggestedMove = null;
                    updateStatusPanel();
                    gameCanvas.repaint();
                    if (state.completed) {
                        showCompletionDialog();
                    }
                }
            }
        });
    }
    
    private void paintGameContent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        for (GraphNode node : state.grid.getNodes()) {
            GridSquare sq = node.square;
            Path2D path = new Path2D.Float();
            
            for (int i = 0; i < sq.nPoints; i++) {
                float px = sq.points[i*2] * gridScale + offsetX;
                float py = sq.points[i*2+1] * gridScale + offsetY;
                if (i == 0) path.moveTo(px, py);
                else path.lineTo(px, py);
            }
            path.closePath();
            
            if (state.blueSquares[node.id]) {
                g2.setColor(new Color(100, 150, 255));
            } else {
                g2.setColor(Color.WHITE);
            }
            g2.fill(path);
            
            if (node.id == state.currentSquare) {
                g2.setColor(new Color(255, 240, 150, 120));
                g2.fill(path);
            }
            
            g2.setColor(new Color(100, 100, 100));
            g2.setStroke(new BasicStroke(node.id == state.currentSquare ? 3 : 2));
            g2.draw(path);
        }
        
        GraphNode current = state.grid.getNode(state.currentSquare);
        int cx = (int)(current.square.x * gridScale) + offsetX;
        int cy = (int)(current.square.y * gridScale) + offsetY;
        
        if (suggestedMove != null && !state.completed && !isAutoSolving) {
            drawHintArrow(g2, current.square, suggestedMove);
        }
        
        drawCube(g2, cx, cy, 40);
    }
    
    private void drawHintArrow(Graphics2D g2, GridSquare from, Direction dir) {
        int x1 = (int)(from.x * gridScale) + offsetX;
        int y1 = (int)(from.y * gridScale) + offsetY;
        int x2 = x1, y2 = y1;
        
        switch (dir) {
            case UP:    y2 -= gridScale; break;
            case DOWN:  y2 += gridScale; break;
            case LEFT:  x2 -= gridScale; break;
            case RIGHT: x2 += gridScale; break;
        }
        
        g2.setColor(new Color(0, 200, 0, 200));
        g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
        
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 20;
        int ax1 = x2 - (int)(arrowSize * Math.cos(angle - Math.PI/6));
        int ay1 = y2 - (int)(arrowSize * Math.sin(angle - Math.PI/6));
        int ax2 = x2 - (int)(arrowSize * Math.cos(angle + Math.PI/6));
        int ay2 = y2 - (int)(arrowSize * Math.sin(angle + Math.PI/6));
        
        g2.fillPolygon(new int[]{x2, ax1, ax2}, new int[]{y2, ay1, ay2}, 3);
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        String symbol = dir.getSymbol();
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(symbol);
        int textHeight = fm.getHeight();
        g2.drawString(symbol, x2 - textWidth/2, y2 + textHeight/4);
    }
    
    private void drawCube(Graphics2D g2, int cx, int cy, int size) {
        double angleY = Math.toRadians(-30);
        
        float[][] vertices = {
            {-0.5f, -0.5f, -0.5f}, {0.5f, -0.5f, -0.5f},
            {-0.5f,  0.5f, -0.5f}, {0.5f,  0.5f, -0.5f},
            {-0.5f, -0.5f,  0.5f}, {0.5f, -0.5f,  0.5f},
            {-0.5f,  0.5f,  0.5f}, {0.5f,  0.5f,  0.5f}
        };
        
        int[][] projected = new int[8][2];
        for (int i = 0; i < 8; i++) {
            float x = vertices[i][0] * size;
            float y = vertices[i][1] * size;
            float z = vertices[i][2] * size;
            projected[i][0] = cx + (int)(x * Math.cos(angleY) - z * Math.cos(angleY));
            projected[i][1] = cy + (int)(x * Math.sin(angleY) + y + z * Math.sin(angleY));
        }
        
        int[][] faceVertices = {
            {4, 5, 7, 6}, {1, 0, 2, 3}, {5, 1, 3, 7},
            {0, 4, 6, 2}, {0, 1, 5, 4}, {2, 6, 7, 3}
        };
        
        int[] drawOrder = {1, 3, 4, 2, 5, 0};
        
        for (int faceIdx : drawOrder) {
            int[] verts = faceVertices[faceIdx];
            int originalFace = state.faceOrientation[faceIdx];
            boolean isBlue = state.faceColors[originalFace];
            
            Polygon poly = new Polygon();
            for (int v : verts) {
                poly.addPoint(projected[v][0], projected[v][1]);
            }
            
            Color baseColor = isBlue ? new Color(70, 130, 255) : new Color(240, 240, 240);
            float shade = (faceIdx == 0) ? 1.0f : (faceIdx == 2) ? 0.8f : 
                         (faceIdx == 5) ? 0.9f : (faceIdx == 4) ? 0.6f : 0.7f;
            
            Color shadedColor = new Color(
                (int)(baseColor.getRed() * shade),
                (int)(baseColor.getGreen() * shade),
                (int)(baseColor.getBlue() * shade)
            );
            
            g2.setColor(shadedColor);
            g2.fill(poly);
            g2.setColor(new Color(50, 50, 50));
            g2.setStroke(new BasicStroke(2));
            g2.draw(poly);
        }
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(20, 15, 20, 15)
        ));
        panel.setPreferredSize(new Dimension(220, 0));
        
        JLabel titleLabel = new JLabel("Controls");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));

        // ---- Section: Hints ----
        JLabel hintSection = new JLabel("— Hints —");
        hintSection.setFont(new Font("Arial", Font.BOLD, 12));
        hintSection.setForeground(new Color(80, 80, 120));
        hintSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(hintSection);
        panel.add(Box.createVerticalStrut(6));

        // Greedy Hint
        JButton hintButton = createStyledButton("💡 Hint (Greedy)", new Color(100, 200, 100));
        hintButton.addActionListener(e -> {
            if (!state.completed && !isAutoSolving) {
                suggestedMove = AISolver.findGreedyMove(state);
                if (suggestedMove != null) {
                    gameCanvas.repaint();
                    JOptionPane.showMessageDialog(CubeGamePanel.this,
                        "Greedy Hint (Merge Sort): Move " + suggestedMove.name() + " (" + suggestedMove.getSymbol() + ")",
                        "Hint", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        panel.add(hintButton);
        panel.add(Box.createVerticalStrut(6));

        // D&C Hint
        JButton dcHintButton = createStyledButton("🎯 Hint (D&C)", new Color(100, 180, 100));
        dcHintButton.addActionListener(e -> {
            if (!state.completed && !isAutoSolving) {
                suggestedMove = DivideConquerSolver.findDivideConquerHint(state);
                if (suggestedMove != null) {
                    gameCanvas.repaint();
                    Set<Integer> firstHalf = new HashSet<>(Arrays.asList(0, 1, 2));
                    boolean firstDone = true;
                    for (int face : firstHalf) {
                        if (!state.faceColors[face]) { firstDone = false; break; }
                    }
                    String subproblem = firstDone ? "faces 3-5" : "faces 0-2";
                    JOptionPane.showMessageDialog(CubeGamePanel.this,
                        "D&C Hint (Merge Sort): Move " + suggestedMove.name() + " (" + suggestedMove.getSymbol() + ")\n" +
                        "Current subproblem: Coloring " + subproblem,
                        "D&C Hint", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        panel.add(dcHintButton);
        panel.add(Box.createVerticalStrut(6));

        // DP Hint
        JButton dpHintButton = createStyledButton("📐 Hint (DP)", new Color(60, 160, 220));
        dpHintButton.addActionListener(e -> {
            if (!state.completed && !isAutoSolving) {
                suggestedMove = DPSolver.findDPHint(state);
                if (suggestedMove != null) {
                    gameCanvas.repaint();
                    JOptionPane.showMessageDialog(CubeGamePanel.this,
                        "DP Hint (Value Function): Move " + suggestedMove.name() + " (" + suggestedMove.getSymbol() + ")\n" +
                        "Uses DP cost-to-go estimate to pick best move.",
                        "DP Hint", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        panel.add(dpHintButton);
        panel.add(Box.createVerticalStrut(6));

        // Backtracking Hint
        JButton btHintButton = createStyledButton("🔍 Hint (BT)", new Color(200, 100, 180));
        btHintButton.addActionListener(e -> {
            if (!state.completed && !isAutoSolving) {
                suggestedMove = BacktrackingSolver.findBacktrackingHint(state);
                if (suggestedMove != null) {
                    gameCanvas.repaint();
                    JOptionPane.showMessageDialog(CubeGamePanel.this,
                        "Backtracking Hint (IDDFS): Move " + suggestedMove.name() + " (" + suggestedMove.getSymbol() + ")\n" +
                        "Uses shallow backtracking search to find optimal next step.",
                        "Backtracking Hint", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        panel.add(btHintButton);
        panel.add(Box.createVerticalStrut(12));

        // ---- Section: Auto-Solve ----
        JLabel autoSection = new JLabel("— Auto-Solve —");
        autoSection.setFont(new Font("Arial", Font.BOLD, 12));
        autoSection.setForeground(new Color(80, 80, 120));
        autoSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(autoSection);
        panel.add(Box.createVerticalStrut(6));

        // A* Auto
        JButton autoSolveButton = createStyledButton("🤖 Auto (A*)", new Color(255, 140, 0));
        autoSolveButton.addActionListener(e -> {
            if (!state.completed && !isAutoSolving) {
                startAutoSolve("A*", AISolver.findSolution(state));
            }
        });
        panel.add(autoSolveButton);
        panel.add(Box.createVerticalStrut(6));

        // D&C Auto
        JButton dcAutoButton = createStyledButton("🔷 Auto (D&C)", new Color(255, 120, 0));
        dcAutoButton.addActionListener(e -> {
            if (!state.completed && !isAutoSolving) {
                startAutoSolveDC();
            }
        });
        panel.add(dcAutoButton);
        panel.add(Box.createVerticalStrut(6));

        // DP Auto
        JButton dpAutoButton = createStyledButton("📊 Auto (DP)", new Color(40, 140, 200));
        dpAutoButton.addActionListener(e -> {
            if (!state.completed && !isAutoSolving) {
                startAutoSolveWithWorker("Dynamic Programming", () -> DPSolver.solveDynamic(state));
            }
        });
        panel.add(dpAutoButton);
        panel.add(Box.createVerticalStrut(6));

        // Backtracking Auto
        JButton btAutoButton = createStyledButton("🔄 Auto (BT)", new Color(180, 80, 160));
        btAutoButton.addActionListener(e -> {
            if (!state.completed && !isAutoSolving) {
                startAutoSolveWithWorker("Backtracking (IDDFS)", () -> BacktrackingSolver.solveBacktracking(state));
            }
        });
        panel.add(btAutoButton);
        panel.add(Box.createVerticalStrut(12));

        // ---- Section: Solutions ----
        JLabel solutionSection = new JLabel("— Solutions —");
        solutionSection.setFont(new Font("Arial", Font.BOLD, 12));
        solutionSection.setForeground(new Color(80, 80, 120));
        solutionSection.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(solutionSection);
        panel.add(Box.createVerticalStrut(6));

        // A* Solution
        JButton showSolutionButton = createStyledButton("📋 Solution (A*)", new Color(100, 150, 255));
        showSolutionButton.addActionListener(e -> {
            if (!state.completed) {
                showSolutionDialog("A*", AISolver.findSolution(state));
            }
        });
        panel.add(showSolutionButton);
        panel.add(Box.createVerticalStrut(6));

        // D&C Solution
        JButton dcSolutionButton = createStyledButton("📊 Solution (D&C)", new Color(100, 130, 255));
        dcSolutionButton.addActionListener(e -> {
            if (!state.completed) {
                showSolutionDialogDC();
            }
        });
        panel.add(dcSolutionButton);
        panel.add(Box.createVerticalStrut(6));

        // DP Solution
        JButton dpSolutionButton = createStyledButton("📐 Solution (DP)", new Color(40, 120, 180));
        dpSolutionButton.addActionListener(e -> {
            if (!state.completed) {
                showSolutionDialogWithWorker("Dynamic Programming (Memoization)", () -> DPSolver.solveDynamic(state));
            }
        });
        panel.add(dpSolutionButton);
        panel.add(Box.createVerticalStrut(6));

        // Backtracking Solution
        JButton btSolutionButton = createStyledButton("🔍 Solution (BT)", new Color(160, 60, 140));
        btSolutionButton.addActionListener(e -> {
            if (!state.completed) {
                showSolutionDialogWithWorker("Backtracking (IDDFS + Pruning)", () -> BacktrackingSolver.solveBacktracking(state));
            }
        });
        panel.add(btSolutionButton);
        panel.add(Box.createVerticalStrut(15));

        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(190, 1));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(10));

        // New Game
        JButton newGameButton = createStyledButton("🔄 New Game", new Color(150, 100, 200));
        newGameButton.addActionListener(e -> {
            stopAutoSolve();
            state = new GameState(SolidType.CUBE, 4, 4);
            suggestedMove = null;
            updateStatusPanel();
            gameCanvas.repaint();
            gameCanvas.requestFocusInWindow();
        });
        panel.add(newGameButton);
        panel.add(Box.createVerticalGlue());

        JTextArea instructions = new JTextArea(
            "Arrow Keys or Click\nto Move Cube\n\n6 Algorithms:\n" +
            "• Greedy (MergeSort)\n• A* Search\n• D&C\n• DP (Memoization)\n• Backtracking\n  (IDDFS+Pruning)"
        );
        instructions.setFont(new Font("Arial", Font.ITALIC, 10));
        instructions.setForeground(new Color(100, 100, 100));
        instructions.setBackground(panel.getBackground());
        instructions.setEditable(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        instructions.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(instructions);
        
        return panel;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(190, 36));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) button.setBackground(bgColor.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        button.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> gameCanvas.requestFocusInWindow());
        });
        
        return button;
    }
    
    private void startAutoSolve(String algorithmName, List<Direction> solution) {
        if (solution.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No solution found or puzzle already solved!",
                algorithmName + " Auto-Solve", JOptionPane.INFORMATION_MESSAGE);
        } else {
            autoSolvePath = solution;
            autoSolveIndex = 0;
            isAutoSolving = true;
            autoSolveTimer.start();
        }
    }

    /**
     * Generic async auto-solve using a background worker.
     * Used for DP and Backtracking (potentially slow).
     */
    private void startAutoSolveWithWorker(String algorithmName, Supplier<List<Direction>> solver) {
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                                            "Computing...", true);
        JLabel progressLabel = new JLabel("Computing " + algorithmName + " solution...", JLabel.CENTER);
        progressLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        progressLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressDialog.add(progressLabel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);

        SwingWorker<List<Direction>, Void> worker = new SwingWorker<List<Direction>, Void>() {
            @Override
            protected List<Direction> doInBackground() {
                return solver.get();
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    List<Direction> solution = get();
                    if (solution.isEmpty()) {
                        JOptionPane.showMessageDialog(CubeGamePanel.this,
                            "Puzzle already solved or no solution found within limits!",
                            algorithmName + " Auto-Solve", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        autoSolvePath = solution;
                        autoSolveIndex = 0;
                        isAutoSolving = true;
                        autoSolveTimer.start();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CubeGamePanel.this,
                        "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }

    private void startAutoSolveDC() {
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                            "Computing...", true);
        JLabel progressLabel = new JLabel("Computing Divide & Conquer solution...", JLabel.CENTER);
        progressLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        progressLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressDialog.add(progressLabel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);
        
        SwingWorker<List<Direction>, Void> worker = new SwingWorker<List<Direction>, Void>() {
            @Override
            protected List<Direction> doInBackground() {
                return DivideConquerSolver.solveDivideAndConquer(state);
            }
         
            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    List<Direction> solution = get();
                    if (solution.isEmpty()) {
                        JOptionPane.showMessageDialog(CubeGamePanel.this,
                            "Puzzle already solved!", "D&C Auto-Solve", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        autoSolvePath = solution;
                        autoSolveIndex = 0;
                        isAutoSolving = true;
                        autoSolveTimer.start();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CubeGamePanel.this,
                        "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void stopAutoSolve() {
        isAutoSolving = false;
        autoSolveTimer.stop();
    }

    private void showSolutionDialogWithWorker(String algorithmName, Supplier<List<Direction>> solver) {
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                                            "Computing...", true);
        JLabel progressLabel = new JLabel("Computing " + algorithmName + " solution...", JLabel.CENTER);
        progressLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        progressLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressDialog.add(progressLabel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);

        SwingWorker<List<Direction>, Void> worker = new SwingWorker<List<Direction>, Void>() {
            @Override
            protected List<Direction> doInBackground() {
                return solver.get();
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    List<Direction> solution = get();
                    showSolutionDialog(algorithmName, solution);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CubeGamePanel.this,
                        "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void showSolutionDialogDC() {
        JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                            "Computing...", true);
        JLabel progressLabel = new JLabel("Computing D&C solution...", JLabel.CENTER);
        progressLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        progressLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressDialog.add(progressLabel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);
        
        SwingWorker<List<Direction>, Void> worker = new SwingWorker<List<Direction>, Void>() {
            @Override
            protected List<Direction> doInBackground() {
                return DivideConquerSolver.solveDivideAndConquer(state);
            }
            
            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    List<Direction> solution = get();
                    showSolutionDialog("Divide & Conquer (Subproblems: Faces 0-2, then 3-5)", solution);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CubeGamePanel.this,
                        "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
        progressDialog.setVisible(true);
    }
    
    private void showSolutionDialog(String algorithmName, List<Direction> solution) {
        StringBuilder text = new StringBuilder();
        text.append(algorithmName).append(" Solution\n");
        text.append("═════════════════════════════════════════════════\n\n");
        
        if (solution.isEmpty()) {
            text.append("Puzzle is already solved or no solution found.\n");
        } else {
            text.append("Current moves: ").append(state.moveCount).append("\n");
            text.append("Moves to solve: ").append(solution.size()).append("\n");
            text.append("Total moves: ").append(state.moveCount + solution.size()).append("\n\n");
            text.append("Step-by-step:\n");
            text.append("───────────────────\n");
            
            for (int i = 0; i < solution.size(); i++) {
                Direction dir = solution.get(i);
                text.append(String.format("%2d. %s  %s\n", (i + 1), dir.getSymbol(), dir.name()));
            }
            text.append("\nAlgorithm: ").append(algorithmName);
        }
        
        JTextArea textArea = new JTextArea(text.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 450));
        
        JOptionPane.showMessageDialog(this, scrollPane, 
            algorithmName + " Solution", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showCompletionDialog() {
        int result = JOptionPane.showOptionDialog(this,
            "🎉 CONGRATULATIONS! 🎉\n\n" +
            "You solved the puzzle in " + state.moveCount + " moves!",
            "Puzzle Completed!",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new String[]{"Play Again", "Close"},
            "Play Again");
        
        if (result == 0) {
            stopAutoSolve();
            state = new GameState(SolidType.CUBE, 4, 4);
            suggestedMove = null;
            updateStatusPanel();
            gameCanvas.repaint();
            gameCanvas.requestFocusInWindow();
        }
    }
}

// ============= MAIN GAME FRAME =============

class Game extends JFrame {
    public Game() {
        setTitle("Cube Puzzle Game - Greedy | A* | D&C | DP | Backtracking");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CubeGamePanel gamePanel = new CubeGamePanel();
        add(gamePanel);

        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");

        JMenuItem newGame = new JMenuItem("New Game");
        newGame.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        newGame.addActionListener(e -> {
            Container contentPane = getContentPane();
            contentPane.removeAll();
            CubeGamePanel newPanel = new CubeGamePanel();
            contentPane.add(newPanel);
            contentPane.revalidate();
            contentPane.repaint();
            newPanel.gameCanvas.requestFocusInWindow();
        });
        gameMenu.add(newGame);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                "🎮 Cube Puzzle Game 🎮\n\n" +
                "GOAL: Make all 6 faces of the cube blue!\n\n" +
                "ALGORITHMS IMPLEMENTED:\n\n" +
                "1. GREEDY (Merge Sort)\n" +
                "   Uses merge-sorted move scores for best-first selection.\n\n" +
                "2. A* SEARCH\n" +
                "   Heuristic-guided optimal pathfinding.\n\n" +
                "3. DIVIDE & CONQUER\n" +
                "   Splits into subproblems: faces {0,1,2} then {3,4,5}.\n\n" +
                "4. DYNAMIC PROGRAMMING (Memoization)\n" +
                "   BFS with parent tracking + DP value function for hints.\n" +
                "   Optimal substructure: dp[s] = 1 + min(dp[s']).\n\n" +
                "5. BACKTRACKING (IDDFS + Pruning)\n" +
                "   Iterative deepening DFS with admissible lower bound pruning,\n" +
                "   cycle detection, and heuristic move ordering.\n\n" +
                "CONTROLS:\n" +
                "• Arrow keys or click to roll cube\n" +
                "• Try all algorithms and compare results!\n\n" +
                "Good luck! 🍀",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
        });
        gameMenu.add(aboutItem);

        menuBar.add(gameMenu);
        setJMenuBar(menuBar);

        pack();
        setSize(1050, 750);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Game game = new Game();
            game.setVisible(true);
        });
    }
}

