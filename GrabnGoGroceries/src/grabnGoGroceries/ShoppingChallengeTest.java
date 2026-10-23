package grabnGoGroceries;

import static org.junit.Assert.*;
import org.junit.Test;

/** Tests challenge requirements and completion. */
public class ShoppingChallengeTest {
    @Test
    public void testGetters() {
        ShoppingChallenge c = new ShoppingChallenge("Movie Night", "Get snacks", 25, 4);
        assertEquals("Movie Night", c.getName());
        assertEquals("Get snacks", c.getDescription());
        assertEquals(25, c.getMaxBudget(), 0.0001);
        assertEquals(4, c.getMinimumItems());
    }

    @Test
    public void testGettersBoundaryValues() {
        ShoppingChallenge c = new ShoppingChallenge("Small", "", 0.01, 1);
        assertEquals("", c.getDescription());
        assertEquals(0.01, c.getMaxBudget(), 0.0001);
        assertEquals(1, c.getMinimumItems());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectInvalidBudget() {
        new ShoppingChallenge("Bad", "", -1, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectInvalidName() {
        new ShoppingChallenge(" ", "", 25, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectInvalidMinimum() {
        new ShoppingChallenge("Bad", "", 25, 0);
    }

    @Test
    public void testCompletedWithinBudget() {
        GroceryBasket basket = new GroceryBasket();
        for (int i = 0; i < 4; i++) {
            basket.addItem(new GroceryItem("Apple", 5, "Produce", "Fresh"));
        }
        assertTrue(new ShoppingChallenge("Movie", "", 25, 4).isCompleted(basket));
        assertTrue(new ShoppingChallenge("Exact", "", 20, 4).isCompleted(basket));
    }

    @Test
    public void testNotCompletedBadBasket() {
        ShoppingChallenge c = new ShoppingChallenge("Movie", "", 25, 4);
        assertFalse(c.isCompleted(null));
        GroceryBasket basket = new GroceryBasket();
        assertFalse(c.isCompleted(basket));
        for (int i = 0; i < 4; i++) {
            basket.addItem(new GroceryItem("Bread", 7, "Bakery", "Loaf"));
        }
        assertFalse(c.isCompleted(basket));
    }
}
