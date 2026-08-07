import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * CodeNext – Personalized Coding Problem Recommendation System
 * 
 * A complete Java console application demonstrating object-oriented design,
 * collections usage, user management, submission tracking, and a dynamic 
 * personalized recommendation algorithm.
 */

// ============================================================================
// MODEL CLASSES
// ============================================================================

/**
 * Represents a user in the system.
 */
class User {
    private String name;
    private String username;
    private String password;

    public User(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public boolean validatePassword(String password) {
        return this.password.equals(password);
    }
}

/**
 * Represents a coding problem in the repository.
 */
class CodingProblem {
    private int id;
    private String title;
    private String topic;
    private String difficulty; // Easy, Medium, Hard
    private String description;
    private String requiredSkillLevel; // Beginner, Intermediate, Advanced

    public CodingProblem(int id, String title, String topic, String difficulty, String description, String requiredSkillLevel) {
        this.id = id;
        this.title = title;
        this.topic = topic;
        this.difficulty = difficulty;
        this.description = description;
        this.requiredSkillLevel = requiredSkillLevel;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getDescription() {
        return description;
    }

    public String getRequiredSkillLevel() {
        return requiredSkillLevel;
    }

    public void displayDetails() {
        System.out.println("ID         : " + id);
        System.out.println("Title      : " + title);
        System.out.println("Topic      : " + topic);
        System.out.println("Difficulty : " + difficulty);
        System.out.println("Description: " + description);
        System.out.println("Req. Level : " + requiredSkillLevel);
    }
}

/**
 * Records a user's attempt on a coding problem.
 */
class Submission {
    private int problemId;
    private boolean solved;
    private String topic;
    private String difficulty;

    public Submission(int problemId, boolean solved, String topic, String difficulty) {
        this.problemId = problemId;
        this.solved = solved;
        this.topic = topic;
        this.difficulty = difficulty;
    }

    public int getProblemId() {
        return problemId;
    }

    public boolean isSolved() {
        return solved;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }
}

/**
 * Represents a past recommendation generated for a user.
 */
class Recommendation {
    private CodingProblem problem;
    private String reason;
    private String timestamp;

    public Recommendation(CodingProblem problem, String reason) {
        this.problem = problem;
        this.reason = reason;
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.timestamp = LocalDateTime.now().format(dtf);
    }

    public CodingProblem getProblem() {
        return problem;
    }

    public String getReason() {
        return reason;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

// ============================================================================
// ENGINE CLASSES
// ============================================================================

/**
 * Handles evaluation of user metrics, skill level, weak topics, and recommendation generation.
 */
class RecommendationEngine {

    /**
     * Calculates user skill level based on solved problems.
     * Rule:
     * - Default: Beginner
     * - Hard solved >= 2 OR (Medium solved >= 4 AND Success Rate >= 70%): Advanced
     * - Medium solved >= 2 OR Total solved >= 5: Intermediate
     * - Else: Beginner
     */
    public String calculateSkillLevel(List<Submission> submissions) {
        int easySolved = 0;
        int mediumSolved = 0;
        int hardSolved = 0;
        int totalAttempted = submissions.size();
        int totalSolved = 0;

        for (Submission s : submissions) {
            if (s.isSolved()) {
                totalSolved++;
                if (s.getDifficulty().equalsIgnoreCase("Easy")) easySolved++;
                else if (s.getDifficulty().equalsIgnoreCase("Medium")) mediumSolved++;
                else if (s.getDifficulty().equalsIgnoreCase("Hard")) hardSolved++;
            }
        }

        double successRate = totalAttempted == 0 ? 0 : ((double) totalSolved / totalAttempted) * 100;

        if (hardSolved >= 2 || (mediumSolved >= 4 && successRate >= 70.0)) {
            return "Advanced";
        } else if (mediumSolved >= 2 || totalSolved >= 5) {
            return "Intermediate";
        } else {
            return "Beginner";
        }
    }

    /**
     * Identifies user's weak topic based on failure rates or low attempts.
     */
    public String identifyWeakTopic(List<Submission> submissions, List<CodingProblem> allProblems) {
        if (submissions.isEmpty()) {
            return "Arrays"; // Default starter topic
        }

        Map<String, Integer> topicAttempts = new HashMap<>();
        Map<String, Integer> topicFailures = new HashMap<>();

        for (Submission s : submissions) {
            String t = s.getTopic();
            topicAttempts.put(t, topicAttempts.getOrDefault(t, 0) + 1);
            if (!s.isSolved()) {
                topicFailures.put(t, topicFailures.getOrDefault(t, 0) + 1);
            }
        }

        // Check if any topic has a high failure count
        String weakTopic = null;
        int maxFailures = 0;

        for (Map.Entry<String, Integer> entry : topicFailures.entrySet()) {
            if (entry.getValue() > maxFailures) {
                maxFailures = entry.getValue();
                weakTopic = entry.getKey();
            }
        }

        if (weakTopic != null) {
            return weakTopic;
        }

        // If no explicit failures, check for unattempted topics in repository
        for (CodingProblem p : allProblems) {
            if (!topicAttempts.containsKey(p.getTopic())) {
                return p.getTopic();
            }
        }

        // Default to most attempted topic to encourage depth
        return submissions.get(0).getTopic();
    }

    /**
     * Generates a personalized problem recommendation using rule-based filtering.
     */
    public Recommendation generateRecommendation(
            List<Submission> submissions, 
            List<CodingProblem> repository, 
            List<Recommendation> history) {

        String currentLevel = calculateSkillLevel(submissions);
        String weakTopic = identifyWeakTopic(submissions, repository);

        // Track already solved problem IDs
        List<Integer> solvedIds = new ArrayList<>();
        for (Submission s : submissions) {
            if (s.isSolved()) {
                solvedIds.add(s.getProblemId());
            }
        }

        // Track recently recommended problem IDs (avoid immediate repeat)
        List<Integer> recentRecommendedIds = new ArrayList<>();
        int historySize = history.size();
        for (int i = Math.max(0, historySize - 3); i < historySize; i++) {
            recentRecommendedIds.add(history.get(i).getProblem().getId());
        }

        CodingProblem selectedProblem = null;
        String reason = "";

        // Strategy 1: Weak Topic + Match Skill Level
        for (CodingProblem p : repository) {
            if (solvedIds.contains(p.getId()) || recentRecommendedIds.contains(p.getId())) {
                continue;
            }
            if (p.getTopic().equalsIgnoreCase(weakTopic) && matchesSkillLevel(p.getDifficulty(), currentLevel)) {
                selectedProblem = p;
                reason = "You need more practice in " + weakTopic + ". The " + p.getDifficulty() + 
                         " difficulty matches your current skill level (" + currentLevel + ").";
                break;
            }
        }

        // Strategy 2: Any unsolved problem in Weak Topic
        if (selectedProblem == null) {
            for (CodingProblem p : repository) {
                if (!solvedIds.contains(p.getId())) {
                    if (p.getTopic().equalsIgnoreCase(weakTopic)) {
                        selectedProblem = p;
                        reason = "Focusing on your target topic (" + weakTopic + ") to build core competency.";
                        break;
                    }
                }
            }
        }

        // Strategy 3: Any unsolved problem matching user's current skill level
        if (selectedProblem == null) {
            for (CodingProblem p : repository) {
                if (!solvedIds.contains(p.getId()) && matchesSkillLevel(p.getDifficulty(), currentLevel)) {
                    selectedProblem = p;
                    reason = "Selected based on your skill level (" + currentLevel + ") to push your problem-solving bounds.";
                    break;
                }
            }
        }

        // Strategy 4: Fallback to any unsolved problem
        if (selectedProblem == null) {
            for (CodingProblem p : repository) {
                if (!solvedIds.contains(p.getId())) {
                    selectedProblem = p;
                    reason = "General recommendation to explore new topic areas.";
                    break;
                }
            }
        }

        if (selectedProblem == null) {
            return null; // All problems solved!
        }

        return new Recommendation(selectedProblem, reason);
    }

    private boolean matchesSkillLevel(String problemDifficulty, String userLevel) {
        if (userLevel.equalsIgnoreCase("Beginner")) {
            return problemDifficulty.equalsIgnoreCase("Easy");
        } else if (userLevel.equalsIgnoreCase("Intermediate")) {
            return problemDifficulty.equalsIgnoreCase("Easy") || problemDifficulty.equalsIgnoreCase("Medium");
        } else { // Advanced
            return problemDifficulty.equalsIgnoreCase("Medium") || problemDifficulty.equalsIgnoreCase("Hard");
        }
    }
}

// ============================================================================
// MAIN APPLICATION CLASS
// ============================================================================

public class CodeNext {

    private static Map<String, User> userMap = new HashMap<>();
    private static List<CodingProblem> problemRepository = new ArrayList<>();
    private static Map<String, List<Submission>> userSubmissions = new HashMap<>();
    private static Map<String, List<Recommendation>> userHistory = new HashMap<>();

    private static User currentUser = null;
    private static Scanner scanner = new Scanner(System.in);
    private static RecommendationEngine engine = new RecommendationEngine();

    public static void main(String[] args) {
        initializeRepository();
        seedSampleData();

        boolean running = true;
        while (running) {
            if (currentUser == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
    }

    // ------------------------------------------------------------------------
    // DATA INITIALIZATION
    // ------------------------------------------------------------------------

    private static void initializeRepository() {
        problemRepository.add(new CodingProblem(101, "Two Sum", "Arrays", "Easy", "Find two numbers in an array that add up to a target sum.", "Beginner"));
        problemRepository.add(new CodingProblem(102, "Best Time to Buy and Sell Stock", "Arrays", "Easy", "Maximize profit by choosing a single day to buy and a future day to sell.", "Beginner"));
        problemRepository.add(new CodingProblem(103, "Container With Most Water", "Arrays", "Medium", "Find two lines that together with the x-axis form a container holding the most water.", "Intermediate"));
        problemRepository.add(new CodingProblem(104, "Valid Anagram", "Strings", "Easy", "Determine if two strings are anagrams of each other.", "Beginner"));
        problemRepository.add(new CodingProblem(105, "Longest Substring Without Repeating Characters", "Strings", "Medium", "Find length of the longest substring without repeating characters.", "Intermediate"));
        problemRepository.add(new CodingProblem(106, "Reverse Linked List", "Linked List", "Easy", "Reverse a singly linked list iteratively and recursively.", "Beginner"));
        problemRepository.add(new CodingProblem(107, "Merge Two Sorted Lists", "Linked List", "Easy", "Merge two sorted linked lists into a single sorted list.", "Beginner"));
        problemRepository.add(new CodingProblem(108, "Valid Parentheses", "Stack", "Easy", "Determine if input string brackets are valid and closed in correct order.", "Beginner"));
        problemRepository.add(new CodingProblem(109, "Implement Queue using Stacks", "Queue", "Easy", "Implement a first-in-first-out (FIFO) queue using only two stacks.", "Beginner"));
        problemRepository.add(new CodingProblem(110, "Climbing Stairs", "Recursion", "Easy", "Count distinct ways to climb n steps taking 1 or 2 steps at a time.", "Beginner"));
        problemRepository.add(new CodingProblem(111, "Merge Sort Implementation", "Sorting", "Medium", "Sort an array using the divide-and-conquer Merge Sort algorithm.", "Intermediate"));
        problemRepository.add(new CodingProblem(112, "Binary Search", "Searching", "Easy", "Search a target value within a sorted array in O(log n) time.", "Beginner"));
        problemRepository.add(new CodingProblem(113, "Group Anagrams", "Hashing", "Medium", "Group strings that are anagrams together using Hash Maps.", "Intermediate"));
        problemRepository.add(new CodingProblem(114, "Coin Change", "Dynamic Programming", "Hard", "Compute fewest coins needed to make up a given target amount.", "Advanced"));
        problemRepository.add(new CodingProblem(115, "Invert Binary Tree", "Trees", "Easy", "Invert a binary tree so left and right subtrees are swapped.", "Beginner"));
        problemRepository.add(new CodingProblem(116, "Number of Islands", "Graphs", "Medium", "Count connected land components in a 2D grid using BFS/DFS.", "Intermediate"));
    }

    private static void seedSampleData() {
        // Create a pre-existing demo user
        User demoUser = new User("Alex Developer", "alex", "pass123");
        userMap.put("alex", demoUser);
        userSubmissions.put("alex", new ArrayList<>());
        userHistory.put("alex", new ArrayList<>());
    }

    // ------------------------------------------------------------------------
    // AUTHENTICATION MENU & HANDLERS
    // ------------------------------------------------------------------------

    private static void showAuthMenu() {
        System.out.println("\n========================================");
        System.out.println("              CODENEXT                  ");
        System.out.println(" Personalized Coding Problem Recommender ");
        System.out.println("========================================");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit System");
        System.out.println("========================================");
        System.out.print("Select an option (1-3): ");

        int choice = readIntInput();

        switch (choice) {
            case 1:
                handleLogin();
                break;
            case 2:
                handleRegistration();
                break;
            case 3:
                System.out.println("\nThank you for using CodeNext. Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("\n[!] Invalid selection. Please enter a number between 1 and 3.");
        }
    }

    private static void handleRegistration() {
        System.out.println("\n--- USER REGISTRATION ---");
        System.out.print("Enter your full name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("[!] Username cannot be empty.");
            return;
        }

        if (userMap.containsKey(username.toLowerCase())) {
            System.out.println("[!] Username already exists. Please choose another or login.");
            return;
        }

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (password.length() < 3) {
            System.out.println("[!] Password must be at least 3 characters long.");
            return;
        }

        User newUser = new User(name, username.toLowerCase(), password);
        userMap.put(username.toLowerCase(), newUser);
        userSubmissions.put(username.toLowerCase(), new ArrayList<>());
        userHistory.put(username.toLowerCase(), new ArrayList<>());

        System.out.println("\n[✓] Registration successful! You can now log in.");
    }

    private static void handleLogin() {
        System.out.println("\n--- USER LOGIN ---");
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim().toLowerCase();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        if (userMap.containsKey(username)) {
            User user = userMap.get(username);
            if (user.validatePassword(password)) {
                currentUser = user;
                System.out.println("\n========================================");
                System.out.println(" Welcome back, " + currentUser.getName() + "!");
                System.out.println(" Ready to upgrade your skills today?");
                System.out.println("========================================");
            } else {
                System.out.println("\n[!] Incorrect password. Please try again.");
            }
        } else {
            System.out.println("\n[!] Username not found. Please register first.");
        }
    }

    // ------------------------------------------------------------------------
    // MAIN APPLICATION MENU & FEATURES
    // ------------------------------------------------------------------------

    private static void showMainMenu() {
        System.out.println("\n========================================");
        System.out.println("              CODENEXT                  ");
        System.out.println(" Personalized Coding Problem Recommender ");
        System.out.println("========================================");
        System.out.println("1. View Coding Problems");
        System.out.println("2. Solve a Problem");
        System.out.println("3. View My Progress");
        System.out.println("4. Get Personalized Recommendation");
        System.out.println("5. View Recommendation History");
        System.out.println("6. Logout");
        System.out.println("7. Exit");
        System.out.println("========================================");
        System.out.print("Select an option (1-7): ");

        int choice = readIntInput();

        switch (choice) {
            case 1:
                viewCodingProblems();
                break;
            case 2:
                solveProblem();
                break;
            case 3:
                viewMyProgress();
                break;
            case 4:
                getPersonalizedRecommendation();
                break;
            case 5:
                viewRecommendationHistory();
                break;
            case 6:
                System.out.println("\n[✓] Logged out successfully. Goodbye, " + currentUser.getName() + "!");
                currentUser = null;
                break;
            case 7:
                System.out.println("\nThank you for practicing with CodeNext. Happy Coding!");
                System.exit(0);
                break;
            default:
                System.out.println("\n[!] Invalid choice. Please select an option between 1 and 7.");
        }
    }

    private static void viewCodingProblems() {
        System.out.println("\n=========================================================================================");
        System.out.println("                               AVAILABLE CODING PROBLEMS                                 ");
        System.out.println("=========================================================================================");
        System.out.printf("%-6s | %-40s | %-20s | %-10s\n", "ID", "Title", "Topic", "Difficulty");
        System.out.println("-----------------------------------------------------------------------------------------");

        List<Submission> submissions = userSubmissions.get(currentUser.getUsername());
        
        for (CodingProblem p : problemRepository) {
            String status = "";
            for (Submission s : submissions) {
                if (s.getProblemId() == p.getId()) {
                    status = s.isSolved() ? " [SOLVED]" : " [FAILED]";
                    break;
                }
            }
            System.out.printf("%-6d | %-40s | %-20s | %-10s%s\n", 
                    p.getId(), p.getTitle(), p.getTopic(), p.getDifficulty(), status);
        }
        System.out.println("=========================================================================================");
    }

    private static void solveProblem() {
        System.out.print("\nEnter Problem ID to solve: ");
        int id = readIntInput();

        CodingProblem targetProblem = null;
        for (CodingProblem p : problemRepository) {
            if (p.getId() == id) {
                targetProblem = p;
                break;
            }
        }

        if (targetProblem == null) {
            System.out.println("\n[!] Problem with ID " + id + " does not exist.");
            return;
        }

        System.out.println("\n----------------------------------------");
        targetProblem.displayDetails();
        System.out.println("----------------------------------------");

        System.out.print("Did you solve this problem successfully? (yes/no): ");
        String response = scanner.nextLine().trim().toLowerCase();

        boolean isSolved = response.equals("yes") || response.equals("y");

        // Record submission
        List<Submission> submissions = userSubmissions.get(currentUser.getUsername());
        submissions.add(new Submission(targetProblem.getId(), isSolved, targetProblem.getTopic(), targetProblem.getDifficulty()));

        System.out.println("\n========================================");
        System.out.println("Problem submitted successfully!");
        System.out.println("Result    : " + (isSolved ? "Solved" : "Failed"));
        System.out.println("Topic     : " + targetProblem.getTopic());
        System.out.println("Difficulty: " + targetProblem.getDifficulty());
        System.out.println("========================================");
    }

    private static void viewMyProgress() {
        List<Submission> submissions = userSubmissions.get(currentUser.getUsername());

        int attempted = submissions.size();
        int solved = 0;
        int failed = 0;

        int easySolved = 0;
        int mediumSolved = 0;
        int hardSolved = 0;

        for (Submission s : submissions) {
            if (s.isSolved()) {
                solved++;
                if (s.getDifficulty().equalsIgnoreCase("Easy")) easySolved++;
                else if (s.getDifficulty().equalsIgnoreCase("Medium")) mediumSolved++;
                else if (s.getDifficulty().equalsIgnoreCase("Hard")) hardSolved++;
            } else {
                failed++;
            }
        }

        double successRate = attempted == 0 ? 0.0 : ((double) solved / attempted) * 100.0;
        String currentLevel = engine.calculateSkillLevel(submissions);
        String weakTopic = engine.identifyWeakTopic(submissions, problemRepository);

        System.out.println("\n========== MY PROGRESS ==========");
        System.out.println("Problems Attempted : " + attempted);
        System.out.println("Problems Solved    : " + solved);
        System.out.println("Problems Failed    : " + failed);
        System.out.println();
        System.out.println("Easy Solved        : " + easySolved);
        System.out.println("Medium Solved      : " + mediumSolved);
        System.out.println("Hard Solved        : " + hardSolved);
        System.out.println();
        System.out.printf("Success Rate       : %.1f%%\n", successRate);
        System.out.println("Current Level      : " + currentLevel);
        System.out.println("Weak Topic         : " + weakTopic);
        System.out.println("=================================");
    }

    private static void getPersonalizedRecommendation() {
        List<Submission> submissions = userSubmissions.get(currentUser.getUsername());
        List<Recommendation> history = userHistory.get(currentUser.getUsername());

        Recommendation rec = engine.generateRecommendation(submissions, problemRepository, history);

        if (rec == null) {
            System.out.println("\n[!] Outstanding! You have successfully solved all available problems in the repository.");
            return;
        }

        // Add to recommendation history
        history.add(rec);

        System.out.println("\n========================================");
        System.out.println("       PERSONALIZED RECOMMENDATION      ");
        System.out.println("========================================");
        System.out.println("Recommended Problem : " + rec.getProblem().getTitle() + " (ID: " + rec.getProblem().getId() + ")");
        System.out.println("Topic                : " + rec.getProblem().getTopic());
        System.out.println("Difficulty           : " + rec.getProblem().getDifficulty());
        System.out.println("\nWhy this problem?");
        System.out.println(rec.getReason());
        System.out.println("========================================");
        System.out.println("Recommendation generated successfully!");
    }

    private static void viewRecommendationHistory() {
        List<Recommendation> history = userHistory.get(currentUser.getUsername());

        System.out.println("\n=========================================================================================");
        System.out.println("                              RECOMMENDATION HISTORY                                     ");
        System.out.println("=========================================================================================");

        if (history.isEmpty()) {
            System.out.println("No recommendations generated yet. Choose option 4 from the menu to get one!");
        } else {
            for (int i = 0; i < history.size(); i++) {
                Recommendation r = history.get(i);
                System.out.println("[" + (i + 1) + "] Timestamp   : " + r.getTimestamp());
                System.out.println("    Problem     : " + r.getProblem().getTitle() + " [" + r.getProblem().getTopic() + " - " + r.getProblem().getDifficulty() + "]");
                System.out.println("    Reason      : " + r.getReason());
                System.out.println("-----------------------------------------------------------------------------------------");
            }
        }
        System.out.println("=========================================================================================");
    }

    // ------------------------------------------------------------------------
    // UTILITY METHODS
    // ------------------------------------------------------------------------

    private static int readIntInput() {
        try {
            int input = scanner.nextInt();
            scanner.nextLine(); // Clear newline buffer
            return input;
        } catch (InputMismatchException e) {
            scanner.nextLine(); // Clear bad input buffer
            return -1;
        }
    }
}