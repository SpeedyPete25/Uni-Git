public class Graph {
    protected final boolean[][] adjacencyMatrix;

    public Graph(int vertices) {
        this.adjacencyMatrix = new boolean[Math.max(vertices, 0)][Math.max(vertices, 0)];
    }

    public int size() {
        return adjacencyMatrix.length;
    }

    public void addEdge(int from, int to) {
        if (isValidVertex(from) && isValidVertex(to) && from != to) {
            adjacencyMatrix[from][to] = true;
        }
    }

    public void removeEdge(int from, int to) {
        if (isValidVertex(from) && isValidVertex(to)) {
            adjacencyMatrix[from][to] = false;
        }
    }

    public boolean hasEdge(int from, int to) {
        if (!isValidVertex(from) || !isValidVertex(to)) {
            return false;
        }
        return adjacencyMatrix[from][to];
    }

    protected boolean isValidVertex(int vertex) {
        return vertex >= 0 && vertex < size();
    }
}
