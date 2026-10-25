package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 122: AILogic$ Sweep (Barrel Down Sokenzan, Charge Across the Araba). */
public class NoAiBatch122Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    // Barrel Down Sokenzan: their Craw Wurm needs two Mountains' 4 damage, and the two tapped ones go back
    @Test
    public void testBarrelDownReturnsJustEnough() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            Card m = addCard("Mountain", ai);
            m.setTapped(i < 2);
        }
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = addCardToZone("Barrel Down Sokenzan", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getSubAbility().getTargetCard());

        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(2, CardLists.count(ai.getCardsIn(ZoneType.Hand), Card::isLand));
        AssertJUnit.assertEquals(3, ai.getLandsInPlay().size());
        AssertJUnit.assertTrue(ai.getLandsInPlay().allMatch(Card::isUntapped));
        AssertJUnit.assertEquals(4, wurm.getDamage());
    }

    // Charge Across the Araba: two unblocked Bears and 8 life to go is +2/+2 each, two Plains
    @Test
    public void testChargeAcrossTheArabaForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        opp.setLife(8, null);
        for (int i = 0; i < 7; i++) {
            addCard("Plains", ai);
        }
        Card a = addCard("Grizzly Bears", ai);
        Card b = addCard("Grizzly Bears", ai);
        SpellAbility sa = addCardToZone("Charge Across the Araba", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(a, opp);
        combat.addAttacker(b, opp);
        combat.setBlocked(a, false);
        combat.setBlocked(b, false);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(4, a.getNetPower());
        AssertJUnit.assertEquals(5, ai.getLandsInPlay().size());
    }
}
