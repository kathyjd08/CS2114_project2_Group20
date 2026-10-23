package grabnGoGroceries;

import static org.junit.Assert.*;
import org.junit.Test;

/** Tests grocery item data, equality, and display. */
public class GroceryItemTest {
    @Test
    public void testGettersNormal() {
        GroceryItem apple = new GroceryItem("Apple", 3.99, "Produce", "Red apple");
        assertEquals("Apple", apple.getName());
        assertEquals(3.99, apple.getPrice(), 0.0001);
        assertEquals("Produce", apple.getCategory());
        assertEquals("Red apple", apple.getDescription());
    }

    @Test
    public void testGettersEdgeZeroPrice() {
        // GroceryApp validates price before making a GroceryItem.
        GroceryItem free = new GroceryItem("Sample", 0, "Other", "Free sample");
        assertEquals(0.0, free.getPrice(), 0.0001);
    }

    @Test
    public void testEqualObjectsAndHashCode() {
        GroceryItem a = new GroceryItem("Apple", 2, "Produce", "Fresh");
        GroceryItem b = new GroceryItem("Apple", 2, "Produce", "Fresh");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, a);
    }

    @Test
    public void testEqualsDifferentOrInvalidObjects() {
        GroceryItem a = new GroceryItem("Apple", 2, "Produce", "Fresh");
        assertNotEquals(a, new GroceryItem("Milk", 2, "Dairy", "Fresh"));
        assertNotEquals(a, null);
        assertNotEquals(a, "Apple");
    }

    @Test
    public void testToString() {
        GroceryItem a = new GroceryItem("Apple", 2.5, "Produce", "Red");
        assertTrue(a.toString().contains("Apple"));
        assertTrue(a.toString().contains("$2.50"));
        assertTrue(a.toString().contains("Produce"));
    }

    @Test
    public void testToStringEmptyText() {
        GroceryItem a = new GroceryItem("", 0, "", "");
        assertNotNull(a.toString());
    }
}
