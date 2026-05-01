import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.Timer;

/**
 * A graphical typing race simulator.
 *
 * This class builds the full Swing interface, allows users to configure
 * typists and race settings, runs the race turn-by-turn, and displays
 * statistics, race history, and an all-time leaderboard.
 *
 * The GUI works with the Typist class, which stores each competitor's
 * progress, accuracy, and burnout state.
 *
 * @author Ecren Donmez
 * @version 1.0
 */

public class TypingRaceGUI extends JFrame
{
    // Colour palette used across the whole interface.
    private static final Color C_BG    = new Color(18,  18,  24);
    private static final Color C_CARD  = new Color(30,  32,  44);
    private static final Color C_BLUE  = new Color(88, 166, 255);
    private static final Color C_GREEN = new Color(63, 185,  80);
    private static final Color C_RED   = new Color(153, 81,  73);
    private static final Color C_GOLD  = new Color(210, 153,  34);
    private static final Color C_TXT   = new Color(220, 225, 235);
    private static final Color C_DIM   = new Color(120, 130, 145);

    // Fonts used consistently throughout the GUI.
    private static final Font F_MONO  = new Font("Courier New", Font.PLAIN, 13);
    private static final Font F_MONOB = new Font("Courier New", Font.BOLD,  13);
    private static final Font F_BODY  = new Font("SansSerif",   Font.PLAIN, 12);
    private static final Font F_BOLD  = new Font("SansSerif",   Font.BOLD,  12);

    // Pre-set passages that the user can choose from or customize.
    private static final String[][] PASSAGES = {
        { "Short  (45 chars)",  "The quick brown fox jumps over the lazy dog." },
        { "Medium (81 chars)",  "Pack my box with five dozen liquor jugs. Sphinx of black quartz, judge my vow." },
        { "Long  (132 chars)",  "A wizard's job is to vex chumps quickly in fog. Five quacking zephyrs jolt my wax bed. The five boxing wizards jump quickly." },
        { "Custom", "" }
    };

    // Typing styles and their modifiers.
    private static final Object[][] STYLES = {
        {"Touch Typist", 0.15, 1.2}, {"Hunt & Peck", -0.10, 0.7},
        {"Phone Thumbs", -0.05, 0.9}, {"Voice-to-Text", -0.20, 0.5}
    };

    // Keyboard types and their modifiers.
    private static final Object[][] KEYBOARDS = {
        {"Mechanical", 0.05, 0.9}, {"Membrane", 0.0, 1.0},
        {"Touchscreen", -0.10, 1.2}, {"Stenography", 0.20, 0.8}
    };

    // Extra race options selected per typist.
    private static final String[] ACCESSORIES = {"None","Wrist Support","Energy Drink","Headphones"};

    // Sponsor names used for leaderboard bonus earnings.
    private static final String[] SPONSORS    = {"None","KeyCorp","TypeMaster","FingerFuel","ByteSpeed"};

     // Main race configuration.
    private int       seats   = 3;
    private String    passage = PASSAGES[0][1];
    private boolean   autocorrect, caffeine, nightShift;

    private Typist[]  typists;
    private Color[]   colors;
    private String[]  symbols;
    private double[]  baseAcc;
    private int[]     accessories, sponsors, keyboards;
    private int[]     burnouts, totalKS, correctKS;
    private boolean[] burntThisRace;
    private long      raceStart;
    private int       turn;
    private boolean   running;
    private Typist    winner;
    private Timer     timer;

    // Cross-race statistics.
    private double[]  bestWPM;
    private int[]     points;
    private double[]  earnings;
    private int       raceCount;

    // All-time leaderboard across typist changes
    // Key = typist name, Value = { points, earnings, bestWPM, races }
    private java.util.LinkedHashMap<String, double[]> allTimeBoard = new java.util.LinkedHashMap<>();

    // Stores text snapshots of previous races for the stats tab.
    private java.util.List<String[]> raceHistory = new java.util.ArrayList<>();

    // Main GUI component references.
    private JTabbedPane       tabs;
    private JPanel            configPanel, racePanel, statsPanel, leaderPanel;
    private JButton           startBtn, resetBtn;
    private JLabel            turnLbl;
    private JTextArea         logArea;
    private TrackPanel[]      tracks;
    private JPanel            tracksBox;
    private DefaultTableModel leaderModel;

    // Setup tab widgets.
    private JComboBox<String> passageBox;
    private JTextArea         customText;
    private JSpinner          seatSpin;
    private JCheckBox         autoBox, cafBox, nightBox;
    private JTextField[]      nameF, symF;
    private JComboBox[]       styleB, kbB, accB, sponsB;
    private JButton[]         colBtn;

    // Starts the GUI safely on Swing's event dispatch thread.
    public static void startRaceGUI() {
        SwingUtilities.invokeLater(() -> new TypingRaceGUI().setVisible(true));
    }

    /**
     * Program entry point.
     *
     * @param args command-line arguments, not used in this program
     */
    public static void main(String[] args) { startRaceGUI(); }

    // Creates the main window, builds all tabs, and connects the main buttons.
    public TypingRaceGUI()
    {
        super("Typing Race Simulator  —  ECS414U");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1000, 700));
        getContentPane().setBackground(C_BG);

        initState(3); // Create the initial default race state with three typists.
        
        // Build the tabbed interface.
        tabs = new JTabbedPane();
        tabs.setBackground(C_CARD);
        tabs.setForeground(new Color(190, 200, 215));
        tabs.setFont(F_BOLD);
        tabs.addTab("Setup",       buildSetupTab());
        tabs.addTab("Race",        buildRaceTab());
        tabs.addTab("Stats",       buildStatsTab());
        tabs.addTab("Leaderboard", buildLeaderTab());

        // Create the main control buttons.
        startBtn = btn("Start Race", C_GREEN);
        resetBtn = btn("Reset",      C_DIM);
        turnLbl  = lbl("Turn: 0",    C_DIM, F_MONO);
        
        // Connect buttons to their event handlers.
        startBtn.addActionListener(e -> onStart());
        resetBtn.addActionListener(e -> onReset());

        // Footer holds the main controls and current turn count.
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        foot.setBackground(C_CARD);
        foot.setBorder(BorderFactory.createMatteBorder(1,0,0,0, C_CARD.brighter()));
        foot.add(startBtn); foot.add(resetBtn);
        foot.add(Box.createHorizontalStrut(16)); foot.add(turnLbl);

        // Add the tabs and footer to the main frame.
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(foot, BorderLayout.SOUTH);
        
        // Size and centre the window.
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Creates or rebuilds the main race data arrays.
     *
     * Existing cross-race statistics are preserved where possible when the
     * number of typists changes.
     *
     * @param n the number of typist seats to initialise
     */
    private void initState(int n)
    {
        // Preserve cross-race stats for seats that still exist
        double[] oldBestWPM  = bestWPM;
        int[]    oldPoints   = points;
        double[] oldEarnings = earnings;

        // Create arrays sized to the current number of typists.
        seats         = n;
        typists       = new Typist[n];

        // Give each typist a default colour and symbol.
        colors        = Arrays.copyOf(new Color[]{
            new Color(88,166,255), new Color(63,185,80), new Color(210,153,34),
            new Color(248,81,73),  new Color(139,98,218), new Color(56,139,53)}, n);
        symbols       = Arrays.copyOf(new String[]{"[1]","[2]","[3]","[4]","[5]","[6]"}, n);
        
         // Default names and base accuracies for initial typists.
        String[] names = {"TURBOFINGERS","QWERTY_QUEEN","HUNT_N_PECK","THE_CLACKER","SPEED_DEMON","TYPO_KING"};
        double[] accs  = {0.85, 0.65, 0.40, 0.75, 0.90, 0.55};
        
        // Create the per-race and cross-race arrays
        baseAcc       = new double[n];  accessories   = new int[n];
        sponsors      = new int[n];     keyboards     = new int[n];
        burnouts      = new int[n];     totalKS       = new int[n];
        correctKS     = new int[n];     burntThisRace = new boolean[n];
        bestWPM       = new double[n];  points        = new int[n];
        earnings      = new double[n];

        // Restore cross-race stats for seats that carried over
        if (oldBestWPM != null)
            for (int i = 0; i < Math.min(n, oldBestWPM.length); i++) {
                bestWPM[i]  = oldBestWPM[i];
                points[i]   = oldPoints[i];
                earnings[i] = oldEarnings[i];
            }

        // Create the Typist objects using default values.
        for (int i = 0; i < n; i++) {
            baseAcc[i] = accs[Math.min(i, accs.length-1)];
            typists[i] = new Typist(symbols[i].charAt(0), names[Math.min(i, names.length-1)], baseAcc[i]);
        }
    }

    /**
     * Builds the setup tab where the user configures passage, typists,
     * keyboard types, accessories, sponsors, and difficulty options.
     *
     * @return the completed setup panel
     */
    private JPanel buildSetupTab()
    {
        configPanel = panel(new BorderLayout(10,10));
        configPanel.setBorder(pad(12));

        // Left column contains passage and race-wide settings.
        JPanel left = panel(null);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(260, 0));
        left.setMaximumSize(new Dimension(260, Integer.MAX_VALUE));

        // Passage card contains the preset dropdown and custom passage box.
        JPanel pCard = card("Passage");
        passageBox = combo(Arrays.stream(PASSAGES).map(p->p[0]).toArray(String[]::new));
        passageBox.setMaximumSize(new Dimension(230, 28));
        
        // Update the passage when the user chooses a preset or custom option.
        passageBox.addActionListener(e -> {
            boolean custom = passageBox.getSelectedIndex() == PASSAGES.length-1;
            customText.setEnabled(custom);
            if (!custom) passage = PASSAGES[passageBox.getSelectedIndex()][1];
        });

        // Custom passage field starts disabled until the user selects Custom.
        customText = new JTextArea(3,18);
        customText.setFont(F_MONO); customText.setBackground(C_BG);
        customText.setForeground(C_TXT); customText.setLineWrap(true);
        customText.setEnabled(false); customText.setCaretColor(C_BLUE);
        
        // Keep the passage variable updated as the user types custom text.
        customText.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { if(customText.isEnabled()) passage=customText.getText(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { if(customText.isEnabled()) passage=customText.getText(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });

        JScrollPane customScroll = new JScrollPane(customText);
        customScroll.setMaximumSize(new Dimension(230, 70));
        pCard.add(passageBox); pCard.add(Box.createVerticalStrut(4));
        pCard.add(customScroll);

        // Race settings card controls the number of typists.
        JPanel sCard = card("Race Settings");
        seatSpin = new JSpinner(new SpinnerNumberModel(3,2,6,1));
        seatSpin.setFont(F_MONO);
        seatSpin.setMaximumSize(new Dimension(60, 28));
        seatSpin.setPreferredSize(new Dimension(60, 28));
        
         // Rebuild typist data and cards when the seat count changes.
        seatSpin.addChangeListener(e -> {
            int n = (Integer)seatSpin.getValue();
            if (n != seats) { initState(n); rebuildTypistCards(); }
        });

        JPanel spinRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        spinRow.setBackground(C_CARD);
        spinRow.add(lbl("Typists (2-6):", C_DIM, F_BOLD));
        spinRow.add(seatSpin);
        sCard.add(spinRow);

        // Difficulty card controls global modifiers.
        JPanel dCard = card("Difficulty");
        autoBox  = chk("Autocorrect (slide-back halved)");
        cafBox   = chk("Caffeine Mode (+acc first 10 turns)");
        nightBox = chk("Night Shift (-0.05 acc all)");
        
        // Store checkbox states in the matching boolean variables.
        autoBox.addActionListener(e  -> autocorrect = autoBox.isSelected());
        cafBox.addActionListener(e   -> caffeine    = cafBox.isSelected());
        nightBox.addActionListener(e -> nightShift  = nightBox.isSelected());
        
        dCard.add(autoBox); dCard.add(Box.createVerticalStrut(3));
        dCard.add(cafBox);  dCard.add(Box.createVerticalStrut(3));
        dCard.add(nightBox);

        // Add all left-column cards.
        left.add(pCard); left.add(Box.createVerticalStrut(8));
        left.add(sCard); left.add(Box.createVerticalStrut(8));
        left.add(dCard); left.add(Box.createVerticalGlue());

        // Right column contains the typist setup cards.
        JPanel right = panel(null);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setName("TYPISTCARDS");
        
        // Scroll pane allows all typist cards to fit neatly.
        JScrollPane scroll = new JScrollPane(right);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(C_BG);

        configPanel.add(left,   BorderLayout.WEST);
        configPanel.add(scroll, BorderLayout.CENTER);
        
        // Build the initial set of typist cards.
        rebuildTypistCards();
        return configPanel;
    }

    /**
     * Rebuilds the typist setup cards.
     *
     * This is used when the number of typists changes or when a visual
     * option such as colour needs to refresh.
     */
    private void rebuildTypistCards()
    {
        // Save any names/symbols already typed before rebuilding
        if (nameF != null)
            for (int i = 0; i < nameF.length && i < typists.length; i++) {
                String n = nameF[i].getText().trim();
                String s = symF[i].getText().trim();
                if (!n.isEmpty()) typists[i].setName(n);
                if (!s.isEmpty()) { symbols[i] = s; typists[i].setSymbol(s.charAt(0)); }
            }

        // Find the panel that holds the typist cards.
        JPanel right = findPanel("TYPISTCARDS");
        if (right == null) return;
        right.removeAll(); // Clear old cards before rebuilding.

        // Recreate the setup input arrays.
        nameF  = new JTextField[seats]; symF   = new JTextField[seats];
        styleB = new JComboBox[seats];  kbB    = new JComboBox[seats];
        accB   = new JComboBox[seats];  sponsB = new JComboBox[seats];
        colBtn = new JButton[seats];

        // Add one card per typist.
        for (int i = 0; i < seats; i++) {
            right.add(buildTypistCard(i));
            right.add(Box.createVerticalStrut(8));
        }
        right.revalidate(); right.repaint(); // Refresh the display after changing components.
    }

    /**
     * Builds one setup card for a single typist.
     *
     * @param i the index of the typist being configured
     * @return the completed typist setup card
     */
    private JPanel buildTypistCard(int i)
    {
        JPanel c = card("Typist " + (i+1));
        
        // Coloured stripe to make each typist easier to recognise.
        c.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,3,0,0, colors[i]),
            BorderFactory.createEmptyBorder(8,10,8,10)));

        // Text fields for editable name and symbol.
        nameF[i] = tf(typists[i].getName(), 12);
        symF[i]  = tf(symbols[i], 3);

        // Colour button opens a colour chooser.
        colBtn[i] = new JButton("●");
        colBtn[i].setBackground(colors[i]); colBtn[i].setForeground(Color.WHITE);
        colBtn[i].setFont(F_MONOB); colBtn[i].setPreferredSize(new Dimension(36,24));
        colBtn[i].setBorder(BorderFactory.createLineBorder(colors[i].brighter()));
        int fi = i;
        colBtn[i].addActionListener(e -> {
            Color ch = JColorChooser.showDialog(this, "Colour", colors[fi]);
            if (ch != null) {
                colors[fi] = ch;
                rebuildTypistCards(); // Rebuild cards so the coloured stripe updates immediately.
            }
        });

        // Dropdowns for typist-specific race options.
        styleB[i] = combo(Arrays.stream(STYLES).map(s->(String)s[0]).toArray(String[]::new));
        kbB[i]    = combo(Arrays.stream(KEYBOARDS).map(k->(String)k[0]).toArray(String[]::new));
        accB[i]   = combo(ACCESSORIES);
        sponsB[i] = combo(SPONSORS);

        // First row: name, symbol, and colour.
        JPanel r1 = row(); r1.add(lbl("Name:",C_DIM,F_BOLD)); r1.add(nameF[i]);
        r1.add(lbl("Sym:",C_DIM,F_BOLD)); r1.add(symF[i]);
        r1.add(lbl("Col:",C_DIM,F_BOLD)); r1.add(colBtn[i]);

        // Second row: typing style and keyboard type.
        JPanel r2 = row(); r2.add(lbl("Style:",C_DIM,F_BOLD)); r2.add(styleB[i]);
        r2.add(lbl("KB:",C_DIM,F_BOLD)); r2.add(kbB[i]);

        // Third row: accessory and sponsor.
        JPanel r3 = row(); r3.add(lbl("Acc.:",C_DIM,F_BOLD)); r3.add(accB[i]);
        r3.add(lbl("Sponsor:",C_DIM,F_BOLD)); r3.add(sponsB[i]);

        // Add all rows to the card.
        c.add(r1); c.add(r2); c.add(r3);
        return c;
    }

    /**
     * Builds the race tab with live tracks and a race log.
     *
     * @return the completed race panel
     */
    private JPanel buildRaceTab()
    {
        racePanel = panel(new BorderLayout(0,8));
        racePanel.setBorder(pad(10));

         // Track area where each typist's race progress is drawn.
        tracksBox = panel(null);
        tracksBox.setLayout(new BoxLayout(tracksBox, BoxLayout.Y_AXIS));
        
        JScrollPane sc = new JScrollPane(tracksBox);
        sc.getViewport().setBackground(C_BG);
        sc.setBorder(BorderFactory.createEmptyBorder());

        // Log area records race events such as mistakes and burnouts.
        logArea = new JTextArea(5,20);
        logArea.setBackground(C_CARD); logArea.setForeground(C_DIM);
        logArea.setFont(new Font("Courier New", Font.PLAIN, 11));
        logArea.setEditable(false);
        
        JScrollPane logSc = new JScrollPane(logArea);
        logSc.setPreferredSize(new Dimension(0,110));
        logSc.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(C_CARD.brighter()), " Race Log ",
            TitledBorder.LEFT, TitledBorder.TOP, F_BOLD, C_DIM));

        racePanel.add(sc,    BorderLayout.CENTER);
        racePanel.add(logSc, BorderLayout.SOUTH);
        return racePanel;
    }

    /**
     * Rebuilds the live race tracks, creating one TrackPanel per typist.
     */
    private void rebuildTracks()
    {
        tracksBox.removeAll();
        tracks = new TrackPanel[seats];
        for (int i = 0; i < seats; i++) {
            tracks[i] = new TrackPanel(i);
            tracksBox.add(tracks[i]);
            tracksBox.add(Box.createVerticalStrut(6));
        }
        tracksBox.revalidate(); tracksBox.repaint();
    }

     /**
     * Builds the stats tab.
     *
     * @return the completed stats panel
     */
    private JPanel buildStatsTab()
    {
        statsPanel = panel(new BorderLayout(8,8));
        statsPanel.setBorder(pad(12));
        statsPanel.add(lbl("Complete a race to see stats.", C_DIM, F_BODY), BorderLayout.CENTER);
        return statsPanel;
    }

    /**
     * Refreshes the stats tab after a race finishes.
     * This rebuilds the last-race table, personal bests section, and race history display.
     */
    private void refreshStats()
    {
        statsPanel.removeAll();
        statsPanel.setLayout(new BorderLayout(8,8));
        statsPanel.setBorder(pad(12));

        // Build the table showing the most recent race results.
        String[] cols = {"Typist","WPM","Accuracy %","Burnouts","Acc Delta","Points","Earnings"};
        Object[][] data = new Object[seats][7];
        
        for (int i = 0; i < seats; i++) {
            double wpm = wpm(i);
            double acc = totalKS[i]>0 ? 100.0*correctKS[i]/totalKS[i] : 0;
            
            data[i] = new Object[]{typists[i].getName(), String.format("%.1f",wpm),
                String.format("%.1f%%",acc), burnouts[i],
                String.format("%.2f",typists[i].getAccuracy()),
                points[i], String.format("$%.0f",earnings[i])};
        }
        
        JTable lastTbl = new JTable(data, cols);
        styleTable(lastTbl);
        
        JScrollPane top = new JScrollPane(lastTbl);
        top.getViewport().setBackground(C_CARD);
        top.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(C_CARD.brighter())," Last Race Results ",
            TitledBorder.LEFT,TitledBorder.TOP,F_BOLD,C_DIM));

        // Show each typist's best WPM so far.
        JPanel bests = panel(null);
        bests.setLayout(new BoxLayout(bests, BoxLayout.Y_AXIS));
        bests.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(C_CARD.brighter())," Personal Bests (WPM) ",
            TitledBorder.LEFT,TitledBorder.TOP,F_BOLD,C_DIM));
        
            for (int i = 0; i < seats; i++)
            bests.add(lbl(String.format("  %s %s : %.1f WPM", symbols[i], typists[i].getName(), bestWPM[i]), colors[i], F_MONO));

        // Race history log
        JTextArea histArea = new JTextArea();
        histArea.setFont(new Font("Courier New", Font.PLAIN, 11));
        histArea.setBackground(C_CARD); histArea.setForeground(C_TXT);
        histArea.setEditable(false);
        histArea.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
        
        StringBuilder sb = new StringBuilder();
        
        for (int r = raceHistory.size()-1; r >= 0; r--) {
            String[] snap = raceHistory.get(r);
            sb.append(snap[0]).append("\n");
            
            for (int i = 1; i < snap.length; i++)
                sb.append("   ").append(snap[i]).append("\n");
           
            sb.append("\n");
        }
       
        histArea.setText(sb.toString());
        histArea.setCaretPosition(0);
       
        JScrollPane histScroll = new JScrollPane(histArea);
        histScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(C_CARD.brighter())," Race History (newest first) ",
            TitledBorder.LEFT,TitledBorder.TOP,F_BOLD,C_DIM));

         // Split panes arrange the results, bests, and history vertically.
        JSplitPane topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bests);
        topSplit.setDividerLocation(160);
        topSplit.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, histScroll);
        mainSplit.setDividerLocation(280);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());

        statsPanel.add(mainSplit, BorderLayout.CENTER);
        statsPanel.revalidate(); statsPanel.repaint();
    }

     /**
     * Builds the leaderboard tab.
     *
     * @return the completed leaderboard panel
     */
    private JPanel buildLeaderTab()
    {
        leaderPanel = panel(new BorderLayout(8,8));
        leaderPanel.setBorder(pad(12));

        // Table columns for all-time race results.
        String[] cols = {"Rank","Typist","Points","Earnings","Best WPM","Races","Title"};
        
        // Non-editable table model so the user cannot accidentally edit results.
        leaderModel = new DefaultTableModel(cols, 0){ public boolean isCellEditable(int r,int c){return false;} };
        
        JTable tbl = new JTable(leaderModel);
        styleTable(tbl);
        
        JScrollPane sc = new JScrollPane(tbl);
        sc.getViewport().setBackground(C_CARD);
        sc.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(C_CARD.brighter())," Global Leaderboard ",
            TitledBorder.LEFT,TitledBorder.TOP,F_BOLD,C_DIM));

         // Sponsor information is shown underneath the leaderboard.
        JPanel sponsorInfo = card("Active Sponsor Deals");
        sponsorInfo.setPreferredSize(new Dimension(0,90));
        
        JTextArea sLbl = new JTextArea("No sponsors selected.");
        sLbl.setFont(F_MONO); sLbl.setBackground(C_CARD); sLbl.setForeground(C_DIM);
        sLbl.setEditable(false); sLbl.setLineWrap(true); sLbl.setWrapStyleWord(true);
        sLbl.setBorder(BorderFactory.createEmptyBorder(2,6,2,6));
        sLbl.setName("SPONSORLBL");
        sponsorInfo.add(sLbl);

        leaderPanel.add(sc,          BorderLayout.CENTER);
        leaderPanel.add(sponsorInfo, BorderLayout.SOUTH);
        return leaderPanel;
    }

    /**
     * Refreshes the all-time leaderboard.
     */
    private void refreshLeaderboard()
    {
        // Clear old rows before rebuilding.
        leaderModel.setRowCount(0);

        // Sort all-time entries by cumulative points descending
        java.util.List<java.util.Map.Entry<String,double[]>> entries =
            new java.util.ArrayList<>(allTimeBoard.entrySet());
        entries.sort((a,b) -> Double.compare(b.getValue()[0], a.getValue()[0]));

        int rank = 1;

        // Add each leaderboard entry to the table.
        for (java.util.Map.Entry<String,double[]> e : entries) {
            double[] v = e.getValue(); // pts, earnings, bestWPM, races
            String title;
           
            if      (rank==1 && v[0]>=9) title = "Speed Demon";
            else if (rank==1)            title = "Champion";
            else if (rank==2)            title = "Runner-Up";
            else if (rank==3)            title = "Third";
            else                         title = "Competitor";

            leaderModel.addRow(new Object[]{
                rank++,
                e.getKey(),
                (int)v[0] + " pts",
                String.format("$%.0f", v[1]),
                String.format("%.1f", v[2]),
                (int)v[3],
                title
            });
        }

        // Build the sponsor deal summary text.
        StringBuilder sb = new StringBuilder();
        
        for (int i=0;i<seats;i++)
            if (sponsors[i]>0)
                sb.append(symbols[i]).append("  ").append(typists[i].getName())
                  .append("  ·  ").append(SPONSORS[sponsors[i]])
                  .append(":  +$100 bonus if zero burnouts\n");
        
        JTextArea sl = findTextArea("SPONSORLBL");
       
        if (sl!=null) sl.setText(sb.length()==0 ? "No sponsors selected." : sb.toString());
       
        leaderPanel.revalidate();
        leaderPanel.repaint();
    }

     /**
     * Starts a new race.
     *
     * This applies the current setup options, resets race data,
     * creates the track panels, and starts the timer loop.
     */
    private void onStart()
    {
        if (running) return; // Prevents a second race from starting while one is already active.
        applyConfig(); // Reads the setup tab values before the race begins.

        burnouts=new int[seats]; totalKS=new int[seats]; correctKS=new int[seats];
        burntThisRace=new boolean[seats]; turn=0; winner=null;
        raceStart=System.currentTimeMillis();

        for (Typist t : typists) t.resetToStart(); // Reset each typist to the start line.

        rebuildTracks();
        logArea.setText("");
        tabs.setSelectedIndex(1); // Switch automatically to the Race tab.

        running=true; 
        startBtn.setEnabled(false); // Stops the user from clicking Start again mid-race.

        timer = new Timer();

        // Runs one race update every 450 milliseconds.
        timer.scheduleAtFixedRate(new TimerTask(){
            public void run(){ SwingUtilities.invokeLater(()->tick()); }
        }, 150, 450);
    }

    /**
     * Runs one turn of the race.
     *
     * Each typist may type correctly, mistype and slide backwards,
     * recover from burnout, or burn out depending on their state.
     */

    private void tick()
    {
        if (!running) return; // Ignore timer events if the race has stopped.
        turn++;

        for (int i=0;i<seats;i++) {
            Typist t = typists[i];

            if (t.isBurntOut()) {
                // Burnt-out typists spend this turn recovering instead of typing.
                t.recoverFromBurnout();
                log(t.getName()+" recovering ("+t.getBurnoutTurnsRemaining()+" turns left)");
            } else {
                // Start with the typist's current accuracy.
                double acc = t.getAccuracy();

                // Caffeine gives a short early boost.
                if (caffeine && turn<=10) acc = Math.min(1.0, acc+0.10);

                // Night shift makes every typist slightly less accurate.
                if (nightShift) acc = Math.max(0.0, acc-0.05);
                totalKS[i]++; // Count this as an attempted keystroke.

                if (Math.random() < acc) {
                    // Correct keystroke: move forward by one character.
                    t.typeCharacter(); correctKS[i]++;
                    double risk = acc * (double)STYLES[0][2] * 0.08; // Higher performance creates burnout risk.
                    if (caffeine && turn>10) risk *= 1.5; // Caffeine crash increases risk after the early boost ends.
                    int boDur = accessories[i]==1 ? 2 : 3; // Wrist support shortens burnout duration.
                  
                    if (!t.isBurntOut() && Math.random()<risk) {
                        t.burnOut(boDur); burnouts[i]++; burntThisRace[i]=true;
                        log(t.getName()+" BURNT OUT ("+boDur+" turns)!");
                    }
                } else {
                    // Mistype: slide backwards by a random amount.
                    int slide = new Random().nextInt(3)+1;
                    slide = (int)Math.max(1, Math.round(slide*(double)KEYBOARDS[keyboards[i]][2])); // Keyboard type affects how punishing the mistake is.
                    if (autocorrect) slide = Math.max(1, slide/2); // Autocorrect reduces the slide-back penalty.
                    if (accessories[i]==3) slide = Math.max(1, slide-1); // Headphones reduce the slide-back penalty slightly.
                   
                    t.slideBack(slide);
                    log(t.getName()+" mistyped -- back "+slide);
                }

                // Energy drink gives a tiny first-half boost, then a tiny second-half drop.
                if (accessories[i]==2)
                    t.setAccuracy(t.getAccuracy()+(t.getProgress()<passage.length()/2 ? 0.001 : -0.001));
            }

            if (t.getProgress() >= passage.length()) {
                // First typist to finish the passage wins.
                winner=t; running=false; timer.cancel();
                for (TrackPanel tp:tracks) tp.repaint();
                turnLbl.setText("Turn: "+turn);
                onFinish(); return;
            }
        }

        // Repaint tracks after every turn so the display stays live.
        for (TrackPanel tp:tracks) tp.repaint();
        turnLbl.setText("Turn: "+turn);
    }

    /**
     * Finishes the race and updates results.
     *
     * This calculates rankings, points, prize money, personal bests,
     * race history, and the all-time leaderboard.
     */
    private void onFinish()
    {
        startBtn.setEnabled(true);

        // Winner gets a small accuracy improvement.
        double prev = winner.getAccuracy();
        winner.setAccuracy(prev+0.02);

        // Sort typists by progress so prizes and points can be assigned by rank.
        Integer[] idx = new Integer[seats];
        for (int i=0;i<seats;i++) idx[i]=i;
        Arrays.sort(idx,(a,b)->typists[b].getProgress()-typists[a].getProgress());
        
        // Base points and prize money by finishing position.
        int[]    ptTable    = {3,2,1,0,0,0};
        double[] prizeTable = {500,300,150,75,50,25};
        
        double[] racePrize  = new double[seats]; // per-race prize for allTimeBoard
        int[]    racePts    = new int[seats]; // Per-race points used for allTimeBoard.

        raceCount++;

        for (int r=0;r<seats;r++) {
            int i = idx[r];
            int pts = ptTable[Math.min(r,ptTable.length-1)];
            double w = wpm(i); if(w>60) pts++; // Fast typists earn a bonus point.
            if (burntThisRace[i]) pts=Math.max(0,pts-1); // Burnout reduces points.
            points[i]+=pts; racePts[i]=pts;

            double prize=prizeTable[Math.min(r,prizeTable.length-1)];

            // Prize adjustments based on WPM, burnout, and sponsor bonus.
            if(w>60) prize+=100; 
            if(burntThisRace[i]) prize-=50;
            if(sponsors[i]>0&&!burntThisRace[i]) prize+=100;

            prize=Math.max(0,prize);
            earnings[i]+=prize; racePrize[i]=prize;

            if(wpm(i)>bestWPM[i]) bestWPM[i]=wpm(i); // Update personal best WPM.
            if(burntThisRace[i]) typists[i].setAccuracy(typists[i].getAccuracy()-0.01); // Burnout slightly lowers future accuracy.
        }

        // Write race summary to the race log.
        log("------------------------------");
        log("WINNER: "+winner.getName()+"!");
        log(String.format("   Accuracy: %.2f -> %.2f", prev, winner.getAccuracy()));
        
        for (int i=0;i<seats;i++)
            log(String.format("   %s %s  WPM:%.1f  Burnouts:%d  $%.0f",
                symbols[i],typists[i].getName(),wpm(i),burnouts[i],earnings[i]));

        // Update all-time leaderboard keyed by name so old typists persist
        for (int i = 0; i < seats; i++) {
            String key = typists[i].getName();
            double[] entry = allTimeBoard.getOrDefault(key, new double[]{0,0,0,0});
           
            entry[0] += racePts[i]; // cumulative points
            entry[1] += racePrize[i]; // cumulative earnings
            entry[2]  = Math.max(entry[2], bestWPM[i]); // best WPM
            entry[3]++; // races entered

            allTimeBoard.put(key, entry);
        }

        // Save a text snapshot for the race history tab.
        String[] snapshot = new String[seats + 1];
        snapshot[0] = "Race " + raceCount + "  —  Winner: " + winner.getName();
       
        for (int i = 0; i < seats; i++)
            snapshot[i+1] = String.format("%s %s  WPM:%.1f  Acc:%.0f%%  Burnouts:%d",
                symbols[i], typists[i].getName(), wpm(i),
                totalKS[i]>0 ? 100.0*correctKS[i]/totalKS[i] : 0, burnouts[i]);
        raceHistory.add(snapshot);

        // Mark the winning track visually.
        for (int i=0;i<seats;i++)
            if(typists[i]==winner) { tracks[i].setWinner(true); tracks[i].repaint(); }
        refreshStats(); refreshLeaderboard();
    }

    /**
     * Resets the current race without clearing long-term statistics.
     */
    private void onReset()
    {
        if (timer!=null) timer.cancel();
        running=false; startBtn.setEnabled(true);
        turnLbl.setText("Turn: 0"); logArea.setText("");
        for (Typist t:typists) t.resetToStart();
        if (tracks!=null) for (TrackPanel tp:tracks) { tp.setWinner(false); tp.repaint(); }
    }

    /**
     * Applies setup tab choices to the typist objects before the race starts.
     */
    private void applyConfig()
    {
        for (int i=0;i<seats;i++) {
            String n=nameF[i].getText().trim(); if(!n.isEmpty()) typists[i].setName(n);
            String s=symF[i].getText().trim(); if(!s.isEmpty()) { symbols[i]=s; typists[i].setSymbol(s.charAt(0)); }
          
            accessories[i] = accB[i].getSelectedIndex();
            sponsors[i]    = sponsB[i].getSelectedIndex();
            keyboards[i]   = kbB[i].getSelectedIndex();
            
            // Combine base accuracy with selected style and keyboard modifiers.
            double acc = baseAcc[i] + (double)STYLES[styleB[i].getSelectedIndex()][1]
                                    + (double)KEYBOARDS[keyboards[i]][1];
            if(accessories[i]==3) acc+=0.05; // Headphones give a small accuracy bonus.
            typists[i].setAccuracy(acc);
        }
    }

    /**
     * Calculates words per minute for one typist.
     * This uses the common estimate of five characters per word.
     *
     * @param i the typist index
     * @return the calculated WPM
     */
    private double wpm(int i)
    {
        long ms = System.currentTimeMillis()-raceStart;
        return ms<=0 ? 0 : (correctKS[i]/5.0)/(ms/60000.0);
    }

    /**
     * Adds a message to the race log.
     *
     * @param msg the message to display
     */
    private void log(String msg)
    {
        logArea.append(msg+"\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // Helper methods
    private JPanel    panel(LayoutManager lm)       { JPanel p=new JPanel(lm); p.setBackground(C_BG); return p; }
    private JPanel    row()                         { JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,4,2)); p.setBackground(C_CARD); return p; }
    private JLabel    lbl(String t,Color c,Font f)  { JLabel l=new JLabel(t); l.setForeground(c); l.setFont(f); return l; }
    private JCheckBox chk(String t)                 { JCheckBox cb=new JCheckBox(t); cb.setFont(F_BODY); cb.setForeground(C_TXT); cb.setBackground(C_CARD); return cb; }
    private JButton   btn(String t,Color fg)        { JButton b=new JButton(t); b.setFont(F_MONOB); b.setForeground(fg); b.setBackground(C_CARD); b.setFocusPainted(false); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(fg.darker()),BorderFactory.createEmptyBorder(5,14,5,14))); return b; }
    private JTextField tf(String v,int cols)        { JTextField f=new JTextField(v,cols); f.setFont(F_MONO); f.setBackground(C_BG); f.setForeground(C_TXT); f.setCaretColor(C_BLUE); f.setBorder(BorderFactory.createLineBorder(C_CARD.brighter())); return f; }
    private JComboBox<String> combo(String[] items) { JComboBox<String> cb=new JComboBox<>(items); cb.setFont(F_BODY); cb.setBackground(C_BG); cb.setForeground(C_TXT); return cb; }
    private EmptyBorder pad(int n)                  { return new EmptyBorder(n,n,n,n); }


    /**
     * Creates a styled card panel with a titled border.
     *
     * @param title the card title
     * @return the styled card panel
     */
    private JPanel card(String title)
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(C_CARD);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        
        p.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(C_CARD.brighter()),
            " "+title+" ", TitledBorder.LEFT, TitledBorder.TOP, F_BOLD, C_DIM));
        return p;
    }

    /**
     * Applies consistent styling to a JTable.
     *
     * @param t the table to style
     */
    private void styleTable(JTable t)
    {
        t.setBackground(C_CARD); t.setForeground(C_TXT); t.setGridColor(C_BG);
        t.setFont(F_MONO); t.setRowHeight(22);
        t.getTableHeader().setBackground(C_BG); t.getTableHeader().setForeground(C_DIM);
        t.getTableHeader().setFont(F_BOLD);
    }

    // Finds the typist card holder panel by name.
    private JPanel    findPanel(String name)    { return findComp(configPanel, name, JPanel.class); }
    
    // Finds the sponsor text area by name.
    private JTextArea findTextArea(String name) { return findComp(leaderPanel, name, JTextArea.class); }

    /**
     * Recursively searches for a named component inside a container.
     * This is used to find panels or text areas after they have been
     * placed inside nested Swing containers.
     *
     * @param root the container to search inside
     * @param name the component name to look for
     * @param type the expected component type
     * @return the matching component, or null if not found
     */
    private <T extends Component> T findComp(Container root, String name, Class<T> type)
    {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName()) && type.isInstance(c)) return type.cast(c);
            if (c instanceof Container) { T r=findComp((Container)c,name,type); if(r!=null) return r; }
        }
        return null;
    }

    /**
     * Custom panel used to draw one typist's live race track.
     */
    private class TrackPanel extends JPanel
    {
        private final int idx; // Index of the typist this track represents.
        private boolean   isWinner; // Whether this track belongs to the winner.

        /**
         * Creates a track panel for one typist.
         *
         * @param idx the typist index
         */
        TrackPanel(int idx) { this.idx=idx; setPreferredSize(new Dimension(0,58)); setBackground(C_BG); }
        
        /**
         * Sets whether this track should be displayed as the winner.
         *
         * @param w true if this typist won
         */
        void setWinner(boolean w) { isWinner=w; }

        /**
         * Draws the live race track. This includes the typist label, 
         * WPM, visible passage characters, cursor position, burnout state,
         * winner state, and the progress bar.
         *
         * @param g the graphics context used for drawing
         */
        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            if (typists==null||idx>=typists.length) return;
            Graphics2D g2=(Graphics2D)g;

            // Turn on smoother drawing for shapes and text.
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int    w   = getWidth(), h=getHeight();
            Typist t   = typists[idx];
            Color  c   = colors[idx];

            // Keep the displayed position inside the passage length.
            int    pos = Math.min(t.getProgress(), passage.length());
            String sym = symbols[idx];

            g2.setColor(C_CARD); g2.fillRoundRect(0,0,w,h-4,6,6); // Draw the card background for the track.
            g2.setFont(F_MONOB); g2.setColor(c); // Draw typist label and status.

            String label = sym+"  "+t.getName();
            if (t.isBurntOut()) label+="  [BURNT OUT "+t.getBurnoutTurnsRemaining()+"t]";
            if (isWinner)       label+="  [WINNER]";
            g2.drawString(label, 8, 15);

            // Draw current WPM on the right side.
            g2.setFont(new Font("Courier New",Font.PLAIN,11)); g2.setColor(C_DIM);
            g2.drawString(String.format("%.0f WPM", wpm(idx)), w-56, 15);

            // Work out which part of the passage is visible on this track.
            int textY=34, charW=Math.max(7,(w-20)/Math.max(1,passage.length()));
            int vis=(w-20)/Math.max(1,charW);
            int start=Math.max(0,pos-vis/2), end=Math.min(passage.length(),start+vis);
            
            g2.setFont(F_MONO);

            // Draw each visible character with different styling based on progress.
            for (int ci=start;ci<end;ci++) {
                int cx=10+(ci-start)*charW;
                if (ci<pos) { // Characters already typed are highlighted.
                    g2.setColor(new Color(40,80,40)); g2.fillRoundRect(cx-1,textY-13,charW,17,3,3);
                    g2.setColor(C_TXT);
                } else if (ci==pos) { // Current cursor position is highlighted in the typist colour.
                    g2.setColor(isWinner?C_GOLD:c); g2.fillRoundRect(cx-1,textY-13,charW+1,17,3,3);
                    g2.setColor(C_BG); g2.drawString(sym.length()>0 ? sym.substring(0,1) : "?",cx,textY); continue;
                } else { g2.setColor(C_DIM); } // Untyped characters are dimmed.
                g2.drawString(String.valueOf(passage.charAt(ci)),cx,textY);
            }

            // Draw the progress bar at the bottom of the track.
            int bx=10, by=h-10, bw=w-20;
            double pct=passage.length()>0 ? (double)pos/passage.length() : 0;
            
            g2.setColor(new Color(35,38,50)); g2.fillRoundRect(bx,by,bw,5,3,3);
            
            // Burnout, winner, and normal states use different progress colours.
            g2.setColor(t.isBurntOut()?C_RED:(isWinner?C_GOLD:c));
            g2.fillRoundRect(bx,by,(int)(bw*pct),5,3,3);
        }
    }
}