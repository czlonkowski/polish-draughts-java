# polish-draughts

Rules and a small computer opponent for **8 × 8 draughts in Java**: Polish draughts
(*warcaby*), which on the 8 × 8 board are the same as **Brazilian draughts**, plus
**Russian draughts** as a second rule set.

- Move generator checked against [pydraughts](https://github.com/AttackingOrDefending/pydraughts)
  on more than 46 000 positions with no differences, plus perft from the starting position.
- FEN compatible with pydraughts and lidraughts (`W:Wa1,Kc3:Bb6`), algebraic moves (`c3-d4`,
  `c3:e5:g7`) and numeric moves (`10-14`, `10x19x28`).
- Alpha-beta engine with five levels, a time limit and cancellation.
- No runtime dependencies, Java 8+, 27 KB jar, no Android-incompatible APIs.

> **Po polsku:** biblioteka z zasadami polskich warcabów 8 × 8 (te same co brazylijskie:
> bicie obowiązkowe i maksymalne, bicie do tyłu, damka dalekosiężna, zbite pionki schodzą
> po całym ruchu) i warcabów rosyjskich, z prostym przeciwnikiem komputerowym. Generator ruchów
> porównano z pydraughts na ponad 46 tys. pozycji bez żadnej różnicy. Powstała do gry dla dzieci
> w aplikacji na Androida „Mała Orbita”. Licencja MIT.

## Rules

Both variants: White starts; play is on the dark squares (a1 is dark); men step one square
diagonally forward and capture both forwards and backwards; capturing is compulsory and a capture
sequence must be played to the end; kings move and capture any distance along a diagonal and may
land on any free square behind the captured piece (if some landing squares allow the capture to
continue, the king must pick one of those); captured pieces are removed only after the whole move,
so a piece cannot be jumped twice and still blocks the way (the "Turkish strike" rule).

| | `Rules.BRAZILIAN` = `Rules.POLISH` | `Rules.RUSSIAN` |
|---|---|---|
| Choice of capture | the sequence taking the most pieces is compulsory (kings count like men) | any capture sequence |
| Man reaching the last row during a capture | stays a man and keeps capturing as a man; crowned only if the move ends there | crowned at once and keeps capturing as a king |
| Draw by king moves | 25 moves each | 15 moves each |

A side with no legal move (including no pieces) loses. Draws: threefold repetition of the same
position with the same side to move, or the number of moves above by each side with only kings
moving and nothing captured. These draw rules are a simplification of the official rules, which
have extra endgame-specific counts.

## Installation

The library is built by [JitPack](https://jitpack.io/#czlonkowski/polish-draughts-java) straight
from this repository.

**Maven**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.czlonkowski</groupId>
    <artifactId>polish-draughts-java</artifactId>
    <version>v0.1.0</version>
</dependency>
```

**Gradle**

```groovy
repositories { maven { url 'https://jitpack.io' } }
dependencies { implementation 'com.github.czlonkowski:polish-draughts-java:v0.1.0' }
```

Or download the jar from the [releases page](https://github.com/czlonkowski/polish-draughts-java/releases),
or copy the nine source files from `src/main/java`: there are no dependencies.

## Usage

```java
import pl.aiadvisors.draughts.*;

Game game = new Game(Rules.POLISH);              // or Rules.RUSSIAN
game.play("c3-d4");                              // algebraic...
game.play("23-19");                              // ...or numeric (1 = a1, 32 = h8)
System.out.println(game.legalMoves());           // [d4:f6]: capturing is compulsory

Engine engine = new Engine(Rules.POLISH).setTimeLimitMillis(1000);
Move reply = engine.bestMove(game, Engine.Level.INTERMEDIATE);
game.play(reply);

GameResult result = game.result();               // ONGOING, or winner + reason
System.out.println(game.position().toFen());     // W:W...:B... as in pydraughts / lidraughts
Position p = Position.parse("W:WKa1:Bc3,f4");    // FEN or short "w:Ka1:c3,f4"
System.out.println(Rules.POLISH.legalMoves(p));  // [a1:e5:g3, a1:e5:h2]
```

This exact code runs as `ReadmeExampleTest`.

### API overview

| Class | What it does |
|---|---|
| `Rules` | `legalMoves(Position)`, `parseMove(Position, String)`, `isLegal`; variants `BRAZILIAN`/`POLISH` and `RUSSIAN` |
| `Position` | immutable board + side to move + quiet-move counter; `parse`, `fromFen`, `toFen`, `toNumericFen`, `toShort`, `pieceAt`, `play(Move)`, `Builder` |
| `Move` | `from()`, `to()`, `path()`, `captured()`, `promotes()`; `toString()` is algebraic, `toNumeric()` numeric. Two moves are equal when they start and end on the same squares and capture the same pieces |
| `Game` | moves played, validation, `undo()`, `result()` |
| `GameResult` | `isOver()`, `isDraw()`, `winner()`, `reason()`: `NO_PIECES`, `NO_MOVES`, `THREEFOLD_REPETITION`, `KING_MOVES_LIMIT` |
| `Engine` | `bestMove(position or game, Level)` or `bestMove(position, depth)`; `setTimeLimitMillis`, `setNodeLimit`, `cancel()` (thread-safe) |
| `Square` | square index 0..63 (`row * 8 + column`, a1 = 0) ↔ name `"c3"` ↔ number 1..32 |

### Notation

- **FEN** (pydraughts, lidraughts): `W:Wa1,c1,Kd4:Bb6,d8` — side to move, White pieces, Black
  pieces, `K` marks a king. Square numbers and ranges are accepted on input (`W:W1-12:B21-32`);
  extra fields after the pieces are ignored.
- **Short notation**: `w:a1,c1,Kd4:b6,d8`.
- **Moves**: `c3-d4` for a step; a capture lists every landing square, `c3:e5:g7`. `x` works as a
  separator too. Numeric moves use the numbering of pydraughts for Russian and Brazilian
  draughts: 1 = a1, 2 = c1, 3 = e1, 4 = g1, 5 = b2, …, 32 = h8 (so the first move `c3-d4` is
  `10-14`). When parsing, a capture may be given by its start and end square only (`a1:g3`) if
  that is unambiguous.

### Engine levels

| Level | Depth (plies) | Random moves |
|---|---|---|
| `BEGINNER` | 1 | 45 % |
| `NOVICE` | 2 | 18 % |
| `INTERMEDIATE` | 4 | – |
| `ADVANCED` | 6 | – |
| `STRONG` | 9 | – |

Captures extend the search (they are forced). The default time limit is 2.5 s per move; on a
laptop `STRONG` takes about 1 s from the opening position (it stops at its node cap of 3 million). `cancel()` stops a search running in
another thread; a cancelled engine returns `null` from then on.

## How it was verified

1. **Against pydraughts.** [`tools/oracle.py`](tools/oracle.py) plays random games and builds
   random positions with men and kings, and writes each position together with the set of all
   positions reachable in one legal move according to pydraughts 0.6.7. `OracleTest` requires
   exactly the same set from this library, and no two of its moves may lead to the same position.
   A full run of 23 188 Brazilian and 23 213 Russian positions (about 20 000 with captures and
   23 600 with kings) gave **0 differences**. The repository keeps a sample of about 600 positions
   per variant, which runs with the normal tests. To repeat the full run:

   ```bash
   python3 -m venv /tmp/pyd && /tmp/pyd/bin/pip install pydraughts==0.6.7
   /tmp/pyd/bin/python -I tools/oracle.py brazilian 300 7 > /tmp/big-brazilian.txt
   /tmp/pyd/bin/python -I tools/oracle.py russian 300 11 > /tmp/big-russian.txt
   mvn test -Dtest=OracleTest -Doracle.brazilian=/tmp/big-brazilian.txt -Doracle.russian=/tmp/big-russian.txt
   ```

2. **Perft** from the starting position, i.e. the number of move sequences of a given length.
   `PerftTest` checks depths 1–7 against pydraughts ([`tools/perft.py`](tools/perft.py)).
   Deeper counts from this library, for reference:

   | Depth | Brazilian / Polish | Russian |
   |---:|---:|---:|
   | 1 | 7 | 7 |
   | 2 | 49 | 49 |
   | 3 | 302 | 302 |
   | 4 | 1 469 | 1 469 |
   | 5 | 7 473 | 7 482 |
   | 6 | 37 628 | 37 986 |
   | 7 | 187 302 | 190 146 |
   | 8 | 907 830 | 929 899 |
   | 9 | 4 431 766 | 4 570 586 |
   | 10 | 21 560 022 | 22 444 032 |
   | 11 | 105 491 257 | 110 917 189 |

   Depth 11 takes about 9 s on a laptop.

3. **Rules one by one** in `RulesTest`: men move forward only, compulsory and maximum capture,
   backward captures, flying kings, landing squares, the Turkish strike, promotion (or not) during a
   capture, wins, both draw rules, replaying games from text.

## Limitations

- The engine is deliberately simple: material, advancement of men and centre control; no
  transposition table, no opening book, no endgame databases. It is meant as a friendly opponent
  for beginners and children, not as a strong analysis engine.
- Draw rules are simplified (see above). There is no clock and no draw offer.
- Moves that start and end on the same squares and capture the same pieces by different routes
  are treated as one move (they lead to the same position); the first route found is kept.

## Building

```bash
mvn verify
```

Java 8 or newer, Maven 3. The jar has no dependencies; JUnit 5 is used for tests only.

## Acknowledgements

[pydraughts](https://github.com/AttackingOrDefending/pydraughts) by Ioannis Pantidis (MIT) served
as the reference implementation for the tests; it is used only as a development tool and is not a
dependency. The library started as the checkers engine of *Mała Orbita*, a home-made Android
app for kids.

## License

[MIT](LICENSE)
