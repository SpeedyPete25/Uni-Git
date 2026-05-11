public class UndirectedGraph extends Graph {
    public UndirectedGraph(int vertices) {
        super(vertices);
    }

    @Override
    public void addEdge(int from, int to) {
        if (!isValidVertex(from) || !isValidVertex(to) || from == to) {
            return;
        }
        adjacencyMatrix[from][to] = true;
        adjacencyMatrix[to][from] = true;
    }

    @Override
    public void removeEdge(int from, int to) {
        if (!isValidVertex(from) || !isValidVertex(to)) {
            return;
        }
        adjacencyMatrix[from][to] = false;
        adjacencyMatrix[to][from] = false;
    }
}
