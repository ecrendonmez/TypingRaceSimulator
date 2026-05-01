# TypingRaceSimulator

**ECS414U — Object-Oriented Programming Project**
author: Ecren Donmez
version 1.0

A two-part Java project: a terminal-based typing race simulator (Part I) and a full Swing GUI version (Part II), with Git integration throughout.

---

## Project Structure

```
TypingRaceSimulator/
├── .git/                   # Git repository (initialised at project start)
├── README.md               # This file
│
├── Part1/
│   ├── Typist.java         # Completed Typist class (Task 1)
│   └── TypingRace.java     # Fixed & improved TypingRace class (Task 2)
│
└── Part2/
    ├── Typist.java         # Same Typist class (shared)
    └── TypingRaceGUI.java  # Full Swing GUI (Part II)
```

## Dependencies
- Java Development Kit (JDK) 11 or higher
- No external libraries required
- Part 1 uses `java.util.Random` and standard terminal output
- Part 2 uses `javax.swing` and `java.awt` (included in the standard JDK)

---

## How to Compile and Run

### How to compile

```bash
cd Part1
javac Typist.java TypingRace.java
```

### How to run

```bash
java TypingRace
```

The race is started by calling `startRace()` on a `TypingRace` object. The `main` method in `TypingRace` sets up three typists and runs the race automatically.

### How to run the tests

```bash
java Typist
```

Manual tests are in the `main` method of `Typist.java`. They cover all five required test areas: typeCharacter(), slideBack() clamping, burnout countdown, resetToStart(),
and setAccuracy() clamping.


## Part 2 — GUI Simulation

### How to compile

```bash
cd Part2
javac Typist.java TypingRaceGUI.java
```

### How to run

```bash
java TypingRaceGUI
```

The graphical version is started by calling `startRaceGUI()`, which launches the full Swing interface with setup, race, stats, and leaderboard tabs.


## Notes
- The starter code in Part1 was originally written by Ty Posaurus. Four bugs were found and fixed, with four additional improvements made during the review.
- All code compiles and runs using standard command-line tools without any IDE-specific configuration.
- ANSI escape codes are used for terminal animation in Part 1. These may not render correctly in all Windows terminals, use Windows Terminal or a POSIX shell for best results.

GitHub Repository: https://github.com/ecrendonmez/TypingRaceSimulator
