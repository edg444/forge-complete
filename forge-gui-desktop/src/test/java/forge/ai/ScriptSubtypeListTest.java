package forge.ai;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardType;
import forge.localinstance.properties.ForgeConstants;

/**
 * A subtype missing from TypeLists.txt still parses, but CardType.getCreatureTypes() filters it out, so the card
 * shows no subtype in game and type checks miss it (Toy Boat's Ship, 2026-10-30, and fourteen other Un and
 * playtest types). Scryfall's type catalog leaves out Un-only types, so they have to be added by hand.
 */
public class ScriptSubtypeListTest extends AITest {

    @Test
    public void testCreatureScriptSubtypesAreListed() throws IOException {
        initAndCreateGame();
        final List<String> missing = new ArrayList<>();
        final File root = new File(ForgeConstants.CARD_DATA_DIR);
        try (Stream<Path> files = Files.walk(root.toPath())) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".txt"))::iterator) {
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    if (!line.startsWith("Types:")) {
                        continue;
                    }
                    final CardType type = CardType.parse(line.substring(6).trim(), false);
                    if (!type.isCreature() && !type.isKindred()) {
                        continue;
                    }
                    for (String sub : type.getSubtypes()) {
                        if (!CardType.isASubType(sub)) {
                            missing.add(p.getFileName() + ": " + sub);
                        }
                    }
                }
            }
        }
        AssertJUnit.assertTrue("subtypes not in TypeLists.txt: " + missing, missing.isEmpty());
    }
}
