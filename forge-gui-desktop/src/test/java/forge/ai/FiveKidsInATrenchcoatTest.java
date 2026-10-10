package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;

/**
 * Five Kids in a Trenchcoat (Mystery Booster playtest card), Oracle text and ruling checked against Scryfall
 * 2026-10-10: five creatures only for effects that determine how many creatures you control. User rulings: anyone's
 * count of the creatures you control, filtered counts and thresholds included; a count of all creatures sees one.
 */
public class FiveKidsInATrenchcoatTest extends AITest {

    @Test
    public void testCountsOfCreaturesYouControl() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Five Kids in a Trenchcoat", p);
        Card crusader = addCard("Crusader of Odric", p);
        Card bears = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);
        // Crusader of Odric: P/T = number of creatures you control
        AssertJUnit.assertEquals(6, crusader.getNetPower());

        // an opponent's count of the creatures you control
        AssertJUnit.assertEquals(6, AbilityUtils.xCount(bears, "Valid Creature.OppCtrl", bears.getFirstSpellAbility()));
        AssertJUnit.assertEquals(1, AbilityUtils.xCount(bears, "Valid Creature.YouCtrl", bears.getFirstSpellAbility()));
        // a filtered count still sees five
        AssertJUnit.assertEquals(5, AbilityUtils.xCount(crusader, "Valid Creature.YouCtrl+Other", crusader.getFirstSpellAbility()));
        // all creatures on the battlefield: one
        AssertJUnit.assertEquals(3, AbilityUtils.xCount(crusader, "Valid Creature", crusader.getFirstSpellAbility()));
    }

    @Test
    public void testThresholds() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card jetmir = addCard("Jetmir, Nexus of Revels", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(jetmir.hasKeyword(Keyword.VIGILANCE));

        Card kids = addCard("Five Kids in a Trenchcoat", p);
        game.getAction().checkStateEffects(true);
        // six "creatures": vigilance and trample, not double strike
        AssertJUnit.assertTrue(jetmir.hasKeyword(Keyword.VIGILANCE));
        AssertJUnit.assertTrue(jetmir.hasKeyword(Keyword.TRAMPLE));
        AssertJUnit.assertFalse(jetmir.hasKeyword(Keyword.DOUBLE_STRIKE));
        AssertJUnit.assertEquals(3, kids.getNetPower());
    }
}
