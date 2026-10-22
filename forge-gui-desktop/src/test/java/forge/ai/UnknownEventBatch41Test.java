package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * The Multifaceted Phyrexian, Incubation Triformer, The Underworld Fantasy Queen, Bin Chicken, Dan, Shrewd Trader,
 * Spice Rack, Mister Cheddar, Surprise, Nerd! and The Gunky Runner (UNK rares), checked against Scryfall 2026-10-22
 * (no rulings).
 */
public class UnknownEventBatch41Test extends AITest {

    @Test
    public void testMultifacetedPhyrexianMakesElk() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card phyrexian = addCard("The Multifaceted Phyrexian", ai);
        final Card angel = addCard("Serra Angel", human);
        final SpellAbility elk = phyrexian.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        elk.setActivatingPlayer(ai);
        elk.getTargets().add(angel);
        AbilityUtils.resolve(elk);
        AssertJUnit.assertEquals(3, angel.getNetPower());
        AssertJUnit.assertTrue(angel.getType().hasCreatureType("Elk"));
        AssertJUnit.assertFalse(angel.getType().hasCreatureType("Angel"));
        AssertJUnit.assertTrue(angel.getColor().hasGreen() && !angel.getColor().hasWhite());
    }

    @Test
    public void testTriformerThirdSide() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card triformer = addCard("Incubation Triformer", ai);
        final SpellAbility etb = triformer.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        final Card incubator = ai.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).findFirst().orElseThrow();
        incubator.changeCardState("Transform", null, null);
        AssertJUnit.assertTrue(incubator.isValid("Creature.Phyrexian+token+YouCtrl+Transformed", ai, triformer, null));
        final SpellAbility third = triformer.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        third.setActivatingPlayer(ai);
        third.getTargets().add(incubator);
        AbilityUtils.resolve(third);
        AssertJUnit.assertEquals("Phyrexian Horror", incubator.getName());
        AssertJUnit.assertEquals("3/3 plus its +1/+1 counter", 4, incubator.getNetPower());
        AssertJUnit.assertTrue(incubator.getType().hasCreatureType("Horror"));
    }

    @Test
    public void testFantasyQueenSeasons() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card queen = addCard("The Underworld Fantasy Queen", ai);
        queen.setChosenMode("Spring");
        final SpellAbility upkeep = queen.getTriggers().stream().filter(t -> t.getParam("TriggerDescription").contains("next season"))
                .findFirst().orElseThrow().ensureAbility();
        upkeep.setActivatingPlayer(ai);
        for (final String next : new String[] {"Summer", "Fall", "Winter", "Spring"}) {
            AbilityUtils.resolve(upkeep);
            AssertJUnit.assertEquals(next, queen.getChosenMode());
        }
    }

    @Test
    public void testBinChickenFoodIsJunk() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Bin Chicken", ai);
        final Card chicken = ai.getCardsIn(ZoneType.Battlefield).getFirst();
        final SpellAbility etb = chicken.getTriggers().get(0).ensureAbility().getAdditionalAbilityList("Choices").get(0);
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        game.getAction().checkStateEffects(true);
        final Card food = ai.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).findFirst().orElseThrow();
        AssertJUnit.assertTrue(food.getType().hasSubtype("Food") && food.getType().hasSubtype("Junk"));
    }

    @Test
    public void testDanBargainsAnyPermanent() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card spell = addCardToZone("Torch the Tower", ai, ZoneType.Hand);
        final SpellAbility cast = spell.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(GameActionUtil.getOptionalCostValues(cast).stream().anyMatch(v -> v.getCost().toString().contains("artifact")));
        addCard("Dan, Shrewd Trader", ai);
        final OptionalCostValue bargain = GameActionUtil.getOptionalCostValues(cast).stream()
                .filter(v -> v.getType() == forge.game.spellability.OptionalCost.Bargain).findFirst().orElseThrow();
        AssertJUnit.assertTrue(bargain.getCost().toString(), bargain.getCost().toString().contains("permanent"));
    }

    @Test
    public void testSpiceRackOneCommander() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card a = addCardToZone("Krenko, Mob Boss", human, ZoneType.Command);
        final Card b = addCardToZone("Llanowar Elves", human, ZoneType.Command);
        human.addCommander(a);
        human.addCommander(b);
        a.setCommander(true);
        b.setCommander(true);
        final Card rack = addCard("Spice Rack", ai);
        rack.setChosenPlayer(human);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, forge.game.card.CardLists.filter(human.getCardsIn(ZoneType.Command), Card::isCommander).size());
        AssertJUnit.assertEquals(1, human.getCardsIn(ZoneType.Graveyard).size());
    }

    @Test
    public void testCheddarSliverDecks() {
        initAndCreateGame();
        final Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("Mister Cheddar, Cheese Sliver"));
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Plague Sliver"), 3);
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Swamp"), 96);
        AssertJUnit.assertNull(DeckFormat.Commander.getDeckConformanceProblem(deck));
        final Deck plain = new Deck();
        plain.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("Bladewing the Risen"));
        plain.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Plague Sliver"), 3);
        plain.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Swamp"), 96);
        AssertJUnit.assertNotNull("without Cheddar it's still singleton", DeckFormat.Commander.getDeckConformanceProblem(plain));
    }

    @Test
    public void testSurpriseNerdInvertsPumps() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Surprise, Nerd!", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card growth = addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final SpellAbility cast = growth.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        cast.getTargets().add(bears);
        AbilityUtils.resolve(cast);
        AssertJUnit.assertEquals("+3/+3 became -3/-3", -1, bears.getNetPower());
    }

    @Test
    public void testGunkyRunnerShufflesGunk() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card runner = addCard("The Gunky Runner", ai);
        final SpellAbility etb = runner.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals(3, forge.game.card.CardLists.filter(human.getCardsIn(ZoneType.Library), c -> c.getName().equals("Gunk")).size());
    }
}
