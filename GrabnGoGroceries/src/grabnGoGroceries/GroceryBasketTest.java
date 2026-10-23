package grabnGoGroceries;
import static org.junit.Assert.*;
import org.junit.Test;
public class GroceryBasketTest {
 private GroceryItem apple(){return new GroceryItem("Apple",2.0,"Produce","Red");}
 @Test public void testAddCountAndGet(){GroceryBasket b=new GroceryBasket();b.addItem(apple());assertEquals(1,b.getItemCount());assertEquals(apple(),b.getItem(0));assertEquals(1,b.countOf(apple()));}
 @Test(expected=IllegalArgumentException.class) public void testRejectNull(){new GroceryBasket().addItem(null);}
 @Test public void testRemoveDuplicate(){GroceryBasket b=new GroceryBasket();b.addItem(apple());b.addItem(apple());assertTrue(b.removeItem(apple()));assertEquals(1,b.getItemCount());assertFalse(b.removeItem(null));}
 @Test public void testFindAndTotal(){GroceryBasket b=new GroceryBasket();b.addItem(apple());assertEquals(apple(),b.findByName(" APPLE "));assertEquals(2.0,b.getTotalPrice(),0.0001);assertTrue(b.toString().contains("Apple"));}
 @Test public void testSnapshotAndRestoreIndependent(){GroceryBasket b=new GroceryBasket();b.addItem(apple());LinkedChain<GroceryItem> snapshot=b.snapshotItems();b.addItem(apple());assertEquals(1,snapshot.size());b.restoreItems(snapshot);assertEquals(1,b.getItemCount());snapshot.clear();assertEquals(1,b.getItemCount());}
 @Test public void testRestoreEmpty(){GroceryBasket b=new GroceryBasket();b.addItem(apple());b.restoreItems(new LinkedChain<GroceryItem>());assertTrue(b.isEmpty());}
 @Test(expected=IllegalArgumentException.class) public void testRestoreNull(){new GroceryBasket().restoreItems(null);}
 @Test(expected=IndexOutOfBoundsException.class) public void testInvalidGet(){new GroceryBasket().getItem(0);}
}
