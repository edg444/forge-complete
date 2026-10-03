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
 * Grusilda, Monster Masher (Unstable) - Oracle text, the four Scryfall rulings and the Unstable FAQ entry checked
 * 2026-10-03.
 */
public class GrusildaTest extends AITest {

    private static SpellAbility combine(Card grusilda) {
        for (SpellAbility sa : grusilda.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Combine) {
                return sa;
            }
        }
        return null;
    }

    private Card mash(Game game, Player me, Card a, Card b) {
        Card grusilda = addCard("Grusilda, Monster Masher", me);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = combine(grusilda);
        sa.setActivatingPlayer(me);
        sa.resetTargets();
        sa.getTargets().add(a);
        sa.getTargets().add(b);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        playUntilStackClear(game);
        return grusilda;
    }

    @Test
    public void testTwoCreaturesBecomeOne() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCardToZone("Grizzly Bears", me, ZoneType.Graveyard);
        Card dragon = addCardToZone("Shivan Dragon", opp, ZoneType.Graveyard);
        mash(game, me, bears, dragon);
        Card combined = game.getCardState(bears).isInZone(ZoneType.Battlefield) ? game.getCardState(bears) : game.getCardState(dragon);
        AssertJUnit.assertTrue(combined.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(me, combined.getController());
        // 2/2 and 5/5
        AssertJUnit.assertEquals(7, combined.getNetPower());
        AssertJUnit.assertEquals(7, combined.getNetToughness());
        AssertJUnit.assertTrue(combined.sharesNameWith("Grizzly Bears"));
        AssertJUnit.assertTrue(combined.sharesNameWith("Shivan Dragon"));
        // {1}{G} and {4}{R}{R}
        AssertJUnit.assertEquals(8, combined.getCMC());
        AssertJUnit.assertTrue(combined.getColor().hasGreen() && combined.getColor().hasRed());
        AssertJUnit.assertTrue(combined.getType().hasCreatureType("Bear"));
        AssertJUnit.assertTrue(combined.getType().hasCreatureType("Dragon"));
        AssertJUnit.assertTrue(combined.hasKeyword(Keyword.FLYING));
        // Grusilda: combined creatures you control have menace
        AssertJUnit.assertTrue(combined.hasKeyword(Keyword.MENACE));

        // it leaves as one, each card to its owner's graveyard
        game.getAction().destroy(combined, null, true, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Graveyard).anyMatch(c -> c.getName().equals("Grizzly Bears")));
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Graveyard).anyMatch(c -> c.getName().equals("Shivan Dragon")));
    }

    @Test
    public void testHostAndAugmentCombineTheAugmentWay() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card kitten = addCardToZone("Adorable Kitten", me, ZoneType.Graveyard);
        Card aug = addCardToZone("Half-Kitten, Half-", me, ZoneType.Graveyard);
        mash(game, me, aug, kitten);
        Card combined = game.getCardState(kitten);
        AssertJUnit.assertTrue(combined.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals("Half-Kitten, Half-Kitten", combined.getName());
    }

    @Test
    public void testAugmentWithANonHostGoesToTheGraveyard() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card bears = addCardToZone("Grizzly Bears", me, ZoneType.Graveyard);
        Card aug = addCardToZone("Half-Kitten, Half-", me, ZoneType.Graveyard);
        mash(game, me, bears, aug);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(game.getCardState(aug).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testTwoHostsBothTriggerAndCanStillBeAugmented() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card kitten = addCardToZone("Adorable Kitten", me, ZoneType.Graveyard);
        Card kangaroo = addCardToZone("Mother Kangaroo", me, ZoneType.Graveyard);
        int life = me.getLife();
        mash(game, me, kitten, kangaroo);
        Card combined = game.getCardState(kitten).isInZone(ZoneType.Battlefield) ? game.getCardState(kitten) : game.getCardState(kangaroo);
        // Adorable Kitten gains life, Mother Kangaroo gets +1/+1 counters: both entered
        AssertJUnit.assertTrue(me.getLife() > life);
        AssertJUnit.assertTrue(combined.getCounters(CounterEnumType.P1P1) > 0);
        AssertJUnit.assertTrue(combined.getType().hasSupertype(CardType.Supertype.Host));
        Card aug = addCardToZone("Half-Kitten, Half-", me, ZoneType.Hand);
        SpellAbility augment = null;
        for (SpellAbility sa : aug.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Augment) {
                augment = sa;
            }
        }
        augment.setActivatingPlayer(me);
        AssertJUnit.assertTrue(augment.canTarget(combined));
    }
}
