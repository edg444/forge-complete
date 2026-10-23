package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** The Dark script review and R&D's Secret Lair printed text (2026-10-23). */
public class TheDarkAuditTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static SpellAbility ability(final Card c, final forge.game.ability.ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testRagManTargetsOnlyOpponents() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card rag = addCard("Rag Man", p);
        SpellAbility sa = ability(rag, forge.game.ability.ApiType.RevealHand);
        sa.setActivatingPlayer(p);
        AssertJUnit.assertFalse(sa.canTarget(p));
        AssertJUnit.assertTrue(sa.canTarget(opp));
    }

    @Test
    public void testBloodMoonMakesBasicMountainsAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        printing("Blood Moon", "DRK", p, ZoneType.Battlefield);
        Card dual = addCard("Tundra", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(dual.getType().hasSubtype("Mountain"));
        AssertJUnit.assertFalse(dual.isBasicLand());

        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(dual.getType().hasSubtype("Mountain"));
        AssertJUnit.assertTrue(dual.isBasicLand());
    }

    @Test
    public void testEaterOfTheDeadWorksUntappedAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card eater = printing("Eater of the Dead", "DRK", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = ability(eater, forge.game.ability.ApiType.ChangeZone);
        sa.setActivatingPlayer(p);
        AssertJUnit.assertFalse(sa.getRestrictions().canPlay(eater, sa));
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(sa.getRestrictions().canPlay(eater, sa));
    }
}
