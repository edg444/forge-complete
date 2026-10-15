package forge.ai;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.localinstance.properties.ForgeConstants;

/**
 * "nontoken" is written {@code !token}: a {@code nonToken} property is read as "doesn't have the card type Token",
 * which every card and token passes, so the filter silently lets tokens through.
 */
public class NonTokenFilterTest extends AITest {

    private static final Pattern NON_TOKEN = Pattern.compile("[.+]non[Tt]oken\\b");

    @Test
    public void noScriptWritesNonToken() throws IOException {
        initAndCreateGame();
        List<String> offenders = new ArrayList<>();
        for (String dir : new String[] {ForgeConstants.CARD_DATA_DIR, ForgeConstants.TOKEN_DATA_DIR}) {
            File root = new File(dir);
            if (!root.isDirectory()) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root.toPath())) {
                for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".txt"))::iterator) {
                    for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        if (NON_TOKEN.matcher(line).find()) {
                            offenders.add(p.getFileName() + ": " + line);
                        }
                    }
                }
            }
        }
        AssertJUnit.assertTrue(String.join("\n", offenders), offenders.isEmpty());
    }

    @Test
    public void timeMachineTargetsOnlyNontokenCreatures() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card machine = addCard("Time Machine", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card token = addToken("w_1_1_soldier", ai);
        SpellAbility sa = machine.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(sa.canTarget(bears));
        AssertJUnit.assertFalse(sa.canTarget(token));
    }
}
