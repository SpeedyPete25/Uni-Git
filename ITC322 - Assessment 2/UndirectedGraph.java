/**
 * Undirected graph that mirrors each edge in both directions.
 */
public class UndirectedGraph extends Graph {
    /**
     * Creates an undirected graph with the requested number of vertices.
     *
     * @param vertices number of vertices
     */
    public UndirectedGraph(int vertices) {
        super(vertices);
    }

    /**
     * Adds an undirected edge by storing both (from, to) and (to, from).
     *
     * @param from first vertex
     * @param to second vertex
     */
    @Override
    public void addEdge(int from, int to) {
        if (!isValidVertex(from) || !isValidVertex(to) || from == to) {
            return;
        }
        super.addEdge(from, to);
        super.addEdge(to, from);
    }

    /**
     * Removes an undirected edge by clearing both directions.
     *
     * @param from first vertex
     * @param to second vertex
     */
    @Override
    public void removeEdge(int from, int to) {
        if (!isValidVertex(from) || !isValidVertex(to)) {
            return;
        }
        super.removeEdge(from, to);
        super.removeEdge(to, from);
    }

    /**
     * Checks whether a vertex id is in range for this graph.
     */
    private boolean isValidVertex(int vertex) {
        return vertex >= 0 && vertex < size();
    }
}
