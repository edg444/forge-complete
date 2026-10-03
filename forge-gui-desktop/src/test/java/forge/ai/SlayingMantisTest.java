package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Slaying Mantis (Unstable) - Oracle text, the seven Scryfall rulings and the Unstable FAQ entry checked 2026-10-03.
 */
public class SlayingMantisTest extends AITest {

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card mantis = addCard("Slaying Mantis", game.getPlayers().get(1));
        String text = mantis.getAbilityText();
        AssertJUnit.assertTrue(text, text.contains("Just a second (As long as this spell is on the stack, players can't move permanents.)"));
        AssertJUnit.assertTrue(text, text.contains("This creature enters by being thrown from a distance of at least three feet."));
        AssertJUnit.assertTrue(text, text.contains("When this creature enters, it fights each creature an opponent controls that it touched as it entered."));
    }

    @Test
    public void testFightsEachTouchedOpposingCreatureAtOnce() {
        // the ruling's example: lands on a 3/3, a 4/4 and a 5/5 - it deals 6 to each and takes 12, so all four die
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card mantis = addCard("Slaying Mantis", me);
        Card three = addCard("Centaur Courser", opp);
        Card four = addCard("Bonebreaker Giant", opp);
        Card five = addCard("Colossapede", opp);
        Card mine = addCard("Grizzly Bears", me);
        AssertJUnit.assertEquals(3, three.getNetToughness());
        AssertJUnit.assertEquals(4, four.getNetToughness());
        AssertJUnit.assertEquals(5, five.getNetToughness());
        mantis.addRemembered(three);
        mantis.addRemembered(four);
        mantis.addRemembered(five);
        mantis.addRemembered(mine);
        SpellAbility fight = mantis.getTriggers().getFirst().ensureAbility().copy(mantis, me, false);
        fight.setActivatingPlayer(me);
        AbilityUtils.resolve(fight);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(three).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(four).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(five).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(mantis).isInZone(ZoneType.Graveyard));
        // only creatures an opponent controls
        AssertJUnit.assertTrue(mine.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(0, mine.getDamage());
    }

    @Test
    public void testThrowAlwaysLandsAndAimsAtWhatItKills() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", ai);
        Card big = addCard("Colossal Dreadmaw", opp); // 6/6: dies to a 6-power fight
        Card huge = addCard("Ancient Brontodon", opp); // 9/9: doesn't
        AssertJUnit.assertEquals(6, big.getNetToughness());
        AssertJUnit.assertEquals(9, huge.getNetToughness());
        int touchedBig = 0;
        for (int i = 0; i < 40; i++) {
            Card mantis = addCardToZone("Slaying Mantis", ai, ZoneType.Hand);
            Card entered = game.getAction().moveToPlay(mantis, ai, null, null);
            // aimed at the Dreadmaw, it lands on it or a neighbor - never on its own side
            AssertJUnit.assertFalse(entered.isRemembered(bears));
            if (entered.isRemembered(big)) {
                touchedBig++;
            }
            entered.clearRemembered();
            game.getAction().moveToGraveyard(entered, null, null);
        }
        AssertJUnit.assertTrue("touched the Dreadmaw " + touchedBig + " of 40", touchedBig > 10);
    }

    @Test
    public void testJustASecondLocksTheStack() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card mantis = addCardToZone("Slaying Mantis", me, ZoneType.Hand);
        AssertJUnit.assertTrue(mantis.hasKeyword(Keyword.JUST_A_SECOND));
        SpellAbility cast = mantis.getFirstSpellAbility();
        cast.setActivatingPlayer(me);
        game.getStack().add(cast);
        AssertJUnit.assertTrue(game.getStack().isSplitSecondOnStack());
    }
}
