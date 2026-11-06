package pl.aiadvisors.draughts;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class EngineTest {
    @Test
    void takesAForcedCapture() {
        Engine e = new Engine(Rules.POLISH, new Random(1));
        assertTrue(e.bestMove(Position.parse("b:a1,e3:f4,h8"), Engine.Level.INTERMEDIATE).isCapture());
    }

    @Test
    void strongLevelBeatsTheWeakestAndPlaysOnlyLegalMoves() {
        for (Rules rules : Rules.values()) {
            int[] wins = new int[3];
            Random seeds = new Random(9);
            for (int game = 0; game < 6; game++) {
                Game g = new Game(rules);
                Color strong = game % 2 == 0 ? Color.WHITE : Color.BLACK;
                Engine a = new Engine(rules, new Random(seeds.nextLong())).setTimeLimitMillis(300);
                Engine b = new Engine(rules, new Random(seeds.nextLong())).setTimeLimitMillis(300);
                for (int ply = 0; ply < 200 && !g.result().isOver(); ply++) {
                    boolean strongToMove = g.sideToMove() == strong;
                    Move m = (strongToMove ? a : b).bestMove(g, strongToMove ? Engine.Level.ADVANCED : Engine.Level.BEGINNER);
                    assertNotNull(m);
                    g.play(m);
                }
                GameResult r = g.result();
                wins[!r.isOver() || r.isDraw() ? 1 : r.winner() == strong ? 0 : 2]++;
            }
            assertTrue(wins[0] >= 5, rules + " strong/draw/weak " + Arrays.toString(wins));
        }
    }

    @Test
    void respectsTheTimeLimit() {
        Engine e = new Engine(Rules.POLISH, new Random(2)).setTimeLimitMillis(500);
        long t0 = System.nanoTime();
        assertNotNull(e.bestMove(Position.start(), 30));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(ms < 1000, "took " + ms + " ms");
        assertTrue(e.lastDepth() >= 4, "depth " + e.lastDepth());
    }

    @Test
    void cancelledEngineReturnsNothing() throws Exception {
        Engine e = new Engine(Rules.POLISH).setTimeLimitMillis(60_000);
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            e.cancel();
        });
        t.start();
        long t0 = System.nanoTime();
        assertNull(e.bestMove(Position.start(), 40));
        assertTrue((System.nanoTime() - t0) / 1_000_000 < 2000);
        t.join();
        assertNull(e.bestMove(Position.start(), Engine.Level.BEGINNER));
    }

    @Test
    void noMoveWhenTheGameIsLost() {
        assertNull(new Engine(Rules.POLISH).bestMove(Position.parse("b:b2,c1:a3"), Engine.Level.STRONG));
    }

    @Test
    void evaluationIsSymmetric() {
        assertEquals(0, Engine.evaluate(Position.start()));
        assertEquals(Engine.evaluate(Position.parse("w:Kc3,e3:d6")), -Engine.evaluate(Position.parse("b:Kc3,e3:d6")));
    }
}
