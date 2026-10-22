package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Memnarchitect, Triple Threat, The Knight of Land Drops, The Wheeling Runner, Blustering Barnyard and Mana Max,
 * Afterburner (UNK rares), checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch43Test extends AITest {

    @Test
    public void testMemnarchitectAffinity() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card memn = addCard("Memnarchitect", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        addCard("Ornithopter", ai);
        final SpellAbility artify = memn.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        artify.setActivatingPlayer(ai);
        artify.getTargets().add(bears);
        AbilityUtils.resolve(artify);
        AssertJUnit.assertTrue(bears.isArtifact());
        final Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        final SpellAbility cast = wurm.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        final ManaCostBeingPaid cost = new ManaCostBeingPaid(cast.getPayCosts().getTotalMana());
        forge.game.cost.CostAdjustment.adjust(cost, cast, ai, null, true, false);
        AssertJUnit.assertEquals("the Bears count, the Ornithopter doesn't", 5, cost.getConvertedManaCost());
    }

    @Test
    public void testTripleThreat() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Triple Threat", ai);
        final Card commander = addCard("Grizzly Bears", ai);
        ai.addCommander(commander);
        commander.setCommander(true);
        human.addDamageAfterPrevention(2, commander, null, true, null);
        AssertJUnit.assertEquals(6, human.getCommanderDamage(commander));
    }

    @Test
    public void testKnightPartners() {
        initAndCreateGame();
        final Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("The Knight of Land Drops"));
        deck.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("Syr Konrad, the Grim"));
        AssertJUnit.assertNull(DeckFormat.Commander.getCommanderConformanceProblem(deck));
        final Deck noKnight = new Deck();
        noKnight.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("The Knight of Land Drops"));
        noKnight.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("Krenko, Mob Boss"));
        AssertJUnit.assertNotNull(DeckFormat.Commander.getCommanderConformanceProblem(noKnight));
    }

    @Test
    public void testWheelingRunnerRandomCards() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card runner = addCard("The Wheeling Runner", ai);
        final SpellAbility random = AbilityFactory.getAbility(runner.getSVar("DBRandom"), runner);
        random.setActivatingPlayer(ai);
        AbilityUtils.resolve(random);
        AssertJUnit.assertEquals(4, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue(CardLists.filter(ai.getCardsIn(ZoneType.Hand), Card::isStartingDeckCard).isEmpty());
    }

    @Test
    public void testBlusteringBarnyard() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card barnyard = addCard("Blustering Barnyard", ai);
        AssertJUnit.assertTrue(barnyard.getType().hasSubtype("Barnyard"));
        final SpellAbility mana = barnyard.getManaAbilities().get(0);
        mana.setActivatingPlayer(ai);
        AbilityUtils.resolve(mana.getSubAbility());
        AssertJUnit.assertEquals("one barnyard: 1 life", 19, ai.getLife());
    }

    @Test
    public void testManaMaxEndStepDamage() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card max = addCard("Mana Max, Afterburner", ai);
        addCard("Forest", human);
        addCard("Forest", human);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.END_OF_TURN, human);
        final SpellAbility end = max.getTriggers().get(0).ensureAbility();
        end.setActivatingPlayer(ai);
        end.setTriggeringObject(forge.game.ability.AbilityKey.Player, human);
        AbilityUtils.resolve(end);
        AssertJUnit.assertEquals(18, human.getLife());
        final SpellAbility lost = max.getTriggers().get(1).ensureAbility();
        lost.setActivatingPlayer(ai);
        lost.setTriggeringObject(forge.game.ability.AbilityKey.Player, human);
        AbilityUtils.resolve(lost);
        AssertJUnit.assertEquals(1, CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Treasure")).size());
    }
}
