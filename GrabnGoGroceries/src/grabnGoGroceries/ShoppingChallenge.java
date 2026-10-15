package grabnGoGroceries;

/** A challenge with a maximum budget and minimum item count. */
public class ShoppingChallenge {
    private String name;
    private String description;
    private double maxBudget;
    private int minimumItems;

    public ShoppingChallenge(String name, String description,
        double maxBudget, int minimumItems) {
        if (name == null || name.trim().isEmpty()
            || !Double.isFinite(maxBudget) || maxBudget <= 0
            || minimumItems < 1) {
            throw new IllegalArgumentException("Invalid challenge requirements");
        }
        this.name = name;
        this.description = description;
        this.maxBudget = maxBudget;
        this.minimumItems = minimumItems;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getMaxBudget() {
        return maxBudget;
    }

    public int getMinimumItems() {
        return minimumItems;
    }

    /** Checks the SAME basket used during regular shopping. */
    public boolean isCompleted(GroceryBasket basket) {
        return basket != null
            && basket.getItemCount() >= minimumItems
            && basket.getTotalPrice() <= maxBudget;
    }
}
