package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** AI:RemoveDeck:All cards, third batch (A...): each test is the situation that card's new AI is for. */
public class NoAiBatch03Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
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

    private Combat attack(Game game, Player attacker, Player defender, Card a, Card blocker) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, attacker, false, 1);
        Combat combat = new Combat(attacker);
        combat.addAttacker(a, defender);
        if (blocker != null) {
            combat.addBlocker(a, blocker);
        }
        combat.setBlocked(a, blocker != null);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        return combat;
    }

    // Akroma's Blessing: "Choose a color. Creatures you control gain protection from the chosen color until end of turn."
    @Test
    public void testAkromasBlessingSavesABlocker() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 3);
        Card bears = addCard("Grizzly Bears", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Akroma's Blessing", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        attack(game, opp, ai, giant, bears);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Alchemist's Gambit: "Take an extra turn after this one. [At the beginning of that turn's end step, you lose the game.]"
    @Test
    public void testAlchemistsGambitOnlyWhenTheExtraTurnWins() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 2);
        lands(ai, "Mountain", 1);
        Card gambit = addCardToZone("Alchemist's Gambit", ai, ZoneType.Hand);
        SpellAbility sa = null;
        for (SpellAbility s : gambit.getSpells()) {
            if (!s.hasParam("NonBasicSpell")) {
                sa = s;
            }
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        addCard("Hill Giant", ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        opp.setLife(3, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        addCard("Grizzly Bears", opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Afterlife Insurance: "Creatures you control gain afterlife 1 until end of turn. Draw a card."
    @Test
    public void testAfterlifeInsuranceIsAtLeastACantrip() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 1);
        lands(ai, "Swamp", 1);
        fillLibrary(ai, 5);
        addCard("Grizzly Bears", ai);
        SpellAbility sa = addCardToZone("Afterlife Insurance", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Al-abara's Carpet: "Prevent all damage that would be dealt to you this turn by attacking creatures without flying."
    @Test
    public void testCarpetOnlyAgainstGroundAttackers() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 5);
        Card carpet = addCard("Al-abara's Carpet", ai);
        ai.setLife(3, null);
        SpellAbility sa = activated(carpet);
        Card angel = addCard("Serra Angel", opp);
        attack(game, opp, ai, angel, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        Card giant = addCard("Hill Giant", opp);
        attack(game, opp, ai, giant, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Agent of Shauku: "{1}{B}, Sacrifice a land: Target creature gets +2/+0 until end of turn."
    @Test
    public void testAgentOfShaukuSacrificesASpareLandForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 8);
        Card agent = addCard("Agent of Shauku", ai);
        Card bears = addCard("Grizzly Bears", ai);
        opp.setLife(4, null);
        SpellAbility sa = activated(agent);
        attack(game, ai, opp, bears, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Amber Prison: "You may choose not to untap this artifact ... That permanent doesn't untap ... for as long as this artifact remains tapped."
    @Test
    public void testAmberPrisonStaysTappedWhileItHoldsSomething() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 4);
        Card prison = addCard("Amber Prison", ai);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = activated(prison).copy(ai);
        sa.getTargets().add(angel);
        prison.tap(null, null);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(angel.isTapped());
        SpellAbility untapQuestion = new SpellAbility.EmptySa(prison, ai);
        AssertJUnit.assertFalse(ai.getController().chooseBinary(untapQuestion, "", BinaryChoiceType.UntapOrLeaveTapped, true));

        game.getAction().destroy(angel, null, false, forge.game.ability.AbilityKey.newMap());
        AssertJUnit.assertTrue(ai.getController().chooseBinary(untapQuestion, "", BinaryChoiceType.UntapOrLeaveTapped, true));
    }

    // Altar of Dementia: "Sacrifice a creature: Target player mills cards equal to the sacrificed creature's power."
    @Test
    public void testAltarOfDementiaSacrificesACreatureThatIsDyingAnyway() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(opp, 10);
        Card altar = addCard("Altar of Dementia", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card wurm = addCard("Craw Wurm", opp);
        SpellAbility sa = activated(altar);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        attack(game, opp, ai, wurm, bears);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    // Airdrop Condor: "{1}{R}, Sacrifice a Goblin creature: This creature deals damage equal to the sacrificed creature's power to any target."
    @Test
    public void testAirdropCondorFlingsAGoblinForTheKill() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 2);
        Card condor = addCard("Airdrop Condor", ai);
        addCard("Raging Goblin", ai);
        opp.setLife(1, null);
        SpellAbility sa = activated(condor);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Freyalise Supplicant: "{T}, Sacrifice a red or white creature: ... damage ... equal to half the sacrificed creature's power, rounded down."
    @Test
    public void testFreyaliseSupplicantCountsHalfThePower() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card supplicant = addCard("Freyalise Supplicant", ai);
        supplicant.setSickness(false);
        addCard("Hill Giant", ai);
        SpellAbility sa = activated(supplicant);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        opp.setLife(2, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        opp.setLife(1, null);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
