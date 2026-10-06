package forge.ai;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Hot Fix (Unstable) - Oracle text, the three Scryfall rulings and the Unstable FAQ entry checked 2026-10-06.
 */
public class HotFixTest extends AITest {

    private void cast(Player me) {
        Card hotFix = addCardToZone("Hot Fix", me, ZoneType.Hand);
        SpellAbility sa = hotFix.getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        AbilityUtils.resolve(sa);
    }

    private void addLibrary(Player me) {
        for (int i = 0; i < 6; i++) {
            addCardToZone("Shivan Dragon", me, ZoneType.Library);
        }
        addCardToZone("Lightning Bolt", me, ZoneType.Library);
        addCardToZone("Mountain", me, ZoneType.Library);
    }

    @Test
    public void testAiPullsWhatItWantsToTheTopAndKeepsTheRest() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        addLibrary(me);
        List<Card> before = Lists.newArrayList(me.getCardsIn(ZoneType.Library));
        cast(me);
        CardCollectionView after = me.getCardsIn(ZoneType.Library);
        // contents never change (ruling)
        AssertJUnit.assertEquals(before.size(), after.size());
        AssertJUnit.assertTrue(after.containsAll(before));
        // no lands out: a land first, then the spell it can cast
        AssertJUnit.assertEquals("Mountain", after.get(0).getName());
        AssertJUnit.assertEquals("Lightning Bolt", after.get(1).getName());
    }

    @Test
    public void testStillTouchingWhenTimeRunsOutShuffles() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        for (int i = 0; i < 40; i++) {
            addCardToZone(i % 2 == 0 ? "Mountain" : "Island", me, ZoneType.Library);
        }
        final int[] asked = {0};
        me.dangerouslySetController(new PlayerControllerAi(game, me, me.getLobbyPlayer()) {
            @Override
            public Pair<CardCollectionView, Boolean> rearrangeInTime(CardCollectionView cards, int seconds, SpellAbility sa) {
                asked[0] = seconds;
                // all Islands on top, but a hand is still on the library at the buzzer
                CardCollection order = new CardCollection();
                cards.forEach(c -> { if (c.getName().equals("Island")) order.add(c); });
                cards.forEach(c -> { if (!c.getName().equals("Island")) order.add(c); });
                return Pair.of(order, true);
            }
        });
        cast(me);
        AssertJUnit.assertEquals(10, asked[0]);
        CardCollectionView after = me.getCardsIn(ZoneType.Library);
        AssertJUnit.assertEquals(40, after.size());
        // shuffled: 20 Islands in a row on top would be a 1 in 137 billion shuffle
        boolean allIslandsOnTop = true;
        for (int i = 0; i < 20; i++) {
            allIslandsOnTop &= after.get(i).getName().equals("Island");
        }
        AssertJUnit.assertFalse(allIslandsOnTop);
        AssertJUnit.assertTrue(game.getGameLog().getLogEntries(null).stream()
                .anyMatch(e -> e.message().contains("still touching")));
    }

    @Test
    public void testLettingGoInTimeKeepsTheOrder() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        addLibrary(me);
        me.dangerouslySetController(new PlayerControllerAi(game, me, me.getLobbyPlayer()) {
            @Override
            public Pair<CardCollectionView, Boolean> rearrangeInTime(CardCollectionView cards, int seconds, SpellAbility sa) {
                CardCollection order = new CardCollection(cards);
                order.add(order.remove(0));
                return Pair.of(order, false);
            }
        });
        Card firstBefore = me.getCardsIn(ZoneType.Library).get(0);
        cast(me);
        CardCollectionView after = me.getCardsIn(ZoneType.Library);
        AssertJUnit.assertEquals(firstBefore, after.get(after.size() - 1));
    }
}
