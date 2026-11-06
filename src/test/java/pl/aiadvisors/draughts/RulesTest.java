package pl.aiadvisors.draughts;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** The rules spelled out one by one. The bulk check against pydraughts is {@link OracleTest}. */
class RulesTest {
    static Set<String> moves(Rules rules, String position) {
        Set<String> s = new TreeSet<>();
        for (Move m : rules.legalMoves(Position.parse(position))) s.add(m.toString());
        return s;
    }

    static Set<String> moves(String position) {
        return moves(Rules.POLISH, position);
    }

    static Set<String> set(String... t) {
        return new TreeSet<>(Arrays.asList(t));
    }

    @Test
    void startingPosition() {
        Position start = Position.start();
        assertEquals(12, start.count(Color.WHITE));
        assertEquals(12, start.count(Color.BLACK));
        assertEquals(Color.WHITE, start.sideToMove());
        assertEquals(7, Rules.POLISH.legalMoves(start).size());
        assertEquals(Position.START, start.toShort());
        for (int i = 0; i < 64; i++) if (start.pieceAt(i) != null) assertTrue(Square.isDark(i));
        assertTrue(Square.isDark(Square.parse("a1")));
        assertFalse(Square.isDark(Square.parse("b1")));
        assertSame(Rules.BRAZILIAN, Rules.POLISH);
    }

    @Test
    void menStepForwardOnly() {
        assertEquals(set("d4-c5", "d4-e5"), moves("w:d4:h8"));
        assertEquals(set("e5-d4", "e5-f4"), moves("b:a1:e5"));
    }

    @Test
    void captureIsCompulsoryAndMenCaptureBackwards() {
        assertEquals(set("d4:b6"), moves("w:d4,a1:c5"));
        assertEquals(set("d4:b2"), moves("w:d4:c3,h8"));
    }

    @Test
    void longestCaptureIsCompulsoryCountingPiecesNotKings() {
        assertEquals(set("a1:c3:e5"), moves("w:a1,h2:b2,d4,g3"));
        assertEquals(set("c1:a3", "h2:f4"), moves("w:c1,h2:Kb2,g3"));
    }

    @Test
    void flyingKings() {
        assertEquals(set("a1:d4", "a1:e5", "a1:f6", "a1:g7", "a1:h8"), moves("w:Ka1:c3"));
        assertEquals(6, moves("w:Ka1:h8").size());
        assertTrue(moves("w:Ka1:b2,c3").isEmpty(), "two in a row cannot be jumped");
        assertEquals(set("g1-f2", "g1-h2"), moves("w:Ka1,g1:b2,c3"));
    }

    @Test
    void kingMustLandWhereTheCaptureContinues() {
        for (Rules r : Rules.values()) assertEquals(set("a1:e5:g3", "a1:e5:h2"), moves(r, "w:Ka1:c3,f4"), r.name());
    }

    @Test
    void turkishStrike() {
        for (Move m : Rules.POLISH.legalMoves(Position.parse("w:Ka1:b2,d4,f4,d6,f6"))) {
            Set<Integer> c = new HashSet<>();
            for (int x : m.captured()) assertTrue(c.add(x), "never jump the same piece twice " + m);
        }
    }

    @Test
    void manPassingTheLastRowStaysAMan() {
        Position p = Position.parse("w:b6,a1:c7,e7,h2");
        List<Move> l = Rules.POLISH.legalMoves(p);
        assertEquals("b6:d8:f6", l.get(0).toString());
        assertFalse(l.get(0).promotes());
        assertEquals(Piece.WHITE_MAN, p.play(l.get(0)).pieceAt("f6"));
        Position crown = Position.parse("w:c5,a1:b6,h2");
        assertEquals(Piece.WHITE_MAN, crown.play(Rules.POLISH.legalMoves(crown).get(0)).pieceAt("a7"));
    }

    @Test
    void promotion() {
        Position end = Position.parse("w:b6,a1:c7,h2");
        Move m = Rules.POLISH.parseMove(end, "b6:d8");
        assertTrue(m.promotes());
        assertEquals(Piece.WHITE_KING, end.play(m).pieceAt("d8"));
        Position step = Position.parse("w:c7,a1:h2");
        assertEquals(Piece.WHITE_KING, step.play(Rules.POLISH.parseMove(step, "c7-b8")).pieceAt("b8"));
        Position black = Position.parse("b:h8:b2");
        assertEquals(Piece.BLACK_KING, black.play(Rules.POLISH.parseMove(black, "b2-a1")).pieceAt("a1"));
    }

    @Test
    void russianPromotesDuringCaptureAndContinuesAsKing() {
        assertEquals(set("b6:d8"), moves(Rules.BRAZILIAN, "w:b6:c7,f6"));
        assertEquals(set("b6:d8:g5", "b6:d8:h4"), moves(Rules.RUSSIAN, "w:b6:c7,f6"));
        Position p = Position.parse("w:b6:c7,f6");
        Move m = Rules.RUSSIAN.parseMove(p, "b6:d8:h4");
        assertTrue(m.promotes());
        assertEquals(Piece.WHITE_KING, p.play(m).pieceAt("h4"));
    }

    @Test
    void russianHasNoMaximumCaptureRule() {
        assertEquals(set("a1:c3:e5"), moves(Rules.BRAZILIAN, "w:a1,h2:b2,d4,g3"));
        assertEquals(set("a1:c3:e5", "h2:f4"), moves(Rules.RUSSIAN, "w:a1,h2:b2,d4,g3"));
    }

    @Test
    void winsAndLosses() {
        Game blocked = new Game(Rules.POLISH, Position.parse("b:b2,c1:a3"));
        assertEquals(Color.WHITE, blocked.result().winner());
        assertEquals(GameResult.Reason.NO_MOVES, blocked.result().reason());
        Game last = new Game(Rules.POLISH, Position.parse("w:c3:d4"));
        last.play("c3:e5");
        assertEquals(Color.WHITE, last.result().winner());
        assertEquals(GameResult.Reason.NO_PIECES, last.result().reason());
        assertFalse(new Game().result().isOver());
        assertSame(GameResult.ONGOING, new Game().result());
        assertThrows(IllegalStateException.class, () -> last.play("e5-f6"));
    }

    @Test
    void threefoldRepetition() {
        Game g = new Game(Rules.POLISH, Position.parse("w:Kc1:Kh8"));
        String[] dance = {"c1-d2", "h8-g7", "d2-c1", "g7-h8"};
        for (int i = 0; i < 8 && !g.result().isOver(); i++) g.play(dance[i % 4]);
        assertTrue(g.result().isDraw());
        assertEquals(GameResult.Reason.THREEFOLD_REPETITION, g.result().reason());
        assertEquals(8, g.moves().size());
    }

    @Test
    void kingMoveLimit() {
        Position slow = Position.parse("w:Ka1,Kc1:Kh8,Kf8").withQuietPlies(50);
        assertEquals(GameResult.Reason.KING_MOVES_LIMIT, new Game(Rules.POLISH, slow).result().reason());
        assertFalse(new Game(Rules.POLISH, slow.withQuietPlies(49)).result().isOver());
        assertEquals(GameResult.Reason.KING_MOVES_LIMIT, new Game(Rules.RUSSIAN, slow.withQuietPlies(30)).result().reason());
        Position reset = Position.parse("w:Ka1,c1:Kh8").withQuietPlies(10);
        assertEquals(0, reset.play(Rules.POLISH.parseMove(reset, "c1-d2")).quietPlies());
        assertEquals(11, reset.play(Rules.POLISH.parseMove(reset, "a1-b2")).quietPlies());
    }

    @Test
    void gamesReplayFromText() {
        for (Rules rules : Rules.values()) {
            Game g = new Game(rules);
            Random r = new Random(4);
            List<String> played = new ArrayList<>();
            for (int i = 0; i < 80 && !g.result().isOver(); i++) {
                List<Move> l = g.legalMoves();
                Move m = l.get(r.nextInt(l.size()));
                played.add(i % 2 == 0 ? m.toString() : m.toNumeric());
                g.play(m);
            }
            Game replay = new Game(rules);
            for (String s : played) replay.play(s);
            assertEquals(g.position(), replay.position());
            assertTrue(replay.undo());
            assertEquals(g.positions().get(g.positions().size() - 2), replay.position());
        }
    }

    @Test
    void illegalInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Position.parse("w:b1:h8"), "light square");
        assertThrows(IllegalArgumentException.class, () -> Position.parse("w:a1,a1:h8"), "twice");
        assertThrows(IllegalArgumentException.class, () -> Position.parse("x:a1:h8"));
        assertThrows(IllegalArgumentException.class, () -> new Game().play("a3-a4"));
        assertThrows(IllegalArgumentException.class, () -> new Game().play("c3-d5"));
        assertThrows(IllegalArgumentException.class, () -> new Game().play("hello"));
    }
}
