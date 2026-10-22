package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Every Unknown Event card builds: a script's abilities are only parsed when a card object is made, so a bad ApiType
 * name or param in a card no other test creates would otherwise go unnoticed until someone plays it.
 */
public class UnknownEventScriptsLoadTest extends AITest {

    @Test
    public void testEveryUnknownEventCardBuilds() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final List<String> broken = Lists.newArrayList();
        int built = 0;
        for (final PaperCard pc : FModel.getMagicDb().getCommonCards().getAllCards()) {
            if (!"UNK".equalsIgnoreCase(pc.getEdition())) {
                continue;
            }
            try {
                final Card c = Card.fromPaperCard(pc, ai);
                AssertJUnit.assertNotNull(c);
                built++;
            } catch (final RuntimeException e) {
                broken.add(pc.getName() + ": " + e.getMessage());
            }
        }
        AssertJUnit.assertTrue("no Unknown Event printings found", built > 0);
        AssertJUnit.assertTrue(String.join("\n", broken), broken.isEmpty());
    }
}
