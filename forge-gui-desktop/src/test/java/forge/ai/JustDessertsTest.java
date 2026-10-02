package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Just Desserts (Unstable): "Just Desserts deals π damage to target creature." - Oracle text, the two rulings and the
 * Unstable FAQ entry checked 2026-10-02: one destroys toughness 3, two 6, four 12 1/2, eight 25; redirected to a
 * player, use 3.14 (20 life becomes 16.86).
 */
public class JustDessertsTest extends AITest {

    private void cast(Player caster, Card target) {
        Card desserts = addCardToZone("Just Desserts", caster, ZoneType.Hand);
        SpellAbility sa = desserts.getFirstSpellAbility();
        sa.setActivatingPlayer(caster);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
        caster.getGame().getAction().checkStateEffects(true);
    }

    private Card withToughness(Player p, int toughness, boolean half) {
        Card c = addCard("Colossal Dreadmaw", p);
        c.setBaseToughness(toughness);
        c.setHalfToughness(half);
        p.getGame().getAction().checkStateEffects(true);
        return c;
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card desserts = addCardToZone("Just Desserts", game.getPlayers().get(1), ZoneType.Hand);
        AssertJUnit.assertEquals("Just Desserts deals π damage to target creature. (π is the ratio of a circle's "
                + "circumference to its diameter. (It's a smidgen more than 3.))", desserts.getAbilityText().trim());
    }

    @Test
    public void testOneDestroysToughnessThree() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card giant = addCard("Hill Giant", opp);
        Card wurm = addCard("Craw Wurm", opp);
        cast(me, giant);
        cast(me, wurm);

        AssertJUnit.assertTrue(game.getCardState(giant).isInZone(ZoneType.Graveyard));
        // Craw Wurm is 6/4: π doesn't reach 4
        AssertJUnit.assertTrue(wurm.isInPlay());
        AssertJUnit.assertEquals("3.14", CardView.get(wurm).getDamageString());
    }

    @Test
    public void testTwoDestroySixButNotSixAndAHalf() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card six = withToughness(opp, 6, false);
        Card sixAndAHalf = withToughness(opp, 6, true);
        cast(me, six);
        cast(me, sixAndAHalf);
        AssertJUnit.assertTrue(six.isInPlay());
        cast(me, six);
        cast(me, sixAndAHalf);

        AssertJUnit.assertTrue(game.getCardState(six).isInZone(ZoneType.Graveyard));
        // 2π = 6.28 < 6 1/2
        AssertJUnit.assertTrue(sixAndAHalf.isInPlay());
        AssertJUnit.assertEquals("6.28", CardView.get(sixAndAHalf).getDamageString());
    }

    @Test
    public void testFourDestroyTwelveAndAHalfAndEightDestroyTwentyFive() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card twelveAndAHalf = withToughness(opp, 12, true);
        Card thirteen = withToughness(opp, 13, false);
        Card twentyFive = withToughness(opp, 25, false);
        for (int i = 0; i < 4; i++) {
            cast(me, twelveAndAHalf);
            cast(me, thirteen);
        }
        // 4π = 12.57
        AssertJUnit.assertTrue(game.getCardState(twelveAndAHalf).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(thirteen.isInPlay());
        AssertJUnit.assertEquals("12.57", CardView.get(thirteen).getDamageString());

        for (int i = 0; i < 7; i++) {
            cast(me, twentyFive);
        }
        AssertJUnit.assertTrue(twentyFive.isInPlay());
        cast(me, twentyFive);
        // 8π = 25.13
        AssertJUnit.assertTrue(game.getCardState(twentyFive).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testDamageWearsOff() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card wurm = addCard("Craw Wurm", game.getPlayers().get(0));
        cast(me, wurm);
        wurm.setDamage(0);
        AssertJUnit.assertEquals(0, wurm.getPiDamage());
        AssertJUnit.assertEquals("0", CardView.get(wurm).getDamageString());
    }

    @Test
    public void testAtAPlayerItsThreePointOneFour() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // the π damage as it would arrive redirected to a player
        Card desserts = addCardToZone("Just Desserts", me, ZoneType.Hand);
        SpellAbility atPlayer = AbilityFactory.getAbility("DB$ DealDamage | Defined$ Opponent | NumDmg$ 3 | Pi$ True", desserts);
        atPlayer.setActivatingPlayer(me);

        AbilityUtils.resolve(atPlayer);
        AssertJUnit.assertEquals(16, opp.getLife());
        AssertJUnit.assertEquals(86, opp.getLifeHundredths());
        AssertJUnit.assertEquals("16.86", PlayerView.get(opp).getLifeString());

        AbilityUtils.resolve(atPlayer);
        AssertJUnit.assertEquals("13.72", PlayerView.get(opp).getLifeString());
    }

    @Test
    public void testHalvesAndHundredthsAddUp() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        opp.changeLifeByHalves(-1, null, null);
        AssertJUnit.assertEquals("19½", PlayerView.get(opp).getLifeString());

        Card desserts = addCardToZone("Just Desserts", me, ZoneType.Hand);
        SpellAbility atPlayer = AbilityFactory.getAbility("DB$ DealDamage | Defined$ Opponent | NumDmg$ 3 | Pi$ True", desserts);
        atPlayer.setActivatingPlayer(me);
        AbilityUtils.resolve(atPlayer);
        // 19.5 - 3.14
        AssertJUnit.assertEquals("16.36", PlayerView.get(opp).getLifeString());

        // a player just above zero hasn't lost, and just below has
        opp.setLife(3, null);
        AbilityUtils.resolve(atPlayer);
        AssertJUnit.assertEquals("-0.14", PlayerView.get(opp).getLifeString());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(opp.hasLost());
    }
}
