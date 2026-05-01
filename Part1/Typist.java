/**
 * Represents an individual competitor in a typing race simulation.
 *
 * Each typist has a name, a Unicode display symbol, an accuracy rating,
 * a progress counter (characters typed correctly), and a burnout state.
 * Progress can go both forwards and backwards, but never below zero.
 * A burnt-out typist cannot type until their burnout counter reaches zero.
 * Starter code originally abandoned by Ty Posaurus.
 *
 * @author (Ecren Donmez)
 * @version 1.0
 */
public class Typist
{
    // Fields 
    private String name; // The display name of this typist (e.g. "TURBOFINGERS").

    private char symbol; // A single Unicode character used to represent this typist on-screen (e.g. '①').

    private int progress; // The typist's current progress through the passage, measured in characters typed correctly so far. Can increase or decrease, but never goes below zero.

    private boolean burntOut; // Whether this typist is currently burnt out and unable to type.

    private int burnoutTurnsRemaining; // The number of turns of burnout remaining. Zero when the typist is not burnt out.

    private double accuracy; // The typist's accuracy rating, kept in the range [0.0, 1.0]. Higher accuracy means more correct keystrokes per turn, but also a higher risk of burnout from pushing too hard.


    /**
     * Creates a new typist with a given symbol, name, and accuracy rating.
     * Progress starts at zero; the typist begins in a non-burnt-out state.
     *
     * @param typistSymbol   a single Unicode character representing this typist on screen
     * @param typistName     the name of the typist (e.g. "TURBOFINGERS")
     * @param typistAccuracy the typist's accuracy rating; clamped to [0.0, 1.0]
     */
    public Typist(char typistSymbol, String typistName, double typistAccuracy)
    {
        symbol                = typistSymbol;
        name                  = typistName;
        progress              = 0;
        burntOut              = false;
        burnoutTurnsRemaining = 0;

        // Use setAccuracy() so clamping is applied consistently from the start.
        setAccuracy(typistAccuracy);
    }


    /**
     * Puts this typist into a burnt-out state lasting the given number of turns.
     * While burnt out the typist cannot type.
     *
     * @param turns the number of turns the burnout will last
     */
    public void burnOut(int turns)
    {
        burntOut              = true;
        burnoutTurnsRemaining = turns;
    }

    /**
     * Reduces the remaining burnout counter by one turn.
     * When the counter reaches zero the typist automatically recovers and can type again on the next turn.
     * Has no effect if the typist is not currently burnt out.
     */
    public void recoverFromBurnout()
    {
        if (!burntOut)
        {
            return; // Nothing to do as not currently burnt out.
        }

        burnoutTurnsRemaining--;

        if (burnoutTurnsRemaining <= 0) // <= 0 rather than == 0 as a defensive measure
        {
            burntOut              = false;
            burnoutTurnsRemaining = 0; // Guard against going negative.
        }
    }

    /**
     * Resets this typist to their initial race-ready state.
     * Progress returns to zero and all burnout state is cleared entirely.
     */
    public void resetToStart()
    {
        progress              = 0;
        burntOut              = false;
        burnoutTurnsRemaining = 0;
    }

    /**
     * Advances the typist forward by one character along the passage.
     * Should only be called when the typist is not burnt out.
     */
    public void typeCharacter()
    {
        progress++;
    }

    /**
     * Moves the typist backwards by the given number of characters (a mistype).
     * Progress cannot go below zero — the typist cannot slide off the start.
     * Validation: if the given amount would push progress below zero, progress is clamped to zero rather than becoming negative.
     * Without this clamp, negative progress would break the race display and make the win condition behave incorrectly.
     *
     * @param amount the number of characters to slide back (must be positive)
     */
    public void slideBack(int amount)
    {
        progress -= amount;
        if (progress < 0)
        {
            progress = 0; // Clamp: cannot go before the start of the passage.
        }
    }

    /**
     * Sets the typist's accuracy rating. Values below 0.0 are clamped to 0.0; values above 1.0 are clamped to 1.0.
     *
     * @param newAccuracy the new accuracy rating
     */
    public void setAccuracy(double newAccuracy)
    {
        if (newAccuracy < 0.0)
        {
            accuracy = 0.0;
        }
        else if (newAccuracy > 1.0)
        {
            accuracy = 1.0;
        }
        else
        {
            accuracy = newAccuracy;
        }
    }

    /**
     * Sets the character symbol used to represent this typist on screen.
     *
     * @param newSymbol the new symbol character
     */
    public void setSymbol(char newSymbol)
    {
        symbol = newSymbol;
    }

    /**
     * Sets the display name of this typist. Not in the original spec but required by the GUI (TypingRaceGUI.java).
     *
     * @param newName the new name string
     */
    public void setName(String newName)
    {
        name = newName;
    }


    /**
     * Returns the typist's accuracy rating.
     *
     * @return accuracy as a double in [0.0, 1.0]
     */
    public double getAccuracy()
    {
        return accuracy;
    }

    /**
     * Returns the typist's current progress through the passage. Progress is measured in characters typed correctly so far.
     *
     * @return progress as a non-negative integer
     */
    public int getProgress()
    {
        return progress;
    }

    /**
     * Returns the name of this typist.
     *
     * @return the typist's name as a String
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the Unicode character used to represent this typist on screen.
     *
     * @return the typist's symbol as a char
     */
    public char getSymbol()
    {
        return symbol;
    }

    /**
     * Returns the number of turns of burnout remaining. Returns 0 if the typist is not currently burnt out.
     *
     * @return burnout turns remaining as a non-negative integer
     */
    public int getBurnoutTurnsRemaining()
    {
        return burnoutTurnsRemaining;
    }

    /**
     * Returns true if this typist is currently burnt out, false otherwise.
     *
     * @return true if burnt out
     */
    public boolean isBurntOut()
    {
        return burntOut;
    }


    // Manual tests
    public static void main(String[] args)
    {
        System.out.println("- Test 1: typeCharacter() forward movement -");
        Typist t = new Typist('A', "TURBOFINGERS", 0.85);
        System.out.println("Progress at start: " + t.getProgress());
        t.typeCharacter();
        t.typeCharacter();
        t.typeCharacter();
        System.out.println("Progress after 3 typeCharacter() calls: " + t.getProgress());

        System.out.println();
        System.out.println("- Test 2: slideBack() clamping -");
        System.out.println("Progress before slideBack(10): " + t.getProgress());
        t.slideBack(10);
        System.out.println("Progress after slideBack(10): " + t.getProgress() + " (expected 0)");
        t.slideBack(5);
        System.out.println("Progress after slideBack(5) from zero: " + t.getProgress() + " (expected 0)");

        System.out.println();
        System.out.println("- Test 3: burnout countdown -");
        t.burnOut(3);
        System.out.println("isBurntOut() after burnOut(3): " + t.isBurntOut() + " (expected true)");
        System.out.println("Turns remaining: " + t.getBurnoutTurnsRemaining() + " (expected 3)");
        t.recoverFromBurnout();
        System.out.println("Turns remaining after 1 recovery: " + t.getBurnoutTurnsRemaining() + " (expected 2)");
        t.recoverFromBurnout();
        System.out.println("Turns remaining after 2 recoveries: " + t.getBurnoutTurnsRemaining() + " (expected 1)");
        t.recoverFromBurnout();
        System.out.println("isBurntOut() after 3 recoveries: " + t.isBurntOut() + " (expected false)");
        System.out.println("Turns remaining after full recovery: " + t.getBurnoutTurnsRemaining() + " (expected 0)");
 
        System.out.println();
        System.out.println("- Test 4: resetToStart() -");
        for (int i = 0; i < 10; i++) t.typeCharacter();
        t.burnOut(5);
        System.out.println("Before reset - progress: " + t.getProgress() + ", burntOut: " + t.isBurntOut());
        t.resetToStart();
        System.out.println("After reset  - progress: " + t.getProgress() + " (expected 0), burntOut: " + t.isBurntOut() + " (expected false)");
 
        System.out.println();
        System.out.println("- Test 5: setAccuracy() clamping -");
        t.setAccuracy(1.5);
        System.out.println("setAccuracy(1.5)  -> " + t.getAccuracy() + " (expected 1.0)");
        t.setAccuracy(-0.3);
        System.out.println("setAccuracy(-0.3) -> " + t.getAccuracy() + " (expected 0.0)");
        t.setAccuracy(0.75);
        System.out.println("setAccuracy(0.75) -> " + t.getAccuracy() + " (expected 0.75)");
    }
}