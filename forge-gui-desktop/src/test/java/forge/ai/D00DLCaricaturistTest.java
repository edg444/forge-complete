package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * D00-DL, Caricaturist (UNF #187), checked against Scryfall and its rulings 2026-10-21: a 4/4 Sketch whose keywords come
 * from what's drawn on it.
 */
public class D00DLCaricaturistTest extends AITest {

    private static final String[] KEYWORDS = {"Flying", "First Strike", "Vigilance", "Menace", "Trample", "Deathtouch",
            "Lifelink", "Haste"};

    @Test
    public void testSketches() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        boolean plain = false;
        boolean featured = false;
        for (int i = 0; i < 20; i++) {
            final Card doodle = addCardToZone("D00-DL, Caricaturist", ai, ZoneType.Hand);
            game.getAction().moveToPlay(doodle, ai, null, AbilityKey.newMap());
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
            while (!game.getStack().isEmpty()) {
                game.getStack().resolveStack();
                game.getAction().checkStateEffects(true);
            }
        }
        final java.util.List<Card> sketches = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Sketch"));
        AssertJUnit.assertEquals(20, sketches.size());
        for (final Card sketch : sketches) {
            AssertJUnit.assertEquals(4, sketch.getNetPower());
            AssertJUnit.assertEquals("Sketch Token", sketch.getName());
            int features = 0;
            for (final String kw : KEYWORDS) {
                features += sketch.hasKeyword(kw) ? 1 : 0;
            }
            plain |= features == 0;
            featured |= features > 0;
        }
        AssertJUnit.assertTrue("the AI draws some features, and sometimes none - so fewer than all eight can be chosen", plain && featured);
    }
}
