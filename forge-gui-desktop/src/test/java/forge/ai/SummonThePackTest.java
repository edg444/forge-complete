package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Summon the Pack (Unstable): "Open a sealed Magic booster pack, reveal the cards, and put all creature cards
 * revealed this way onto the battlefield under your control. They're Zombies in addition to their other types." -
 * Oracle text, rulings and the Unstable FAQ entry checked 2026-10-01. The pack is a random Forge booster.
 */
public class SummonThePackTest extends AITest {

    @Test
    public void testAllCreaturesAsZombies() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        int creatures = 0;
        for (int i = 0; i < 10 && !game.isGameOver(); i++) {
            CardCollection before = new CardCollection(me.getCardsIn(ZoneType.Battlefield));
            SpellAbility sa = addCardToZone("Summon the Pack", me, ZoneType.Hand).getFirstSpellAbility();
            sa.setActivatingPlayer(me);
            AbilityUtils.resolve(sa);
            game.getAction().checkStateEffects(true);
            for (Card c : me.getCardsIn(ZoneType.Battlefield)) {
                if (before.contains(c)) {
                    continue;
                }
                creatures++;
                AssertJUnit.assertTrue(c.getName(), c.isCreature());
                AssertJUnit.assertTrue(c.getName(), c.getType().hasCreatureType("Zombie"));
                AssertJUnit.assertEquals(c.getName(), me, c.getOwner());
            }
        }
        // a pack with no creature card at all is possible, ten in a row isn't plausible
        AssertJUnit.assertTrue(creatures > 0);
    }

    @Test
    public void testBoosterTutorStillTakesOne() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        SpellAbility sa = addCardToZone("Booster Tutor", me, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        int hand = me.getCardsIn(ZoneType.Hand).size();
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(hand + 1, me.getCardsIn(ZoneType.Hand).size());
    }
}
