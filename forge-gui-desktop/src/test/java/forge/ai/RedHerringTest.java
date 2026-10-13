package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Red Herring (CMB1 #62, "P-Red Herring" beside Murders at Karlov Manor's Red Herring), checked against Scryfall and its
 * 5 rulings 2026-10-13: it trades places with a permanent or spell you control, taking over its state and the targets
 * on it, unless it isn't a legal target for them.
 */
public class RedHerringTest extends AITest {

    private static SpellAbility castOnto(final Player p, final Card c, final Object target) {
        Card onStack = p.getGame().getAction().moveToStack(c, null);
        SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add((forge.game.GameObject) target);
        }
        p.getGame().getStack().add(sa);
        return sa;
    }

    private static SpellAbility herring(final Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void takesAPermanentsPlaceAndTheRemoval() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCards("Island", 2, ai);
        Card angel = addCard("Serra Angel", ai);
        angel.addCounterInternal(CounterEnumType.P1P1, 1, ai, false, null, null);
        angel.setTapped(true);
        Card herring = addCardToZone("P-Red Herring", ai, ZoneType.Hand);
        Card murder = addCardToZone("Murder", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);

        castOnto(opp, murder, angel);
        SpellAbility ab = herring(herring);
        ab.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(ab));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, ab, null));
        game.getStack().resolveStack();

        // the permanent object stays and is now Red Herring, with the Angel's counter and tapped
        AssertJUnit.assertEquals("P-Red Herring", angel.getName());
        AssertJUnit.assertTrue(angel.isTapped());
        AssertJUnit.assertEquals(1, angel.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals("Serra Angel", herring.getName());
        AssertJUnit.assertTrue(herring.isInZone(ZoneType.Hand));

        game.getStack().resolveStack(); // Murder, now at Red Herring
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(angel).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("P-Red Herring", game.getCardState(angel).getName());
        AssertJUnit.assertTrue(game.getCardState(herring).isInZone(ZoneType.Hand));
    }

    @Test
    public void takesASpellsPlaceAndTheCounterspell() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCards("Island", 2, ai);
        Card giant = addCardToZone("Hill Giant", ai, ZoneType.Hand);
        Card herring = addCardToZone("P-Red Herring", ai, ZoneType.Hand);
        Card cancel = addCardToZone("Cancel", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility giantSa = castOnto(ai, giant, null);
        castOnto(opp, cancel, giantSa);
        SpellAbility ab = herring(herring);
        ab.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, ab, null));
        game.getStack().resolveStack();

        AssertJUnit.assertEquals(2, game.getStack().size());
        Card onStack = game.getStack().peekAbility().getTargets().getFirstTargetedSpell().getHostCard();
        AssertJUnit.assertEquals("Cancel now targets Red Herring", "P-Red Herring", onStack.getName());
        AssertJUnit.assertEquals("Hill Giant", herring.getName());
        AssertJUnit.assertTrue(herring.isInZone(ZoneType.Hand));

        game.getStack().resolveStack(); // Cancel counters Red Herring
        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertTrue(game.getCardState(onStack).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void anIllegalNewTargetLeavesTheOldOne() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCards("Island", 2, ai);
        Card divination = addCardToZone("Divination", ai, ZoneType.Hand);
        Card herring = addCardToZone("P-Red Herring", ai, ZoneType.Hand);
        Card negate = addCardToZone("Negate", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility divSa = castOnto(ai, divination, null);
        SpellAbility negateSa = castOnto(opp, negate, divSa);
        SpellAbility ab = herring(herring);
        ab.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, ab, null));
        game.getStack().resolveStack();

        AssertJUnit.assertTrue("Negate can't target a creature spell", negateSa.getTargets().contains(divSa));
        game.getStack().resolveStack(); // Negate: its target is gone
        game.getStack().resolveStack(); // Red Herring resolves
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.getName().equals("P-Red Herring")));
        AssertJUnit.assertTrue(game.getCardState(herring).isInZone(ZoneType.Hand));
    }
}
