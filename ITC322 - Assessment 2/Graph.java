/**
 * Directed graph backed by an adjacency matrix.
 */
public class Graph {
    protected final boolean[][] adjacencyMatrix;

    /**
     * Creates a graph with the requested number of vertices.
     *
     * @param vertices number of vertices
     */
    public Graph(int vertices) {
        this.adjacencyMatrix = new boolean[Math.max(vertices, 0)][Math.max(vertices, 0)];
    }

    /**
     * Returns the number of vertices in the graph.
     *
     * @return total vertices
     */
    public int size() {
        return adjacencyMatrix.length;
    }

    /**
     * Adds a directed edge from one vertex to another.
     *
     * @param from source vertex
     * @param to destination vertex
     */
    public void addEdge(int from, int to) {
        if (isValidVertex(from) && isValidVertex(to) && from != to) {
            adjacencyMatrix[from][to] = true;
        }
    }

    /**
     * Removes a directed edge from one vertex to another.
     *
     * @param from source vertex
     * @param to destination vertex
     */
    public void removeEdge(int from, int to) {
        if (isValidVertex(from) && isValidVertex(to)) {
            adjacencyMatrix[from][to] = false;
        }
    }

    /**
     * Checks whether a directed edge exists.
     *
     * @param from source vertex
     * @param to destination vertex
     * @return true if an edge exists; otherwise false
     */
    public boolean hasEdge(int from, int to) {
        if (!isValidVertex(from) || !isValidVertex(to)) {
            return false;
        }
        return adjacencyMatrix[from][to];
    }

    /**
     * Checks whether a vertex index is valid for this graph.
     *
     * @param vertex vertex index
     * @return true if valid; otherwise false
     */
    protected boolean isValidVertex(int vertex) {
        return vertex >= 0 && vertex < size();
    }
}
