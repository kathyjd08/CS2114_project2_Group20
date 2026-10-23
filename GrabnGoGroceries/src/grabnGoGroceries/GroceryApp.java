package grabnGoGroceries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Console application for Grab 'n' Go Groceries.
 *
 * <p>The whole trip happens in one session: shop, edit the basket, set a
 * budget, run challenges (with a scoring rubric), check out, and start the
 * next trip without ever restarting the program. Handy commands work from
 * almost any screen; type "help" to see them.</p>
 */
public class GroceryApp {
    private static final double TAX_RATE = 0.06;
    private static final int BAR_WIDTH = 20;
    private static final int WIDTH = 60;
    private static final int MAX_QUANTITY = 99;
    private static final int MAX_HISTORY = 10;
    private static final int MAX_BUDGET = 100000;

    // Rubric weights (they add up to 100).
    private static final int ITEM_POINTS = 20;
    private static final int BUDGET_POINTS = 40;
    private static final int SAVINGS_POINTS = 20;
    private static final int VARIETY_POINTS = 20;
    private static final int POINTS_PER_CATEGORY = 5;

    /** "3", "3x2", "3 x 2", "3*2" or "3 2": an item number and optional quantity. */
    private static final Pattern NUMBER_PICK = Pattern.compile(
        "^(\\d{1,4})(?:(?:\\s*[x*]\\s*|\\s+)(\\d{1,3}))?$",
        Pattern.CASE_INSENSITIVE);
    /** "milk 2", "milk x2" or "milk*2": a name with a quantity at the end. */
    private static final Pattern QUANTITY_SUFFIX = Pattern.compile(
        "^(.+?)(?:\\s+x?\\s*|\\s*\\*\\s*)(\\d{1,3})$",
        Pattern.CASE_INSENSITIVE);
    /** Basket edits such as "-2", "+2 3", "r 1 all" or "del 2". */
    private static final Pattern BASKET_EDIT = Pattern.compile(
        "^(\\+|-|add|a|remove|rm|r|delete|del|d)\\s*(\\d{1,4})"
        + "(?:\\s+(\\d{1,3}|all))?$",
        Pattern.CASE_INSENSITIVE);

    private static final String[] AISLES = {
        "Fruit", "Vegetables", "Meat & Seafood", "Dairy & Eggs",
        "Bakery", "Snacks", "Drinks"
    };

    private static final GroceryItem[][] CATALOG = {
        { // Fruit
            new GroceryItem("Apple", 0.99, "Fruit", "Crisp and sweet"),
            new GroceryItem("Banana", 0.25, "Fruit", "Ripe, sold each"),
            new GroceryItem("Orange", 1.10, "Fruit", "Juicy navel orange"),
            new GroceryItem("Strawberries", 4.49, "Fruit", "1 lb container"),
            new GroceryItem("Grapes", 3.99, "Fruit", "Seedless red grapes")
        },
        { // Vegetables
            new GroceryItem("Carrots", 1.99, "Vegetables", "1 lb bag"),
            new GroceryItem("Broccoli", 2.49, "Vegetables", "Fresh crown"),
            new GroceryItem("Spinach", 3.29, "Vegetables", "Washed baby spinach"),
            new GroceryItem("Tomato", 0.89, "Vegetables", "Vine-ripened"),
            new GroceryItem("Potatoes", 3.49, "Vegetables", "5 lb bag")
        },
        { // Meat & Seafood
            new GroceryItem("Chicken Breast", 6.99, "Meat & Seafood", "Boneless, 1 lb"),
            new GroceryItem("Ground Beef", 5.99, "Meat & Seafood", "80% lean, 1 lb"),
            new GroceryItem("Bacon", 5.49, "Meat & Seafood", "Hickory smoked"),
            new GroceryItem("Salmon Fillet", 9.99, "Meat & Seafood", "Fresh, 1 lb")
        },
        { // Dairy & Eggs
            new GroceryItem("Milk", 3.49, "Dairy & Eggs", "Whole, 1 gallon"),
            new GroceryItem("Cheddar Cheese", 3.99, "Dairy & Eggs", "Sharp, 8 oz block"),
            new GroceryItem("Eggs", 3.29, "Dairy & Eggs", "Large, dozen"),
            new GroceryItem("Butter", 4.19, "Dairy & Eggs", "Salted sticks"),
            new GroceryItem("Yogurt", 1.29, "Dairy & Eggs", "Vanilla cup")
        },
        { // Bakery
            new GroceryItem("Bread", 2.99, "Bakery", "Whole wheat loaf"),
            new GroceryItem("Bagels", 3.49, "Bakery", "Plain, 6 count"),
            new GroceryItem("Croissants", 4.99, "Bakery", "Butter, 4 count")
        },
        { // Snacks
            new GroceryItem("Chips", 3.49, "Snacks", "Classic salted"),
            new GroceryItem("Popcorn", 2.79, "Snacks", "Microwave butter"),
            new GroceryItem("Cookies", 3.99, "Snacks", "Chocolate chip"),
            new GroceryItem("Pretzels", 2.99, "Snacks", "Mini twists")
        },
        { // Drinks
            new GroceryItem("Soda", 1.99, "Drinks", "2 liter bottle"),
            new GroceryItem("Orange Juice", 3.99, "Drinks", "No pulp, 52 oz"),
            new GroceryItem("Bottled Water", 4.99, "Drinks", "24 pack"),
            new GroceryItem("Coffee", 8.99, "Drinks", "Medium roast, 12 oz")
        }
    };

    /** One row of the basket: an item and how many copies of it. */
    private static class Line {
        private final GroceryItem item;
        private int quantity;

        private Line(GroceryItem item) {
            this.item = item;
            this.quantity = 1;
        }
    }

    /** The result of scoring a basket against a challenge. */
    private static class Grade {
        private int itemPoints;
        private int budgetPoints;
        private int savingsPoints;
        private int varietyPoints;
        private int score;
        private String letter = "F";
        private boolean passed;
        private int itemCount;
        private long spentCents;
        private int categories;
    }

    /** A finished order, kept for the history screen. */
    private static class Order {
        private final String date;
        private final int items;
        private final long cents;
        private final String note;

        private Order(String date, int items, long cents, String note) {
            this.date = date;
            this.items = items;
            this.cents = cents;
            this.note = note;
        }
    }

    private final Scanner input;
    private final File saveFile;      // null means progress is not saved
    private GroceryBasket basket;
    private final LinkedChain<ShoppingChallenge> challenges;
    private final LinkedChain<LinkedChain<GroceryItem>> undoStack;
    private final LinkedChain<Order> history;
    private final int[] bestScores;    // best passing score per challenge, -1 = none
    private ShoppingChallenge activeChallenge;
    private int activeIndex;           // index of activeChallenge, -1 = none
    private Grade lastGrade;           // most recent submission, null = none
    private long budgetCents;          // 0 means no budget is set
    private boolean exitRequested;     // true once the user asks to leave
    private boolean goHome;            // true while backing out to the main menu
    private boolean restored;          // true if a saved trip was loaded
    private boolean saveWarned;        // so a failing save only complains once

    /** Creates an app that does not save progress. */
    public GroceryApp(Scanner input) {
        this(input, null);
    }

    /**
     * Creates an app that saves progress to the given file (and restores
     * from it if it already exists). A null file turns saving off.
     */
    public GroceryApp(Scanner input, File saveFile) {
        if (input == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        this.input = input;
        this.saveFile = saveFile;
        this.basket = new GroceryBasket();
        this.challenges = new LinkedChain<ShoppingChallenge>();
        this.undoStack = new LinkedChain<LinkedChain<GroceryItem>>();
        this.history = new LinkedChain<Order>();
        this.activeChallenge = null;
        this.activeIndex = -1;
        this.lastGrade = null;
        this.budgetCents = 0;
        this.exitRequested = false;
        this.goHome = false;
        this.saveWarned = false;

        challenges.addToEnd(new ShoppingChallenge(
            "Movie Night", "Get snacks for movie night", 25.00, 4));
        challenges.addToEnd(new ShoppingChallenge(
            "Weekly Shop", "Stock up for the week", 60.00, 8));
        challenges.addToEnd(new ShoppingChallenge(
            "Party Prep", "Feed a crowd", 100.00, 15));

        this.bestScores = new int[challenges.size()];
        for (int i = 0; i < bestScores.length; i++) {
            bestScores[i] = -1;
        }

        load();
        this.restored = hasProgress();
    }

    /**
     * Starts the app. Progress is saved in a file in the user's home folder.
     * Set the system property grocery.save to another path to move it, or to
     * an empty string to turn saving off.
     */
    public static void main(String[] args) {
        String path = System.getProperty("grocery.save");
        File saveFile;
        if (path == null) {
            saveFile = new File(System.getProperty("user.home"),
                ".grabngo_save.properties");
        }
        else if (path.isEmpty()) {
            saveFile = null;
        }
        else {
            saveFile = new File(path);
        }
        new GroceryApp(new Scanner(System.in), saveFile).run();
    }

    /** Runs the app until the user quits (or the input runs out). */
    public void run() {
        if (restored) {
            System.out.println("\nWelcome back! Your trip is right where you left it.");
        }
        else {
            System.out.println("\nWelcome to Grab 'n' Go Groceries!");
            System.out.println("Tip: type 'help' at any time to see shortcuts.");
        }
        while (!exitRequested) {
            goHome = false;
            printMainMenu();
            String text = ask("Choose an option: ");
            if (text == null || text.isEmpty()) {
                continue;
            }
            handleMainChoice(text);
        }
        save();
        if (saveFile != null && hasProgress() && !saveWarned) {
            System.out.println(
                "Your trip is saved. Run the app again to pick up where you left off.");
        }
        System.out.println("Thanks for shopping!");
    }

    private void handleMainChoice(String text) {
        switch (text.toLowerCase()) {
            case "1":
                browseAisles();
                break;
            case "2":
                searchPrompt();
                break;
            case "3":
                basketScreen();
                break;
            case "4":
                budgetScreen();
                break;
            case "5":
                challengeHub();
                break;
            case "6":
                checkout();
                break;
            case "7":
                showHistory();
                break;
            case "8":
                newTrip();
                break;
            case "9":
                printHelp();
                break;
            case "0":
                exitRequested = true;
                break;
            default:
                typedCommand(text);
        }
    }

    /**
     * Commands that work from the main menu and from most lists, so nobody
     * has to back out just to do something else.
     */
    private void typedCommand(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        int space = trimmed.indexOf(' ');
        String cmd = (space < 0 ? trimmed : trimmed.substring(0, space))
            .toLowerCase();
        String rest = space < 0 ? "" : trimmed.substring(space + 1).trim();
        switch (cmd) {
            case "shop":
            case "aisles":
            case "browse":
                browseAisles();
                return;
            case "search":
            case "find":
                if (rest.isEmpty()) {
                    searchPrompt();
                }
                else {
                    searchFor(rest);
                }
                return;
            case "add":
            case "buy":
                quickAdd(rest, true);
                return;
            case "remove":
            case "rm":
            case "delete":
                quickRemove(rest);
                return;
            case "basket":
            case "cart":
            case "edit":
            case "view":
                basketScreen();
                return;
            case "undo":
                undo();
                return;
            case "clear":
                clearBasket();
                return;
            case "budget":
                budgetCommand(rest);
                return;
            case "challenge":
            case "challenges":
                challengeHub();
                return;
            case "submit":
                submitChallenge();
                return;
            case "rubric":
            case "score":
            case "grade":
                previewRubric();
                return;
            case "abandon":
                abandonChallenge();
                return;
            case "checkout":
            case "pay":
                checkout();
                return;
            case "history":
            case "orders":
                showHistory();
                return;
            case "new":
            case "reset":
                newTrip();
                return;
            default:
                break;
        }
        int aisle = findAisle(trimmed);
        if (aisle >= 0) {
            browseAisle(aisle);
            return;
        }
        quickAdd(trimmed, false);
    }

    // ------------------------------------------------------------------
    // Saving and loading
    // ------------------------------------------------------------------

    private boolean hasProgress() {
        return !basket.isEmpty() || budgetCents > 0 || activeIndex >= 0;
    }

    private void save() {
        if (saveFile == null) {
            return;
        }
        Properties p = new Properties();
        p.setProperty("budget", Double.toString(budgetCents / 100.0));
        p.setProperty("challenge", Integer.toString(activeIndex));
        if (lastGrade != null && activeIndex >= 0) {
            p.setProperty("lastScore", Integer.toString(lastGrade.score));
            p.setProperty("lastPassed", Boolean.toString(lastGrade.passed));
            p.setProperty("lastItems", Integer.toString(lastGrade.itemCount));
            p.setProperty("lastCents", Long.toString(lastGrade.spentCents));
        }
        for (int i = 0; i < bestScores.length; i++) {
            p.setProperty("best." + i, Integer.toString(bestScores[i]));
        }
        LinkedChain<GroceryItem> items = basket.snapshotItems();
        p.setProperty("items", Integer.toString(items.size()));
        for (int i = 0; i < items.size(); i++) {
            String prefix = "item." + i + ".";
            p.setProperty(prefix + "name", items.get(i).getName());
            p.setProperty(prefix + "price", Double.toString(items.get(i).getPrice()));
            p.setProperty(prefix + "category", items.get(i).getCategory());
            p.setProperty(prefix + "description", items.get(i).getDescription());
        }
        p.setProperty("orders", Integer.toString(history.size()));
        for (int i = 0; i < history.size(); i++) {
            Order order = history.get(i);
            String prefix = "order." + i + ".";
            p.setProperty(prefix + "date", order.date);
            p.setProperty(prefix + "items", Integer.toString(order.items));
            p.setProperty(prefix + "cents", Long.toString(order.cents));
            p.setProperty(prefix + "note", order.note);
        }
        try (Writer writer = new OutputStreamWriter(
            new FileOutputStream(saveFile), StandardCharsets.UTF_8)) {
            p.store(writer, "Grab 'n' Go Groceries saved trip");
            saveWarned = false;
        }
        catch (IOException e) {
            if (!saveWarned) {
                System.out.println("(Could not save your progress.)");
                saveWarned = true;
            }
        }
    }

    /** Restores a saved trip. A missing or damaged file starts fresh. */
    private void load() {
        if (saveFile == null || !saveFile.isFile()) {
            return;
        }
        Properties p = new Properties();
        try (Reader reader = new InputStreamReader(
            new FileInputStream(saveFile), StandardCharsets.UTF_8)) {
            p.load(reader);
        }
        catch (IOException | IllegalArgumentException e) {
            return;
        }
        try {
            double savedBudget = Double.parseDouble(p.getProperty("budget", "0"));
            if (!Double.isFinite(savedBudget) || savedBudget < 0
                || savedBudget > MAX_BUDGET) {
                throw new IllegalArgumentException("bad budget");
            }
            int index = Integer.parseInt(p.getProperty("challenge", "-1"));
            if (index < -1 || index >= challenges.size()) {
                throw new IllegalArgumentException("bad challenge");
            }
            Grade grade = null;
            int lastScore = Integer.parseInt(p.getProperty("lastScore", "-1"));
            if (index >= 0 && lastScore >= 0) {
                if (lastScore > 100) {
                    throw new IllegalArgumentException("bad score");
                }
                grade = new Grade();
                grade.score = lastScore;
                grade.letter = letterGrade(lastScore);
                grade.passed = Boolean.parseBoolean(
                    p.getProperty("lastPassed", "false"));
                grade.itemCount = Integer.parseInt(p.getProperty("lastItems", "0"));
                grade.spentCents = Long.parseLong(p.getProperty("lastCents", "0"));
            }
            LinkedChain<Integer> bests = new LinkedChain<Integer>();
            for (int i = 0; i < bestScores.length; i++) {
                int score = Integer.parseInt(p.getProperty("best." + i, "-1"));
                bests.addToEnd(score);
                if (score < -1 || score > 100) {
                    throw new IllegalArgumentException("bad best score");
                }
            }
            int count = Integer.parseInt(p.getProperty("items", "0"));
            if (count < 0) {
                throw new IllegalArgumentException("bad item count");
            }
            GroceryBasket loaded = new GroceryBasket();
            for (int i = 0; i < count; i++) {
                String prefix = "item." + i + ".";
                String name = p.getProperty(prefix + "name");
                String category = p.getProperty(prefix + "category");
                String description = p.getProperty(prefix + "description");
                String priceText = p.getProperty(prefix + "price");
                if (name == null || category == null
                    || description == null || priceText == null) {
                    throw new IllegalArgumentException("missing item data");
                }
                double price = Double.parseDouble(priceText);
                if (!Double.isFinite(price) || price <= 0) {
                    throw new IllegalArgumentException("bad item price");
                }
                loaded.addItem(new GroceryItem(name, price, category, description));
            }
            int orderCount = Integer.parseInt(p.getProperty("orders", "0"));
            if (orderCount < 0 || orderCount > 1000) {
                throw new IllegalArgumentException("bad order count");
            }
            LinkedChain<Order> loadedOrders = new LinkedChain<Order>();
            for (int i = 0; i < orderCount; i++) {
                String prefix = "order." + i + ".";
                String date = p.getProperty(prefix + "date");
                String itemsText = p.getProperty(prefix + "items");
                String centsText = p.getProperty(prefix + "cents");
                if (date == null || itemsText == null || centsText == null) {
                    throw new IllegalArgumentException("missing order data");
                }
                int orderItems = Integer.parseInt(itemsText);
                long orderCents = Long.parseLong(centsText);
                if (orderItems < 0 || orderCents < 0) {
                    throw new IllegalArgumentException("bad order data");
                }
                loadedOrders.addToEnd(new Order(date, orderItems, orderCents,
                    p.getProperty(prefix + "note", "")));
            }
            while (loadedOrders.size() > MAX_HISTORY) {
                loadedOrders.remove();
            }
            // Everything was valid, so apply it.
            basket = loaded;
            budgetCents = Math.round(savedBudget * 100);
            activeIndex = index;
            activeChallenge = index >= 0 ? challenges.get(index) : null;
            lastGrade = grade;
            for (int i = 0; i < bests.size(); i++) {
                bestScores[i] = bests.get(i);
            }
            history.clear();
            for (int i = 0; i < loadedOrders.size(); i++) {
                history.addToEnd(loadedOrders.get(i));
            }
        }
        catch (IllegalArgumentException e) {
            // Damaged save file: ignore it and start with a fresh trip.
            return;
        }
    }

    // ------------------------------------------------------------------
    // Screens: layout helpers, menu, status, help
    // ------------------------------------------------------------------

    private static String fill(char ch, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    private static String rule() {
        return fill('-', WIDTH);
    }

    private static void header(String title) {
        StringBuilder sb = new StringBuilder("== ");
        sb.append(title).append(' ');
        while (sb.length() < WIDTH) {
            sb.append('=');
        }
        System.out.println();
        System.out.println(sb);
    }

    private static String itemWord(int count) {
        return count == 1 ? "item" : "items";
    }

    private static String money(long cents) {
        return String.format("$%.2f", cents / 100.0);
    }

    private static long cents(double dollars) {
        return Math.round(dollars * 100);
    }

    private void printTwo(String left, String right) {
        System.out.printf("  %-30s%s%n", left, right);
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println(fill('=', WIDTH));
        System.out.println("  GRAB 'N' GO GROCERIES");
        System.out.println(fill('=', WIDTH));
        printStatus();
        System.out.println(rule());
        printTwo("1) Shop by aisle", "6) Checkout");
        printTwo("2) Search for an item", "7) Order history");
        printTwo("3) View / edit basket", "8) New trip");
        printTwo("4) Budget", "9) Help");
        printTwo("5) Challenges", "0) Save & quit");
        System.out.println(rule());
        System.out.println(" Shortcuts: add milk 2 | undo | submit | help");
    }

    /** Prints the basket, budget and challenge in a few aligned lines. */
    private void printStatus() {
        int count = basket.getItemCount();
        System.out.printf(" Basket     %d %s | $%.2f%n",
            count, itemWord(count), basket.getTotalPrice());
        if (budgetCents > 0) {
            System.out.println(" Budget     " + budgetLine());
        }
        String challenge = challengeStatus();
        if (challenge != null) {
            System.out.println(" Challenge  " + challenge);
        }
    }

    /** Budget summary with a progress bar. Only call when a budget is set. */
    private String budgetLine() {
        long spent = totalCents();
        long left = budgetCents - spent;
        double fraction = Math.min(1.0, (double)spent / budgetCents);
        int filled = (int)Math.round(fraction * BAR_WIDTH);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < BAR_WIDTH; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        bar.append(']');
        String status = left >= 0
            ? money(left) + " left"
            : "OVER by " + money(-left);
        return money(budgetCents) + " " + bar + " " + status;
    }

    /** One-line challenge summary, or null when no challenge is running. */
    private String challengeStatus() {
        if (activeChallenge == null) {
            return null;
        }
        ShoppingChallenge c = activeChallenge;
        int count = basket.getItemCount();
        long spent = totalCents();
        StringBuilder sb = new StringBuilder(c.getName());
        sb.append(" | ").append(count).append('/').append(c.getMinimumItems())
            .append(" items | ")
            .append(money(spent)).append(" of ").append(money(cents(c.getMaxBudget())));
        if (spent > cents(c.getMaxBudget())) {
            sb.append(" | OVER BUDGET");
        }
        else if (count >= c.getMinimumItems()) {
            sb.append(" | READY - type 'submit'");
        }
        if (lastGrade != null) {
            sb.append(" | last: ").append(lastGrade.letter).append(' ')
                .append(lastGrade.score).append("/100");
            if (lastGradeIsStale()) {
                sb.append(" (basket changed)");
            }
        }
        return sb.toString();
    }

    private void printHelp() {
        header("HELP");
        System.out.println(" Anywhere   home = main menu | 0 or back = step back");
        System.out.println("            quit = save and exit | help = this screen");
        System.out.println(" Shopping   shop | search <word> | add milk 2 | remove milk");
        System.out.println("            or just type an item name, like: milk");
        System.out.println(" In a list  3 = pick #3 | 3x2 = two of #3 | milk 2");
        System.out.println("            next / prev = change aisle | dairy = jump to an aisle");
        System.out.println(" Basket     basket (view and edit) | undo | clear");
        System.out.println("            inside the basket: -2 remove one | -2 all | +2 add one");
        System.out.println(" Money      budget 40 | budget off | checkout | history");
        System.out.println(" Challenge  challenge | rubric (preview your score)");
        System.out.println("            submit (get graded) | abandon");
        System.out.println(" Trip       new = start over (history and best scores are kept)");
    }

    // ------------------------------------------------------------------
    // Input helpers
    // ------------------------------------------------------------------

    /**
     * Reads one line. Returns null when the user quits, goes home, or the
     * input runs out; otherwise returns the trimmed text (possibly empty).
     * "help" is answered right here and the prompt is shown again.
     */
    private String ask(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!input.hasNextLine()) {
                System.out.println();
                exitRequested = true;
                return null;
            }
            String text = input.nextLine().trim();
            String lower = text.toLowerCase();
            if (lower.equals("q") || lower.equals("quit") || lower.equals("exit")) {
                exitRequested = true;
                return null;
            }
            if (lower.equals("home") || lower.equals("menu")
                || lower.equals("m") || lower.equals("h")) {
                goHome = true;
                return null;
            }
            if (lower.equals("help") || lower.equals("?")) {
                printHelp();
                continue;
            }
            return text;
        }
    }

    private static boolean isBack(String text) {
        return text.equals("0") || text.equalsIgnoreCase("b")
            || text.equalsIgnoreCase("back") || text.equalsIgnoreCase("cancel");
    }

    private static int toInt(String text) {
        if (text.matches("\\d{1,6}")) {
            return Integer.parseInt(text);
        }
        return -1;
    }

    /**
     * Reads a whole number in [min, max]. Returns 0 if the user goes back
     * and -1 if they quit, went home, or the input ran out.
     */
    private int readNumber(String prompt, int min, int max) {
        while (true) {
            String text = ask(prompt);
            if (text == null) {
                return -1;
            }
            if (isBack(text)) {
                return 0;
            }
            int value = toInt(text);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("Please enter a number from " + min + " to "
                + max + ", or 0 to go back.");
        }
    }

    /**
     * Reads free text (names, search words). Returns null on "cancel" or when
     * the input ends. A blank answer uses the default, or asks again.
     */
    private String readText(String prompt, String defaultValue) {
        while (true) {
            System.out.print(prompt);
            if (!input.hasNextLine()) {
                System.out.println();
                exitRequested = true;
                return null;
            }
            String value = input.nextLine().trim();
            if (value.equalsIgnoreCase("cancel")) {
                return null;
            }
            if (!value.isEmpty()) {
                return value;
            }
            if (defaultValue != null) {
                return defaultValue;
            }
            System.out.println("Please enter something, or type cancel.");
        }
    }

    /** Reads a positive price. Returns NaN on "cancel" or when input ends. */
    private double readPrice() {
        while (true) {
            String text = readText("Price: $", null);
            if (text == null) {
                return Double.NaN;
            }
            if (text.startsWith("$")) {
                text = text.substring(1).trim();
            }
            try {
                double price = Double.parseDouble(text);
                if (Double.isFinite(price) && price >= 0.01 && price <= 10000) {
                    return price;
                }
            }
            catch (NumberFormatException e) {
                // Invalid text is handled by the message below.
            }
            System.out.println(
                "Price must be a positive amount like 2.50. Try again.");
        }
    }

    private boolean askYesNo(String prompt) {
        while (true) {
            String text = ask(prompt);
            if (text == null) {
                return false;
            }
            String lower = text.toLowerCase();
            if (lower.equals("y") || lower.equals("yes")) {
                return true;
            }
            if (lower.equals("n") || lower.equals("no") || isBack(lower)) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }

    /** Splits "milk 2" / "milk x2" into {"milk", "2"}; quantity may be "". */
    private static LinkedChain<String> splitQuantity(String text) {
        Matcher m = QUANTITY_SUFFIX.matcher(text.trim());
        if (m.matches()) {
            LinkedChain<String> result = new LinkedChain<String>();
            result.addToEnd(m.group(1).trim());
            result.addToEnd(m.group(2));
            return result;
        }
        LinkedChain<String> result = new LinkedChain<String>();
        result.addToEnd(text.trim());
        result.addToEnd("");
        return result;
    }

    private static int quantityOf(String digits) {
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    // ------------------------------------------------------------------
    // Basket helpers
    // ------------------------------------------------------------------

    private long totalCents() {
        return cents(basket.getTotalPrice());
    }

    private int countInBasket(GroceryItem item) {
        int count = 0;
        for (int i = 0; i < basket.getItemCount(); i++) {
            GroceryItem other = basket.getItem(i);
            if (other.equals(item)) {
                count++;
            }
        }
        return count;
    }

    private int countCategoryInBasket(String category) {
        int count = 0;
        for (int i = 0; i < basket.getItemCount(); i++) {
            GroceryItem item = basket.getItem(i);
            if (item.getCategory().trim().equalsIgnoreCase(category)) {
                count++;
            }
        }
        return count;
    }

    /** Groups identical items and sorts the lines without an array. */
    private LinkedChain<Line> basketLines() {
        LinkedChain<Line> unsorted = new LinkedChain<Line>();
        for (int i = 0; i < basket.getItemCount(); i++) {
            GroceryItem item = basket.getItem(i);
            Line found = null;
            for (int j = 0; j < unsorted.size(); j++) {
                Line candidate = unsorted.get(j);
                if (candidate.item.equals(item)) {
                    found = candidate;
                    break;
                }
            }
            if (found == null) {
                unsorted.addToEnd(new Line(item));
            }
            else {
                found.quantity++;
            }
        }
        LinkedChain<Line> sorted = new LinkedChain<Line>();
        while (!unsorted.isEmpty()) {
            Line smallest = unsorted.get(0);
            for (int i = 1; i < unsorted.size(); i++) {
                Line candidate = unsorted.get(i);
                if (compareLines(candidate, smallest) < 0) {
                    smallest = candidate;
                }
            }
            sorted.addToEnd(smallest);
            unsorted.remove(smallest);
        }
        return sorted;
    }

    private static int compareLines(Line a, Line b) {
        int byName = a.item.getName().compareToIgnoreCase(b.item.getName());
        if (byName != 0) {
            return byName;
        }
        return Double.compare(a.item.getPrice(), b.item.getPrice());
    }

    /** Saves an independent chain of item references for undo. */
    private void snapshot() {
        undoStack.add(basket.snapshotItems());
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        LinkedChain<GroceryItem> previous = undoStack.remove();
        GroceryBasket restoredBasket = new GroceryBasket();
        restoredBasket.restoreItems(previous);
        basket = restoredBasket;
        System.out.printf("Undid your last change. Basket: %d %s | $%.2f%n",
            basket.getItemCount(), itemWord(basket.getItemCount()),
            basket.getTotalPrice());
        reportBudget();
        save();
    }

    private void clearBasket() {
        if (basket.isEmpty()) {
            System.out.println("Your basket is already empty.");
            return;
        }
        snapshot();
        basket = new GroceryBasket();
        System.out.println("Basket cleared. Type 'undo' to bring it back.");
        save();
    }

    private void printBasketTable(LinkedChain<Line> lines) {
        System.out.println("  #  Item                 Qty    Each     Total");
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            long each = cents(l.item.getPrice());
            System.out.printf(" %2d  %-20s %3d  %7s  %8s%n", i + 1,
                l.item.getName(), l.quantity, money(each),
                money(each * l.quantity));
        }
        System.out.println(rule());
        int count = basket.getItemCount();
        System.out.printf(" %d %s | Basket total: $%.2f%n",
            count, itemWord(count), basket.getTotalPrice());
    }

    /** The basket screen: view it, and change it without leaving. */
    private void basketScreen() {
        while (!exitRequested && !goHome) {
            LinkedChain<Line> lines = basketLines();
            header("YOUR BASKET");
            if (lines.size() == 0) {
                System.out.println(
                    "Your basket is empty. Type 'shop' (or pick 1) to fill it.");
                return;
            }
            printBasketTable(lines);
            if (budgetCents > 0) {
                System.out.println(" Budget: " + budgetLine());
            }
            String challenge = challengeStatus();
            if (challenge != null) {
                System.out.println(" Challenge: " + challenge);
            }
            System.out.println(
                " Edit: -2 remove one | -2 all | +2 add one | clear | undo | 0 back");
            String text = ask("Edit basket: ");
            if (text == null) {
                return;
            }
            if (text.isEmpty()) {
                continue;
            }
            if (isBack(text)) {
                return;
            }
            String lower = text.toLowerCase();
            if (lower.equals("basket") || lower.equals("cart")
                || lower.equals("edit") || lower.equals("view")) {
                continue;
            }
            Matcher m = BASKET_EDIT.matcher(text);
            if (m.matches()) {
                editLine(lines, m.group(1), m.group(2), m.group(3));
            }
            else {
                typedCommand(text);
            }
        }
    }

    private void editLine(LinkedChain<Line> lines, String op, String lineText, String arg) {
        int number = Integer.parseInt(lineText);
        if (number < 1 || number > lines.size()) {
            System.out.println("There is no line " + number + ". Choose 1 to "
                + lines.size() + ".");
            return;
        }
        Line line = lines.get(number - 1);
        String o = op.toLowerCase();
        boolean adding = o.equals("+") || o.equals("a") || o.equals("add");
        boolean all = o.startsWith("d");
        int quantity = 1;
        if (arg != null) {
            if (arg.equalsIgnoreCase("all")) {
                all = true;
            }
            else {
                quantity = Integer.parseInt(arg);
            }
        }
        if (quantity < 1) {
            System.out.println("Use a quantity of 1 or more.");
        }
        else if (adding && all) {
            System.out.println("To add several, give a number: +" + number + " 3");
        }
        else if (adding) {
            addToBasket(line.item, quantity);
        }
        else {
            removeFromBasket(line, all ? line.quantity
                : Math.min(quantity, line.quantity));
        }
    }

    private void removeFromBasket(Line line, int quantity) {
        snapshot();
        int removed = 0;
        for (int i = 0; i < quantity; i++) {
            if (basket.removeItem(line.item)) {
                removed++;
            }
            else {
                break;
            }
        }
        if (removed == 0) {
            undoStack.remove();    // nothing changed, so drop the snapshot
            System.out.println("Could not remove that item.");
            return;
        }
        System.out.printf("Removed %d x %s.%n", removed, line.item.getName());
        System.out.printf("Basket: %d %s | $%.2f%n", basket.getItemCount(),
            itemWord(basket.getItemCount()), basket.getTotalPrice());
        reportBudget();
        reportChallenge(false);
        save();
    }

    /** "remove milk", "remove milk 2" or "remove milk all". */
    private void quickRemove(String rest) {
        if (rest.isEmpty()) {
            System.out.println("Usage: remove <item> [qty|all]   e.g. remove milk");
            System.out.println("(Or open the basket and use -2 to remove line 2.)");
            return;
        }
        String name;
        int quantity = 1;
        boolean all = false;
        if (rest.toLowerCase().matches(".*\\s+all")) {
            all = true;
            name = rest.substring(0, rest.length() - 3).trim();
        }
        else {
            LinkedChain<String> split = splitQuantity(rest);
            name = split.get(0);
            if (!split.get(1).isEmpty()) {
                quantity = Integer.parseInt(split.get(1));
            }
        }
        if (quantity < 1) {
            System.out.println("Use a quantity of 1 or more.");
            return;
        }
        Line line = findLine(basketLines(), name);
        if (line != null) {
            removeFromBasket(line, all ? line.quantity
                : Math.min(quantity, line.quantity));
        }
    }

    /** Finds a basket line by name; says why when it cannot pick one. */
    private Line findLine(LinkedChain<Line> lines, String name) {
        String needle = name.trim().toLowerCase();
        Line only = null;
        int matches = 0;
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            String lineName = line.item.getName().toLowerCase();
            if (lineName.equals(needle)) {
                return line;
            }
            if (lineName.contains(needle)) {
                only = line;
                matches++;
            }
        }
        if (matches == 1) {
            return only;
        }
        if (matches == 0) {
            System.out.println("Nothing named \"" + name + "\" in your basket.");
        }
        else {
            System.out.println("\"" + name + "\" matches more than one item. "
                + "Open the basket and use -2 style edits.");
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Shopping
    // ------------------------------------------------------------------

    /** Aisle menu: "What are you looking for?" */
    private void browseAisles() {
        int custom = AISLES.length + 1;
        while (!exitRequested && !goHome) {
            header("WHAT ARE YOU LOOKING FOR?");
            printStatus();
            System.out.println(rule());
            for (int i = 0; i < AISLES.length; i++) {
                int inBasket = countCategoryInBasket(AISLES[i]);
                System.out.printf(" %d) %-16s %s%n", i + 1, AISLES[i],
                    inBasket > 0 ? "(" + inBasket + " in basket)" : "");
            }
            System.out.println(" " + custom + ") Something else (add your own item)");
            System.out.println(" 0) Back");
            System.out.println(
                "Tip: type an aisle or item name, like \"dairy\" or \"milk 2\".");
            String text = ask("Choose an aisle: ");
            if (text == null) {
                return;
            }
            if (text.isEmpty()) {
                continue;
            }
            if (isBack(text)) {
                return;
            }
            int number = toInt(text);
            if (number >= 1 && number <= AISLES.length) {
                browseAisle(number - 1);
            }
            else if (number == custom) {
                addCustomItem();
            }
            else if (number >= 0) {
                System.out.println("Please pick 1 to " + custom + ", or 0 to go back.");
            }
            else {
                typedCommand(text);
            }
        }
    }

    private static LinkedChain<GroceryItem> aisleItems(int index) {
        LinkedChain<GroceryItem> result = new LinkedChain<GroceryItem>();
        for (int i = 0; i < CATALOG[index].length; i++) {
            result.addToEnd(CATALOG[index][i]);
        }
        return result;
    }

    /** Walks one aisle; "next", "prev" and aisle names move between aisles. */
    private void browseAisle(int start) {
        int current = start;
        while (!exitRequested && !goHome) {
            int jump = browseItems("AISLE " + (current + 1) + " OF "
                + AISLES.length + ": " + AISLES[current].toUpperCase(),
                aisleItems(current), false, current);
            if (jump < 0) {
                return;
            }
            current = jump;
        }
    }

    /**
     * Shows a list of items and adds whatever the user picks, staying on the
     * list so several things can be added in a row. Returns the index of an
     * aisle to jump to, or -1 when the user is done with the list.
     */
    private int browseItems(String title, LinkedChain<GroceryItem> items,
        boolean showCategory, int aisleIndex) {
        while (!exitRequested && !goHome) {
            header(title);
            printStatus();
            System.out.println(rule());
            printItemTable(items, showCategory);
            System.out.println(" 0) Back");
            if (aisleIndex >= 0) {
                System.out.println(
                    "Pick: 3 | 3x2 (two of #3) | milk 2 | next | prev | an aisle name");
            }
            else {
                System.out.println("Pick: 3 | 3x2 (two of #3) | milk 2 | an aisle name");
            }
            String text = ask("Pick an item: ");
            if (text == null) {
                return -1;
            }
            if (text.isEmpty()) {
                continue;
            }
            if (isBack(text)) {
                return -1;
            }
            String lower = text.toLowerCase();
            if (aisleIndex >= 0 && (lower.equals("next") || lower.equals("n"))) {
                return (aisleIndex + 1) % AISLES.length;
            }
            if (aisleIndex >= 0 && (lower.equals("prev") || lower.equals("p")
                || lower.equals("previous"))) {
                return (aisleIndex + AISLES.length - 1) % AISLES.length;
            }
            Matcher m = NUMBER_PICK.matcher(text);
            if (m.matches()) {
                int number = Integer.parseInt(m.group(1));
                if (number < 1 || number > items.size()) {
                    System.out.println("There is no item #" + number
                        + ". Pick 1 to " + items.size() + ", or 0 to go back.");
                }
                else {
                    pickItem(items.get(number - 1),
                        m.group(2) == null ? 0 : Integer.parseInt(m.group(2)));
                }
                continue;
            }
            LinkedChain<String> split = splitQuantity(text);
            int found = findInList(items, split.get(0));
            if (found >= 0) {
                pickItem(items.get(found), quantityOf(split.get(1)));
                continue;
            }
            int aisle = findAisle(text);
            if (aisle >= 0) {
                if (aisle != aisleIndex) {
                    return aisle;
                }
                continue;
            }
            typedCommand(text);
        }
        return -1;
    }

    private void printItemTable(LinkedChain<GroceryItem> items, boolean showCategory) {
        System.out.println("  #  Item              Price   Details");
        for (int i = 0; i < items.size(); i++) {
            int inBasket = countInBasket(items.get(i));
            String where = showCategory ? "[" + items.get(i).getCategory() + "] " : "";
            String mark = inBasket > 0 ? "  <- x" + inBasket + " in basket" : "";
            System.out.printf(" %2d) %-16s %6s  %s%s%s%n", i + 1,
                items.get(i).getName(), money(cents(items.get(i).getPrice())), where,
                items.get(i).getDescription(), mark);
        }
    }

    /** Index of the list item a typed name refers to, or -1. */
    private static int findInList(LinkedChain<GroceryItem> items, String name) {
        String needle = name.trim().toLowerCase();
        if (needle.isEmpty()) {
            return -1;
        }
        int onlyStart = -1;
        int starts = 0;
        int onlyContains = -1;
        int contains = 0;
        for (int i = 0; i < items.size(); i++) {
            String itemName = items.get(i).getName().toLowerCase();
            if (itemName.equals(needle)) {
                return i;
            }
            if (itemName.startsWith(needle)) {
                onlyStart = i;
                starts++;
            }
            else if (itemName.contains(needle)) {
                onlyContains = i;
                contains++;
            }
        }
        if (starts == 1) {
            return onlyStart;
        }
        if (starts == 0 && contains == 1) {
            return onlyContains;
        }
        return -1;
    }

    /** Index of the aisle a typed word names (3+ letters of its start), or -1. */
    private static int findAisle(String text) {
        String needle = text.trim().toLowerCase();
        if (needle.length() < 3) {
            return -1;
        }
        for (int i = 0; i < AISLES.length; i++) {
            if (AISLES[i].toLowerCase().startsWith(needle)) {
                return i;
            }
        }
        return -1;
    }

    /** Adds an item; a quantity of 0 means "ask how many". */
    private void pickItem(GroceryItem item, int quantity) {
        if (quantity > MAX_QUANTITY) {
            System.out.println("You can add up to " + MAX_QUANTITY + " at a time.");
            return;
        }
        int amount = quantity > 0 ? quantity : readQuantity(item);
        if (amount >= 1) {
            addToBasket(item, amount);
        }
    }

    /** Asks how many. Enter means 1; returns 0 if cancelled. */
    private int readQuantity(GroceryItem item) {
        StringBuilder hint = new StringBuilder();
        if (budgetCents > 0) {
            long left = budgetCents - totalCents();
            long price = Math.max(1, cents(item.getPrice()));
            hint.append("budget fits ").append(left > 0 ? left / price : 0);
        }
        if (activeChallenge != null) {
            int need = activeChallenge.getMinimumItems() - basket.getItemCount();
            if (hint.length() > 0) {
                hint.append(", ");
            }
            hint.append(need > 0
                ? "challenge needs " + need + " more" : "challenge minimum met");
        }
        if (hint.length() > 0) {
            System.out.println("  (" + hint + ")");
        }
        while (true) {
            String text = ask("How many " + item.getName()
                + "? [1] (0 to cancel): ");
            if (text == null || isBack(text)) {
                return 0;
            }
            if (text.isEmpty()) {
                return 1;
            }
            int value = toInt(text);
            if (value >= 1 && value <= MAX_QUANTITY) {
                return value;
            }
            System.out.println("Enter a number from 1 to " + MAX_QUANTITY
                + ", press Enter for 1, or 0 to cancel.");
        }
    }

    /** Adds copies of an item and reports on the budget and challenge. */
    private void addToBasket(GroceryItem item, int quantity) {
        boolean wasMet = challengeMet();
        snapshot();
        for (int i = 0; i < quantity; i++) {
            basket.addItem(item);
        }
        System.out.printf("Added %d x %s ($%.2f each).%n",
            quantity, item.getName(), item.getPrice());
        System.out.printf("Basket: %d %s | $%.2f%n", basket.getItemCount(),
            itemWord(basket.getItemCount()), basket.getTotalPrice());
        reportBudget();
        reportChallenge(wasMet);
        save();
    }

    /** "add milk", "add milk 2", or just "milk" (which asks how many). */
    private void quickAdd(String rest, boolean explicit) {
        if (rest.isEmpty()) {
            System.out.println("Usage: add <item> [qty]   e.g. add milk 2");
            return;
        }
        LinkedChain<String> split = splitQuantity(rest);
        int quantity = quantityOf(split.get(1));
        LinkedChain<GroceryItem> matches = catalogMatches(split.get(0));
        if (matches.size() == 0) {
            System.out.println("No items match \"" + split.get(0) + "\". "
                + "Try 'shop' to browse, 'help' for commands, or add your own "
                + "item from the aisle menu.");
            return;
        }
        if (quantity > MAX_QUANTITY) {
            System.out.println("You can add up to " + MAX_QUANTITY + " at a time.");
            return;
        }
        if (matches.size() == 1) {
            pickItem(matches.get(0), quantity == 0 && explicit ? 1 : quantity);
            return;
        }
        int jump = browseItems("MATCHES FOR \"" + split.get(0) + "\"",
            matches, true, -1);
        if (jump >= 0) {
            browseAisle(jump);
        }
    }

    /** Best catalog matches for a typed name: exact, then name, then anything. */
    private static LinkedChain<GroceryItem> catalogMatches(String term) {
        String needle = term.trim().toLowerCase();
        LinkedChain<GroceryItem> exact = new LinkedChain<GroceryItem>();
        LinkedChain<GroceryItem> byName = new LinkedChain<GroceryItem>();
        LinkedChain<GroceryItem> broad = new LinkedChain<GroceryItem>();
        for (GroceryItem[] aisle : CATALOG) {
            for (GroceryItem item : aisle) {
                String name = item.getName().toLowerCase();
                if (name.equals(needle)) {
                    exact.addToEnd(item);
                }
                else if (name.contains(needle)) {
                    byName.addToEnd(item);
                }
                else if (matchesAnything(item, needle)) {
                    broad.addToEnd(item);
                }
            }
        }
        LinkedChain<GroceryItem> best = !exact.isEmpty() ? exact
            : !byName.isEmpty() ? byName : broad;
        return best;
    }

    private static boolean matchesAnything(GroceryItem item, String needle) {
        return item.getName().toLowerCase().contains(needle)
            || item.getDescription().toLowerCase().contains(needle)
            || item.getCategory().toLowerCase().contains(needle);
    }

    private void searchPrompt() {
        String term = readText("Search for an item (or 'cancel'): ", null);
        if (term != null) {
            searchFor(term);
        }
    }

    /** Lists every catalog item matching a name, description or aisle. */
    private void searchFor(String term) {
        String needle = term.toLowerCase();
        LinkedChain<GroceryItem> matches = new LinkedChain<GroceryItem>();
        for (GroceryItem[] aisle : CATALOG) {
            for (GroceryItem item : aisle) {
                if (matchesAnything(item, needle)) {
                    matches.addToEnd(item);
                }
            }
        }
        if (matches.isEmpty()) {
            System.out.println("No items match \"" + term + "\". "
                + "Try another word, or type 'shop' to browse the aisles.");
            return;
        }
        int jump = browseItems("RESULTS FOR \"" + term + "\"",
            matches, true, -1);
        if (jump >= 0) {
            browseAisle(jump);
        }
    }

    /** The type-it-yourself flow for items not in the catalog. */
    private void addCustomItem() {
        System.out.println("(Type 'cancel' at any prompt to go back.)");
        String name = readText("Item name: ", null);
        if (name == null) {
            return;
        }
        double price = readPrice();
        if (Double.isNaN(price)) {
            return;
        }
        String category = readText("Category (Enter for Other): ", "Other");
        if (category == null) {
            return;
        }
        String description = readText("Description (Enter to skip): ",
            "Custom item");
        if (description == null) {
            return;
        }
        GroceryItem item = new GroceryItem(name, price, category, description);
        pickItem(item, 0);
    }

    // ------------------------------------------------------------------
    // Budget
    // ------------------------------------------------------------------

    /** Prints how much budget remains (or a warning) after a change. */
    private void reportBudget() {
        if (budgetCents <= 0) {
            return;
        }
        long left = budgetCents - totalCents();
        if (left >= 0) {
            System.out.printf("Budget left: %s%n", money(left));
        }
        else {
            System.out.printf("Warning: you're %s over your budget!%n",
                money(-left));
        }
    }

    private void budgetScreen() {
        header("BUDGET");
        if (budgetCents > 0) {
            System.out.println(" Current: " + budgetLine());
        }
        else {
            System.out.println(" No budget set.");
        }
        System.out.println(
            " Enter an amount like 40, 'off' to remove it, or 0 to go back.");
        while (true) {
            String text = ask("Budget: $");
            if (text == null || isBack(text)) {
                return;
            }
            if (applyBudget(text)) {
                return;
            }
            System.out.println(
                "Budget must be a positive amount like 40 or 25.50 (or 'off').");
        }
    }

    /** "budget" alone opens the screen; "budget 40" and "budget off" work too. */
    private void budgetCommand(String rest) {
        if (rest.isEmpty()) {
            budgetScreen();
        }
        else if (!applyBudget(rest)) {
            System.out.println(
                "Budget must be a positive amount like 40 or 25.50 (or 'off').");
        }
    }

    private boolean applyBudget(String text) {
        String t = text.trim().toLowerCase();
        if (t.startsWith("$")) {
            t = t.substring(1).trim();
        }
        if (t.equals("off") || t.equals("none") || t.equals("clear")
            || t.equals("0")) {
            budgetCents = 0;
            System.out.println("Budget removed.");
            save();
            return true;
        }
        try {
            double value = Double.parseDouble(t);
            if (Double.isFinite(value) && value >= 0.01 && value <= MAX_BUDGET) {
                budgetCents = cents(value);
                System.out.printf("Budget set to %s.%n", money(budgetCents));
                reportBudget();
                save();
                return true;
            }
        }
        catch (NumberFormatException e) {
            // Invalid text is handled by the caller.
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Challenges and the scoring rubric
    // ------------------------------------------------------------------

    private static String letterGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    /** True when the basket meets the active challenge's requirements. */
    private boolean challengeMet() {
        return activeChallenge != null
            && basket.getItemCount() >= activeChallenge.getMinimumItems()
            && totalCents() <= cents(activeChallenge.getMaxBudget());
    }

    private boolean lastGradeIsStale() {
        return lastGrade != null
            && (lastGrade.itemCount != basket.getItemCount()
                || lastGrade.spentCents != totalCents());
    }

    /** Prints progress toward the active challenge after a basket change. */
    private void reportChallenge(boolean wasMet) {
        if (activeChallenge == null) {
            return;
        }
        ShoppingChallenge c = activeChallenge;
        int need = c.getMinimumItems() - basket.getItemCount();
        long left = cents(c.getMaxBudget()) - totalCents();
        if (left < 0) {
            System.out.printf(
                "Challenge: you're %s over the challenge budget. "
                + "Remove something to pass!%n", money(-left));
        }
        else if (need > 0) {
            System.out.printf(
                "Challenge: %d more %s to go, %s of challenge budget left.%n",
                need, itemWord(need), money(left));
        }
        else if (!wasMet) {
            System.out.println("Challenge: requirements met! Type 'submit' to "
                + "get graded, or keep shopping to raise your score.");
        }
        else {
            System.out.println("Challenge: requirements met - type 'submit' "
                + "when you're happy with your basket.");
        }
    }

    private void challengeHub() {
        while (!exitRequested && !goHome) {
            if (activeChallenge == null) {
                int index = pickChallenge();
                if (index < 0 || !beginChallenge(index)) {
                    return;
                }
                continue;
            }
            header("CHALLENGE: " + activeChallenge.getName().toUpperCase());
            printChallengeCard();
            System.out.println(rule());
            System.out.println(" 1) See score breakdown (preview, not submitted)");
            System.out.println(" 2) Submit for grading");
            System.out.println(" 3) Switch to a different challenge");
            System.out.println(" 4) Abandon this challenge");
            System.out.println(" 5) Keep shopping");
            System.out.println(" 0) Back");
            int choice = readNumber("Choose: ", 0, 5);
            switch (choice) {
                case 1:
                    previewRubric();
                    break;
                case 2:
                    submitChallenge();
                    break;
                case 3:
                    int index = pickChallenge();
                    if (index >= 0) {
                        beginChallenge(index);
                    }
                    break;
                case 4:
                    if (abandonChallenge()) {
                        return;
                    }
                    break;
                case 5:
                    browseAisles();
                    break;
                default:
                    return;
            }
        }
    }

    /** Lists the challenges. Returns the chosen index, or -1 for none. */
    private int pickChallenge() {
        header("CHOOSE A CHALLENGE");
        for (int i = 0; i < challenges.size(); i++) {
            ShoppingChallenge c = challenges.get(i);
            System.out.printf(" %d) %-13s %s%n", i + 1, c.getName(),
                c.getDescription());
            String best = bestScores[i] >= 0
                ? "   Best: " + letterGrade(bestScores[i])
                    + " (" + bestScores[i] + "/100)"
                : "";
            System.out.printf("      Buy at least %d items for $%.2f or less.%s%n",
                c.getMinimumItems(), c.getMaxBudget(), best);
        }
        System.out.println(" 0) Back");
        int choice = readNumber("Choose a challenge: ", 0, challenges.size());
        return choice - 1;
    }

    /** Starts a challenge. Returns false if the user cancelled. */
    private boolean beginChallenge(int index) {
        if (!basket.isEmpty()) {
            System.out.printf("You already have %d %s in your basket ($%.2f).%n",
                basket.getItemCount(), itemWord(basket.getItemCount()),
                basket.getTotalPrice());
            System.out.println(" 1) Keep them - they count toward the challenge");
            System.out.println(" 2) Start with an empty basket (you can 'undo' this)");
            System.out.println(" 0) Cancel");
            int keep = readNumber("Choose: ", 0, 2);
            if (keep <= 0) {
                return false;
            }
            if (keep == 2) {
                snapshot();
                basket = new GroceryBasket();
            }
        }
        activeIndex = index;
        activeChallenge = challenges.get(index);
        lastGrade = null;
        System.out.println("Challenge started: " + activeChallenge.getName());
        reportChallenge(false);
        save();
        return true;
    }

    private boolean abandonChallenge() {
        if (activeChallenge == null) {
            System.out.println("No challenge is running.");
            return false;
        }
        if (!askYesNo("Abandon " + activeChallenge.getName()
            + "? Your basket stays as it is. (y/n): ")) {
            System.out.println("Okay, the challenge is still on.");
            return false;
        }
        System.out.println("Challenge abandoned. Your basket is untouched.");
        activeChallenge = null;
        activeIndex = -1;
        lastGrade = null;
        save();
        return true;
    }

    private void printChallengeCard() {
        ShoppingChallenge c = activeChallenge;
        System.out.println(" " + c.getDescription());
        System.out.printf(" Goal:      at least %d items for $%.2f or less%n",
            c.getMinimumItems(), c.getMaxBudget());
        System.out.printf(" Graded on: items %d | in budget %d | savings %d"
            + " | variety %d%n",
            ITEM_POINTS, BUDGET_POINTS, SAVINGS_POINTS, VARIETY_POINTS);
        System.out.printf(" Right now: %d/%d items | %s of %s%n",
            basket.getItemCount(), c.getMinimumItems(), money(totalCents()),
            money(cents(c.getMaxBudget())));
        if (lastGrade != null) {
            System.out.printf(" Last submitted: %s (%d/100)%s%n", lastGrade.letter,
                lastGrade.score, lastGradeIsStale() ? " - basket has changed" : "");
        }
        if (bestScores[activeIndex] >= 0) {
            System.out.printf(" Best: %s (%d/100)%n",
                letterGrade(bestScores[activeIndex]), bestScores[activeIndex]);
        }
    }

    /** Distinct basket categories, in the order they first appear. */
    private LinkedChain<String> distinctCategories() {
        LinkedChain<String> names = new LinkedChain<String>();
        LinkedChain<String> keys = new LinkedChain<String>();
        for (int i = 0; i < basket.getItemCount(); i++) {
            GroceryItem item = basket.getItem(i);
            String name = item.getCategory().trim();
            String key = name.toLowerCase();
            if (!keys.contains(key)) {
                keys.addToEnd(key);
                names.addToEnd(name);
            }
        }
        return names;
    }

    /** Scores the basket against a challenge. */
    private Grade computeGrade(ShoppingChallenge c) {
        Grade g = new Grade();
        int count = basket.getItemCount();
        long spent = totalCents();
        long max = cents(c.getMaxBudget());
        boolean itemsMet = count >= c.getMinimumItems();
        boolean within = spent <= max;
        g.itemCount = count;
        g.spentCents = spent;
        g.passed = itemsMet && within;
        g.itemPoints = itemsMet ? ITEM_POINTS
            : (int)Math.round((double)ITEM_POINTS * count / c.getMinimumItems());
        g.budgetPoints = g.passed ? BUDGET_POINTS : 0;
        if (g.passed) {
            int savings = (int)Math.round(
                (double)SAVINGS_POINTS * (max - spent) / max);
            g.savingsPoints = Math.max(0, Math.min(SAVINGS_POINTS, savings));
        }
        g.categories = distinctCategories().size();
        g.varietyPoints = Math.min(VARIETY_POINTS,
            g.categories * POINTS_PER_CATEGORY);
        g.score = g.itemPoints + g.budgetPoints + g.savingsPoints
            + g.varietyPoints;
        g.letter = letterGrade(g.score);
        return g;
    }

    private void previewRubric() {
        if (activeChallenge == null) {
            System.out.println("No challenge is running. Type 'challenge' to pick one.");
            return;
        }
        printRubric(computeGrade(activeChallenge), false);
    }

    private void submitChallenge() {
        if (activeChallenge == null) {
            System.out.println("No challenge is running. Type 'challenge' to pick one.");
            return;
        }
        ShoppingChallenge c = activeChallenge;
        int need = c.getMinimumItems() - basket.getItemCount();
        if (need > 0) {
            System.out.printf("Not ready to submit: you need %d more %s "
                + "(the minimum is %d).%n", need, itemWord(need),
                c.getMinimumItems());
            printRubric(computeGrade(c), false);
            return;
        }
        Grade g = computeGrade(c);
        if (!g.passed && !askYesNo("You're over the challenge budget, so this "
            + "will FAIL. Submit anyway? (y/n): ")) {
            System.out.println("Okay - remove something and submit again.");
            return;
        }
        printRubric(g, true);
        lastGrade = g;
        if (g.passed) {
            System.out.println("  Challenge completed! Great job!");
            int best = bestScores[activeIndex];
            if (g.score > best) {
                if (best >= 0) {
                    System.out.println("  New personal best!");
                }
                bestScores[activeIndex] = g.score;
            }
        }
        else {
            System.out.printf("  Challenge failed: over budget by %s.%n",
                money(g.spentCents - cents(c.getMaxBudget())));
        }
        System.out.println("  Keep shopping and type 'submit' again to improve, "
            + "or 'challenge' to try another.");
        save();
    }

    private static String mark(int points, int max) {
        if (points >= max) {
            return "x";
        }
        return points > 0 ? "~" : " ";
    }

    private void printRubric(Grade g, boolean official) {
        ShoppingChallenge c = activeChallenge;
        long max = cents(c.getMaxBudget());
        boolean itemsMet = g.itemCount >= c.getMinimumItems();
        System.out.println();
        System.out.println(fill('=', WIDTH));
        System.out.println(official
            ? "  CHALLENGE GRADE: " + c.getName()
            : "  SCORE PREVIEW: " + c.getName() + " (not submitted)");
        System.out.println(fill('=', WIDTH));

        System.out.printf("  [%s] %-15s %2d/%-2d  %d of %d items%n",
            mark(g.itemPoints, ITEM_POINTS), "Item minimum", g.itemPoints,
            ITEM_POINTS, g.itemCount, c.getMinimumItems());

        String budgetNote;
        if (g.spentCents > max) {
            budgetNote = money(g.spentCents) + " of " + money(max)
                + " - OVER by " + money(g.spentCents - max);
        }
        else if (!itemsMet) {
            budgetNote = money(g.spentCents) + " of " + money(max)
                + " - counts once you have enough items";
        }
        else {
            budgetNote = money(g.spentCents) + " of " + money(max);
        }
        System.out.printf("  [%s] %-15s %2d/%-2d  %s%n",
            mark(g.budgetPoints, BUDGET_POINTS), "Stay in budget",
            g.budgetPoints, BUDGET_POINTS, budgetNote);

        String savingsNote = g.passed
            ? Math.round(100.0 * (max - g.spentCents) / max)
                + "% of the budget left over"
            : "unlocks once you pass";
        System.out.printf("  [%s] %-15s %2d/%-2d  %s%n",
            mark(g.savingsPoints, SAVINGS_POINTS), "Savings bonus",
            g.savingsPoints, SAVINGS_POINTS, savingsNote);

        LinkedChain<String> categories = distinctCategories();
        System.out.printf("  [%s] %-15s %2d/%-2d  %d %s%s%n",
            mark(g.varietyPoints, VARIETY_POINTS), "Variety bonus",
            g.varietyPoints, VARIETY_POINTS, g.categories,
            g.categories == 1 ? "category" : "categories",
            g.categories == 0 ? "" : " (" + joinNames(categories) + ")");

        System.out.println(rule());
        System.out.printf("  SCORE: %d/100   GRADE: %s   %s%n", g.score, g.letter,
            g.passed ? (official ? "PASSED" : "PASSING")
                : (official ? "FAILED" : "NOT PASSING YET"));
        printTips(g, c, categories);
        System.out.println(fill('=', WIDTH));
    }

    private static String joinNames(LinkedChain<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    /** Aisles that are not represented in the basket yet (up to a few). */
    private static String missingAisles(LinkedChain<String> have, int limit) {
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (String aisle : AISLES) {
            boolean present = false;
            for (int i = 0; i < have.size(); i++) {
                if (have.get(i).equalsIgnoreCase(aisle)) {
                    present = true;
                }
            }
            if (!present && shown < limit) {
                if (shown > 0) {
                    sb.append(", ");
                }
                sb.append(aisle);
                shown++;
            }
        }
        return sb.toString();
    }

    private void printTips(Grade g, ShoppingChallenge c,
        LinkedChain<String> categories) {
        LinkedChain<String> tips = new LinkedChain<String>();
        long max = cents(c.getMaxBudget());
        int need = c.getMinimumItems() - g.itemCount;
        if (need > 0) {
            tips.addToEnd("Add " + need + " more " + itemWord(need)
                + " to reach the minimum of " + c.getMinimumItems()
                + " (+" + (ITEM_POINTS - g.itemPoints) + " points, and it "
                + "unlocks the budget and savings points).");
        }
        if (g.spentCents > max) {
            tips.addToEnd("You're " + money(g.spentCents - max) + " over. "
                + "Remove about that much (try 'undo' or swap in cheaper items) "
                + "to earn the " + BUDGET_POINTS + " budget points and pass.");
        }
        if (g.varietyPoints < VARIETY_POINTS) {
            int needed = (VARIETY_POINTS - g.varietyPoints + POINTS_PER_CATEGORY - 1)
                / POINTS_PER_CATEGORY;
            String missing = missingAisles(categories, 3);
            tips.addToEnd("Shop " + needed + " more "
                + (needed == 1 ? "category" : "categories") + " for +"
                + (VARIETY_POINTS - g.varietyPoints) + " points"
                + (missing.isEmpty() ? "." : " (try: " + missing + ")."));
        }
        if (g.passed && g.savingsPoints < SAVINGS_POINTS) {
            tips.addToEnd("Spend less to keep more of the budget: every 5% left "
                + "over is worth 1 savings point.");
        }
        if (g.passed && g.score < 90) {
            int target = g.score < 70 ? 70 : g.score < 80 ? 80 : 90;
            String next = letterGrade(target);
            tips.addToEnd((target - g.score) + " more "
                + (target - g.score == 1 ? "point" : "points") + " for "
                + ("A".equals(next) ? "an " : "a ") + next + ".");
        }
        System.out.println();
        if (tips.isEmpty()) {
            System.out.println("  Perfect basket - nothing left to improve!");
            return;
        }
        System.out.println("  How to improve:");
        for (int i = 0; i < tips.size(); i++) {
            System.out.println("   - " + tips.get(i));
        }
    }

    // ------------------------------------------------------------------
    // Checkout, history, new trip
    // ------------------------------------------------------------------

    private void checkout() {
        if (basket.isEmpty()) {
            System.out.println("Your basket is empty. Add something first.");
            return;
        }
        if (activeChallenge != null
            && basket.getItemCount() >= activeChallenge.getMinimumItems()
            && (lastGrade == null || lastGradeIsStale())) {
            if (askYesNo("You have a challenge in progress. Grade it before "
                + "checking out? (y/n): ")) {
                submitChallenge();
            }
            if (exitRequested || goHome) {
                return;
            }
        }
        header("CHECKOUT");
        printBasketTable(basketLines());
        long subtotal = totalCents();
        long tax = Math.round(subtotal * TAX_RATE);
        System.out.printf("Subtotal: %s%n", money(subtotal));
        System.out.printf("Estimated tax (%d%%): %s%n",
            (int)Math.round(TAX_RATE * 100), money(tax));
        System.out.printf("Total: %s%n", money(subtotal + tax));
        if (budgetCents > 0) {
            System.out.println("Budget: " + budgetLine()
                + "  (tracked before tax)");
        }
        String note = "";
        if (activeChallenge != null) {
            if (lastGrade == null) {
                note = activeChallenge.getName() + ": not submitted";
            }
            else {
                note = activeChallenge.getName() + ": " + lastGrade.letter
                    + " (" + lastGrade.score + "/100)"
                    + (lastGrade.passed ? "" : " failed");
            }
            System.out.println("Challenge " + note);
        }
        if (!askYesNo("Complete checkout? (y/n): ")) {
            System.out.println("Okay, keep shopping.");
            return;
        }
        history.addToEnd(new Order(LocalDate.now().toString(),
            basket.getItemCount(), subtotal + tax, note));
        while (history.size() > MAX_HISTORY) {
            history.remove();
        }
        System.out.println("Checkout complete. Enjoy your groceries!");
        resetTrip();
        System.out.println("A fresh trip is ready - keep shopping, or type "
            + "'history' to see your past orders.");
    }

    private void showHistory() {
        header("ORDER HISTORY");
        if (history.isEmpty()) {
            System.out.println(" No orders yet. Check out to record your first trip.");
        }
        else {
            int number = 1;
            for (int i = history.size() - 1; i >= 0; i--) {
                Order order = history.get(i);
                System.out.printf(" %2d) %s  %2d %-5s $%7.2f  %s%n", number,
                    order.date, order.items, itemWord(order.items),
                    order.cents / 100.0, order.note);
                number++;
            }
        }
        System.out.println(rule());
        System.out.println(" Challenge records:");
        for (int i = 0; i < challenges.size(); i++) {
            System.out.printf("   %-13s %s%n", challenges.get(i).getName(),
                bestScores[i] >= 0
                    ? letterGrade(bestScores[i]) + " (" + bestScores[i] + "/100)"
                    : "not passed yet");
        }
    }

    /** Clears the basket, budget and challenge (history and bests are kept). */
    private void resetTrip() {
        basket = new GroceryBasket();
        budgetCents = 0;
        activeChallenge = null;
        activeIndex = -1;
        lastGrade = null;
        undoStack.clear();
        save();
    }

    private void newTrip() {
        if (askYesNo("Clear your basket, budget and challenge and start "
            + "fresh? (y/n): ")) {
            resetTrip();
            System.out.println("Fresh start! Your basket is empty "
                + "(order history and best scores are kept).");
        }
        else {
            System.out.println("Okay, nothing was cleared.");
        }
    }
}