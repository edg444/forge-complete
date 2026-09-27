package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.NumberInstances;
import forge.game.card.NumberNudge;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * More or Less: one printed number on a spell or permanent reads 1 higher or lower until end of turn.
 */
public class MoreOrLessTest extends AITest {

    private static NumberNudge find(Card c, String labelStart) {
        for (NumberNudge n : NumberInstances.of(c)) {
            if (n.label.startsWith(labelStart)) {
                return n;
            }
        }
        AssertJUnit.fail("no \"" + labelStart + "\" among " + labels(c));
        return null;
    }

    private static List<String> labels(Card c) {
        return NumberInstances.of(c).stream().map(n -> n.label).toList();
    }

    private static void nudge(Game game, Card c, String labelStart, int to) {
        c.addNumberNudge(find(c, labelStart).changedTo(to, game.getNextTimestamp()));
        game.getAction().checkStateEffects(true);
    }

    @Test
    public void testOnlyPrintedNumbersAreOffered() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        List<String> opt = labels(addCardToZone("Opt", me, ZoneType.Hand));
        // "Scry 1." prints its 1; "Draw a card." prints "a", which isn't a number word
        AssertJUnit.assertTrue(opt.toString(), opt.stream().anyMatch(l -> l.contains("Scry 1")));
        AssertJUnit.assertFalse(opt.toString(), opt.stream().anyMatch(l -> l.contains("Draw a card")));
        AssertJUnit.assertTrue(labels(addCardToZone("Divination", me, ZoneType.Hand)).stream()
                .anyMatch(l -> l.startsWith("The 2 in \"Draw two cards.\"")));
    }

    @Test
    public void testPowerToughnessAndManaValue() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", me);
        nudge(game, bears, "Power", 3);
        AssertJUnit.assertEquals(3, bears.getNetPower());
        nudge(game, bears, "The {1}", 2);
        AssertJUnit.assertEquals(3, bears.getCMC());

        Card elves = addCard("Llanowar Elves", me);
        nudge(game, elves, "Toughness", 0);
        // "If you change a toughness of 1 to 0, the creature will probably die."
        AssertJUnit.assertTrue(me.getZone(ZoneType.Graveyard).contains(game.getCardState(elves)));
    }

    @Test
    public void testAbilityNumberAndItsText() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card divination = addCardToZone("Divination", me, ZoneType.Hand);
        nudge(game, divination, "The 2", 3);
        SpellAbility sa = divination.getFirstSpellAbility();
        AssertJUnit.assertEquals("3", sa.getParam("NumCards"));
        AssertJUnit.assertTrue(sa.getDescription(), sa.getDescription().contains("Draw three cards."));
        // a copy, as on the stack, reads it the same way once its text is refreshed
        SpellAbility copy = sa.copy(divination, false);
        copy.changeText();
        AssertJUnit.assertEquals("3", copy.getParam("NumCards"));

        Card shivan = addCard("Shivan Dragon", me);
        nudge(game, shivan, "The 1", 2);
        SpellAbility pump = null;
        for (SpellAbility a : shivan.getSpellAbilities()) {
            if (a.isActivatedAbility()) {
                pump = a;
            }
        }
        AssertJUnit.assertEquals("+2", pump.getParam("NumAtt"));
        AssertJUnit.assertTrue(pump.getDescription(), pump.getDescription().contains("+2/+0"));
    }

    @Test
    public void testActivationCostAndKeyword() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card stone = addCard("Mind Stone", me);
        nudge(game, stone, "The {1} in the cost", 2);
        SpellAbility draw = null;
        for (SpellAbility a : stone.getSpellAbilities()) {
            if (a.hasParam("NumCards")) {
                draw = a;
            }
        }
        AssertJUnit.assertEquals(2, draw.getPayCosts().getTotalMana().getCMC());

        Card ronin = addCard("Battle-Mad Ronin", me);
        nudge(game, ronin, "The 2 in Bushido", 3);
        AssertJUnit.assertTrue(ronin.hasKeyword("Bushido:3"));
        AssertJUnit.assertFalse(ronin.hasKeyword("Bushido:2"));
    }

    @Test
    public void testEndsAtEndOfTurnAndOnZoneChange() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", opp);

        Card mol = addCardToZone("More or Less", me, ZoneType.Hand);
        SpellAbility sa = mol.getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        sa.getTargets().add(bears);
        forge.game.ability.AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        // the AI lowers numbers on an opponent's creature, toughness first
        AssertJUnit.assertEquals(1, bears.getNetToughness());

        game.getEndOfTurn().executeUntil();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, bears.getNetToughness());

        nudge(game, bears, "Power", 3);
        Card bounced = game.getAction().moveToHand(bears, null, null);
        AssertJUnit.assertTrue(bounced.getNumberNudges().isEmpty());
        AssertJUnit.assertEquals(2, bounced.getBasePower());
        AssertJUnit.assertFalse(bounced.hasKeyword(Keyword.FLYING));
    }
}
