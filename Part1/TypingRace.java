import java.util.concurrent.TimeUnit;
import java.lang.Math;

/**
 * A typing race simulation. Three typists race to complete a passage of text,
 * advancing character by character.
 *
 * Originally written by Ty Posaurus who later on, abandoned.
 *
 * @author (Ecren Donmez)
 * @version 1.0
 */
public class TypingRace
{
    private int passageLength; // Total characters in the passage to type
    private Typist seat1Typist;
    private Typist seat2Typist;
    private Typist seat3Typist;

    // Accuracy thresholds for mistype and burnout events.
    private static final double MISTYPE_BASE_CHANCE = 0.3;
    private static final int    SLIDE_BACK_AMOUNT   = 2;
    private static final int    BURNOUT_DURATION    = 3;

    // Accuracy adjustments applied after the race.
    private static final double WINNER_BOOST        = 0.02;
    private static final double BURNOUT_PENALTY     = 0.01;

    // Track burnout events during the race to apply accuracy penalties.
    private boolean seat1BurntOutThisRace = false;
    private boolean seat2BurntOutThisRace = false;
    private boolean seat3BurntOutThisRace = false;

    /**
     * Constructor for objects of class TypingRace.
     * Sets up the race with a passage of the given length.
     * Initially there are no typists seated.
     *
     * @param passageLength the number of characters in the passage to type
     */
    public TypingRace(int passageLength)
    {
        this.passageLength = passageLength;
        seat1Typist = null;
        seat2Typist = null;
        seat3Typist = null;
    }

    /**
     * Seats a typist at the given seat number (1, 2, or 3).
     *
     * @param theTypist  the typist to seat
     * @param seatNumber the seat to place them in (1–3)
     */
    public void addTypist(Typist theTypist, int seatNumber)
    {
        if (seatNumber == 1)
        {
            seat1Typist = theTypist;
        }
        else if (seatNumber == 2)
        {
            seat2Typist = theTypist;
        }
        else if (seatNumber == 3)
        {
            seat3Typist = theTypist;
        }
        else
        {
            System.out.println("Cannot seat typist at seat " + seatNumber + " — there is no such seat.");
        }
    }

    /**
     * Starts the typing race.
     * All seated typists are reset to the beginning, then the simulation runs
     * turn by turn until one typist completes the full passage.
     * The winner's name is announced and accuracy ratings are updated.
     */
    public void startRace()
    {
        boolean finished = false;

        // BUG 1 FIX: reset ALL seated typists, not just seats 1 and 2.
        // Original only called seat1Typist.resetToStart() and seat2Typist.resetToStart(),
        // leaving seat3 either with stale progress or crashing if null.
        // Null guards added throughout aswell to prevent crashes when a seat is empty.
        if (seat1Typist != null) { seat1Typist.resetToStart(); }
        if (seat2Typist != null) { seat2Typist.resetToStart(); }
        if (seat3Typist != null) { seat3Typist.resetToStart(); }

        // Reset per-race burnout tracking.
        seat1BurntOutThisRace = false;
        seat2BurntOutThisRace = false;
        seat3BurntOutThisRace = false;

        while (!finished)
        {
            // Advance each seated typist by one turn.
            if (seat1Typist != null) { advanceTypist(seat1Typist, 1); }
            if (seat2Typist != null) { advanceTypist(seat2Typist, 2); }
            if (seat3Typist != null) { advanceTypist(seat3Typist, 3); }

            // Print the current state of the race.
            printRace();

            // BUG 2 FIX: use raceFinishedBy() which now correctly uses >=.
            if ( (seat1Typist != null && raceFinishedBy(seat1Typist)) ||
                 (seat2Typist != null && raceFinishedBy(seat2Typist)) ||
                 (seat3Typist != null && raceFinishedBy(seat3Typist)) )
            {
                finished = true;
            }

            // Wait 200ms between turns so the animation is visible.
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (Exception e) {}
        }

        // BUG 4 FIX: announce the winner and update accuracy ratings.
        // Original had only: TODO (Task 2a): Print the winner's name here
        Typist winner = determineWinner();

        if (winner != null)
        {
            double previousAccuracy = winner.getAccuracy();
            winner.setAccuracy(previousAccuracy + WINNER_BOOST);

            System.out.println();
            System.out.println(" And the winner is... " + winner.getName() + "!");
            System.out.printf(" Final accuracy: %.2f (improved from %.2f)%n",
                    winner.getAccuracy(), previousAccuracy);
        }

        // Apply burnout accuracy penalties.
        if (seat1Typist != null && seat1BurntOutThisRace)
        {
            seat1Typist.setAccuracy(seat1Typist.getAccuracy() - BURNOUT_PENALTY);
        }
        if (seat2Typist != null && seat2BurntOutThisRace)
        {
            seat2Typist.setAccuracy(seat2Typist.getAccuracy() - BURNOUT_PENALTY);
        }
        if (seat3Typist != null && seat3BurntOutThisRace)
        {
            seat3Typist.setAccuracy(seat3Typist.getAccuracy() - BURNOUT_PENALTY);
        }
    }

    /**
     * Determines and returns the winning typist (the one who finished the passage).
     * Returns null if somehow no typist has finished (should not happen post-loop).
     *
     * @return the winning Typist, or null
     */
    private Typist determineWinner()
    {
        if (seat1Typist != null && raceFinishedBy(seat1Typist)) { return seat1Typist; }
        if (seat2Typist != null && raceFinishedBy(seat2Typist)) { return seat2Typist; }
        if (seat3Typist != null && raceFinishedBy(seat3Typist)) { return seat3Typist; }
        return null;
    }

    /**
     * Simulates one turn for a typist.
     *
     * If the typist is burnt out, they recover one turn's worth and skip typing.
     * Otherwise:
     *   - They may type a character (advancing progress) based on their accuracy.
     *   - They may mistype (sliding back) lower accuracy means higher mistype risk.
     *   - They may burn out more likely for high-accuracy typists pushing hard.
     *
     * @param theTypist  the typist to advance
     * @param seatNumber the seat number (1–3), used to track burnout per seat
     */
    private void advanceTypist(Typist theTypist, int seatNumber)
    {
        if (theTypist.isBurntOut())
        {
            // Recovering from burnout skip this turn.
            theTypist.recoverFromBurnout();
            return;
        }

        // Attempt to type a character.
        if (Math.random() < theTypist.getAccuracy())
        {
            theTypist.typeCharacter();

            // Burnout check moved INSIDE the successful-type block.
            // Original ran burnout check unconditionally, meaning a typist could
            // type a character and immediately burn out in the same turn,
            // wasting their progress and unfairly freezing them straight away.
            if (Math.random() < 0.05 * theTypist.getAccuracy() * theTypist.getAccuracy())
            {
                theTypist.burnOut(BURNOUT_DURATION);

                // Record that this typist burnt out during this race.
                if (seatNumber == 1) { seat1BurntOutThisRace = true; }
                else if (seatNumber == 2) { seat2BurntOutThisRace = true; }
                else if (seatNumber == 3) { seat3BurntOutThisRace = true; }
            }
        }

        // BUG 3 FIX: mistype probability now uses (1 - accuracy) instead of accuracy.
        // Original: Math.random() < theTypist.getAccuracy() * MISTYPE_BASE_CHANCE
        // This made MORE accurate typists mistype MORE often — completely backwards.
        // Fix: higher accuracy → lower mistype chance, as intended.
        if (Math.random() < (1.0 - theTypist.getAccuracy()) * MISTYPE_BASE_CHANCE)
        {
            theTypist.slideBack(SLIDE_BACK_AMOUNT);
        }
    }

    /**
     * Returns true if the given typist has completed the full passage.
     *
     * @param theTypist the typist to check
     * @return true if their progress has reached or passed the passage length
     */
    private boolean raceFinishedBy(Typist theTypist)
    {
        // BUG 2 FIX: changed == to >= 
        // Original used ==, which the spec explicitly warns against because
        // progress CAN overshoot passageLength in edge cases.
        return theTypist.getProgress() >= passageLength;
    }

    /**
     * Prints the current state of the race to the terminal.
     * Shows each typist's position along the passage and their burnout state.
     */
    private void printRace()
    {
        System.out.print('\u000C'); // Clear terminal.

        System.out.println("  TYPING RACE \u2014 passage length: " + passageLength + " chars");
        multiplePrint('=', passageLength + 3);
        System.out.println();

        // Null guards prevent crash when a seat has no typist.
        if (seat1Typist != null) { printSeat(seat1Typist); System.out.println(); }
        if (seat2Typist != null) { printSeat(seat2Typist); System.out.println(); }
        if (seat3Typist != null) { printSeat(seat3Typist); System.out.println(); }

        multiplePrint('=', passageLength + 3);
        System.out.println();
        System.out.println("  [~] = burnt out    [<] = just mistyped");
    }

    /**
     * Prints a single typist's lane, showing their current position on the track.
     *
     * @param theTypist the typist whose lane to print
     */
    private void printSeat(Typist theTypist)
    {
        // Clamp position so spacesAfter never goes negative.
        // If progress overshoots passageLength, spacesAfter was negative before.
        int pos          = Math.min(theTypist.getProgress(), passageLength);
        int spacesBefore = pos;
        int spacesAfter  = passageLength - pos;

        System.out.print('|');
        multiplePrint(' ', spacesBefore);

        // Show the typist's symbol; append ~ when burnt out.
        System.out.print(theTypist.getSymbol());
        if (theTypist.isBurntOut())
        {
            System.out.print('~');
            spacesAfter--; // symbol + ~ together take two characters.
            if (spacesAfter < 0) { spacesAfter = 0; }
        }

        multiplePrint(' ', spacesAfter);
        System.out.print('|');
        System.out.print(' ');

        // Print name and accuracy.
        if (theTypist.isBurntOut())
        {
            System.out.print(theTypist.getName()
                + " (Accuracy: " + theTypist.getAccuracy() + ")"
                + " BURNT OUT (" + theTypist.getBurnoutTurnsRemaining() + " turns)");
        }
        else
        {
            System.out.print(theTypist.getName()
                + " (Accuracy: " + theTypist.getAccuracy() + ")");
        }
    }

    /**
     * Prints a character a given number of times.
     *
     * @param aChar the character to print
     * @param times how many times to print it
     */
    private void multiplePrint(char aChar, int times)
    {
        int i = 0;
        while (i < times)
        {
            System.out.print(aChar);
            i = i + 1;
        }
    }

    // Entry point
    /**
     * Demonstrates the race with three typists on a 40-character passage.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args)
    {
        TypingRace race = new TypingRace(40);
        race.addTypist(new Typist('\u2460', "TURBOFINGERS", 0.85), 1);
        race.addTypist(new Typist('\u2461', "QWERTY_QUEEN", 0.60), 2);
        race.addTypist(new Typist('\u2462', "HUNT_N_PECK",  0.30), 3);
        race.startRace();
    }
}