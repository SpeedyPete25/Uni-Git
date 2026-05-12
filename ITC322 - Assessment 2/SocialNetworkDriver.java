import java.util.List;
import java.util.Scanner;

/**
 * Console driver for the social network program.
 */
public class SocialNetworkDriver {
    // Default files are expected in the project directory.
    private static final String DEFAULT_INDEX_FILE = "index.txt";
    private static final String DEFAULT_FRIEND_FILE = "friend.txt";

    /**
     * Entry point for the program.
     *
     * @param args command line args
     */
    public static void main(String[] args) {
        SocialNetwork network = new SocialNetwork();
        Scanner scanner = new Scanner(System.in);

        loadDefaultNetwork(network);
        runMenu(network, scanner);
        scanner.close();
    }

    /**
     * Loads the default network files from the project directory.
     */
    private static void loadDefaultNetwork(SocialNetwork network) {
        boolean loaded = network.loadNetwork(DEFAULT_INDEX_FILE, DEFAULT_FRIEND_FILE);
        if (loaded) {
            System.out.println("Default social network loaded from index.txt and friend.txt.");
        } else {
            System.out.println("Default network could not be loaded. Use menu option 1 to load files.");
        }
    }

    /**
     * Runs the main menu loop until the user chooses to exit.
     */
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
                default -> printError("Please enter a number from 1 to 7.");
            }
        }
    }

    /**
     * Prints available menu options.
     */
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

    /**
     * Loads a replacement network from user-provided file names.
     */
    private static void loadNewNetwork(SocialNetwork network, Scanner scanner) {
        System.out.print("Enter friend filename: ");
        String friendFile = scanner.nextLine().trim();
        System.out.print("Enter index filename: ");
        String indexFile = scanner.nextLine().trim();

        boolean loaded = network.loadNetwork(indexFile, friendFile);
        if (loaded) {
            System.out.println("The new social network was loaded successfully.");
        } else {
            printError("Could not load the new social network. The network is now empty.");
        }
    }

    /**
     * Handles menu option 2.
     */
    private static void showFriends(SocialNetwork network, Scanner scanner) {
        if (ensureNetworkNotEmpty(network)) {
            return;
        }
        String name = promptName(scanner, "Enter a member name: ");
        if (name.isEmpty()) {
            printError("Please enter a name.");
            return;
        }
        List<String> friends = network.getFriends(name);
        if (friends == null) {
            printError("That name does not exist in this social network.");
            return;
        }
        printNameList("Friends", name, friends);
    }

    /**
     * Handles menu option 3.
     */
    private static void showFriendsAndFriendsOfFriends(SocialNetwork network, Scanner scanner) {
        if (ensureNetworkNotEmpty(network)) {
            return;
        }
        String name = promptName(scanner, "Enter a member name: ");
        if (name.isEmpty()) {
            printError("Please enter a name.");
            return;
        }
        List<String> friends = network.getFriendsAndFriendsOfFriends(name);
        if (friends == null) {
            printError("That name does not exist in this social network.");
            return;
        }
        printNameList("Friends and friends of friends", name, friends);
    }

    /**
     * Handles menu option 4.
     */
    private static void showCommonFriends(SocialNetwork network, Scanner scanner) {
        if (ensureNetworkNotEmpty(network)) {
            return;
        }
        String first = promptName(scanner, "Enter the first member name: ");
        String second = promptName(scanner, "Enter the second member name: ");
        if (first.isEmpty() || second.isEmpty()) {
            printError("Please enter both names.");
            return;
        }

        if (!network.memberExists(first) || !network.memberExists(second)) {
            printError("One or both names do not exist in this social network.");
            return;
        }

        List<String> common = network.getCommonFriends(first, second);
        printCommonFriends(first, second, common);
    }

    /**
     * Handles menu option 5.
     */
    private static void deleteMember(SocialNetwork network, Scanner scanner) {
        if (ensureNetworkNotEmpty(network)) {
            return;
        }
        String name = promptName(scanner, "Enter the member name to delete: ");
        if (name.isEmpty()) {
            printError("Please enter a name.");
            return;
        }
        if (!network.memberExists(name)) {
            printError("That name does not exist in this social network.");
            return;
        }

        System.out.print("Are you sure you want to delete " + name + "? Enter Y to confirm: ");
        String confirmation = scanner.nextLine().trim();
        boolean confirmed = confirmation.equalsIgnoreCase("Y");

        SocialNetwork.DeleteResult result = network.deleteMember(name, confirmed);
        switch (result) {
            case DELETED -> System.out.println(name + " was deleted from the social network.");
            case CANCELLED -> System.out.println("Deletion cancelled.");
            case NAME_NOT_FOUND -> printError("That name does not exist in this social network.");
        }
    }

    /**
     * Handles menu option 6.
     */
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

    /**
     * Ensures network data exists before running a menu action.
     */
    private static boolean ensureNetworkNotEmpty(SocialNetwork network) {
        if (!network.isEmpty()) {
            return false;
        }
        printError("The social network is empty. Please load a network first.");
        return true;
    }

    /**
     * Prints user-facing error messages to standard error.
     */
    private static void printError(String message) {
        System.err.println(message);
    }

    /**
     * Prompts for a name and returns trimmed input.
     */
    private static String promptName(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /**
     * Prints a heading and then each name in a list.
     */
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

    /**
     * Prints common friends between two members.
     */
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
