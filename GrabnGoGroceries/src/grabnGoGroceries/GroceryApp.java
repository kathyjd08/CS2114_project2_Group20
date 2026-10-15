package grabnGoGroceries;

import java.util.Scanner;

/** Console application for Grab 'n' Go Groceries. */
public class GroceryApp {
    private final Scanner input;
    private final GroceryBasket basket;
    private final LinkedChain<ShoppingChallenge> challenges;
    private ShoppingChallenge activeChallenge;

    public GroceryApp(Scanner input) {
        if (input == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        this.input = input;
        this.basket = new GroceryBasket();
        this.challenges = new LinkedChain<ShoppingChallenge>();
        this.activeChallenge = null;

        challenges.addToEnd(new ShoppingChallenge(
            "Movie Night", "Get snacks for movie night", 25.00, 4));
        challenges.addToEnd(new ShoppingChallenge(
            "Weekly Shop", "Stock up for the week", 60.00, 8));
        challenges.addToEnd(new ShoppingChallenge(
            "Party Prep", "Feed a crowd", 100.00, 15));
    }

    public static void main(String[] args) {
        new GroceryApp(new Scanner(System.in)).run();
    }

    public void run() {
        while (true) {
            printMenu();
            System.out.print("Choose an option: ");
            if (!input.hasNextLine()) {
                return;
            }
            String choice = input.nextLine().trim();
            switch (choice) {
                case "1":
                    addItem();
                    break;
                case "2":
                    removeItem();
                    break;
                case "3":
                    displayBasket();
                    break;
                case "4":
                    startChallenge();
                    break;
                case "5":
                    checkChallenge();
                    break;
                case "6":
                    System.out.println("Thanks for shopping!");
                    return;
                default:
                    System.out.println("Invalid menu option. Try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== Grab 'n' Go Groceries ===");
        System.out.println("1. Add item");
        System.out.println("2. Remove item");
        System.out.println("3. Display basket");
        System.out.println("4. Start a challenge");
        System.out.println("5. Check challenge");
        System.out.println("6. Exit");
    }

    private String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!input.hasNextLine()) {
                return null;
            }
            String value = input.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Input cannot be empty. Try again.");
        }
    }

    private double readPositivePrice() {
        while (true) {
            System.out.print("Price: $");
            if (!input.hasNextLine()) {
                return Double.NaN;
            }
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
        System.out.println("Number of items: " + basket.getItemCount());
        System.out.printf("Total price: $%.2f%n", basket.getTotalPrice());
    }

    private void startChallenge() {
        System.out.println("\n--- Available Challenges ---");
        for (int i = 0; i < challenges.size(); i++) {
            ShoppingChallenge challenge = challenges.get(i);
            System.out.printf("%d. %s - at least %d items, budget $%.2f%n",
                i + 1, challenge.getName(), challenge.getMinimumItems(),
                challenge.getMaxBudget());
        }
        while (true) {
            System.out.print("Choose a challenge (1-" + challenges.size() + "): ");
            if (!input.hasNextLine()) {
                return;
            }
            String choice = input.nextLine().trim();
            try {
                int index = Integer.parseInt(choice) - 1;
                if (index >= 0 && index < challenges.size()) {
                    activeChallenge = challenges.get(index);
                    System.out.println("Challenge started: "
                        + activeChallenge.getName());
                    return;
                }
            }
            catch (NumberFormatException e) {
                // Invalid selection handled below.
            }
            System.out.println("Invalid challenge selection. Try again.");
        }
    }

    private void checkChallenge() {
        if (activeChallenge == null) {
            System.out.println("Start a challenge first.");
        }
        else if (activeChallenge.isCompleted(basket)) {
            System.out.println("Challenge completed! Great job!");
        }
        else {
            System.out.println("Challenge not completed yet.");
            System.out.printf("Need at least %d items and a total of $%.2f or less.%n",
                activeChallenge.getMinimumItems(), activeChallenge.getMaxBudget());
        }
    }
}