package pl.aiadvisors.draughts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Leaf counts from the starting position; reference numbers from pydraughts 0.6.7 ({@code tools/perft.py}). */
class PerftTest {
    static final long[] BRAZILIAN = {1, 7, 49, 302, 1469, 7473, 37628, 187302};
    static final long[] RUSSIAN = {1, 7, 49, 302, 1469, 7482, 37986, 190146};

    static long perft(Rules rules, Position p, int depth) {
        if (depth == 0) return 1;
        long n = 0;
        for (Move m : rules.legalMoves(p)) n += depth == 1 ? 1 : perft(rules, p.play(m), depth - 1);
        return n;
    }

    @Test
    void brazilian() {
        for (int d = 1; d < BRAZILIAN.length; d++) assertEquals(BRAZILIAN[d], perft(Rules.BRAZILIAN, Position.start(), d), "depth " + d);
    }

    @Test
    void russian() {
        for (int d = 1; d < RUSSIAN.length; d++) assertEquals(RUSSIAN[d], perft(Rules.RUSSIAN, Position.start(), d), "depth " + d);
    }
}
