package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Soulmates (CMB1 #84), checked against Scryfall and its 2 rulings 2026-10-15: it enchants two creatures, needs both to
 * enter, goes to the graveyard when either leaves, and destroys the other only when one dies.
 */
public class SoulmatesTest extends AITest {

    private static Card cast(final Game game, final Player p, final Card inHand, final Card... targets) {
        Card onStack = game.getAction().moveToStack(inHand, null);
        SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        for (Card t : targets) {
            sa.getTargets().add(t);
        }
        game.getStack().add(sa);
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
        return game.getCardState(onStack);
    }

    private static void settle(final Game game) {
        for (int i = 0; i < 5; i++) {
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
            if (game.getStack().isEmpty()) {
                return;
            }
            game.getStack().resolveStack();
        }
    }

    @Test
    public void itEnchantsBoth() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", ai);
        Card mates = cast(game, ai, addCardToZone("Soulmates", ai, ZoneType.Hand), bears, giant);

        AssertJUnit.assertTrue(mates.isInPlay());
        AssertJUnit.assertEquals(3, bears.getNetPower());
        AssertJUnit.assertEquals(4, giant.getNetToughness());
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.HEXPROOF));
        AssertJUnit.assertTrue(giant.hasKeyword(Keyword.HEXPROOF));
        AssertJUnit.assertTrue(bears.isEnchantedBy(mates) && giant.isEnchantedBy(mates));
    }

    @Test
    public void whenOneDiesTheOtherIsDestroyed() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", ai);
        Card mates = cast(game, ai, addCardToZone("Soulmates", ai, ZoneType.Hand), bears, giant);

        game.getAction().destroy(giant, null, true, null);
        settle(game);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(mates).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void leavingWithoutDyingSparesTheOther() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", ai);
        Card mates = cast(game, ai, addCardToZone("Soulmates", ai, ZoneType.Hand), bears, giant);

        game.getAction().moveToHand(bears, null);
        settle(game);
        AssertJUnit.assertTrue(game.getCardState(mates).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(giant.isInPlay());
        AssertJUnit.assertEquals("no longer enchanted", 3, giant.getNetPower());
        AssertJUnit.assertFalse(giant.isEnchantedBy(mates));
    }

    @Test
    public void anIllegalTargetKeepsItOffTheBattlefield() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", ai);
        Card onStack = game.getAction().moveToStack(addCardToZone("Soulmates", ai, ZoneType.Hand), null);
        SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        sa.getTargets().add(giant);
        game.getStack().add(sa);
        game.getAction().moveToHand(bears, null);
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(game.getCardState(onStack).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertFalse(giant.isEnchanted());
    }

    @Test
    public void theAiPicksTwo() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCards("Forest", 3, ai);
        addCard("Grizzly Bears", ai);
        addCard("Hill Giant", ai);
        Card mates = addCardToZone("Soulmates", ai, ZoneType.Hand);
        SpellAbility sa = mates.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertEquals(2, sa.getTargets().size());
    }
}
