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
 * Unknown Event (UNK) cards, checked against Scryfall and their rulings 2026-10-21.
 */
public class UnknownEventBatch1Test extends AITest {

    @Test
    public void testContainerOfBoosterPacks() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card container = addCard("A Container of Booster Packs", ai);
        final SpellAbility sa = container.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility)
                .findFirst().orElseThrow();
        sa.setActivatingPlayer(ai);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("one card from the opened pack", hand + 1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue("the rest stays out of the game", ai.getCardsIn(ZoneType.Exile).isEmpty());
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Graveyard).isEmpty());
    }
}
