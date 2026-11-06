package pl.aiadvisors.draughts;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compares move generation with pydraughts 0.6.7 on positions from {@code tools/oracle.py}: for each
 * position, the set of successor positions must be identical and no two of our moves may lead to the
 * same position. A larger file can be checked with
 * {@code mvn test -Dtest=OracleTest -Doracle.brazilian=big.txt -Doracle.russian=big2.txt}.
 */
class OracleTest {
    @Test
    void brazilianMatchesPydraughts() throws IOException {
        check(Rules.BRAZILIAN, "oracle-brazilian.txt", System.getProperty("oracle.brazilian"));
    }

    @Test
    void russianMatchesPydraughts() throws IOException {
        check(Rules.RUSSIAN, "oracle-russian.txt", System.getProperty("oracle.russian"));
    }

    static String norm(String fen) {
        String[] s = fen.split(":", -1);
        return s[0] + ":W" + sort(s[1].substring(1)) + ":B" + sort(s[2].substring(1));
    }

    static String sort(String list) {
        if (list.isEmpty()) return "";
        String[] t = list.split(",");
        Arrays.sort(t);
        return String.join(",", t);
    }

    static void check(Rules rules, String resource, String file) throws IOException {
        InputStream in = file != null ? new FileInputStream(file) : OracleTest.class.getResourceAsStream("/" + resource);
        assertNotNull(in, resource);
        int lines = 0, bad = 0, captures = 0, kings = 0;
        StringBuilder report = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                if (line.isEmpty()) continue;
                lines++;
                String[] part = line.split("\\|", -1);
                Set<String> want = new TreeSet<>(Arrays.asList(part[1].split(";")));
                Position p = Position.fromFen(part[0]);
                assertEquals(norm(part[0]), norm(p.toFen()), "FEN round trip");
                List<Move> moves = rules.legalMoves(p);
                Set<String> got = new TreeSet<>();
                for (Move m : moves) got.add(norm(p.play(m).toFen()));
                if (!moves.isEmpty() && moves.get(0).isCapture()) captures++;
                if (part[0].contains("K")) kings++;
                if (!got.equals(want) || got.size() != moves.size()) {
                    if (++bad <= 5) {
                        Set<String> onlyTheirs = new TreeSet<>(want), onlyOurs = new TreeSet<>(got);
                        onlyTheirs.removeAll(got);
                        onlyOurs.removeAll(want);
                        report.append("\n").append(part[0]).append("\n  only pydraughts: ").append(onlyTheirs)
                                .append("\n  only ours: ").append(onlyOurs).append(" moves=").append(moves);
                    }
                }
            }
        }
        System.out.println(rules + ": " + lines + " positions (" + captures + " with captures, " + kings + " with kings), mismatches: " + bad);
        assertTrue(lines > 0, "no positions read");
        assertEquals(0, bad, "move generation differs from pydraughts:" + report);
    }
}
