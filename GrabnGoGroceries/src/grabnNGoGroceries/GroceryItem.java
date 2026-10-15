package grabnNGoGroceries;

import java.util.Objects;

/** A grocery item with a name, price, category, and description. */
public class GroceryItem {
    private String name;
    private double price;
    private String category;
    private String description;

    public GroceryItem(String name, double price, String category,
        String description) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    /** Items match when all their details are equal. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GroceryItem)) {
            return false;
        }
        GroceryItem item = (GroceryItem)other;
        return Objects.equals(name, item.name)
            && Double.compare(price, item.price) == 0
            && Objects.equals(category, item.category)
            && Objects.equals(description, item.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price, category, description);
    }

    @Override
    public String toString() {
        return String.format("%s - $%.2f (%s): %s",
            name, price, category, description);
    }
}
