# ITC322 Assessment 2 - Social Network (Adjacency Matrix)

## Overview
This project implements a simple social network using an undirected graph backed by an adjacency matrix.

## Files
- Graph.java: directed adjacency-matrix base graph.
- UndirectedGraph.java: extends Graph and mirrors edges in both directions.
- SocialNetwork.java: network logic for loading files, searching, deleting, and reporting.
- SocialNetworkDriver.java: menu-based driver program (Task 7).
- index.txt and friend.txt: default data files in project root.

## Build and Run
From the project folder:

javac *.java
java SocialNetworkDriver

## Task Coverage
1. Read social network data:
   - Reads index.txt and friend.txt.
   - Uses File("index.txt") / File("friend.txt") style paths.
   - Prints clear System.err messages on file/format failures.
2. Friends list:
   - Case-insensitive name search.
   - Sorted alphabetical output (case-insensitive).
   - Handles empty network and missing names.
3. Friends and friends-of-friends:
   - Unique output names only.
   - Excludes original person.
   - Sorted alphabetical output (case-insensitive).
4. Common friends:
   - Finds mutual friends for two members.
   - Sorted alphabetical output (case-insensitive).
   - Handles empty network and missing names.
5. Delete member:
   - Requires explicit Y/y confirmation.
   - Any other input cancels deletion.
   - Removes all associated edges.
6. Print all members by popularity:
   - Sort by friend count descending.
   - Tie-break by name A-Z (case-insensitive).
   - Displays title and columns.
7. Driver menu:
   - Provides options 1 to 7 exactly as required.
   - Option 1 loads a new network and replaces old data.

## Sample Default Data
index.txt
6
0 Gromit
1 Gwendolyn
2 Le-Spiderman
3 Wallace
4 Batman
5 Superman

friend.txt
5
0 3
1 3
0 1
2 4
1 5
