package grabnGoGroceries;

/** A shopping basket stored in a generic LinkedChain of GroceryItem. */
public class GroceryBasket {
    private LinkedChain<GroceryItem> items;

    public GroceryBasket() {
        items = new LinkedChain<GroceryItem>();
    }

    public void addItem(GroceryItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        items.addToEnd(item);
    }

    public boolean removeItem(GroceryItem item) {
        return items.remove(item);
    }

    public GroceryItem findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (int i = 0; i < items.size(); i++) {
            GroceryItem item = items.get(i);
            if (item.getName() != null
                && item.getName().equalsIgnoreCase(name.trim())) {
                return item;
            }
        }
        return null;
    }

    public GroceryItem getItem(int index) {
        return items.get(index);
    }

    public int countOf(GroceryItem item) {
        return items.count(item);
    }

    /** Returns a separate chain with the same item references, in order. */
    public LinkedChain<GroceryItem> snapshotItems() {
        LinkedChain<GroceryItem> copy = new LinkedChain<GroceryItem>();
        for (int i = 0; i < items.size(); i++) {
            copy.addToEnd(items.get(i));
        }
        return copy;
    }

    /** Restores the basket from a chain without sharing its nodes. */
    public void restoreItems(LinkedChain<GroceryItem> source) {
        if (source == null) {
            throw new IllegalArgumentException("Items cannot be null");
        }
        LinkedChain<GroceryItem> copy = new LinkedChain<GroceryItem>();
        for (int i = 0; i < source.size(); i++) {
            copy.addToEnd(source.get(i));
        }
        items = copy;
    }

    public int getItemCount() {
        return items.size();
    }

    public double getTotalPrice() {
        double total = 0;
        for (int i = 0; i < items.size(); i++) {
            total += items.get(i).getPrice();
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
        for (int i = 0; i < items.size(); i++) {
            listing.append(i + 1).append(". ").append(items.get(i));
            if (i < items.size() - 1) {
                listing.append(System.lineSeparator());
            }
        }
        return listing.toString();
    }
}
