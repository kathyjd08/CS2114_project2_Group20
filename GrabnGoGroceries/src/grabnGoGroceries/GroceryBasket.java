package grabnGoGroceries;

/** A shopping basket stored in a generic LinkedChain of GroceryItem. */
public class GroceryBasket {
    private LinkedChain<GroceryItem> items;

    public GroceryBasket() {
        items = new LinkedChain<GroceryItem>();
    }

    /** Adds an item. Duplicate items are allowed. */
    public void addItem(GroceryItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        items.addToEnd(item);
    }

    /** Removes one matching copy, or returns false if none is found. */
    public boolean removeItem(GroceryItem item) {
        return items.remove(item);
    }

    /** Finds a stored item by its name, ignoring capitalization. */
    public GroceryItem findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (GroceryItem item : items.toArray(new GroceryItem[items.size()])) {
            if (item.getName() != null
                && item.getName().equalsIgnoreCase(name.trim())) {
                return item;
            }
        }
        return null;
    }

public GroceryItem[] getItems() {
    return items.toArray(new GroceryItem[items.size()]);
}


    public int getItemCount() {
        return items.size();
    }

    public double getTotalPrice() {
        double total = 0;
        for (GroceryItem item : items.toArray(new GroceryItem[items.size()])) {
            total += item.getPrice();
        }
        return total;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public String toString() {
        if (items.isEmpty()) {
            return "Basket is empty.";
        }
        StringBuilder listing = new StringBuilder();
        GroceryItem[] entries = items.toArray(new GroceryItem[items.size()]);
        for (int i = 0; i < entries.length; i++) {
            listing.append(i + 1).append(". ").append(entries[i]);
            if (i < entries.length - 1) {
                listing.append(System.lineSeparator());
            }
        }
        return listing.toString();
    }
}
