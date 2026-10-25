package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** AILogic$ PreventCombat (CombatPreventionAi): prevention is used once blocks are known, for what it saves. */
public class NoAiCombatPreventionTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility activated(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    private void lands(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    private Combat blocks(Game game, Player attacker, Card att, Player defender, Card blocker) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attacker, false, 1);
        Combat combat = new Combat(attacker);
        combat.addAttacker(att, defender);
        if (blocker != null) {
            combat.addBlocker(att, blocker);
        }
        // blocks are declared by the time anyone gets priority in that step
        combat.setBlocked(att, blocker != null);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    // Benalish Missionary: "Prevent all combat damage that would be dealt by target blocked creature this turn."
    @Test
    public void testMissionarySavesTheBlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 2);
        Card missionary = addCard("Benalish Missionary", ai);
        missionary.setSickness(false);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = activated(missionary);

        blocks(game, opp, giant, ai, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        blocks(game, opp, giant, ai, bears);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getTargetCard());
    }

    // Divine Light: "Prevent all damage that would be dealt this turn to creatures you control."
    @Test
    public void testDivineLightOnlyWhenItSavesSomething() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 2);
        Card wall = addCard("Wall of Stone", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Divine Light", ai, ZoneType.Hand).getFirstSpellAbility();

        blocks(game, opp, giant, ai, wall);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        blocks(game, opp, giant, ai, bears);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Ebony Horse: "Untap target attacking creature you control. Prevent all combat damage that would be dealt to and
    // dealt by that creature this turn."
    @Test
    public void testEbonyHorseRescuesABlockedAttacker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 2);
        Card horse = addCard("Ebony Horse", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = activated(horse);

        blocks(game, ai, bears, opp, giant);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Hunter's Ambush: "Prevent all combat damage that would be dealt by nongreen creatures this turn."
    @Test
    public void testHuntersAmbushStopsLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Forest", 3);
        ai.setLife(3, null);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Hunter's Ambush", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        blocks(game, opp, giant, ai, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Forfend: "Prevent all damage that would be dealt to creatures this turn." - both sides' creatures
    @Test
    public void testForfendNotWhenItSavesTheirCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 1);
        Card giant = addCard("Hill Giant", ai);
        Card bears = addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Forfend", ai, ZoneType.Hand).getFirstSpellAbility();

        blocks(game, opp, bears, ai, giant);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        Card ogre = addCard("Hill Giant", opp);
        Card wall = addCard("Grizzly Bears", ai);
        blocks(game, opp, ogre, ai, wall);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Redeem: "Prevent all damage that would be dealt this turn to up to two target creatures."
    @Test
    public void testRedeemProtectsTheDoomedBlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 2);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Redeem", ai, ZoneType.Hand).getFirstSpellAbility();

        blocks(game, opp, giant, ai, bears);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(1, sa.getTargets().size());
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }
}
