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

public class SocialNetwork {
    private UndirectedGraph graph;
    private String[] names;
    private boolean[] active;
    private final Map<String, Integer> nameToIndex;

    public SocialNetwork() {
        this.graph = new UndirectedGraph(0);
        this.names = new String[0];
        this.active = new boolean[0];
        this.nameToIndex = new HashMap<>();
    }

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

    public boolean isEmpty() {
        return countActiveMembers() == 0;
    }

    public List<String> getFriends(String name) {
        Integer index = findIndexByName(name);
        if (index == null) {
            return null;
        }
        List<String> result = collectFriendsForIndex(index);
        sortCaseInsensitive(result);
        return result;
    }

    public List<String> getFriendsAndFriendsOfFriends(String name) {
        Integer index = findIndexByName(name);
        if (index == null) {
            return null;
        }
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

    public List<MemberPopularity> listMembersByPopularity() {
        List<MemberPopularity> report = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            if (isEligibleMember(i)) {
                report.add(new MemberPopularity(names[i], countFriends(i)));
            }
        }
        report.sort(Comparator
                .comparingInt(MemberPopularity::friendCount)
                .reversed()
                .thenComparing(MemberPopularity::name, String.CASE_INSENSITIVE_ORDER));
        return report;
    }

    public boolean memberExists(String name) {
        return findIndexByName(name) != null;
    }

    private LoadedIndex readIndexFile(String filename) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(filename));
        int people = readNonNegativeInt(scanner, "index file is missing the number of people");
        String[] loadedNames = new String[people];
        Map<String, Integer> duplicateCheck = new HashMap<>();

        for (int i = 0; i < people; i++) {
            if (!scanner.hasNextInt()) {
                scanner.close();
                throw new IllegalArgumentException("index file has an invalid person index");
            }
            int fileIndex = scanner.nextInt();
            if (!scanner.hasNext()) {
                scanner.close();
                throw new IllegalArgumentException("index file is missing a name");
            }
            String name = scanner.next();
            if (fileIndex < 0 || fileIndex >= people) {
                scanner.close();
                throw new IllegalArgumentException("person index out of range: " + fileIndex);
            }
            String normalized = normalize(name);
            if (duplicateCheck.containsKey(normalized)) {
                scanner.close();
                throw new IllegalArgumentException("duplicate name found in index file: " + name);
            }
            loadedNames[fileIndex] = name;
            duplicateCheck.put(normalized, fileIndex);
        }
        scanner.close();
        for (int i = 0; i < loadedNames.length; i++) {
            if (loadedNames[i] == null) {
                throw new IllegalArgumentException("index file is missing person entry for index " + i);
            }
        }
        return new LoadedIndex(loadedNames);
    }

    private UndirectedGraph readFriendFile(String filename, int people) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(filename));
        UndirectedGraph loadedGraph = new UndirectedGraph(people);
        int pairs = readNonNegativeInt(scanner, "friend file is missing the number of friend pairs");

        for (int i = 0; i < pairs; i++) {
            int first = readVertex(scanner, people, "friend file has an invalid friend index");
            int second = readVertex(scanner, people, "friend file has an invalid friend index");
            loadedGraph.addEdge(first, second);
        }
        scanner.close();
        return loadedGraph;
    }

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

    private void rebuildNameLookup() {
        nameToIndex.clear();
        for (int i = 0; i < names.length; i++) {
            if (active[i]) {
                nameToIndex.put(normalize(names[i]), i);
            }
        }
    }

    private Integer findIndexByName(String name) {
        if (name == null) {
            return null;
        }
        return nameToIndex.get(normalize(name));
    }

    private List<String> collectFriendsForIndex(int index) {
        List<String> friends = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            if (isEligibleMember(i) && graph.hasEdge(index, i)) {
                friends.add(names[i]);
            }
        }
        return friends;
    }

    private void markFriends(int source, boolean[] selected) {
        for (int i = 0; i < names.length; i++) {
            if (isEligibleMember(i) && graph.hasEdge(source, i)) {
                selected[i] = true;
            }
        }
    }

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

    private void removeAllEdges(int index) {
        for (int i = 0; i < names.length; i++) {
            graph.removeEdge(index, i);
        }
    }

    private int countFriends(int index) {
        int total = 0;
        for (int i = 0; i < names.length; i++) {
            if (isEligibleMember(i) && graph.hasEdge(index, i)) {
                total++;
            }
        }
        return total;
    }

    private int countActiveMembers() {
        int total = 0;
        for (boolean memberActive : active) {
            if (memberActive) {
                total++;
            }
        }
        return total;
    }

    private boolean isEligibleMember(int index) {
        return index >= 0 && index < active.length && active[index];
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private void sortCaseInsensitive(List<String> namesToSort) {
        Collections.sort(namesToSort, String.CASE_INSENSITIVE_ORDER);
    }

    private void clearNetwork() {
        this.graph = new UndirectedGraph(0);
        this.names = new String[0];
        this.active = new boolean[0];
        this.nameToIndex.clear();
    }

    private record LoadedIndex(String[] names) {
    }

    public record MemberPopularity(String name, int friendCount) {
    }

    public enum DeleteResult {
        DELETED,
        CANCELLED,
        NAME_NOT_FOUND
    }
}
