package grabnGoGroceries;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Scanner;
import org.junit.Test;

/** End-to-end console tests with scripted input. */
public class GroceryAppTest {
    private String runInput(String commands) {
        return runWithSave(null, commands);
    }

    private String runWithSave(File save, String commands) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output));
            new GroceryApp(new Scanner(commands), save).run();
        }
        finally {
            System.setOut(original);
        }
        return output.toString();
    }

    private File tempSave() throws Exception {
        File file = File.createTempFile("grocery", ".properties");
        file.deleteOnExit();
        return file;
    }

    private int occurrences(String text, String part) {
        int count = 0;
        int at = text.indexOf(part);
        while (at >= 0) {
            count++;
            at = text.indexOf(part, at + part.length());
        }
        return count;
    }

    // Main menu: 1 shop, 2 search, 3 basket, 4 budget, 5 challenges,
    // 6 checkout, 7 history, 8 new trip, 9 help, 0 quit.
    // Aisle menu: 1-7 aisles, 8 custom item, 0 back.
    // In an item list: a number (or 3x2), Enter at "How many?" means 1.
    // Challenges: 1 Movie Night (4 items, $25), 2 Weekly Shop, 3 Party Prep.
    // Handy commands: add, remove, search, basket, undo, clear, budget,
    // submit, rubric, abandon, checkout, history, new, help, home, quit.

    // ---- basics and navigation ----

    @Test
    public void testBrowseAddAndViewBasket() {
        // shop, Fruit, Apple, quantity 2, back, back, basket, back, quit
        String result = runInput("1\n1\n1\n2\n0\n0\n3\n0\nq\n");
        assertTrue(result.contains("Added 2 x Apple"));
        assertTrue(result.contains("Basket total: $1.98"));
        assertTrue(result.contains("Thanks for shopping!"));
    }

    @Test
    public void testListStaysOpenAndShowsWhatIsInBasket() {
        // two adds in a row without leaving the Fruit list
        String result = runInput("1\n1\n1\n1\n2\n1\nq\n");
        assertEquals(2, occurrences(result, "Added 1 x"));
        assertTrue(result.contains("<- x1 in basket"));
    }

    @Test
    public void testPickWithQuantityShortcuts() {
        String result = runInput("1\n1\n1x3\n2 2\nq\n");
        assertTrue(result.contains("Added 3 x Apple"));
        assertTrue(result.contains("Added 2 x Banana"));
    }

    @Test
    public void testPickByNameInsideList() {
        String result = runInput("1\n1\nbanana 4\nq\n");
        assertTrue(result.contains("Added 4 x Banana"));
    }

    @Test
    public void testQuantityRepromptsAndCancels() {
        String result = runInput("1\n1\n1\nabc\n100\n0\n0\n0\n3\nq\n");
        assertTrue(result.contains("Enter a number from 1 to 99"));
        assertTrue(result.contains("basket is empty"));
        assertFalse(result.contains("Added"));
    }

    @Test
    public void testBadNumbersAreExplained() {
        assertTrue(runInput("1\n99\nq\n").contains("Please pick 1 to 8"));
        assertTrue(runInput("1\n1\n9\nq\n").contains("There is no item #9"));
    }

    @Test
    public void testUnknownTextIsHandledNotFatal() {
        String result = runInput("hello\n0\n");
        assertTrue(result.contains("No items match \"hello\""));
        assertTrue(result.contains("Thanks for shopping!"));
    }

    @Test
    public void testQuitFromAnyMenu() {
        assertTrue(runInput("q\n").contains("Thanks for shopping!"));
        assertTrue(runInput("exit\n").contains("Thanks for shopping!"));
        assertTrue(runInput("1\n1\nq\n").contains("Thanks for shopping!"));
        assertTrue(runInput("5\nquit\n").contains("Thanks for shopping!"));
    }

    @Test
    public void testHomeJumpsStraightBackToMainMenu() {
        String result = runInput("1\n1\nhome\n0\n");
        assertEquals(2, occurrences(result, "GRAB 'N' GO GROCERIES"));
        assertTrue(result.contains("Thanks for shopping!"));
    }

    @Test
    public void testNextPrevAndAisleNames() {
        String next = runInput("1\n1\nnext\nq\n");
        assertTrue(next.contains("AISLE 2 OF 7: VEGETABLES"));
        String prev = runInput("1\n1\nprev\nq\n");
        assertTrue(prev.contains("AISLE 7 OF 7: DRINKS"));
        String jump = runInput("dairy\nq\n");
        assertTrue(jump.contains("AISLE 4 OF 7: DAIRY & EGGS"));
        String inside = runInput("1\n1\nbakery\nq\n");
        assertTrue(inside.contains("AISLE 5 OF 7: BAKERY"));
    }

    @Test
    public void testHelpWorksEverywhere() {
        assertTrue(runInput("help\nq\n").contains("== HELP"));
        assertTrue(runInput("1\n1\n?\nq\n").contains("== HELP"));
        assertTrue(runInput("9\nq\n").contains("== HELP"));
    }

    // ---- typed shortcuts ----

    @Test
    public void testAddShortcut() {
        String result = runInput("add milk 2\nbasket\n0\nq\n");
        assertTrue(result.contains("Added 2 x Milk"));
        assertTrue(result.contains("Basket total: $6.98"));
    }

    @Test
    public void testPlainNameAsksHowManyAndEnterMeansOne() {
        String result = runInput("milk\n\nq\n");
        assertTrue(result.contains("How many Milk? [1]"));
        assertTrue(result.contains("Added 1 x Milk"));
    }

    @Test
    public void testAmbiguousNameShowsChoices() {
        String result = runInput("add chips\nq\n");
        assertTrue(result.contains("Added 1 x Chips"));
        String many = runInput("add dairy\n0\nq\n");
        assertTrue(many.contains("MATCHES FOR \"dairy\""));
        String broad = runInput("add 1 lb\n0\nq\n");
        assertTrue(broad.contains("MATCHES FOR"));
    }

    @Test
    public void testSearchFindsAndAdds() {
        String result = runInput("2\nmilk\n1\n\n0\n3\n0\nq\n");
        assertTrue(result.contains("RESULTS FOR \"milk\""));
        assertTrue(result.contains("Basket total: $3.49"));
    }

    @Test
    public void testSearchByAisleAndNoResults() {
        assertTrue(runInput("search fruit\n0\nq\n").contains("Banana"));
        assertTrue(runInput("search zzzz\nq\n").contains("No items match"));
    }

    @Test
    public void testCustomItemFlowAndValidation() {
        String result = runInput(
            "1\n8\n\nApple\nabc\n-2\n0\nNaN\n2\nProduce\nRed\n\n0\n3\n0\nq\n");
        assertTrue(result.contains("Please enter something"));
        assertTrue(result.contains("Price must be a positive amount"));
        assertTrue(result.contains("Added 1 x Apple"));
        assertTrue(result.contains("Basket total: $2.00"));
    }

    @Test
    public void testCustomItemCancel() {
        String result = runInput("1\n8\ncancel\n0\nq\n");
        assertFalse(result.contains("Added"));
        assertTrue(result.contains("Thanks for shopping!"));
    }

    // ---- basket editing and undo ----

    @Test
    public void testEmptyBasketScreen() {
        assertTrue(runInput("3\nq\n").contains("Your basket is empty"));
    }

    @Test
    public void testBasketEditing() {
        // Apple is line 1, Milk is line 2 (sorted by name)
        String result = runInput(
            "add apple 3\nadd milk\n3\n-1\n+1 2\n-2 all\n0\nq\n");
        assertTrue(result.contains("Removed 1 x Apple"));
        assertTrue(result.contains("Added 2 x Apple"));
        assertTrue(result.contains("Removed 1 x Milk"));
        assertTrue(result.contains("Basket total: $3.96"));
    }

    @Test
    public void testBasketEditMistakes() {
        String result = runInput("add apple\n3\n-9\n+1 all\n0\nq\n");
        assertTrue(result.contains("There is no line 9"));
        assertTrue(result.contains("To add several, give a number"));
    }

    @Test
    public void testRemoveByNameAndMissing() {
        String result = runInput("add apple 2\nremove apple\nremove milk\nq\n");
        assertTrue(result.contains("Removed 1 x Apple"));
        assertTrue(result.contains("Nothing named \"milk\""));
        assertTrue(runInput("add apple 3\nremove apple all\nq\n")
            .contains("Removed 3 x Apple"));
    }

    @Test
    public void testUndoAndClear() {
        String result = runInput("add apple 2\nclear\nundo\n3\n0\nq\n");
        assertTrue(result.contains("Basket cleared"));
        assertTrue(result.contains("Undid your last change"));
        assertTrue(result.contains("Basket total: $1.98"));
        assertTrue(runInput("undo\nq\n").contains("Nothing to undo"));
        assertTrue(runInput("clear\nq\n").contains("already empty"));
    }

    // ---- budget ----

    @Test
    public void testBudgetShortcutAndRemaining() {
        String result = runInput("budget 10\nadd apple 3\nq\n");
        assertTrue(result.contains("Budget set to $10.00"));
        assertTrue(result.contains("Budget left: $7.03"));
    }

    @Test
    public void testOverBudgetWarning() {
        String result = runInput("budget 2\nadd apple 3\nq\n");
        assertTrue(result.contains("over your budget"));
        assertTrue(result.contains("OVER by $0.97"));
    }

    @Test
    public void testBudgetScreenValidationOffAndCancel() {
        String result = runInput("4\nabc\n-5\n25\nq\n");
        assertTrue(result.contains("Budget must be a positive amount"));
        assertTrue(result.contains("Budget set to $25.00"));
        assertTrue(runInput("budget 5\nbudget off\nq\n").contains("Budget removed"));
        assertFalse(runInput("4\n0\nq\n").contains("Budget set to"));
        assertTrue(runInput("budget abc\nq\n").contains("Budget must be"));
    }

    @Test
    public void testQuantityPromptShowsAffordableCount() {
        String result = runInput("budget 5\n1\n1\n1\n");
        assertTrue(result.contains("budget fits 5"));
    }

    // ---- challenges ----

    @Test
    public void testStartChallengeShowsProgress() {
        String result = runInput("5\n1\n0\nq\n");
        assertTrue(result.contains("Challenge started: Movie Night"));
        assertTrue(result.contains("Movie Night | 0/4 items"));
        assertTrue(result.contains("4 more items to go"));
    }

    @Test
    public void testChallengeGradedOnSubmitWithRubric() {
        // 4 apples ($3.96): 20 + 40 + 17 + 5 = 82 -> B
        String result = runInput("5\n1\n0\nadd apple 4\nsubmit\nq\n");
        assertTrue(result.contains("requirements met"));
        assertTrue(result.contains("CHALLENGE GRADE: Movie Night"));
        assertTrue(result.contains("Item minimum"));
        assertTrue(result.contains("20/20"));
        assertTrue(result.contains("40/40"));
        assertTrue(result.contains("SCORE: 82/100   GRADE: B   PASSED"));
        assertTrue(result.contains("Challenge completed!"));
        assertTrue(result.contains("How to improve"));
        assertTrue(result.contains("last: B 82/100"));
    }

    @Test
    public void testSubmitBeforeReadyExplainsWhatIsMissing() {
        String result = runInput("5\n1\n0\nadd apple\nsubmit\nq\n");
        assertTrue(result.contains("Not ready to submit: you need 3 more items"));
        assertTrue(result.contains("SCORE PREVIEW"));
        assertFalse(result.contains("CHALLENGE GRADE"));
    }

    @Test
    public void testRubricPreviewAnytime() {
        String result = runInput("5\n1\n0\nadd apple 2\nrubric\nq\n");
        assertTrue(result.contains("SCORE PREVIEW: Movie Night (not submitted)"));
        assertTrue(result.contains("Add 2 more items"));
        assertTrue(runInput("rubric\nq\n").contains("No challenge is running"));
    }

    @Test
    public void testSubmitOverBudgetFailsAfterConfirm() {
        // 4 salmon = $39.96 against a $25 budget: 20 + 0 + 0 + 5 = 25
        String result = runInput("5\n1\n0\nadd salmon 4\nsubmit\ny\nq\n");
        assertTrue(result.contains("OVER by $14.96"));
        assertTrue(result.contains("SCORE: 25/100   GRADE: F   FAILED"));
        assertTrue(result.contains("Challenge failed: over budget by $14.96"));
        String declined = runInput("5\n1\n0\nadd salmon 4\nsubmit\nn\nq\n");
        assertFalse(declined.contains("CHALLENGE GRADE"));
        assertTrue(declined.contains("remove something and submit again"));
    }

    @Test
    public void testChallengeWarnsWhenOverChallengeBudget() {
        String result = runInput("5\n1\n0\nadd salmon 3\nq\n");
        assertTrue(result.contains("over the challenge budget"));
    }

    @Test
    public void testNoLimitOnItemsBeyondMinimum() {
        String result = runInput("5\n1\n0\nadd apple 9\nbasket\n0\nq\n");
        assertTrue(result.contains("Added 9 x Apple"));
        assertTrue(result.contains("9 items | Basket total: $8.91"));
    }

    @Test
    public void testVarietyBonusCountsCategories() {
        // apple + carrots + milk + bread = $9.26: 20 + 40 + 13 + 20 = 93 (A)
        String result = runInput("5\n1\n0\nadd apple\nadd carrots\nadd milk\n"
            + "add bread\nsubmit\nq\n");
        assertTrue(result.contains("4 categories"));
        assertTrue(result.contains("GRADE: A"));
    }

    @Test
    public void testRubricSuggestsMissingAislesForVariety() {
        String result = runInput("5\n1\n0\nadd apple 4\nsubmit\nq\n");
        assertTrue(result.contains("Shop 3 more categories for +15 points"));
        assertTrue(result.contains("try: Vegetables"));
    }

    @Test
    public void testStartingAChallengeDuringOneOffersSwitching() {
        String result = runInput("5\n1\n0\nchallenge\n0\nq\n");
        assertTrue(result.contains("Switch to a different challenge"));
        assertTrue(result.contains("Abandon this challenge"));
        String switched = runInput("5\n1\n0\n5\n3\n2\n0\nq\n");
        assertTrue(switched.contains("Challenge started: Weekly Shop"));
        assertTrue(switched.contains("Weekly Shop | 0/8 items"));
    }

    @Test
    public void testAbandonChallenge() {
        String result = runInput("5\n1\n0\nabandon\ny\nq\n");
        assertTrue(result.contains("Challenge abandoned"));
        assertEquals(1, occurrences(result, "Challenge  Movie Night"));
        assertTrue(runInput("abandon\nq\n").contains("No challenge is running"));
        assertTrue(runInput("5\n1\n0\nabandon\nn\nq\n")
            .contains("the challenge is still on"));
    }

    @Test
    public void testStartChallengeKeepingOrClearingBasket() {
        String keep = runInput("add apple 4\n5\n1\n1\n0\nq\n");
        assertTrue(keep.contains("requirements met"));
        assertTrue(keep.contains("Movie Night | 4/4 items"));
        String empty = runInput("add apple 4\n5\n1\n2\n0\nq\n");
        assertTrue(empty.contains("Movie Night | 0/4 items"));
        String cancelled = runInput("add apple 4\n5\n1\n0\nq\n");
        assertFalse(cancelled.contains("Challenge started"));
    }

    @Test
    public void testRetrySubmitUpdatesBestScore() {
        // 4 apples = 82, then add other categories and resubmit for a record
        String result = runInput("5\n1\n0\nadd apple 4\nsubmit\nadd milk\n"
            + "add bread\nadd carrots\nundo\nsubmit\nq\n");
        assertEquals(2, occurrences(result, "CHALLENGE GRADE: Movie Night"));
        assertTrue(result.contains("(basket changed)"));
    }

    @Test
    public void testChallengePickerShowsBestScores() throws Exception {
        File save = tempSave();
        runWithSave(save, "5\n1\n0\nadd apple 4\nsubmit\nabandon\ny\nq\n");
        String second = runWithSave(save, "5\n0\nq\n");
        assertTrue(second.contains("Best: B (82/100)"));
    }

    // ---- checkout, history, new trip ----

    @Test
    public void testCheckoutKeepsTheAppRunning() {
        String result = runInput("add apple 2\ncheckout\ny\n3\nq\n");
        assertTrue(result.contains("Subtotal: $1.98"));
        assertTrue(result.contains("Total: $2.10"));
        assertTrue(result.contains("Checkout complete"));
        assertTrue(result.contains("basket is empty"));
        assertTrue(result.contains("Thanks for shopping!"));
    }

    @Test
    public void testCheckoutDeclinedAndEmpty() {
        String declined = runInput("add apple\ncheckout\nn\nq\n");
        assertTrue(declined.contains("keep shopping"));
        assertTrue(runInput("checkout\nq\n").contains("basket is empty"));
    }

    @Test
    public void testHistoryRecordsOrders() {
        String result = runInput("add apple 2\ncheckout\ny\nhistory\nq\n");
        assertTrue(result.contains("== ORDER HISTORY"));
        assertTrue(result.contains("2 items"));
        assertTrue(result.contains("$   2.10"));
        assertTrue(runInput("history\nq\n").contains("No orders yet"));
    }

    @Test
    public void testCheckoutOffersToGradeChallenge() {
        String result = runInput(
            "5\n1\n0\nadd apple 4\ncheckout\ny\ny\nhistory\nq\n");
        assertTrue(result.contains("Grade it before checking out"));
        assertTrue(result.contains("CHALLENGE GRADE: Movie Night"));
        assertTrue(result.contains("Checkout complete"));
        assertTrue(result.contains("Movie Night: B (82/100)"));
        assertTrue(result.contains("B (82/100)"));
    }

    @Test
    public void testNewTrip() {
        String cleared = runInput("budget 10\nadd apple\nnew\ny\n3\nq\n");
        assertTrue(cleared.contains("Fresh start"));
        assertTrue(cleared.contains("basket is empty"));
        assertTrue(runInput("new\nn\nq\n").contains("nothing was cleared"));
    }

    // ---- saving and restoring ----

    @Test
    public void testProgressIsSavedAndRestored() throws Exception {
        File save = tempSave();
        String first = runWithSave(save,
            "5\n2\n0\nbudget 20\nadd apple 2\nq\n");
        assertTrue(first.contains("Your trip is saved"));

        String second = runWithSave(save, "3\n0\nq\n");
        assertTrue(second.contains("Welcome back"));
        assertTrue(second.contains("Basket total: $1.98"));
        assertTrue(second.contains("$20.00"));
        assertTrue(second.contains("Weekly Shop | 2/8 items"));
    }

    @Test
    public void testGradeAndBestScoreSurviveRestart() throws Exception {
        File save = tempSave();
        runWithSave(save, "5\n1\n0\nadd apple 4\nsubmit\nq\n");
        String second = runWithSave(save, "5\n0\nq\n");
        assertTrue(second.contains("Last submitted: B (82/100)"));
        assertTrue(second.contains("Best: B (82/100)"));
    }

    @Test
    public void testHistorySurvivesRestartButTripIsCleared() throws Exception {
        File save = tempSave();
        runWithSave(save, "add apple 2\ncheckout\ny\nq\n");
        String second = runWithSave(save, "history\n3\nq\n");
        assertFalse(second.contains("Welcome back"));
        assertTrue(second.contains("2 items"));
        assertTrue(second.contains("basket is empty"));
    }

    @Test
    public void testOldSaveFormatStillLoads() throws Exception {
        File save = tempSave();
        String old = "budget=15.0\nchallenge=0\nchallengeDone=false\n"
            + "challengePassed=false\nchallengeScore=0\nchallengeGrade=\n"
            + "best.0=82\nbest.1=-1\nbest.2=-1\nitems=1\n"
            + "item.0.name=Apple\nitem.0.price=0.99\nitem.0.category=Fruit\n"
            + "item.0.description=Crisp and sweet\n";
        Files.write(save.toPath(), old.getBytes("UTF-8"));
        String result = runWithSave(save, "3\n0\nq\n");
        assertTrue(result.contains("Welcome back"));
        assertTrue(result.contains("Basket total: $0.99"));
        assertTrue(result.contains("Movie Night | 1/4 items"));
    }

    @Test
    public void testDamagedSaveStartsFresh() throws Exception {
        File save = tempSave();
        Files.write(save.toPath(), "items=abc\n".getBytes("UTF-8"));
        assertTrue(runWithSave(save, "3\nq\n").contains("basket is empty"));

        Files.write(save.toPath(), "items=1\n".getBytes("UTF-8"));
        assertTrue(runWithSave(save, "3\nq\n").contains("basket is empty"));

        Files.write(save.toPath(), "budget=NaN\n".getBytes("UTF-8"));
        assertFalse(runWithSave(save, "q\n").contains("Welcome back"));

        Files.write(save.toPath(), "orders=1\n".getBytes("UTF-8"));
        assertFalse(runWithSave(save, "q\n").contains("Welcome back"));
    }

    @Test
    public void testSaveFailureDoesNotCrashAndWarnsOnce() {
        File folder = new File(System.getProperty("java.io.tmpdir"));
        String result = runWithSave(folder, "add apple\nadd apple\nq\n");
        assertEquals(1, occurrences(result, "Could not save"));
        assertTrue(result.contains("Thanks for shopping!"));
    }

    // ---- misc ----

    @Test
    public void testEmptyInputSurvives() {
        assertTrue(runInput("").contains("Choose an option"));
        assertTrue(runInput("1\n").contains("WHAT ARE YOU LOOKING FOR"));
        assertTrue(runInput("1\n1\n").contains("Pick an item"));
        assertTrue(runInput("1\n1\n1\n").contains("How many"));
        assertTrue(runInput("2\n").contains("Search for an item"));
        assertTrue(runInput("5\n").contains("CHOOSE A CHALLENGE"));
        assertTrue(runInput("4\n").contains("Budget: $"));
        assertTrue(runInput("add apple\n3\n").contains("Edit basket"));
        assertTrue(runInput("add apple\ncheckout\n").contains("Complete checkout?"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectNullScanner() {
        new GroceryApp(null);
    }

    @Test
    public void testMainMethod() throws Exception {
        java.io.InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        String originalSave = System.getProperty("grocery.save");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setProperty("grocery.save", "");
            System.setIn(new ByteArrayInputStream("0\n".getBytes("UTF-8")));
            System.setOut(new PrintStream(captured));
            GroceryApp.main(new String[0]);
            assertTrue(captured.toString().contains("Thanks for shopping!"));
        }
        finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            if (originalSave == null) {
                System.clearProperty("grocery.save");
            }
            else {
                System.setProperty("grocery.save", originalSave);
            }
        }
    }
}