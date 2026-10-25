package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 121: script hints (Utopia Sprawl, Shimmerwilds Growth, Gaea's Liege), Megatherium, Knight of the Mists. */
public class NoAiBatch121Test extends AITest {

    // Utopia Sprawl: a mostly blue deck names blue for the extra mana
    @Test
    public void testUtopiaSprawlNamesItsMainColor() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Forest", ai);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Air Elemental", ai, ZoneType.Library);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card sprawl = addCardToZone("Utopia Sprawl", ai, ZoneType.Hand);
        SpellAbility choose = forge.game.ability.AbilityFactory.getAbility(sprawl.getSVar("ChooseColor"), sprawl);
        choose.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(java.util.List.of(MagicColor.Constant.BLUE),
                ComputerUtilCard.chooseColor(choose, 1, 1, MagicColor.Constant.ONLY_COLORS));
    }

    // Gaea's Liege: its Forests make it a 2/2, so it's cast
    @Test
    public void testGaeasLiegeIsCast() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCard("Forest", ai);
        }
        SpellAbility sa = addCardToZone("Gaea's Liege", ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision());
    }

    // Megatherium: with four cards left in hand, four lands can't cast it and pay 4 too; seven can
    @Test
    public void testMegatheriumOnlyWhenItCanPay() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 4; i++) {
            addCard("Forest", ai);
            addCardToZone("Hill Giant", ai, ZoneType.Hand);
        }
        SpellAbility sa = addCardToZone("Megatherium", ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision());

        for (int i = 0; i < 3; i++) {
            addCard("Forest", ai);
        }
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision());
    }

    // Knight of the Mists: their White Knight is destroyed rather than paying {U} to spare it
    @Test
    public void testKnightOfTheMistsKillsTheirKnight() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Island", ai);
        }
        Card white = addCard("White Knight", opp);
        Card knight = addCardToZone("Knight of the Mists", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        Card played = game.getAction().moveToPlay(knight, null, null);
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        AssertJUnit.assertFalse(white.isInPlay());
        AssertJUnit.assertTrue(played.isInPlay());
    }
}
