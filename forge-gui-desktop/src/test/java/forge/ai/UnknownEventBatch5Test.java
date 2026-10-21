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
 * Joraga Peach, Naturalize the Phyresis, That's No Moonmist, Growing Detective, Investi-Gate, Merfolk Surveyor and
 * Fear of Unsleeved Decks (UNK CG01-CG04), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch5Test extends AITest {

    @Test
    public void testJoragaPeach() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card peach = addCard("Joraga Peach", ai);
        peach.addCounterInternal(CounterEnumType.P1P1, 2, ai, false, null, null);
        peach.addCounterInternal(CounterEnumType.CHARGE, 1, ai, false, null, null);
        addCard("Forest", ai);
        addCard("Forest", ai);
        final SpellAbility sa = peach.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility)
                .findFirst().orElseThrow();
        sa.setActivatingPlayer(ai);
        sa.getSubAbility().getTargets().add(bears);
        final int life = ai.getLife();
        AssertJUnit.assertTrue(ComputerUtil.playNoStack(ai, sa, game, true));
        AssertJUnit.assertEquals("1 life for each counter", life + 3, ai.getLife());
        AssertJUnit.assertEquals("its +1/+1 counters moved", 2, bears.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testNaturalizeThePhyresis() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final int base = ai.getPoisonCountersToLose();
        addCard("Naturalize the Phyresis", ai);
        AssertJUnit.assertEquals(base + 1, ai.getPoisonCountersToLose());
    }

    @Test
    public void testGrowingDetective() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card detective = addCard("Growing Detective", ai);
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(detective, "X", null));
    }

    @Test
    public void testInvestiGate() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card spell = addCardToZone("Investi-Gate", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        final long gates = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Gate")).count();
        AssertJUnit.assertEquals("two guildgate copies", 2, gates);
        AssertJUnit.assertTrue("and a Clue", ai.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c -> c.getType().hasSubtype("Clue")));
    }

    @Test
    public void testMerfolkSurveyor() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Serra Angel", ai, ZoneType.Library);
        addCardToZone("Island", human, ZoneType.Library);
        final Card surveyor = addCard("Merfolk Surveyor", ai);
        final SpellAbility etb = surveyor.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        final int life = ai.getLife();
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals("one nonland revealed", 1, surveyor.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals("the land went to its owner's hand", 1, human.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals("5 beats 0: won", life + 3, ai.getLife());
    }

    @Test
    public void testFearOfUnsleevedDecks() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card fear = addCard("Fear of Unsleeved Decks", ai);
        ai.getLobbyPlayer().setSleeveIndex(0);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("the plain card back isn't a sleeve", ai.isSleeved());
        AssertJUnit.assertEquals(5, fear.getNetPower());
        AssertJUnit.assertTrue(fear.hasKeyword(Keyword.TRAMPLE));
        ai.getLobbyPlayer().setSleeveIndex(3);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(ai.isSleeved());
        AssertJUnit.assertEquals(4, fear.getNetPower());
        AssertJUnit.assertFalse(fear.hasKeyword(Keyword.TRAMPLE));
    }
}
