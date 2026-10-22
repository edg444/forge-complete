package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Bringer of the Red Beard, The Wiley Speedster, Mijo, the Bull, Mzed, Mercenary Leader, At Least It's a Dry Heat,
 * Auntie Ant, Ant Auntie, The Powerful Dragon, Tragda, the Burner, Brutal Command, Math is for Blockers and Snapsail
 * Rider (UNK red rares), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch16Test extends AITest {

    @Test
    public void testRedBeard() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bringer = addCard("The Bringer of the Red Beard", ai);
        final SpellAbility etb = bringer.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        final Card beard = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Red Beard")).findFirst().orElseThrow();
        final Card bears = addCard("Grizzly Bears", ai);
        beard.attachToEntity(bears, null);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue("equipped creature is red", bears.getColor().hasRed());
        AssertJUnit.assertEquals("red +1, equipped +1", 4, bears.getNetPower());
    }

    @Test
    public void testMijoFromDowntown() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Mijo, the Bull", ai);
        final Card shock = addCardToZone("Shock", ai, ZoneType.Hand);
        final SpellAbility sa = shock.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(human);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("exactly 2 becomes 3", 17, human.getLife());
    }

    @Test
    public void testDryHeat() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card giant = addCard("Colossal Dreadmaw", human);
        final Card heat = addCardToZone("At Least It's a Dry Heat", ai, ZoneType.Hand);
        final SpellAbility sa = heat.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(giant);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(giant).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testAuntieAnt() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card ant = addCard("Auntie Ant, Ant Auntie", ai);
        final SpellAbility etb = ant.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals("multiblight 4", 4, ant.getCounters(CounterEnumType.M1M1));
        final SpellAbility attack = ant.getTriggers().get(1).ensureAbility();
        attack.setActivatingPlayer(ai);
        AbilityUtils.resolve(attack);
        final long insects = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasCreatureType("Insect")).count();
        AssertJUnit.assertEquals("one counter removed: one Insect", 1, insects);
        AssertJUnit.assertEquals(3, ant.getCounters(CounterEnumType.M1M1));
    }

    @Test
    public void testTragdaAndMath() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card tragda = addCard("Tragda, the Burner", ai);
        final Card land = addCardToZone("Mountain", ai, ZoneType.Hand);
        game.getAction().moveToGraveyard(land, null);
        // the trigger itself: a test's direct move doesn't collect triggers from other permanents
        final SpellAbility grows = tragda.getTriggers().get(0).ensureAbility();
        grows.setActivatingPlayer(ai);
        AbilityUtils.resolve(grows);
        AssertJUnit.assertEquals(2, tragda.getCounters(CounterEnumType.P1P1));
        final Card math = addCardToZone("Math is for Blockers", ai, ZoneType.Hand);
        final SpellAbility sa = math.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(tragda.hasKeyword(Keyword.DOUBLE_STRIKE));
        AssertJUnit.assertTrue(tragda.hasKeyword(Keyword.TRAMPLE));
    }

    @Test
    public void testBrutalCommandModes() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card command = addCardToZone("Brutal Command", ai, ZoneType.Hand);
        final java.util.List<? extends SpellAbility> modes = command.getFirstSpellAbility().getAdditionalAbilityList("Choices");
        for (int i = 0; i < 3; i++) {
            modes.get(i).setActivatingPlayer(ai);
            AbilityUtils.resolve(modes.get(i));
        }
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c -> c.isToken() && c.getName().equals("Gingerbrute")));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c -> c.isToken() && c.getName().equals("Brute Suit")));
        final Card brute = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("The Brute")).findFirst().orElseThrow();
        AssertJUnit.assertEquals(bears, brute.getAttachedTo());
    }
}
