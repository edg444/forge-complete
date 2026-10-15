package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Abian, Luvion Usurper (CMB1 #87), checked against Scryfall and its 5 rulings 2026-10-15: you become Abian - your life
 * total becomes its loyalty, and from then on each follows the other until it leaves the battlefield.
 */
public class AbianTest extends AITest {

    private static Card cast(final Game game, final Player p, final Card inHand, final GameObject target) {
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

    private static SpellAbility loyalty(final Card abian, final String api) {
        return abian.getSpellAbilities().stream().filter(sa -> sa.isActivatedAbility() && sa.getApi().name().equals(api)).findFirst().get();
    }

    @Test
    public void youBecomeAbian() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card abian = cast(game, ai, addCardToZone("Abian, Luvion Usurper", ai, ZoneType.Hand), null);

        AssertJUnit.assertTrue(abian.isInPlay());
        AssertJUnit.assertEquals("your life total becomes their loyalty", 5, ai.getLife());
        AssertJUnit.assertEquals(5, abian.getCounters(CounterEnumType.LOYALTY));
        AssertJUnit.assertEquals(ai, abian.getEmbodiedBy());

        // +1: loyalty is gained life
        SpellAbility plus1 = loyalty(abian, "Token");
        plus1.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, plus1, null));
        game.getStack().resolveStack();
        AssertJUnit.assertEquals(6, ai.getLife());
        AssertJUnit.assertEquals(6, abian.getCounters(CounterEnumType.LOYALTY));
        AssertJUnit.assertEquals(1, ai.getCreaturesInPlay().size());

        // damage to you is loyalty lost, and damage to Abian is life lost
        cast(game, opp, addCardToZone("Shock", opp, ZoneType.Hand), ai);
        AssertJUnit.assertEquals(4, ai.getLife());
        AssertJUnit.assertEquals(4, abian.getCounters(CounterEnumType.LOYALTY));
        cast(game, opp, addCardToZone("Shock", opp, ZoneType.Hand), abian);
        AssertJUnit.assertEquals(2, ai.getLife());
        AssertJUnit.assertEquals(2, abian.getCounters(CounterEnumType.LOYALTY));

        // leaving the battlefield, you're just a player again; your life stays
        game.getAction().moveToHand(abian, null);
        AssertJUnit.assertEquals(2, ai.getLife());
        AssertJUnit.assertNull(ai.getEmbodied());
        ai.gainLife(3, null, null);
        AssertJUnit.assertEquals(5, ai.getLife());
    }

    @Test
    public void spendingYourLife() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card abian = cast(game, ai, addCardToZone("Abian, Luvion Usurper", ai, ZoneType.Hand), null);

        SpellAbility minus = loyalty(abian, "DealDamage");
        minus.setActivatingPlayer(ai);
        minus.setXManaCostPaid(3);
        minus.getTargets().add(opp);
        AssertJUnit.assertTrue(minus.getPayCosts().canPay(minus, ai, false));
        abian.subtractCounter(CounterEnumType.LOYALTY, 3, ai);
        AssertJUnit.assertEquals("paying -3 is losing 3 life", 2, ai.getLife());

        abian.subtractCounter(CounterEnumType.LOYALTY, 2, ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("no loyalty, no life", ai.hasLost());
    }

    @Test
    public void theAiDoesntSpendItsLastLife() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card abian = cast(game, ai, addCardToZone("Abian, Luvion Usurper", ai, ZoneType.Hand), null);
        opp.setLife(5, null);

        SpellAbility minus = loyalty(abian, "DealDamage");
        minus.setActivatingPlayer(ai);
        AiPlayDecision d = ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(minus);
        AssertJUnit.assertNotNull(d);
        AssertJUnit.assertTrue("X stays below its life", minus.getXManaCostPaid() == null || minus.getXManaCostPaid() < ai.getLife());
    }
}
