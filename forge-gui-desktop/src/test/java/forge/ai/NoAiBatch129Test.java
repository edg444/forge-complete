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

/** Final pass, batch 129: ExtraCardAi, Hour of Need, Channel the Suns. */
public class NoAiBatch129Test extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision();
    }

    private static SpellAbility ability(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                return sa;
            }
        }
        return null;
    }

    // Alpha Brawl: their Craw Wurm's 6 kills both their Bears, and the Bears' 4 kills the Wurm
    @Test
    public void testAlphaBrawlWipesTheirBoard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wurm = addCard("Craw Wurm", opp);
        addCard("Grizzly Bears", opp);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Alpha Brawl", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }

    // Grim Contest: its Craw Wurm's toughness 4 kills their Hill Giant, whose 3 doesn't kill the Wurm
    @Test
    public void testGrimContestByToughness() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Craw Wurm", ai);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Grim Contest", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(giant, sa.getSubAbility().getTargetCard());
    }

    // Pack Hunt: its Grizzly Bears, with two more in its library
    @Test
    public void testPackHuntFindsCopies() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", ai);
        addCard("Hill Giant", ai);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        SpellAbility sa = addCardToZone("Pack Hunt", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Hour of Need: its Llanowar Elves into a 4/4 flying Sphinx
    @Test
    public void testHourOfNeedUpgrades() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard("Island", ai);
        }
        Card elves = addCard("Llanowar Elves", ai);
        SpellAbility sa = addCardToZone("Hour of Need", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(elves, sa.getTargetCard());
    }

    // Surprise Deployment: its Serra Angel from hand blocks and kills their attacking Hill Giant
    @Test
    public void testSurpriseDeploymentBlocks() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Surprise Deployment", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(giant, ai);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Chariot of the Sun: its attacking Craw Wurm flies over a board with no fliers
    @Test
    public void testChariotOfTheSun() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Plains", ai);
        addCard("Plains", ai);
        Card wurm = addCard("Craw Wurm", ai);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = ability(addCard("Chariot of the Sun", ai));
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(wurm, opp);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wurm, sa.getTargetCard());
    }

    // Aven Mimeomancer: their Craw Wurm becomes a 3/1
    @Test
    public void testAvenMimeomancerShrinksTheirBest() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wurm = addCard("Craw Wurm", opp);
        Card aven = addCard("Aven Mimeomancer", ai);
        SpellAbility trig = forge.game.ability.AbilityFactory.getAbility(aven.getSVar("TrigPutCounter"), aven);
        trig.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        AssertJUnit.assertTrue(forge.ai.ability.ExtraCardAi.decide(ai, trig).willingToPlay());
        AssertJUnit.assertEquals(wurm, trig.getTargetCard());
    }
}
