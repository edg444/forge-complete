package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Transcantation (CMB1 #67), checked against Scryfall and its 4 rulings 2026-10-14: the target spell becomes a
 * Lightning Bolt with no targets until its controller picks some, loses whatever its additional or alternative costs
 * would have done, and is its normal self again once it resolves.
 */
public class TranscantationTest extends AITest {

    private static SpellAbility castOnto(final Player p, final Card c, final GameObject target) {
        Card onStack = p.getGame().getAction().moveToStack(c, null);
        SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add(target);
        }
        p.getGame().getStack().add(sa);
        return sa;
    }

    @Test
    public void theAiTurnsAWrathIntoABolt() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCards("Mountain", 2, ai);
        Card angel = addCard("Serra Angel", ai);
        Card trans = addCardToZone("Transcantation", ai, ZoneType.Hand);
        Card inHand = addCardToZone("Wrath of God", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);

        Card wrath = castOnto(opp, inHand, null).getHostCard();
        SpellAbility sa = trans.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        game.getStack().resolveStack();

        AssertJUnit.assertEquals(1, game.getStack().size());
        SpellAbility bolt = game.getStack().peekAbility();
        AssertJUnit.assertEquals("Lightning Bolt", wrath.getName());
        AssertJUnit.assertSame(wrath, bolt.getHostCard());
        AssertJUnit.assertEquals(opp, bolt.getActivatingPlayer());
        AssertJUnit.assertEquals("its controller chose a target", 1, bolt.getTargets().size());

        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(angel.isInPlay());
        AssertJUnit.assertTrue("the Bolt hit something of ours", ai.getLife() == 17 || angel.getDamage() == 3);
        Card inYard = game.getCardState(wrath);
        AssertJUnit.assertTrue(inYard.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("Wrath of God", inYard.getName());
    }

    @Test
    public void theAiLeavesCheapSpellsAndItsOwnAlone() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCards("Mountain", 2, ai);
        Card trans = addCardToZone("Transcantation", ai, ZoneType.Hand);
        Card shock = addCardToZone("Shock", opp, ZoneType.Hand);
        Card divination = addCardToZone("Divination", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = trans.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AiController brains = ((PlayerControllerAi) ai.getController()).getAi();

        castOnto(opp, shock, ai);
        AssertJUnit.assertFalse(brains.canPlaySa(sa) == AiPlayDecision.WillPlay);
        game.getStack().resolveStack();
        castOnto(ai, divination, null);
        AssertJUnit.assertFalse(brains.canPlaySa(sa) == AiPlayDecision.WillPlay);
    }

    @Test
    public void whatTargetedTheSpellStillDoes() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card divination = addCardToZone("Divination", ai, ZoneType.Hand);
        Card cancel = addCardToZone("Cancel", opp, ZoneType.Hand);
        Card trans = addCardToZone("Transcantation", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility divSa = castOnto(ai, divination, null);
        SpellAbility cancelSa = castOnto(opp, cancel, divSa);
        castOnto(opp, trans, divSa);
        game.getStack().resolveStack();

        SpellAbility bolt = cancelSa.getTargets().getFirstTargetedSpell();
        AssertJUnit.assertNotSame(divSa, bolt);
        AssertJUnit.assertEquals("Lightning Bolt", bolt.getHostCard().getName());
        game.getStack().resolveStack(); // Cancel counters the Bolt
        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertEquals(20, opp.getLife());
        AssertJUnit.assertEquals(20, ai.getLife());
        AssertJUnit.assertEquals("Divination", game.getCardState(divination).getName());
        AssertJUnit.assertTrue(game.getCardState(divination).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void flashbackNoLongerExilesIt() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCards("Island", 3, opp);
        Card think = addCardToZone("Think Twice", opp, ZoneType.Graveyard);
        Card trans = addCardToZone("Transcantation", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);

        SpellAbility flashback = think.getAllPossibleAbilities(opp, true).stream().filter(SpellAbility::isFlashback).findFirst().orElse(null);
        AssertJUnit.assertNotNull(flashback);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(opp, flashback, null));
        Card onStack = game.getStack().peekAbility().getHostCard();
        castOnto(ai, trans, game.getStack().peekAbility());
        game.getStack().resolveStack();
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(game.getStack().isEmpty());
        Card after = game.getCardState(onStack);
        AssertJUnit.assertTrue(after.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("Think Twice", after.getName());
    }
}
