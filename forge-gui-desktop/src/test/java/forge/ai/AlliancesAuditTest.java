package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Alliances script review and R&D's Secret Lair printed text (2026-10-23). */
public class AlliancesAuditTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    /** The one ability of this API whose activation restrictions currently pass. */
    private static SpellAbility usable(final Card c, final ApiType api, final Player p) {
        SpellAbility found = null;
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() != api) {
                continue;
            }
            sa.setActivatingPlayer(p);
            if (sa.getRestrictions().canPlay(c, sa)) {
                AssertJUnit.assertNull("only one version should be usable", found);
                found = sa;
            }
        }
        return found;
    }

    @Test
    public void testHeartOfYavimayaEntersBeforeItsSacrificeAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card heart = printing("Heart of Yavimaya", "ALL", p, ZoneType.Hand);
        heart = game.getAction().moveToPlay(heart, p, null, null);
        AssertJUnit.assertTrue("in play while the trigger waits", heart.isInPlay());
        resolveAll(game);
        AssertJUnit.assertTrue("no Forest to sacrifice, so it's buried", p.getCardsIn(ZoneType.Graveyard).contains(heart));
    }

    @Test
    public void testSoldeviSentryDrawIsUnconditionalAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card sentry = printing("Soldevi Sentry", "ALL", p, ZoneType.Battlefield);
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = usable(sentry, ApiType.Regenerate, p);
        AssertJUnit.assertEquals(ApiType.Draw, sa.getSubAbility().getApi());
    }

    @Test
    public void testMysticCompassNeedsAManaLandAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card compass = printing("Mystic Compass", "ALL", p, ZoneType.Battlefield);
        Card maze = addCard("Maze of Ith", p);
        Card forest = addCard("Forest", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(usable(compass, ApiType.ChooseType, p).getSubAbility().canTarget(maze));
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        SpellAbility animate = usable(compass, ApiType.ChooseType, p).getSubAbility();
        AssertJUnit.assertFalse(animate.canTarget(maze));
        AssertJUnit.assertTrue(animate.canTarget(forest));
    }
}
