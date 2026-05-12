import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Core social network service using an undirected adjacency matrix graph.
 */
public class SocialNetwork {
    // Graph stores friendships in an adjacency matrix.
    private UndirectedGraph graph;
    // names[i] is the name label for vertex i.
    private String[] names;
    // active[i] tracks whether the member is still in the network.
    private boolean[] active;
    // Case-insensitive lookup from name to index.
    private final Map<String, Integer> nameToIndex;

    /**
     * Creates an empty social network.
     */
    public SocialNetwork() {
        this.graph = new UndirectedGraph(0);
        this.names = new String[0];
        this.active = new boolean[0];
        this.nameToIndex = new HashMap<>();
    }

    /**
     * Loads a new social network from index and friend files.
     *
     * @param indexFilename index file name
     * @param friendFilename friend file name
     * @return true if both files are loaded successfully, otherwise false
     */
    public boolean loadNetwork(String indexFilename, String friendFilename) {
        try {
            LoadedIndex loadedIndex = readIndexFile(indexFilename);
            UndirectedGraph loadedGraph = readFriendFile(friendFilename, loadedIndex.names.length);
            this.names = loadedIndex.names;
            this.active = new boolean[names.length];
            Arrays.fill(this.active, true);
            this.graph = loadedGraph;
            rebuildNameLookup();
            return true;
        } catch (FileNotFoundException e) {
            System.err.println("Could not find file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Could not read network files: " + e.getMessage());
        }
        clearNetwork();
        return false;
    }

    /**
     * Checks if there are any active members.
     *
     * @return true if no active members exist
     */
    public boolean isEmpty() {
        return countActiveMembers() == 0;
    }

    /**
     * Returns all direct friends of the given member.
     *
     * @param name member name
     * @return sorted friend names, or null if name does not exist
     */
    public List<String> getFriends(String name) {
        Integer index = findIndexByName(name);
        if (index == null) {
            return null;
        }
        List<String> result = collectFriendsForIndex(index);
        sortCaseInsensitive(result);
        return result;
    }

    /**
     * Returns unique direct friends and friends-of-friends for the member.
     *
     * @param name member name
     * @return sorted names, excluding the original member, or null if missing
     */
    public List<String> getFriendsAndFriendsOfFriends(String name) {
        Integer index = findIndexByName(name);
        if (index == null) {
            return null;
        }
        // Use a boolean selection map so duplicate names are automatically removed.
        boolean[] selected = new boolean[names.length];
        selected[index] = true;
        for (int i = 0; i < names.length; i++) {
            if (!isEligibleMember(i)) {
                continue;
            }
            if (graph.hasEdge(index, i)) {
                selected[i] = true;
                markFriends(i, selected);
            }
        }
        return toSortedListExcluding(index, selected);
    }

    /**
     * Returns common friends shared by two members.
     *
     * @param firstName first member
     * @param secondName second member
     * @return sorted common friends, or null if either name does not exist
     */
    public List<String> getCommonFriends(String firstName, String secondName) {
        Integer firstIndex = findIndexByName(firstName);
        Integer secondIndex = findIndexByName(secondName);
        if (firstIndex == null || secondIndex == null) {
            return null;
        }
        List<String> common = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            if (!isEligibleMember(i)) {
                continue;
            }
            boolean firstKnows = graph.hasEdge(firstIndex, i);
            boolean secondKnows = graph.hasEdge(secondIndex, i);
            if (firstKnows && secondKnows) {
                common.add(names[i]);
            }
        }
        sortCaseInsensitive(common);
        return common;
    }

    /**
     * Deletes a member after a confirmed request.
     *
     * @param name member to delete
     * @param confirmed true if user confirmed deletion
     * @return deletion result status
     */
    public DeleteResult deleteMember(String name, boolean confirmed) {
        Integer index = findIndexByName(name);
        if (index == null) {
            return DeleteResult.NAME_NOT_FOUND;
        }
        if (!confirmed) {
            return DeleteResult.CANCELLED;
        }
        removeAllEdges(index);
        active[index] = false;
        nameToIndex.remove(normalize(name));
        return DeleteResult.DELETED;
    }

    /**
     * Lists active members sorted by popularity and then by name.
     *
     * @return sorted popularity report
     */
    public List<MemberPopularity> listMembersByPopularity() {
        List<MemberPopularity> report = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            if (isEligibleMember(i)) {
                report.add(new MemberPopularity(names[i], countFriends(i)));
            }
        }
        // Popularity is primary key (descending), then name A-Z for ties.
        report.sort(Comparator
                .comparingInt(MemberPopularity::friendCount)
                .reversed()
                .thenComparing(MemberPopularity::name, String.CASE_INSENSITIVE_ORDER));
        return report;
    }

    /**
     * Checks if a member exists in the active network.
     *
     * @param name member name
     * @return true if the member exists
     */
    public boolean memberExists(String name) {
        return findIndexByName(name) != null;
    }

    /**
     * Reads and validates the index file.
     */
    private LoadedIndex readIndexFile(String filename) throws FileNotFoundException {
        try (Scanner scanner = new Scanner(new File(filename))) {
            int people = readNonNegativeInt(scanner, "index file is missing the number of people");
            String[] loadedNames = new String[people];
            Map<String, Integer> duplicateCheck = new HashMap<>();
            readIndexEntries(scanner, loadedNames, duplicateCheck);
            validateAllIndexEntriesExist(loadedNames);
            return new LoadedIndex(loadedNames);
        }
    }

    /**
     * Reads and validates the friend file.
     */
    private UndirectedGraph readFriendFile(String filename, int people) throws FileNotFoundException {
        try (Scanner scanner = new Scanner(new File(filename))) {
            UndirectedGraph loadedGraph = new UndirectedGraph(people);
            int pairs = readNonNegativeInt(scanner, "friend file is missing the number of friend pairs");
            for (int i = 0; i < pairs; i++) {
                int first = readVertex(scanner, people, "friend file has an invalid friend index");
                int second = readVertex(scanner, people, "friend file has an invalid friend index");
                loadedGraph.addEdge(first, second);
            }
            return loadedGraph;
        }
    }

    /**
     * Reads index rows from the scanner and validates each row.
     */
    private void readIndexEntries(Scanner scanner, String[] loadedNames,
                                  Map<String, Integer> duplicateCheck) {
        for (int i = 0; i < loadedNames.length; i++) {
            int fileIndex = readIndexEntryPosition(scanner, loadedNames.length);
            String name = readIndexEntryName(scanner);
            validateAndStoreIndexEntry(loadedNames, duplicateCheck, fileIndex, name);
        }
    }

    /**
     * Reads a person index in the index file.
     */
    private int readIndexEntryPosition(Scanner scanner, int people) {
        if (!scanner.hasNextInt()) {
            throw new IllegalArgumentException("index file has an invalid person index");
        }
        int fileIndex = scanner.nextInt();
        if (fileIndex < 0 || fileIndex >= people) {
            throw new IllegalArgumentException("person index out of range: " + fileIndex);
        }
        return fileIndex;
    }

    /**
     * Reads a person name in the index file.
     */
    private String readIndexEntryName(Scanner scanner) {
        if (!scanner.hasNext()) {
            throw new IllegalArgumentException("index file is missing a name");
        }
        return scanner.next();
    }

    /**
     * Stores a validated index entry while enforcing case-insensitive uniqueness.
     */
    private void validateAndStoreIndexEntry(String[] loadedNames,
                                            Map<String, Integer> duplicateCheck,
                                            int fileIndex,
                                            String name) {
        String normalized = normalize(name);
        if (duplicateCheck.containsKey(normalized)) {
            throw new IllegalArgumentException("duplicate name found in index file: " + name);
        }
        loadedNames[fileIndex] = name;
        duplicateCheck.put(normalized, fileIndex);
    }

    /**
     * Ensures each index in the name array has a corresponding entry.
     */
    private void validateAllIndexEntriesExist(String[] loadedNames) {
        for (int i = 0; i < loadedNames.length; i++) {
            if (loadedNames[i] == null) {
                throw new IllegalArgumentException("index file is missing person entry for index " + i);
            }
        }
    }

    /**
     * Reads a non-negative integer or throws a clear file-format exception.
     */
    private int readNonNegativeInt(Scanner scanner, String errorMessage) {
        if (!scanner.hasNextInt()) {
            throw new IllegalArgumentException(errorMessage);
        }
        int value = scanner.nextInt();
        if (value < 0) {
            throw new IllegalArgumentException("negative values are not allowed in input files");
        }
        return value;
    }

    /**
     * Reads and validates a vertex id in friend file input.
     */
    private int readVertex(Scanner scanner, int people, String errorMessage) {
        if (!scanner.hasNextInt()) {
            throw new IllegalArgumentException(errorMessage);
        }
        int value = scanner.nextInt();
        if (value < 0 || value >= people) {
            throw new IllegalArgumentException("friend index out of range: " + value);
        }
        return value;
    }

    /**
     * Rebuilds case-insensitive lookup from member name to vertex index.
     */
    private void rebuildNameLookup() {
        nameToIndex.clear();
        for (int i = 0; i < names.length; i++) {
            if (active[i]) {
                nameToIndex.put(normalize(names[i]), i);
            }
        }
    }

    /**
     * Finds a member index by name using case-insensitive lookup.
     */
    private Integer findIndexByName(String name) {
        if (name == null) {
            return null;
        }
        return nameToIndex.get(normalize(name));
    }

    /**
     * Collects direct friend names for a member index.
     */
    private List<String> collectFriendsForIndex(int index) {
        List<String> friends = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            if (isEligibleMember(i) && graph.hasEdge(index, i)) {
                friends.add(names[i]);
            }
        }
        return friends;
    }

    /**
     * Marks all direct friends of the source member in the selection map.
     */
    private void markFriends(int source, boolean[] selected) {
        for (int i = 0; i < names.length; i++) {
            if (isEligibleMember(i) && graph.hasEdge(source, i)) {
                selected[i] = true;
            }
        }
    }

    /**
     * Converts selected indices to sorted names while excluding one index.
     */
    private List<String> toSortedListExcluding(int excludedIndex, boolean[] selected) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < selected.length; i++) {
            if (selected[i] && i != excludedIndex && isEligibleMember(i)) {
                result.add(names[i]);
            }
        }
        sortCaseInsensitive(result);
        return result;
    }

    /**
     * Removes all edges connected to a member index.
     */
    private void removeAllEdges(int index) {
        for (int i = 0; i < names.length; i++) {
            graph.removeEdge(index, i);
        }
    }

    /**
     * Counts direct friends for a member index.
     */
    private int countFriends(int index) {
        int total = 0;
        for (int i = 0; i < names.length; i++) {
            if (isEligibleMember(i) && graph.hasEdge(index, i)) {
                total++;
            }
        }
        return total;
    }

    /**
     * Counts active members in the network.
     */
    private int countActiveMembers() {
        int total = 0;
        for (boolean memberActive : active) {
            if (memberActive) {
                total++;
            }
        }
        return total;
    }

    /**
     * Checks if an index points to an active member.
     */
    private boolean isEligibleMember(int index) {
        return index >= 0 && index < active.length && active[index];
    }

    /**
     * Normalizes names to lower case for case-insensitive matching.
     */
    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * Sorts a list of names using case-insensitive ordering.
     */
    private void sortCaseInsensitive(List<String> namesToSort) {
        Collections.sort(namesToSort, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Clears all loaded network data.
     */
    private void clearNetwork() {
        this.graph = new UndirectedGraph(0);
        this.names = new String[0];
        this.active = new boolean[0];
        this.nameToIndex.clear();
    }

    /**
     * Lightweight holder for names loaded from index file.
     */
    private record LoadedIndex(String[] names) {
    }

    /**
     * Report item for member popularity output.
     */
    public record MemberPopularity(String name, int friendCount) {
    }

    /**
     * Outcomes of delete member requests.
     */
    public enum DeleteResult {
        DELETED,
        CANCELLED,
        NAME_NOT_FOUND
    }
}
