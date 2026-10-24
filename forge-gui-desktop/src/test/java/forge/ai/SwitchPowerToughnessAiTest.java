package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** Cards that switch power and toughness, which were AI:RemoveDeck:All until PumpAi learned what a switch does. */
public class SwitchPowerToughnessAiTest extends AITest {

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private static SpellAbility activated(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
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

    // About Face: "Switch target creature's power and toughness until end of turn."
    @Test
    public void testSwitchKillsAZeroPowerCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 1);
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("About Face", ai, ZoneType.Hand).getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        Card wall = addCard("Wall of Wood", opp);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(wall, sa.getTargetCard());
    }

    @Test
    public void testSwitchKillsADamagedCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 1);
        // 1/4 with 1 damage: switched it's a 4/1 with lethal damage
        Card turtle = addCard("Horned Turtle", opp);
        SpellAbility sa = addCardToZone("About Face", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        turtle.setDamage(1);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(turtle, sa.getTargetCard());
    }

    // Twisted Image: "Switch target creature's power and toughness until end of turn. Draw a card."
    @Test
    public void testCantripSwitchCyclesOnASquareCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 1);
        fillLibrary(ai, 5);
        Card bears = addCard("Grizzly Bears", ai);
        addCard("Hill Giant", opp);
        SpellAbility sa = addCardToZone("Twisted Image", ai, ZoneType.Hand).getFirstSpellAbility();

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }

    // Crag Puca: 2/4, "{U/R}: Switch this creature's power and toughness until end of turn."
    @Test
    public void testSelfSwitchForLethal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 2);
        opp.setLife(4, null);
        Card puca = addCard("Crag Puca", ai);
        puca.setSickness(false);
        SpellAbility sa = activated(puca);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(puca, opp);
        combat.setBlocked(puca, false);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        // already switched: switching again would only undo it
        puca.addHiddenExtrinsicKeywords(game.getNextTimestamp(), 0, List.of("CARDNAME's power and toughness are switched"));
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Mannichi, the Fevered Dream: "{1}{R}: Switch each creature's power and toughness until end of turn."
    @Test
    public void testSwitchEverythingWhenOnlyTheirsDie() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 2);
        Card mannichi = addCard("Mannichi, the Fevered Dream", ai);
        addCard("Grizzly Bears", opp);
        SpellAbility sa = activated(mannichi);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        addCard("Wall of Wood", opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        addCard("Wall of Wood", ai);
        addCard("Wall of Wood", ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Crookclaw Transmuter: "When this creature enters, switch target creature's power and toughness until end of turn."
    @Test
    public void testMandatorySwitchPicksTheKill() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", ai);
        addCard("Hill Giant", opp);
        Card wall = addCard("Wall of Wood", opp);
        Card bird = addCard("Crookclaw Transmuter", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        SpellAbility trig = null;
        for (Trigger t : bird.getTriggers()) {
            trig = t.ensureAbility().copy(ai);
        }
        trig.resetTargets();
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(trig).doTriggerNoCostWithSubs(ai, trig, true).willingToPlay());
        AssertJUnit.assertEquals(wall, trig.getTargetCard());
    }
}
