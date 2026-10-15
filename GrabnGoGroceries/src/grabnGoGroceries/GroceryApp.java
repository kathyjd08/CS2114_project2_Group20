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
import java.util.Properties;
import java.util.Scanner;

/** Console application for Grab 'n' Go Groceries. */
public class GroceryApp {
    private static final double TAX_RATE = 0.06;
    private static final int BAR_WIDTH = 20;

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

    private final Scanner input;
    private final File saveFile;    // null means progress is not saved
    private GroceryBasket basket;
    private final LinkedChain<ShoppingChallenge> challenges;
    private final int[] bestScores; // best passing score per challenge, -1 = none
    private ShoppingChallenge activeChallenge;
    private int activeIndex;        // index of activeChallenge, -1 = none
    private boolean challengeDone;  // true once the challenge has been graded
    private boolean challengePassed;
    private int challengeScore;
    private String challengeGrade;
    private double budget;          // 0 means no budget is set
    private boolean exitRequested;  // true once the user asks to leave
    private boolean restored;       // true if a saved trip was loaded

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
        this.activeChallenge = null;
        this.activeIndex = -1;
        this.challengeDone = false;
        this.challengePassed = false;
        this.challengeScore = 0;
        this.challengeGrade = "";
        this.budget = 0;
        this.exitRequested = false;

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

    public void run() {
        if (restored) {
            System.out.println("Welcome back! Picking up where you left off.");
        }
        while (!exitRequested) {
            printMenu();
            System.out.print("Choose an option: ");
            if (!input.hasNextLine()) {
                break;
            }
            String choice = input.nextLine().trim().toLowerCase();
            switch (choice) {
                case "1":
                    browseAisles();
                    break;
                case "2":
                    searchItems();
                    break;
                case "3":
                    displayBasket();
                    break;
                case "4":
                    removeItem();
                    break;
                case "5":
                    setBudget();
                    break;
                case "6":
                    startChallenge();
                    break;
                case "7":
                    checkChallenge();
                    break;
                case "8":
                    checkout();
                    break;
                case "9":
                    newTrip();
                    break;
                case "0":
                case "q":
                case "exit":
                    exitRequested = true;
                    break;
                default:
                    System.out.println("Invalid menu option. Try again.");
            }
        }
        save();
        if (saveFile != null && hasProgress()) {
            System.out.println(
                "Your trip is saved. Run the app again to pick up where you left off.");
        }
        System.out.println("Thanks for shopping!");
    }

    // ------------------------------------------------------------------
    // Saving and loading
    // ------------------------------------------------------------------

    private boolean hasProgress() {
        return !basket.isEmpty() || budget > 0 || activeIndex >= 0;
    }

    private void save() {
        if (saveFile == null) {
            return;
        }
        Properties p = new Properties();
        p.setProperty("budget", Double.toString(budget));
        p.setProperty("challenge", Integer.toString(activeIndex));
        p.setProperty("challengeDone", Boolean.toString(challengeDone));
        p.setProperty("challengePassed", Boolean.toString(challengePassed));
        p.setProperty("challengeScore", Integer.toString(challengeScore));
        p.setProperty("challengeGrade", challengeGrade);
        for (int i = 0; i < bestScores.length; i++) {
            p.setProperty("best." + i, Integer.toString(bestScores[i]));
        }
        GroceryItem[] items = basket.getItems();
        p.setProperty("items", Integer.toString(items.length));
        for (int i = 0; i < items.length; i++) {
            String prefix = "item." + i + ".";
            p.setProperty(prefix + "name", items[i].getName());
            p.setProperty(prefix + "price", Double.toString(items[i].getPrice()));
            p.setProperty(prefix + "category", items[i].getCategory());
            p.setProperty(prefix + "description", items[i].getDescription());
        }
        try (Writer writer = new OutputStreamWriter(
            new FileOutputStream(saveFile), StandardCharsets.UTF_8)) {
            p.store(writer, "Grab 'n' Go Groceries saved trip");
        }
        catch (IOException e) {
            System.out.println("(Could not save your progress.)");
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
            if (!Double.isFinite(savedBudget) || savedBudget < 0) {
                throw new IllegalArgumentException("bad budget");
            }
            int index = Integer.parseInt(p.getProperty("challenge", "-1"));
            if (index < -1 || index >= challenges.size()) {
                throw new IllegalArgumentException("bad challenge");
            }
            boolean done = index >= 0
                && Boolean.parseBoolean(p.getProperty("challengeDone", "false"));
            boolean passed = done
                && Boolean.parseBoolean(p.getProperty("challengePassed", "false"));
            int score = Integer.parseInt(p.getProperty("challengeScore", "0"));
            String grade = p.getProperty("challengeGrade", "");
            int[] bests = new int[bestScores.length];
            for (int i = 0; i < bests.length; i++) {
                bests[i] = Integer.parseInt(p.getProperty("best." + i, "-1"));
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
            // Everything was valid, so apply it.
            basket = loaded;
            budget = savedBudget;
            activeIndex = index;
            activeChallenge = index >= 0 ? challenges.get(index) : null;
            challengeDone = done;
            challengePassed = passed;
            challengeScore = score;
            challengeGrade = grade;
            for (int i = 0; i < bests.length; i++) {
                bestScores[i] = bests[i];
            }
        }
        catch (IllegalArgumentException e) {
            // Damaged save file: ignore it and start with a fresh trip.
            return;
        }
    }

    // ------------------------------------------------------------------
    // Menus and status display
    // ------------------------------------------------------------------

    private void printMenu() {
        System.out.println("\n=== Grab 'n' Go Groceries ===");
        printStatus();
        System.out.println("1. Shop by aisle");
        System.out.println("2. Search for an item");
        System.out.println("3. View basket");
        System.out.println("4. Remove an item");
        System.out.println("5. Set budget");
        System.out.println("6. Start a challenge");
        System.out.println("7. Check challenge");
        System.out.println("8. Checkout");
        System.out.println("9. New trip (clear everything)");
        System.out.println("0. Exit   (Q works in any menu; your trip is saved)");
    }

    private String itemWord(int count) {
        return count == 1 ? "item" : "items";
    }

    private void printStatus() {
        int count = basket.getItemCount();
        System.out.printf("Basket: %d %s | $%.2f%n",
            count, itemWord(count), basket.getTotalPrice());
        if (budget > 0) {
            System.out.println(budgetLine());
        }
        if (activeChallenge != null) {
            if (challengeDone) {
                System.out.printf("Challenge: %s | %s - Grade %s (%d/100)%n",
                    activeChallenge.getName(),
                    challengePassed ? "PASSED" : "FAILED",
                    challengeGrade, challengeScore);
            }
            else {
                System.out.printf("Challenge: %s | %d/%d items | $%.2f of $%.2f%n",
                    activeChallenge.getName(), count,
                    activeChallenge.getMinimumItems(),
                    basket.getTotalPrice(), activeChallenge.getMaxBudget());
            }
        }
    }

    /** Budget summary with a progress bar. Only call when budget > 0. */
    private String budgetLine() {
        long spentCents = Math.round(basket.getTotalPrice() * 100);
        long budgetCents = Math.round(budget * 100);
        long leftCents = budgetCents - spentCents;
        double fraction = Math.min(1.0, (double)spentCents / budgetCents);
        int filled = (int)Math.round(fraction * BAR_WIDTH);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < BAR_WIDTH; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        bar.append(']');
        String status;
        if (leftCents >= 0) {
            status = String.format("$%.2f left", leftCents / 100.0);
        }
        else {
            status = String.format("OVER by $%.2f", -leftCents / 100.0);
        }
        return String.format("Budget: $%.2f | %s %s", budget, bar, status);
    }

    /** Prints how much budget remains (or a warning) after a change. */
    private void reportBudget() {
        if (budget <= 0) {
            return;
        }
        long leftCents = Math.round(budget * 100)
            - Math.round(basket.getTotalPrice() * 100);
        if (leftCents >= 0) {
            System.out.printf("Budget left: $%.2f%n", leftCents / 100.0);
        }
        else {
            System.out.printf("Warning: you're $%.2f over your budget!%n",
                -leftCents / 100.0);
        }
    }

    /** Prints progress toward the active, ungraded challenge. */
    private void reportChallenge() {
        if (!challengeActive()) {
            return;
        }
        int need = activeChallenge.getMinimumItems() - basket.getItemCount();
        long leftCents = Math.round(activeChallenge.getMaxBudget() * 100)
            - Math.round(basket.getTotalPrice() * 100);
        if (leftCents >= 0) {
            System.out.printf(
                "Challenge: %d more %s to go, $%.2f of challenge budget left.%n",
                need, itemWord(need), leftCents / 100.0);
        }
        else {
            System.out.printf(
                "Challenge warning: you're $%.2f over the challenge budget. "
                + "Remove something to pass!%n", -leftCents / 100.0);
        }
    }

    // ------------------------------------------------------------------
    // Input helpers
    // ------------------------------------------------------------------

    /**
     * Reads a whole number in [min, max]. Returns 0 if the user goes back
     * (0, B, back, cancel) or types Q (which also sets exitRequested), and
     * -1 if the input runs out.
     */
    private int readChoice(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!input.hasNextLine()) {
                return -1;
            }
            String text = input.nextLine().trim();
            if (text.equalsIgnoreCase("q")) {
                exitRequested = true;
                return 0;
            }
            if (text.equalsIgnoreCase("b") || text.equalsIgnoreCase("back")
                || text.equalsIgnoreCase("cancel")) {
                return 0;
            }
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            }
            catch (NumberFormatException e) {
                // Invalid text is handled by the message below.
            }
            System.out.println("Please enter a number from " + min
                + " to " + max + " (or Q to exit).");
        }
    }

    /** Reads non-empty text. Returns null on "cancel" or when input ends. */
    private String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!input.hasNextLine()) {
                return null;
            }
            String value = input.nextLine().trim();
            if (value.equalsIgnoreCase("cancel")) {
                return null;
            }
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Input cannot be empty. Try again.");
        }
    }

    /** Reads a positive price. Returns NaN on "cancel" or when input ends. */
    private double readPositivePrice() {
        while (true) {
            System.out.print("Price: $");
            if (!input.hasNextLine()) {
                return Double.NaN;
            }
            String value = input.nextLine().trim();
            if (value.equalsIgnoreCase("cancel")) {
                return Double.NaN;
            }
            try {
                double price = Double.parseDouble(value);
                if (Double.isFinite(price) && price > 0) {
                    return price;
                }
            }
            catch (NumberFormatException e) {
                // Invalid text is handled by the message below.
            }
            System.out.println("Price must be a positive number. Try again.");
        }
    }

    private boolean askYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!input.hasNextLine()) {
                return false;
            }
            String text = input.nextLine().trim().toLowerCase();
            if (text.equals("y") || text.equals("yes")) {
                return true;
            }
            if (text.equals("n") || text.equals("no")) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }

    // ------------------------------------------------------------------
    // Shopping
    // ------------------------------------------------------------------

    /** Aisle menu: "What are you looking for?" */
    private void browseAisles() {
        int customChoice = AISLES.length + 1;
        while (!exitRequested) {
            System.out.println("\n--- What are you looking for? ---");
            printStatus();
            for (int i = 0; i < AISLES.length; i++) {
                System.out.printf("%d. %s%n", i + 1, AISLES[i]);
            }
            System.out.println(customChoice
                + ". Something else (enter your own item)");
            System.out.println("0. Back to main menu");
            int choice = readChoice("Choose an aisle: ", 0, customChoice);
            if (choice <= 0) {
                return;
            }
            if (choice == customChoice) {
                addCustomItem();
            }
            else {
                browseItems(AISLES[choice - 1], CATALOG[choice - 1], false);
            }
        }
    }

    /** Shows a list of items with prices and adds what the user picks. */
    private void browseItems(String title, GroceryItem[] items,
        boolean showCategory) {
        while (!exitRequested) {
            System.out.println("\n--- " + title + " ---");
            printStatus();
            for (int i = 0; i < items.length; i++) {
                String extra = showCategory
                    ? items[i].getCategory() + ": " : "";
                System.out.printf("%d. %-16s $%5.2f  %s%s%n", i + 1,
                    items[i].getName(), items[i].getPrice(), extra,
                    items[i].getDescription());
            }
            System.out.println("0. Back");
            int choice = readChoice("Pick an item: ", 0, items.length);
            if (choice <= 0) {
                return;
            }
            GroceryItem item = items[choice - 1];
            int quantity = readQuantity(item);
            if (quantity < 1) {
                continue;
            }
            addToBasket(item, quantity);
        }
    }

    private int readQuantity(GroceryItem item) {
        StringBuilder prompt = new StringBuilder("How many? (1-99");
        if (budget > 0) {
            long leftCents = Math.round(budget * 100)
                - Math.round(basket.getTotalPrice() * 100);
            long priceCents = Math.max(1, Math.round(item.getPrice() * 100));
            long fits = leftCents > 0 ? leftCents / priceCents : 0;
            prompt.append(", your budget fits ").append(fits);
        }
        if (challengeActive()) {
            int need = activeChallenge.getMinimumItems() - basket.getItemCount();
            prompt.append(", challenge needs ").append(need).append(" more");
        }
        prompt.append(", 0 to cancel): ");
        return readChoice(prompt.toString(), 0, 99);
    }

    /**
     * Adds copies of an item. During a challenge, adding stops at the
     * challenge's item limit and the basket is graded right away.
     */
    private void addToBasket(GroceryItem item, int quantity) {
        int toAdd = quantity;
        boolean capped = false;
        if (challengeActive()) {
            int room = activeChallenge.getMinimumItems() - basket.getItemCount();
            if (toAdd > room) {
                toAdd = Math.max(room, 0);
                capped = true;
            }
        }
        for (int i = 0; i < toAdd; i++) {
            basket.addItem(item);
        }
        if (toAdd > 0) {
            System.out.printf("Item added! %d x %s ($%.2f each)%n",
                toAdd, item.getName(), item.getPrice());
        }
        if (capped) {
            System.out.printf("Challenge limit reached: added only %d of %d.%n",
                toAdd, quantity);
        }
        reportBudget();
        if (!gradeIfReached()) {
            reportChallenge();
        }
        save();
    }

    /** The type-it-yourself flow for items not in the catalog. */
    private void addCustomItem() {
        System.out.println("(Type 'cancel' at any prompt to go back.)");
        String name = readNonEmpty("Item name: ");
        if (name == null) return;
        double price = readPositivePrice();
        if (Double.isNaN(price)) return;
        String category = readNonEmpty("Category: ");
        if (category == null) return;
        String description = readNonEmpty("Description: ");
        if (description == null) return;
        addToBasket(new GroceryItem(name, price, category, description), 1);
    }

    /** Finds catalog items by name, description or aisle. */
    private void searchItems() {
        String term = readNonEmpty("Search for an item (or 'cancel'): ");
        if (term == null) {
            return;
        }
        String needle = term.toLowerCase();
        LinkedChain<GroceryItem> matches = new LinkedChain<GroceryItem>();
        for (GroceryItem[] aisle : CATALOG) {
            for (GroceryItem item : aisle) {
                if (item.getName().toLowerCase().contains(needle)
                    || item.getDescription().toLowerCase().contains(needle)
                    || item.getCategory().toLowerCase().contains(needle)) {
                    matches.addToEnd(item);
                }
            }
        }
        if (matches.isEmpty()) {
            System.out.println("No items match \"" + term + "\".");
            return;
        }
        browseItems("Search results for \"" + term + "\"",
            matches.toArray(new GroceryItem[matches.size()]), true);
    }

    private void removeItem() {
        if (basket.isEmpty()) {
            System.out.println("Your basket is empty. Nothing to remove.");
            return;
        }
        System.out.println(basket);
        String name = readNonEmpty("Name of item to remove (or 'cancel'): ");
        if (name == null) return;
        GroceryItem item = basket.findByName(name);
        if (item == null) {
            System.out.println("Item not found.");
        }
        else if (basket.removeItem(item)) {
            System.out.println("Removed one copy of " + name + ".");
            reportBudget();
            reportChallenge();
            save();
        }
        else {
            System.out.println("Item not found.");
        }
    }

    private void displayBasket() {
        System.out.println("\n--- Your Basket ---");
        System.out.println(basket);
        System.out.println("Number of items: " + basket.getItemCount());
        System.out.printf("Total price: $%.2f%n", basket.getTotalPrice());
        if (budget > 0) {
            System.out.println(budgetLine());
        }
    }

    // ------------------------------------------------------------------
    // Budget, challenges, grading, checkout
    // ------------------------------------------------------------------

    private void setBudget() {
        System.out.println("\n--- Set Budget ---");
        if (budget > 0) {
            System.out.printf("Current budget: $%.2f%n", budget);
        }
        System.out.println(
            "Enter an amount, 0 to clear your budget, or 'cancel' to go back.");
        while (true) {
            System.out.print("Budget: $");
            if (!input.hasNextLine()) {
                return;
            }
            String text = input.nextLine().trim();
            if (text.equalsIgnoreCase("cancel")) {
                return;
            }
            try {
                double value = Double.parseDouble(text);
                if (value == 0) {
                    budget = 0;
                    System.out.println("Budget cleared.");
                    save();
                    return;
                }
                if (Double.isFinite(value) && value >= 0.01) {
                    budget = value;
                    System.out.printf("Budget set to $%.2f.%n", budget);
                    reportBudget();
                    save();
                    return;
                }
            }
            catch (NumberFormatException e) {
                // Invalid text is handled by the message below.
            }
            System.out.println(
                "Budget must be a positive number (or 0 for none). Try again.");
        }
    }

    /** True while a challenge is running and has not been graded yet. */
    private boolean challengeActive() {
        return activeChallenge != null && !challengeDone;
    }

    private static String letterGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    private void startChallenge() {
        System.out.println("\n--- Available Challenges ---");
        for (int i = 0; i < challenges.size(); i++) {
            ShoppingChallenge challenge = challenges.get(i);
            String best = bestScores[i] >= 0
                ? String.format("  [best: %s %d/100]",
                    letterGrade(bestScores[i]), bestScores[i])
                : "";
            System.out.printf("%d. %s - at least %d items, budget $%.2f (%s)%s%n",
                i + 1, challenge.getName(), challenge.getMinimumItems(),
                challenge.getMaxBudget(), challenge.getDescription(), best);
        }
        System.out.println("0. Back");
        int choice = readChoice("Choose a challenge: ", 0, challenges.size());
        if (choice <= 0) {
            return;
        }
        if (!basket.isEmpty()
            && askYesNo("Start with an empty basket? (y/n): ")) {
            basket = new GroceryBasket();
        }
        activeIndex = choice - 1;
        activeChallenge = challenges.get(activeIndex);
        challengeDone = false;
        challengePassed = false;
        challengeScore = 0;
        challengeGrade = "";
        System.out.println("Challenge started: " + activeChallenge.getName());
        if (!gradeIfReached()) {
            reportChallenge();
        }
        save();
    }

    /** Grades the challenge as soon as the basket hits its item limit. */
    private boolean gradeIfReached() {
        if (!challengeActive()
            || basket.getItemCount() < activeChallenge.getMinimumItems()) {
            return false;
        }
        gradeChallenge();
        return true;
    }

    private int countCategories() {
        LinkedChain<String> seen = new LinkedChain<String>();
        for (GroceryItem item : basket.getItems()) {
            String key = item.getCategory().trim().toLowerCase();
            if (!seen.contains(key)) {
                seen.add(key);
            }
        }
        return seen.size();
    }

    /**
     * Scores the basket out of 100: 60 for staying within budget, up to 20
     * for money left over, and up to 20 for variety (5 per category).
     */
    private void gradeChallenge() {
        ShoppingChallenge c = activeChallenge;
        double total = basket.getTotalPrice();
        boolean passed = c.isCompleted(basket);

        int budgetPoints = passed ? 60 : 0;
        int savingsPoints = 0;
        if (passed) {
            savingsPoints = (int)Math.round(
                20.0 * (c.getMaxBudget() - total) / c.getMaxBudget());
            savingsPoints = Math.max(0, Math.min(20, savingsPoints));
        }
        int categories = countCategories();
        int varietyPoints = Math.min(20, categories * 5);
        int score = budgetPoints + savingsPoints + varietyPoints;
        String grade = letterGrade(score);

        System.out.println("\n==============================");
        System.out.println("  CHALLENGE GRADE: " + c.getName());
        System.out.println("==============================");
        System.out.printf("Items:  %d (needed %d)%n",
            basket.getItemCount(), c.getMinimumItems());
        System.out.printf("Spent:  $%.2f of $%.2f%n", total, c.getMaxBudget());
        System.out.printf("Stay within budget ..... %2d/60%n", budgetPoints);
        System.out.printf("Savings bonus .......... %2d/20%n", savingsPoints);
        System.out.printf("Variety bonus .......... %2d/20 (%d %s)%n",
            varietyPoints, categories,
            categories == 1 ? "category" : "categories");
        System.out.println("------------------------------");
        System.out.printf("Score: %d/100   Grade: %s%n", score, grade);
        if (passed) {
            System.out.println("Challenge completed! Great job!");
            if (score > bestScores[activeIndex]) {
                if (bestScores[activeIndex] >= 0) {
                    System.out.println("New personal best!");
                }
                bestScores[activeIndex] = score;
            }
        }
        else {
            System.out.printf("Challenge failed: over budget by $%.2f.%n",
                total - c.getMaxBudget());
            System.out.println(
                "Start the challenge again from the menu to retry.");
        }
        challengeDone = true;
        challengePassed = passed;
        challengeScore = score;
        challengeGrade = grade;
    }

    private void checkChallenge() {
        if (activeChallenge == null) {
            System.out.println("Start a challenge first.");
            return;
        }
        if (challengeDone) {
            if (challengePassed) {
                System.out.printf("Challenge completed! %s - Grade %s (%d/100)%n",
                    activeChallenge.getName(), challengeGrade, challengeScore);
            }
            else {
                System.out.printf("Challenge failed: %s - Grade %s (%d/100)%n",
                    activeChallenge.getName(), challengeGrade, challengeScore);
                System.out.println(
                    "Start it again from the challenge menu to retry.");
            }
            return;
        }
        System.out.println("Challenge not completed yet.");
        System.out.printf("Need at least %d items and a total of $%.2f or less.%n",
            activeChallenge.getMinimumItems(), activeChallenge.getMaxBudget());
        System.out.printf("Progress: %d/%d items, $%.2f spent.%n",
            basket.getItemCount(), activeChallenge.getMinimumItems(),
            basket.getTotalPrice());
    }

    private void checkout() {
        if (basket.isEmpty()) {
            System.out.println("Your basket is empty. Add something first.");
            return;
        }
        System.out.println("\n--- Checkout ---");
        System.out.println(basket);
        double subtotal = basket.getTotalPrice();
        double tax = Math.round(subtotal * TAX_RATE * 100) / 100.0;
        System.out.printf("Subtotal: $%.2f%n", subtotal);
        System.out.printf("Estimated tax (%d%%): $%.2f%n",
            (int)Math.round(TAX_RATE * 100), tax);
        System.out.printf("Total: $%.2f%n", subtotal + tax);
        if (budget > 0) {
            System.out.println(budgetLine());
            System.out.println("(Budget is tracked before tax.)");
        }
        if (activeChallenge != null) {
            if (challengeDone) {
                System.out.printf("Challenge %s: %s, Grade %s (%d/100)%n",
                    activeChallenge.getName(),
                    challengePassed ? "COMPLETED!" : "failed.",
                    challengeGrade, challengeScore);
            }
            else {
                System.out.println("Challenge " + activeChallenge.getName()
                    + ": not completed.");
            }
        }
        if (askYesNo("Complete checkout and exit? (y/n): ")) {
            System.out.println("Checkout complete. Enjoy your groceries!");
            resetTrip();
            exitRequested = true;
        }
        else {
            System.out.println("Okay, keep shopping.");
        }
    }

    /** Clears the basket, budget and challenge (best scores are kept). */
    private void resetTrip() {
        basket = new GroceryBasket();
        budget = 0;
        activeChallenge = null;
        activeIndex = -1;
        challengeDone = false;
        challengePassed = false;
        challengeScore = 0;
        challengeGrade = "";
        save();
    }

    private void newTrip() {
        if (askYesNo(
            "Clear your basket, budget and challenge and start fresh? (y/n): ")) {
            resetTrip();
            System.out.println("Fresh start! Your basket is empty.");
        }
        else {
            System.out.println("Okay, nothing was cleared.");
        }
    }
}
