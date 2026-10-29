package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.ChooseGenericAi;
import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** AI:RemoveDeck:All cards, second batch (A...): each test is the situation that card's new AI is for. */
public class NoAiBatch02Test extends AITest {

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

    private void library(Player p, String name, int n) {
        for (int i = 0; i < n; i++) {
            addCardToZone(name, p, ZoneType.Library);
        }
    }

    // Thran Portal: "As this land enters, choose a basic land type." It used to always be Island.
    @Test
    public void testThranPortalNamesTheColorTheDeckUses() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        library(ai, "Lightning Bolt", 4);
        addCardToZone("Shock", ai, ZoneType.Hand);
        Card portal = addCardToZone("Thran Portal", ai, ZoneType.Hand);
        SpellAbility choose = AbilityFactory.getAbility(portal.getSVar("DBChooseBasic"), portal);
        choose.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("Mountain", ComputerUtil.chooseSomeType(ai, "Basic Land", choose, CardType.getBasicTypes()));
    }

    // Alrund: "choose a card type, then reveal the top three cards of your library. Put all cards of the chosen type ... into your hand"
    @Test
    public void testAlrundNamesWhatTheLibraryHoldsMost() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        library(ai, "Forest", 6);
        library(ai, "Grizzly Bears", 2);
        Card alrund = addCard("Alrund, God of the Cosmos", ai);
        SpellAbility choose = null;
        for (Trigger t : alrund.getTriggers()) {
            if (t.ensureAbility() != null && t.ensureAbility().getApi() == forge.game.ability.ApiType.ChooseType) {
                choose = t.ensureAbility();
            }
        }
        choose.setActivatingPlayer(ai);
        List<String> types = new ArrayList<>();
        for (CardType.CoreType t : CardType.CoreType.values()) {
            types.add(t.name());
        }
        AssertJUnit.assertEquals("Land", ComputerUtil.chooseSomeType(ai, "Card", choose, types));
    }

    // Abundant Harvest: "Choose land or nonland. Reveal cards from the top of your library until you reveal a card of the chosen kind."
    @Test
    public void testAbundantHarvestTakesALandWhenShort() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        library(ai, "Forest", 3);
        library(ai, "Grizzly Bears", 3);
        lands(ai, "Forest", 2);
        Card harvest = addCardToZone("Abundant Harvest", ai, ZoneType.Hand);
        SpellAbility sa = harvest.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        List<SpellAbility> choices = new ArrayList<>(sa.getAdditionalAbilityList("Choices"));
        ChooseGenericAi brain = new ChooseGenericAi();
        AssertJUnit.assertEquals("Card.Land", brain.chooseSingleSpellAbility(ai, sa, choices, null).getParam("Valid"));

        lands(ai, "Forest", 4);
        addCardToZone("Forest", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("Card.nonLand", brain.chooseSingleSpellAbility(ai, sa, choices, null).getParam("Valid"));
    }

    // Acidic Soil: "deals damage to each player equal to the number of lands they control."
    @Test
    public void testAcidicSoilWhenTheyHaveMoreLands() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Mountain", 3);
        lands(opp, "Forest", 6);
        SpellAbility sa = addCardToZone("Acidic Soil", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        lands(ai, "Mountain", 4);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        opp.setLife(6, null);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        ai.setLife(7, null);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Ad Nauseam: "... You lose life equal to its mana value. You may repeat this process any number of times."
    @Test
    public void testAdNauseamRepeatsWhileSafe() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Swamp", 4);
        library(ai, "Grizzly Bears", 10);
        SpellAbility sa = addCardToZone("Ad Nauseam", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));

        SpellAbility repeat = sa;
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(repeat).confirmAction(ai, repeat, null, "", null));
        addCardToZone("Craw Wurm", ai, ZoneType.Library);
        ai.setLife(10, null);
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(repeat).confirmAction(ai, repeat, null, "", null));
    }

    // Acorn Catapult: "deals 1 damage to any target. That permanent's controller or that player creates a 1/1 ... Squirrel"
    @Test
    public void testAcornCatapultGoesFaceOnlyForTheKill() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 1);
        Card catapult = addCard("Acorn Catapult", ai);
        SpellAbility sa = activated(catapult);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        opp.setLife(1, null);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    // Adarkar Windform: "{1}{S}: Target creature loses flying until end of turn."
    @Test
    public void testWindformGroundsAnAttackerTheAiCanBlock() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Snow-Covered Island", 2);
        Card windform = addCard("Adarkar Windform", ai);
        windform.setSickness(false);
        windform.tap(null, null);
        Card angel = addCard("Serra Angel", opp);
        SpellAbility sa = activated(windform);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, false, 1);
        Combat combat = new Combat(opp);
        combat.addAttacker(angel, ai);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));

        addCard("Hill Giant", ai);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(angel, sa.getTargetCard());

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, opp);
        sa.resetTargets();
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
    }

    // Adanto Vanguard: "Pay 4 life: This creature gains indestructible until end of turn."
    @Test
    public void testVanguardPaysLifeToSurviveTheBlock() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card vanguard = addCard("Adanto Vanguard", ai);
        vanguard.setSickness(false);
        Card giant = addCard("Hill Giant", opp);
        SpellAbility sa = activated(vanguard);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false, 1);
        Combat combat = new Combat(ai);
        combat.addAttacker(vanguard, opp);
        combat.addBlocker(vanguard, giant);
        combat.setBlocked(vanguard, true);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
    }
}
