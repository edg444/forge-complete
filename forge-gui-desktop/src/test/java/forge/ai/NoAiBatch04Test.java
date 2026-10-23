package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** AI:RemoveDeck:All cards, fourth batch (A...): each test is the situation that card's new AI is for. */
public class NoAiBatch04Test extends AITest {

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

    // Aphetto Dredging: "Return up to three target creature cards of the creature type of your choice from your graveyard to your hand."
    @Test
    public void testAphettoDredgingAnnouncesTheGraveyardsType() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        lands(ai, "Swamp", 4);
        addCardToZone("Gravedigger", ai, ZoneType.Graveyard);
        addCardToZone("Walking Corpse", ai, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        Card dredging = addCardToZone("Aphetto Dredging", ai, ZoneType.Hand);
        SpellAbility sa = dredging.getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals("Zombie", dredging.getChosenType());
        AssertJUnit.assertEquals(2, sa.getTargets().size());
    }

    // Archon of Valor's Reach: "choose artifact, enchantment, instant, sorcery, or planeswalker. Players can't cast spells of the chosen type."
    @Test
    public void testArchonShutsOffWhatTheOpponentUses() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        addCardToZone("Ornithopter", ai, ZoneType.Library);
        addCardToZone("Lightning Bolt", opp, ZoneType.Graveyard);
        addCardToZone("Shock", opp, ZoneType.Graveyard);
        Card archon = addCardToZone("Archon of Valor's Reach", ai, ZoneType.Hand);
        SpellAbility choose = AbilityFactory.getAbility(archon.getSVar("ChooseT"), archon);
        choose.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("Instant", ComputerUtil.chooseSomeType(ai, "Card", choose,
                List.of("Artifact", "Enchantment", "Instant", "Sorcery", "Planeswalker")));
    }

    // Argent Mutation: "Target permanent becomes an artifact in addition to its other types until end of turn. Draw a card."
    @Test
    public void testArgentMutationCyclesAtTheEndOfTheOpponentsTurn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Island", 3);
        fillLibrary(ai, 5);
        SpellAbility sa = addCardToZone("Argent Mutation", ai, ZoneType.Hand).getFirstSpellAbility();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, sa));
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(ai, sa.getTargetCard().getController());
    }

    // Animation Module: "{3}, {T}: Choose a counter on target permanent or player. Give that permanent or player another counter of that kind."
    @Test
    public void testAnimationModuleAddsToItsOwnCounters() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        lands(ai, "Plains", 3);
        Card module = addCard("Animation Module", ai);
        Card bears = addCard("Grizzly Bears", ai);
        bears.addCounterInternal(CounterEnumType.P1P1, 1, ai, false, null, null);
        SpellAbility sa = activated(module);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, sa));
        AssertJUnit.assertEquals(bears, sa.getTargetCard());
    }
}
