package forge.ai;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.testng.AssertJUnit;
import org.testng.SkipException;
import org.testng.annotations.Test;

import forge.game.keyword.Keyword;
import forge.localinstance.properties.ForgeConstants;
import forge.util.Lang;

/**
 * Guard against K: lines rewritten for display. A K: line is engine input, not only text: its head is parsed
 * into a Keyword ("Start your engines", never "Start your engines!"), and sentence keywords are found by their
 * exact text from Java (hasKeyword("You may choose not to untap CARDNAME during your untap step.")). The in-game
 * text retemplating of 2026-09-30 broke 154 such lines silently; the Oracle wording belongs in the display code
 * (CardFactoryUtil.keywordAsPrinted), and K: lines keep upstream's text.
 */
public class KeywordScriptTextTest extends AITest {

    private static final Pattern JAVA_STRING = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"");
    private static final String[] SOURCE_ROOTS = {"forge-core", "forge-game", "forge-ai", "forge-gui"};
    private static final String[] SELF_NOUNS = {"creature", "artifact", "land", "enchantment", "permanent", "card",
            "planeswalker", "battle", "Aura", "Equipment", "Saga", "Vehicle", "Spacecraft", "Contraption", "Siege",
            "Class", "Case", "Attraction", "Room", "spell", "token"};

    private record KLine(String file, String face, String text) { }

    private static List<KLine> keywordLines() throws IOException {
        List<KLine> out = new ArrayList<>();
        for (String dir : new String[] {ForgeConstants.CARD_DATA_DIR, ForgeConstants.TOKEN_DATA_DIR}) {
            File root = new File(dir);
            if (!root.isDirectory()) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root.toPath())) {
                for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".txt"))::iterator) {
                    String face = "";
                    for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        if (line.startsWith("Name:")) {
                            face = line.substring(5).trim();
                        } else if (line.startsWith("K:")) {
                            out.add(new KLine(p.getFileName().toString(), face, line.substring(2).trim()));
                        }
                    }
                }
            }
        }
        return out;
    }

    private static Set<String> javaStringLiterals() throws IOException {
        Set<String> out = new HashSet<>();
        for (String module : SOURCE_ROOTS) {
            File src = new File("../" + module + "/src/main/java");
            if (!src.isDirectory()) {
                continue;
            }
            try (Stream<Path> files = Files.walk(src.toPath())) {
                for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                    Matcher m = JAVA_STRING.matcher(Files.readString(p, StandardCharsets.UTF_8));
                    while (m.find()) {
                        out.add(m.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
                    }
                }
            }
        }
        return out;
    }

    @Test
    public void testKeywordHeadsStillParse() throws IOException {
        initAndCreateGame();
        List<KLine> lines = keywordLines();
        AssertJUnit.assertTrue("card scripts not found", lines.size() > 10000);
        List<String> broken = new ArrayList<>();
        for (KLine k : lines) {
            if (Keyword.getInstance(k.text()).getKeyword() != Keyword.UNDEFINED) {
                continue;
            }
            // "Start your engines!" or "Flying." - a known keyword with print punctuation added
            String head = k.text().split(":", 2)[0].replaceAll("[.!]+$", "");
            if (Keyword.smartValueOf(head) != Keyword.UNDEFINED) {
                broken.add(k.file() + ": K:" + k.text());
            }
        }
        AssertJUnit.assertTrue("K: lines that no longer parse as their keyword:\n" + String.join("\n", broken),
                broken.isEmpty());
    }

    // "Escalate:Discard.<1/Card>" parsed as Escalate {0}: the cost parser doesn't know "Discard." and drops it
    @Test
    public void testKeywordCostsHaveNoPrintPunctuation() throws IOException {
        initAndCreateGame();
        List<KLine> lines = keywordLines();
        AssertJUnit.assertTrue("card scripts not found", lines.size() > 10000);
        Pattern glued = Pattern.compile("(?:^|[: ])[A-Za-z]+[.!,;]<");
        List<String> broken = new ArrayList<>();
        for (KLine k : lines) {
            if (glued.matcher(k.text()).find()) {
                broken.add(k.file() + ": K:" + k.text());
            }
        }
        AssertJUnit.assertTrue("K: line costs with punctuation in the cost type:\n" + String.join("\n", broken),
                broken.isEmpty());
    }

    @Test
    public void testSentenceKeywordsKeepTheTextJavaLooksUp() throws IOException {
        initAndCreateGame();
        Set<String> literals = javaStringLiterals();
        if (literals.isEmpty()) {
            throw new SkipException("engine sources not found next to forge-gui-desktop");
        }
        List<String> longLiterals = new ArrayList<>();
        for (String l : literals) {
            if (l.length() >= 20 && l.contains("CARDNAME")) {
                longLiterals.add(l);
            }
        }
        List<KLine> lines = keywordLines();
        AssertJUnit.assertTrue("card scripts not found", lines.size() > 10000);
        List<String> broken = new ArrayList<>();
        for (KLine k : lines) {
            String text = k.text();
            if (Keyword.getInstance(text).getKeyword() != Keyword.UNDEFINED || literals.contains(text)) {
                continue;
            }
            String name = Lang.rulesTextName(k.face());
            Set<String> selves = new HashSet<>();
            for (String noun : SELF_NOUNS) {
                selves.add("this " + noun);
                selves.add("This " + noun);
            }
            if (!name.isEmpty()) {
                selves.add(name);
                selves.add(Lang.getInstance().getNickName(name));
            }
            for (String self : selves) {
                if (!text.contains(self)) {
                    continue;
                }
                for (String as : new String[] {"CARDNAME", name}) {
                    if (as.isEmpty() || as.equals(self)) {
                        continue;
                    }
                    String original = text.replace(self, as);
                    boolean lookedUp = literals.contains(original)
                            || longLiterals.stream().anyMatch(original::startsWith);
                    if (lookedUp) {
                        broken.add(k.file() + ": K:" + text + "  (the engine looks for: " + original + ")");
                    }
                }
            }
        }
        AssertJUnit.assertTrue("K: lines reworded away from the text the engine looks up (keep the K: line as"
                + " upstream wrote it; display wording goes in CardFactoryUtil.keywordAsPrinted):\n"
                + String.join("\n", broken), broken.isEmpty());
    }
}
