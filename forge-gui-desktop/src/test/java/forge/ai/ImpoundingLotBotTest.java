package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Impounding Lot-Bot (UNF #14), checked against Scryfall and its 2 rulings 2026-10-21: saying "INCARCERATE!" is
 * honor system (the AI, a computer, always says it), and the exile lasts until the Lot-Bot leaves.
 */
public class ImpoundingLotBotTest extends AITest {

    private SpellAbility etb(final Card bot, final Player p, final Card target) {
        final SpellAbility sa = bot.getTriggers().get(0).ensureAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(target);
        return sa;
    }

    @Test
    public void testTheAiSaysItAndTheExileLastsUntilItLeaves() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card relic = addCard("Ornithopter", opp);
        final Card bot = addCard("Impounding Lot-Bot", p);
        AbilityUtils.resolve(etb(bot, p, relic));
        AssertJUnit.assertTrue(game.getCardState(relic).isInZone(ZoneType.Exile));

        game.getAction().destroy(bot, null, true, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("it comes back", game.getCardState(relic).isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testCantTargetItself() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card bot = addCard("Impounding Lot-Bot", p);
        final SpellAbility sa = bot.getTriggers().get(0).ensureAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertFalse(sa.canTarget(bot));
    }
}
