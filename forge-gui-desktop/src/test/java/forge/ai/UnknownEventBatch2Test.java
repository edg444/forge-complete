package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.ColorSet;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Your Mana Rock (UNK CA02b), Unicycling Automaton (UNK CA03), Color Pie (UNK CA11b) and Melira's Snacks (UNK CA12),
 * checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch2Test extends AITest {

    private static SpellAbility activated(final Card c) {
        return c.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
    }

    @Test
    public void testYourManaRockColorsAsTheGameBegins() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card rock = addCardToZone("Your Mana Rock", ai, ZoneType.Library);
        AssertJUnit.assertFalse(rock.hasMarkedColor());
        final java.lang.reflect.Method pre = game.getAction().getClass()
                .getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), ai);
        AssertJUnit.assertTrue("two colors written as the game begins", rock.hasMarkedColor());
        AssertJUnit.assertEquals(2, rock.getMarkedColors().countColors());
    }

    @Test
    public void testYourManaRockMana() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card rock = addCard("Your Mana Rock", ai);
        rock.setMarkedColors(ColorSet.fromNames("white", "blue"));
        final SpellAbility mana = rock.getManaAbilities().get(0);
        AssertJUnit.assertEquals("W U", mana.getManaPart().getComboColors(mana));
    }

    @Test
    public void testMeliraSnacksPoisonTolerance() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final int base = ai.getPoisonCountersToLose();
        addCard("Melira's Snacks", ai);
        AssertJUnit.assertEquals(base + 3, ai.getPoisonCountersToLose());
        addCard("Melira's Snacks", ai);
        AssertJUnit.assertEquals("instances add up", base + 6, ai.getPoisonCountersToLose());
        AssertJUnit.assertEquals("an opponent's doesn't help", base, game.getPlayers().get(0).getPoisonCountersToLose());
    }

    @Test
    public void testColorPie() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Island", ai, ZoneType.Library);
        final Card dead = addCardToZone("Grizzly Bears", human, ZoneType.Graveyard);
        final Card pie = addCard("Color Pie", ai);
        final SpellAbility sa = activated(pie);
        sa.setActivatingPlayer(ai);
        sa.getSubAbility().getSubAbility().getTargets().add(dead);
        sa.getSubAbility().getSubAbility().getSubAbility().getSubAbility().getTargets().add(ai);
        final int life = ai.getLife();
        final int humanLife = human.getLife();
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(life + 3, ai.getLife());
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue(dead.getGame().getCardState(dead).isInZone(ZoneType.Exile));
        AssertJUnit.assertEquals(humanLife - 1, human.getLife());
        AssertJUnit.assertEquals(1, ai.getManaPool().totalMana());
    }

    @Test
    public void testUnicyclingAutomatonHandBackup() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final Card automaton = addCard("Unicycling Automaton", ai);
        final SpellAbility etb = automaton.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue("basic landcycling in hand", bears.hasKeyword(Keyword.TYPECYCLING));

        final SpellAbility cast = bears.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        final Card onStack = game.getAction().moveToStack(bears, cast);
        onStack.setCastSA(cast);
        onStack.setCastFrom(ai.getZone(ZoneType.Hand));
        final Card entered = game.getAction().moveToPlay(onStack, ai, cast, null);
        AssertJUnit.assertEquals("enters with a +1/+1 counter", 1, entered.getCounters(CounterEnumType.P1P1));
    }
}
