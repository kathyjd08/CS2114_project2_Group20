package grabnNGoGroceries;

import java.util.Scanner;

/** Runs the text-based Grab n' Go Groceries application. */
public class GroceryApp {
    private GroceryBasket basket;
    private ShoppingChallenge activeChallenge;
    private LinkedChain<ShoppingChallenge> challenges;
    private Scanner input;

    public GroceryApp() {
        this(new Scanner(System.in));
    }

    /** Allows a supplied scanner so console input can be tested. */
    public GroceryApp(Scanner scanner) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        input = scanner;
        basket = new GroceryBasket();
        challenges = new LinkedChain<ShoppingChallenge>();
        challenges.addToEnd(new ShoppingChallenge("Movie Night", "Four items under $25", 25.0, 4));
        challenges.addToEnd(new ShoppingChallenge("Breakfast", "Three items under $20", 20.0, 3));
        challenges.addToEnd(new ShoppingChallenge("Budget Basket", "Five items under $40", 40.0, 5));
    }

    public static void main(String[] args) {
        new GroceryApp().run();
    }

    public void run() {
        boolean running = true;
        while (running && input.hasNextLine()) {
            displayMenu();
            String choice = input.nextLine().trim();
            switch (choice) {
                case "1": addItem(); break;
                case "2": removeItem(); break;
                case "3": displayBasket(); break;
                case "4": startChallenge(); break;
                case "5": checkChallenge(); break;
                case "6": System.out.println("Thanks for shopping!"); running = false; break;
                default: System.out.println("Invalid menu option. Please try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n--- Grab n' Go Groceries ---");
        System.out.println("1. Add grocery item");
        System.out.println("2. Remove grocery item");
        System.out.println("3. View basket");
        System.out.println("4. Start challenge");
        System.out.println("5. Check challenge");
        System.out.println("6. Exit");
        System.out.print("Choose an option: ");
    }

    private String readNonEmpty(String prompt) {
        while (input.hasNextLine()) {
            System.out.print(prompt);
            String value = input.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Input cannot be empty. Try again.");
        }
        return null;
    }

    private double readPositivePrice() {
        while (input.hasNextLine()) {
            System.out.print("Price: $");
            String value = input.nextLine().trim();
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
        return Double.NaN;
    }

    private void addItem() {
        String name = readNonEmpty("Item name: ");
        if (name == null) return;
        double price = readPositivePrice();
        if (Double.isNaN(price)) return;
        String category = readNonEmpty("Category: ");
        if (category == null) return;
        String description = readNonEmpty("Description: ");
        if (description == null) return;
        basket.addItem(new GroceryItem(name, price, category, description));
        System.out.println("Item added!");
    }

    private void removeItem() {
        if (basket.isEmpty()) {
            System.out.println("Your basket is empty. Nothing to remove.");
            return;
        }
        System.out.println(basket);
        String name = readNonEmpty("Name of item to remove: ");
        if (name == null) return;
        // Use the actual stored object so its full details match equals().
        // GroceryBasket needs a findByName method for this operation.
        GroceryItem item = basket.findByName(name);
        if (item == null) {
            System.out.println("Item not found.");
        }
        else if (basket.removeItem(item)) {
            System.out.println("Removed one copy of " + name + ".");
        }
        else {
            System.out.println("Item not found.");
        }
    }

    private void displayBasket() {
        System.out.println("\n--- Your Basket ---");
        System.out.println(basket);
