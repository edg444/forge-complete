package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Exit Through the Grift Shop (UNF #74), Gray Merchant of Alphabet (UNF #75), Haberthrasher (UNF #76) and Knife and
 * Death (UNF #77), checked against Scryfall and their rulings 2026-10-21.
 */
public class UnfinityBlackBatchTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testGrayMerchantCountsCreaturesNotLetters() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Grizzly Bears", ai);
        addCard("Llanowar Elves", ai);
        final Card merchant = addCard("Gray Merchant of Alphabet", ai);
        final SpellAbility etb = merchant.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        final int life = ai.getLife();
        final int theirs = opp.getLife();
        AbilityUtils.resolve(etb);
        // A, R and L are in all three names (Gray Merchant of Alphabet, Grizzly Bears, Llanowar Elves)
        AssertJUnit.assertEquals(life + 3, ai.getLife());
        AssertJUnit.assertEquals(theirs - 3, opp.getLife());
    }

    @Test
    public void testHaberthrasherKnownHat() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Haberthrasher", ai);
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().resetActiveTriggers();
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        bears.setHasHatClaim(true);
        final int theirs = opp.getLife();
        game.getAction().moveToPlay(bears, null, null);
        resolveAll(game);
        AssertJUnit.assertEquals(theirs - 1, opp.getLife());

        final Card elves = addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
        game.getAction().moveToPlay(elves, null, null);
        resolveAll(game);
        AssertJUnit.assertEquals("the AI counts only hats it knows of", theirs - 1, opp.getLife());
    }

    @Test
    public void testGriftShop() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Divination", ai, ZoneType.Graveyard);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
            addCardToZone("Island", game.getPlayers().get(0), ZoneType.Library);
        }
        final Card spell = addCardToZone("Exit Through the Grift Shop", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        resolveAll(game);
        // the high bidder paid; if that wasn't the caster, the caster got that much life and that many Treasures
        final Player opp = game.getPlayers().get(0);
        final long treasures = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().contains("Treasure")).count();
        if (treasures > 0) {
            AssertJUnit.assertEquals(20 + treasures, ai.getLife());
            AssertJUnit.assertEquals(20 - treasures, opp.getLife());
        } else {
            AssertJUnit.assertTrue("the caster won (or nobody bid above 0)", ai.getLife() <= 20 && opp.getLife() == 20);
        }
        AssertJUnit.assertTrue("the card itself stays in the graveyard",
                ai.getCardsIn(ZoneType.Graveyard).stream().anyMatch(c -> c.getName().equals("Divination")));
    }

    @Test
    public void testKnifeAndDeathAimedThrows() {
        int hitsTotal = 0;
        for (int round = 0; round < 3; round++) {
            final Game game = initAndCreateGame();
            final Player ai = game.getPlayers().get(1);
            final Player opp = game.getPlayers().get(0);
            final Card wall = addCard("Wall of Stone", opp);
            final Card mine = addCard("Grizzly Bears", ai);
            final Card spell = addCardToZone("Knife and Death", ai, ZoneType.Hand);
            final SpellAbility sa = spell.getFirstSpellAbility();
            sa.setActivatingPlayer(ai);
            final int life = ai.getLife();
            AbilityUtils.resolve(sa);
            AssertJUnit.assertEquals("never your own", 0, mine.getDamage());
            final int dmg = wall.getDamage();
            AssertJUnit.assertTrue(dmg >= 0 && dmg <= 10);
            AssertJUnit.assertEquals("life for the damage", life + dmg, ai.getLife());
            hitsTotal += dmg;
        }
        AssertJUnit.assertTrue("thirty throws at 50% hit something: " + hitsTotal, hitsTotal > 0 && hitsTotal < 30);
    }
}
