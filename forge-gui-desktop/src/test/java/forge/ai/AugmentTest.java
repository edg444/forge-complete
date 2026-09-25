package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Unstable's host/augment. Expected characteristics follow the "Monkey- Kitten" example in the Unstable mechanics
 * article and the Unstable FAQ.
 */
public class AugmentTest extends AITest {

    private static SpellAbility augmentAbility(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Augment) {
                return sa;
            }
        }
        return null;
    }

    private static void augment(Player p, Card augmentCard, Card host) {
        SpellAbility sa = augmentAbility(augmentCard);
        sa.setActivatingPlayer(p);
        sa.resetTargets();
        sa.getTargets().add(host);
        AbilityUtils.resolve(sa);
        p.getGame().getAction().checkStateEffects(true);
    }

    @Test
    public void testCombinedCharacteristics() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card kitten = addCard("Adorable Kitten", me);
        kitten.setSickness(false);
        kitten.addCounterInternal(CounterEnumType.P1P1, 1, me, false, null, null);
        Card bears = addCard("Grizzly Bears", me);
        Card aug = addCardToZone("Half-Kitten, Half-", me, ZoneType.Hand);

        // an augment card is 0/0 on its own; "+1/+2" only adjusts its host
        AssertJUnit.assertEquals(0, aug.getNetPower());
        SpellAbility sa = augmentAbility(aug);
        AssertJUnit.assertTrue(sa.canTarget(kitten));
        AssertJUnit.assertFalse(sa.canTarget(bears));

        augment(me, aug, kitten);

        AssertJUnit.assertTrue(kitten.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(aug.isInZone(ZoneType.Merged));
        AssertJUnit.assertEquals("Half-Kitten, Half-Kitten", kitten.getName());
        // 1/1 host + 1/2 augment + the host's +1/+1 counter, which it keeps as the same object
        AssertJUnit.assertEquals(3, kitten.getNetPower());
        AssertJUnit.assertEquals(4, kitten.getNetToughness());
        AssertJUnit.assertFalse(kitten.isSick());
        AssertJUnit.assertFalse(kitten.getType().hasSupertype(CardType.Supertype.Host));
        AssertJUnit.assertTrue(kitten.hasKeyword(Keyword.AUGMENT));
        AssertJUnit.assertEquals("{W}", kitten.getManaCost().getShortString());
        AssertJUnit.assertFalse(augmentAbility(addCardToZone("Half-Kitten, Half-", me, ZoneType.Hand)).canTarget(kitten));
    }

    @Test
    public void testArtifactHostKeepsWhatIsRightOfTheBar() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card rocket = addCard("Angelic Rocket", me);
        augment(me, addCardToZone("Half-Kitten, Half-", me, ZoneType.Hand), rocket);

        AssertJUnit.assertEquals("Half-Kitten, Half-Rocket", rocket.getName());
        AssertJUnit.assertTrue(rocket.getType().isArtifact());
        AssertJUnit.assertTrue(rocket.getType().hasCreatureType("Cat"));
        AssertJUnit.assertTrue(rocket.getType().hasCreatureType("Angel"));
        AssertJUnit.assertTrue(rocket.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertEquals(5, rocket.getNetPower());
        AssertJUnit.assertEquals(6, rocket.getNetToughness());
        AssertJUnit.assertTrue(rocket.getColor().hasWhite());
    }

    @Test
    public void testHostEffectCompletesAugmentConditionInPlay() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card kitten = addCard("Adorable Kitten", me);
        augment(me, addCardToZone("Half-Kitten, Half-", me, ZoneType.Hand), kitten);

        Card shock = addCardToZone("Shock", opp, ZoneType.Hand);
        SpellAbility bolt = shock.getFirstSpellAbility();
        bolt.setActivatingPlayer(opp);
        bolt.getTargets().add(me);
        AbilityUtils.resolve(bolt);
        game.getAction().checkStateEffects(true);
        playUntilStackClear(game);

        // 20 - 2, then "roll a six-sided die. You gain life equal to the result"
        AssertJUnit.assertTrue(me.getLife() >= 19 && me.getLife() <= 24);
    }

    @Test
    public void testBothCardsLeaveTogetherAndLoneAugmentDies() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card kitten = addCard("Adorable Kitten", me);
        Card aug = addCardToZone("Half-Kitten, Half-", me, ZoneType.Hand);
        augment(me, aug, kitten);

        game.getAction().moveToHand(kitten, null);
        AssertJUnit.assertEquals(2, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Merged).isEmpty());

        Card lone = game.getAction().moveToPlay(game.getCardState(aug), me, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(lone).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testAiOnlyAugmentsItsOwnHosts() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Mer Man", opp);
        SpellAbility sa = augmentAbility(addCardToZone("Half-Kitten, Half-", me, ZoneType.Hand));
        sa.setActivatingPlayer(me);
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(sa).canPlayWithSubs(me, sa).willingToPlay());

        Card mine = addCard("Adorable Kitten", me);
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(sa).canPlayWithSubs(me, sa).willingToPlay());
        AssertJUnit.assertEquals(mine, sa.getTargets().getFirstTargetedCard());
    }
}
