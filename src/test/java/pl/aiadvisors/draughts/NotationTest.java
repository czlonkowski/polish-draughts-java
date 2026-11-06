package pl.aiadvisors.draughts;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotationTest {
    @Test
    void squareNumbersFollowPydraughts() {
        assertEquals("a1", Square.name(Square.fromNumber(1)));
        assertEquals("g1", Square.name(Square.fromNumber(4)));
        assertEquals("b2", Square.name(Square.fromNumber(5)));
        assertEquals("a3", Square.name(Square.fromNumber(9)));
        assertEquals("h8", Square.name(Square.fromNumber(32)));
        for (int n = 1; n <= 32; n++) assertEquals(n, Square.number(Square.fromNumber(n)));
        assertEquals(Square.parse("c3"), Square.parse("10"));
        assertThrows(IllegalArgumentException.class, () -> Square.number(Square.parse("b1")));
    }

    @Test
    void openingMovesInBothNotations() {
        // pydraughts lists 9-13, 10-13, 10-14, 11-14, 11-15, 12-15, 12-16 for the start position.
        List<String> numeric = new ArrayList<>();
        for (Move m : Rules.POLISH.legalMoves(Position.start())) numeric.add(m.toNumeric());
        numeric.sort(null);
        assertEquals("[10-13, 10-14, 11-14, 11-15, 12-15, 12-16, 9-13]", numeric.toString());
        Move m = Rules.POLISH.parseMove(Position.start(), "9-13");
        assertEquals("a3-b4", m.toString());
    }

    @Test
    void fenAsWrittenByPydraughts() {
        String pyd = "W:Wa1,c3,e3,g3,c1,e1,g1,b2,d2,f2,h2,a3:Bb6,d6,f6,h6,a7,c7,e7,g7,b8,d8,f8,h8";
        assertEquals(Position.start(), Position.fromFen(pyd));
        assertEquals(Position.start(), Position.parse(pyd));
        assertEquals(Position.start(), Position.parse("W:W1-12:B21-32"));
        assertEquals("W:W1,2,3,4,5,6,7,8,9,10,11,12:B21,22,23,24,25,26,27,28,29,30,31,32", Position.start().toNumericFen());
        Position p = Position.parse("B:WKa1,c3:Bb6,Kh8");
        assertEquals("B:WKa1,c3:Bb6,Kh8", p.toFen());
        assertEquals("b:Ka1,c3:b6,Kh8", p.toShort());
        assertEquals(p, Position.parse(p.toShort()));
        assertEquals(p, Position.parse(p.toNumericFen()));
        assertEquals(p, Position.parse("B:WKa1,c3:Bb6,Kh8:H0:F1"), "extra fields are ignored");
        assertEquals("W:W:B", Position.builder().build().toFen());
        assertEquals(Piece.WHITE_KING, Position.parse("W:Wb8:Ba1").pieceAt("b8"), "a man on the last row is a king");
    }

    @Test
    void builder() {
        Position p = Position.builder().sideToMove(Color.BLACK).put("c3", Piece.WHITE_MAN).put("d4", Piece.BLACK_KING).build();
        assertEquals("B:Wc3:BKd4", p.toFen());
        assertEquals(1, p.count(Piece.BLACK_KING));
    }

    @Test
    void movesByEndpointsOnly() {
        Position p = Position.parse("w:Ka1:c3,f4");
        assertEquals("a1:e5:g3", Rules.POLISH.parseMove(p, "a1:g3").toString());
        assertEquals("a1:e5:g3", Rules.POLISH.parseMove(p, "a1xe5xg3").toString());
        assertEquals("1x19x12", Rules.POLISH.parseMove(p, "a1:g3").toNumeric());
        // The king on b8 reaches e1 by two routes taking different pieces: the endpoints alone are ambiguous.
        Position two = Position.parse("w:Kb8:d2,h2,d4,h4,c5,c7");
        assertThrows(IllegalArgumentException.class, () -> Rules.POLISH.parseMove(two, "b8:e1"));
        assertEquals("b8:d6:b4:e1", Rules.POLISH.parseMove(two, "b8:d6:b4:e1").toString());
        assertEquals("b8:e5:c3:e1", Rules.POLISH.parseMove(two, "b8:c3:e1").toString());
        assertEquals("b8:e5:c3:e1", Rules.POLISH.parseMove(two, "29x19x10x3").toString());
    }
}
