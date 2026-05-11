import java.util.List;
import java.util.Scanner;

public class SocialNetworkDriver {
    private static final String DEFAULT_INDEX_FILE = "index.txt";
    private static final String DEFAULT_FRIEND_FILE = "friend.txt";

    public static void main(String[] args) {
        SocialNetwork network = new SocialNetwork();
        Scanner scanner = new Scanner(System.in);

        loadDefaultNetwork(network);
        runMenu(network, scanner);
        scanner.close();
    }

    private static void loadDefaultNetwork(SocialNetwork network) {
        boolean loaded = network.loadNetwork(DEFAULT_INDEX_FILE, DEFAULT_FRIEND_FILE);
        if (loaded) {
            System.out.println("Default social network loaded from index.txt and friend.txt.");
        } else {
            System.out.println("Default network could not be loaded. Use menu option 1 to load files.");
        }
    }

    private static void runMenu(SocialNetwork network, Scanner scanner) {
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> loadNewNetwork(network, scanner);
                case "2" -> showFriends(network, scanner);
                case "3" -> showFriendsAndFriendsOfFriends(network, scanner);
                case "4" -> showCommonFriends(network, scanner);
                case "5" -> deleteMember(network, scanner);
                case "6" -> showMembersByPopularity(network);
                case "7" -> {
                    System.out.println("Goodbye.");
                    return;
                }
                default -> System.out.println("Please enter a number from 1 to 7.");
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("Social Network Menu");
        System.out.println("1. Load a new network from files");
        System.out.println("2. List all friends of a member");
        System.out.println("3. List friends and friends of friends");
        System.out.println("4. List common friends of two members");
        System.out.println("5. Delete a member");
        System.out.println("6. List all members by popularity");
        System.out.println("7. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void loadNewNetwork(SocialNetwork network, Scanner scanner) {
        System.out.print("Enter friend filename: ");
        String friendFile = scanner.nextLine().trim();
        System.out.print("Enter index filename: ");
        String indexFile = scanner.nextLine().trim();

        boolean loaded = network.loadNetwork(indexFile, friendFile);
        if (loaded) {
            System.out.println("The new social network was loaded successfully.");
        } else {
            System.out.println("Could not load the new social network. The network is now empty.");
        }
    }

    private static void showFriends(SocialNetwork network, Scanner scanner) {
        if (ensureNetworkNotEmpty(network)) {
            return;
        }
        String name = promptName(scanner, "Enter a member name: ");
        List<String> friends = network.getFriends(name);
        if (friends == null) {
            System.out.println("That name does not exist in this social network.");
            return;
        }
        printNameList("Friends", name, friends);
    }

    private static void showFriendsAndFriendsOfFriends(SocialNetwork network, Scanner scanner) {
        if (ensureNetworkNotEmpty(network)) {
            return;
        }
        String name = promptName(scanner, "Enter a member name: ");
        List<String> friends = network.getFriendsAndFriendsOfFriends(name);
        if (friends == null) {
            System.out.println("That name does not exist in this social network.");
            return;
        }
        printNameList("Friends and friends of friends", name, friends);
    }

    private static void showCommonFriends(SocialNetwork network, Scanner scanner) {
        if (ensureNetworkNotEmpty(network)) {
            return;
        }
        String first = promptName(scanner, "Enter the first member name: ");
        String second = promptName(scanner, "Enter the second member name: ");

        if (!network.memberExists(first) || !network.memberExists(second)) {
            System.out.println("One or both names do not exist in this social network.");
            return;
        }

        List<String> common = network.getCommonFriends(first, second);
        printCommonFriends(first, second, common);
    }

    private static void deleteMember(SocialNetwork network, Scanner scanner) {
        if (ensureNetworkNotEmpty(network)) {
            return;
        }
        String name = promptName(scanner, "Enter the member name to delete: ");
        if (!network.memberExists(name)) {
            System.out.println("That name does not exist in this social network.");
            return;
        }

        System.out.print("Are you sure you want to delete " + name + "? Enter Y to confirm: ");
        String confirmation = scanner.nextLine().trim();
        boolean confirmed = confirmation.equalsIgnoreCase("Y");

        SocialNetwork.DeleteResult result = network.deleteMember(name, confirmed);
        switch (result) {
            case DELETED -> System.out.println(name + " was deleted from the social network.");
            case CANCELLED -> System.out.println("Deletion cancelled.");
            case NAME_NOT_FOUND -> System.out.println("That name does not exist in this social network.");
        }
    }

    private static void showMembersByPopularity(SocialNetwork network) {
        if (ensureNetworkNotEmpty(network)) {
            return;
        }
        List<SocialNetwork.MemberPopularity> members = network.listMembersByPopularity();
        System.out.println();
        System.out.println("Social Network Members by Popularity");
        System.out.println("Name                 Number of Friends");
        System.out.println("--------------------------------------");
        for (SocialNetwork.MemberPopularity member : members) {
            System.out.printf("%-20s %d%n", member.name(), member.friendCount());
        }
    }

    private static boolean ensureNetworkNotEmpty(SocialNetwork network) {
        if (!network.isEmpty()) {
            return false;
        }
        System.out.println("The social network is empty. Please load a network first.");
        return true;
    }

    private static String promptName(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static void printNameList(String title, String name, List<String> names) {
        System.out.println();
        System.out.println(title + " for " + name + ":");
        if (names.isEmpty()) {
            System.out.println("No names to display.");
            return;
        }
        for (String entry : names) {
            System.out.println(entry);
        }
    }

    private static void printCommonFriends(String first, String second, List<String> common) {
        System.out.println();
        System.out.println("Common friends for " + first + " and " + second + ":");
        if (common.isEmpty()) {
            System.out.println("No common friends found.");
            return;
        }
        for (String name : common) {
            System.out.println(name);
        }
    }
}
