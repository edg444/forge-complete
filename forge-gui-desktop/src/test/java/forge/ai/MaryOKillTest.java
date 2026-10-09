package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mary O'Kill (Unstable) - Oracle text, the three Scryfall rulings and the Unstable FAQ entry checked 2026-10-09.
 */
public class MaryOKillTest extends AITest {

    private void activate(final Card mary, final Player me) {
        SpellAbility sa = null;
        for (final SpellAbility s : mary.getSpellAbilities()) {
            if (s.getApi() == ApiType.Switch) {
                sa = s;
            }
        }
        AssertJUnit.assertNotNull(sa);
        sa.setActivatingPlayer(me);
        // activatable wherever Mary is, the hand included (rulings)
        AssertJUnit.assertTrue(sa.getRestrictions().canPlay(mary, sa));
        AbilityUtils.resolve(sa);
    }

    private Card cardNamed(final Player p, final ZoneType zone, final String name) {
        for (final Card c : p.getCardsIn(zone)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    @Test
    public void killbotPullsOffTheMask() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card bot = addCard("Curious Killbot", me);
        final Card aura = addCard("Holy Strength", me);
        aura.attachToEntity(bot, null, true);
        bot.setTapped(true);
        bot.addCounterInternal(CounterEnumType.P1P1, 1, me, false, null, null);
        bot.setDamage(1);
        final Card mary = addCardToZone("Mary O'Kill", me, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        // activated from the hand (rulings), switching Mary herself with the Killbot
        activate(mary, me);
        game.getAction().checkStateEffects(true);

        // the permanent is the same object in the same situation, but it's Mary now
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Battlefield).contains(bot));
        AssertJUnit.assertEquals("Mary O'Kill", bot.getName());
        AssertJUnit.assertTrue(bot.isTapped());
        AssertJUnit.assertEquals(1, bot.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, bot.getDamage());
        AssertJUnit.assertEquals(bot, aura.getAttachedTo());
        // 5/5, +1/+1 counter, Holy Strength +1/+2
        AssertJUnit.assertEquals(7, bot.getNetPower());
        AssertJUnit.assertEquals(8, bot.getNetToughness());
        // and the card in hand is the Killbot
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Hand).contains(mary));
        AssertJUnit.assertEquals("Curious Killbot", mary.getName());
        AssertJUnit.assertEquals(me, bot.getOwner());
    }

    @Test
    public void switchingWithAnOpponentsMary() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        final Card theirMary = addCard("Mary O'Kill", opp);
        final Card myBot = addCardToZone("Curious Killbot", me, ZoneType.Hand);
        final Card myMary = addCardToZone("Mary O'Kill", me, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        activate(myMary, me);
        game.getAction().checkStateEffects(true);

        // their permanent is now my Killbot, still under their control (user: any on the battlefield)
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Battlefield).contains(theirMary));
        AssertJUnit.assertEquals("Curious Killbot", theirMary.getName());
        AssertJUnit.assertEquals(me, theirMary.getOwner());
        AssertJUnit.assertEquals(opp, theirMary.getController());
        // their Mary is in my hand, still theirs, held by me
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Hand).contains(myBot));
        AssertJUnit.assertEquals("Mary O'Kill", myBot.getName());
        AssertJUnit.assertEquals(opp, myBot.getOwner());
        AssertJUnit.assertEquals(me, myBot.getController());
        AssertJUnit.assertNotNull(cardNamed(me, ZoneType.Hand, "Mary O'Kill"));
    }

    @Test
    public void changelingsAreKillbots() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card changeling = addCard("Avian Changeling", me);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(changeling.isValid("Permanent.Killbot", me, null, null));
    }
}
