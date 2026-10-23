package grabnGoGroceries;

import static org.junit.Assert.*;
import org.junit.Test;

/** Checks instructor-provided LinkedChain's public API. */
public class LinkedChainTest {
    @Test
    public void testAddCountAndContains() {
        LinkedChain<String> c = new LinkedChain<String>();
        assertTrue(c.add("Apple"));
        assertTrue(c.add("Apple"));
        assertEquals(2, c.size());
        assertEquals(2, c.count("Apple"));
        assertTrue(c.contains("Apple"));
        assertFalse(c.contains("Milk"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNull() {
        new LinkedChain<String>().add(null);
    }

    @Test
    public void testAddToEndAndGet() {
        LinkedChain<Integer> c = new LinkedChain<Integer>();
        assertTrue(c.addToEnd(1));
        assertTrue(c.addToEnd(2));
        assertEquals(Integer.valueOf(2), c.get(1));
        assertEquals(Integer.valueOf(1), c.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddToEndNull() {
        new LinkedChain<Integer>().addToEnd(null);
    }

    @Test
    public void testRemoveOneAndRemoveMissing() {
        LinkedChain<String> c = new LinkedChain<String>();
        c.add("Apple");
        c.add("Apple");
        assertTrue(c.remove("Apple"));
        assertEquals(1, c.size());
        assertFalse(c.remove("Milk"));
        assertFalse(c.remove((String)null));
    }

    @Test
    public void testRemoveFirstEmpty() {
        LinkedChain<String> c = new LinkedChain<String>();
        assertNull(c.remove());
        c.add("A");
        assertEquals("A", c.remove());
        assertTrue(c.isEmpty());
    }

    @Test
    public void testClearEmptyAndNonEmpty() {
        LinkedChain<String> c = new LinkedChain<String>();
        c.clear();
        assertTrue(c.isEmpty());
        c.add("A");
        c.clear();
        assertEquals(0, c.size());
        assertEquals(0, c.count("A"));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetInvalidIndex() {
        new LinkedChain<Integer>().get(0);
    }

    @Test
    public void testToArrayAndToString() {
        LinkedChain<String> c = new LinkedChain<String>();
        c.addToEnd("A");
        c.addToEnd("B");
        assertArrayEquals(new String[] {"A", "B"}, c.toArray(new String[c.size()]));
        assertEquals("[A, B]", c.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToArrayTooSmall() {
        LinkedChain<String> c = new LinkedChain<String>();
        c.add("A");
        c.toArray(new String[0]);
    }

    @Test
    public void testTwoGenericTypes() {
        LinkedChain<GroceryItem> items = new LinkedChain<GroceryItem>();
        LinkedChain<ShoppingChallenge> challenges = new LinkedChain<ShoppingChallenge>();
        items.add(new GroceryItem("A", 1, "", ""));
        challenges.add(new ShoppingChallenge("B", "", 5, 1));
        assertEquals("A", items.get(0).getName());
        assertEquals("B", challenges.get(0).getName());
    }
}
