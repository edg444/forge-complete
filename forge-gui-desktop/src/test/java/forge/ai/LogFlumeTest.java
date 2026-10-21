package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Log Flume (UNF #220), checked against Scryfall and its rulings 2026-10-21: attacking logmates are blocked all together
 * or not at all, and a spell you control that targets one targets each of them.
 */
public class LogFlumeTest extends AITest {

    private Card logOf(final Game game, final Player p, final Card a, final Card b) {
        final Card flume = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard("Log Flume"), p);
        p.getZone(ZoneType.Battlefield).add(flume);
        flume.setChosenCards(new CardCollection(java.util.List.of(a, b)));
        final SpellAbility jump = AbilityFactory.getAbility(flume.getSVar("DBLog"), flume);
        jump.setActivatingPlayer(p);
        AbilityUtils.resolve(jump);
        return flume;
    }

    @Test
    public void testBlockedTogether() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card a = addCard("Grizzly Bears", me);
        final Card b = addCard("Grizzly Bears", me);
        final Card blocker1 = addCard("Llanowar Elves", opp);
        final Card blocker2 = addCard("Llanowar Elves", opp);
        logOf(game, me, a, b);
        AssertJUnit.assertNotNull(a.getFlumeLog());
        AssertJUnit.assertSame(a.getFlumeLog(), b.getFlumeLog());

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, me);
        final Combat combat = new Combat(me);
        combat.addAttacker(a, opp);
        combat.addAttacker(b, opp);
        combat.addBlocker(a, blocker1);
        AssertJUnit.assertNotNull("one logmate blocked, the other not", CombatUtil.validateBlocks(combat, opp));
        combat.addBlocker(b, blocker2);
        AssertJUnit.assertNull(CombatUtil.validateBlocks(combat, opp));
    }

    @Test
    public void testTargetsTheWholeLog() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
        final Card a = addCard("Grizzly Bears", me);
        final Card b = addCard("Grizzly Bears", me);
        addCard("Forest", me);
        logOf(game, me, a, b);
        game.getAction().checkStateEffects(true);
        final Card growth = addCardToZone("Giant Growth", me, ZoneType.Hand);
        final SpellAbility pump = growth.getFirstSpellAbility();
        pump.setActivatingPlayer(me);
        pump.getTargets().add(a);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(me, pump, null));
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        AssertJUnit.assertEquals(5, a.getNetPower());
        AssertJUnit.assertEquals("targeted too, as its logmate", 5, b.getNetPower());
    }
}
