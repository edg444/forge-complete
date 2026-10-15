package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Interplanar Brushwagg (CMB1 #79), checked against Scryfall and its 11 rulings 2026-10-15: it enters onto the
 * interplanar battlefield, where no player controls it and it isn't an opponent's either, and it joins attacks by
 * creatures with power 4 or greater - its owner included.
 */
public class InterplanarBrushwaggTest extends AITest {

    private static Card cast(final Game game, final Player p, final Card inHand, final forge.game.GameObject target) {
        Card onStack = game.getAction().moveToStack(inHand, null);
        SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add(target);
        }
        game.getStack().add(sa);
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
        return game.getCardState(onStack);
    }

    @Test
    public void nobodyControlsIt() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card wagg = cast(game, ai, addCardToZone("Interplanar Brushwagg", ai, ZoneType.Hand), null);

        AssertJUnit.assertTrue(wagg.isInPlay());
        AssertJUnit.assertTrue(wagg.getController().isInterplanar());
        AssertJUnit.assertEquals(ai, wagg.getOwner());
        AssertJUnit.assertFalse(ai.getCardsIn(ZoneType.Battlefield).contains(wagg));
        AssertJUnit.assertTrue(game.getCardsIn(ZoneType.Battlefield).contains(wagg));
        AssertJUnit.assertFalse(game.getPlayers().contains(wagg.getController()));
        for (Player p : new Player[] {ai, opp}) {
            AssertJUnit.assertFalse(wagg.isValid("Creature.YouCtrl", p, null, null));
            AssertJUnit.assertFalse("not an opponent's either", wagg.isValid("Creature.OppCtrl", p, null, null));
            AssertJUnit.assertTrue(wagg.isValid("Creature.YouDontCtrl", p, null, null));
        }

        // gaining control of it doesn't happen
        addCards("Mountain", 3, opp);
        cast(game, opp, addCardToZone("Act of Treason", opp, ZoneType.Hand), wagg);
        AssertJUnit.assertTrue(game.getCardState(wagg).getController().isInterplanar());

        // but it's on the battlefield like anything else
        cast(game, opp, addCardToZone("Wrath of God", opp, ZoneType.Hand), null);
        AssertJUnit.assertTrue(game.getCardState(wagg).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Graveyard).contains(game.getCardState(wagg)));
    }

    private static void runTurn(final Game game, final Player ai) {
        PhaseHandler ph = game.getPhaseHandler();
        ph.devModeSet(PhaseType.MAIN1, ai);
        for (int i = 0; i < 400 && !game.isGameOver(); i++) {
            ph.mainLoopStep();
            if (ph.is(PhaseType.MAIN2) && ph.getPlayerTurn() == ai) {
                break;
            }
        }
    }

    @Test
    public void itJoinsABigAttackEvenAtItsOwner() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card wagg = cast(game, opp, addCardToZone("Interplanar Brushwagg", opp, ZoneType.Hand), null);
        addCard("Craw Wurm", ai).setSickness(false);
        fillLibrary(opp, 5);
        fillLibrary(ai, 5);

        runTurn(game, ai);
        AssertJUnit.assertEquals("the Wurm's 6 and the Brushwagg's 6", 8, opp.getLife());
        AssertJUnit.assertFalse("it was never declared, and has vigilance anyway", game.getCardState(wagg).isTapped());
    }

    @Test
    public void itCanBeBlocked() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card wagg = cast(game, opp, addCardToZone("Interplanar Brushwagg", opp, ZoneType.Hand), null);
        addCard("Serra Angel", ai).setSickness(false); // a flyer, so the Wall gives the AI no reason not to attack
        Card wall = addCard("Wall of Stone", opp);
        fillLibrary(opp, 5);
        fillLibrary(ai, 5);

        PhaseHandler ph = game.getPhaseHandler();
        ph.devModeSet(PhaseType.MAIN1, ai);
        boolean checked = false;
        for (int i = 0; i < 400 && !game.isGameOver() && !checked; i++) {
            ph.mainLoopStep();
            if (ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) && ph.inCombat()) {
                AssertJUnit.assertTrue(ph.getCombat().isAttacking(game.getCardState(wagg)));
                AssertJUnit.assertEquals(opp, ph.getCombat().getDefenderPlayerByAttacker(game.getCardState(wagg)));
                AssertJUnit.assertTrue(forge.game.combat.CombatUtil.canBlock(game.getCardState(wagg), wall));
                checked = true;
            }
        }
        AssertJUnit.assertTrue(checked);
    }

    @Test
    public void notForSmallAttackers() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        cast(game, opp, addCardToZone("Interplanar Brushwagg", opp, ZoneType.Hand), null);
        addCard("Hill Giant", ai).setSickness(false);
        fillLibrary(opp, 5);
        fillLibrary(ai, 5);

        runTurn(game, ai);
        AssertJUnit.assertEquals(17, opp.getLife());
    }
}
