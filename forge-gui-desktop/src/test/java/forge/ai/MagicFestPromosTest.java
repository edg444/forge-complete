package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * MagicFest 2025 (Second City, All-You-Can-Eat Buffet, Spaghetti Junction) and 2026 (In Residence, Bicycle Elemental,
 * Auspicious Aquarium), checked against Scryfall 2026-10-22 (no rulings). User rulings 2026-10-22: the Aquarium's twelve
 * different creatures are differently named; In Residence's introductions are typed and can't repeat in a game.
 */
public class MagicFestPromosTest extends AITest {

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testSecondCity() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card city = addCard("Second City", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        AssertJUnit.assertFalse("the first spell can't use it", bolt.isValid("Spell.SecondSpellThisTurn", ai, city, null));
        final Card first = addCardToZone("Shock", ai, ZoneType.Hand);
        final SpellAbility shock = first.getFirstSpellAbility();
        shock.setActivatingPlayer(ai);
        shock.getTargets().add(game.getPlayers().get(0));
        game.getStack().add(shock);
        resolveAll(game);
        AssertJUnit.assertTrue(bolt.isValid("Spell.SecondSpellThisTurn", ai, city, null));
        AssertJUnit.assertEquals(2, city.getManaAbilities().size());
    }

    @Test
    public void testAllYouCanEatBuffet() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 3);
        addCard("All-You-Can-Eat Buffet", ai);
        game.getAction().checkStateEffects(true);
        final Card top = ai.getCardsIn(ZoneType.Library).get(0);
        AssertJUnit.assertTrue(top.getType().hasSubtype("Food"));
        AssertJUnit.assertFalse("a creature card on top may be cast", top.mayPlay(ai).isEmpty());
        AssertJUnit.assertTrue(top.mayPlayerLook(ai));
    }

    @Test
    public void testSpaghettiJunction() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card junction = addCard("Spaghetti Junction", ai);
        junction.addCounterInternal(CounterType.getType("TRAFFIC"), 7, ai, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(junction.isInPlay());
        AbilityUtils.resolve(svar(junction, "TrigTraffic", ai));
        resolveAll(game);
        AssertJUnit.assertFalse("sacrificed at eight", game.getCardState(junction).isInPlay());
        final Card atlantakul = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Atlantakul")).get(0);
        AssertJUnit.assertEquals(20, atlantakul.getNetPower());
        AssertJUnit.assertTrue(atlantakul.getType().isLegendary());
        AssertJUnit.assertTrue(atlantakul.hasKeyword(Keyword.ANNIHILATOR));
    }

    @Test
    public void testAuspiciousAquarium() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card aquarium = addCard("Auspicious Aquarium", ai);
        final String[] aquatic = {"Merfolk Looter", "Coral Merfolk", "Silvergill Adept", "Cursecatcher", "Vodalian Soldiers",
                "Sea Monster", "Giant Octopus", "Ancient Crab", "Kraken Hatchling", "Coral Eel", "Man-o'-War"};
        Card looter = null;
        for (final String n : aquatic) {
            final Card c = addCard(n, ai);
            if (n.equals("Merfolk Looter")) {
                looter = c;
            }
        }
        addCard("Merfolk Looter", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, looter.getNetPower());
        final SpellAbility sa = aquarium.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("a second Merfolk Looter isn't a twelfth", 11, AbilityUtils.calculateAmount(aquarium, "Aquatic", sa));
        addCard("Sea Serpent", ai);
        AssertJUnit.assertEquals(12, AbilityUtils.calculateAmount(aquarium, "Aquatic", sa));
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(aquarium, "Brushwaggs", sa));
        fillLibrary(ai, 3);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        AbilityUtils.resolve(svar(aquarium, "TrigWin", ai));
        AssertJUnit.assertTrue(ai.hasWon() || game.isGameOver());
    }

    @Test
    public void testBicycleElemental() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bike = addCard("Bicycle Elemental", ai);
        bike.setChosenType("Goblin");
        final Card goblin = addCardToZone("Goblin Piker", ai, ZoneType.Hand);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final Card goblinText = addCardToZone("Goblin King", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(goblin.hasKeyword(Keyword.CYCLING));
        AssertJUnit.assertTrue(goblinText.hasKeyword(Keyword.CYCLING));
        AssertJUnit.assertFalse(bolt.hasKeyword(Keyword.CYCLING));
        AssertJUnit.assertFalse("a whole word", forge.game.card.CardProperty.hasWord(goblin, "Gob"));

        for (int i = 0; i < 6; i++) {
            addCardToZone("Goblin Piker", ai, ZoneType.Library);
        }
        AssertJUnit.assertEquals("the word on the most of its cards", "creature", forge.ai.ComputerUtil.chooseSomeType(ai, "word", svar(bike, "ChooseWord", ai), java.util.List.of("apple")));
    }

    @Test
    public void testInResidence() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card residence = addCard("In Residence", ai);
        final Card legend = addCardToZone("Isamaru, Hound of Konda", ai, ZoneType.Exile);
        residence.addRemembered(legend);
        final SpellAbility intro = svar(residence, "TrigIntroduce", ai);
        AbilityUtils.resolve(intro);
        game.getAction().checkStateEffects(true);
        final Card isamaru = game.getCardState(legend);
        AssertJUnit.assertTrue(isamaru.isInPlay());
        AssertJUnit.assertTrue(isamaru.hasKeyword(Keyword.HASTE));
        AssertJUnit.assertEquals("an introduction used", 1, ((java.util.Set<?>) ai.getGameNote("IntroductionsUsed")).size());
    }
}
