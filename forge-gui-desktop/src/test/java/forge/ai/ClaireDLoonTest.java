package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Claire D'Loon, Joy Sculptor (UNF #165), checked against Scryfall and its rulings 2026-10-21: your tokens go on existing
 * after they leave the battlefield, and can come back from your graveyard or hand.
 */
public class ClaireDLoonTest extends AITest {

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
    public void testTokensStayCards() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card claire = addCardToZone("Claire D'Loon, Joy Sculptor", ai, ZoneType.Hand);
        game.getAction().moveToPlay(claire, ai, null, AbilityKey.newMap());
        resolveAll(game);

        final Card soldier = addToken("w_1_1_soldier", ai);
        final Card theirs = addToken("w_1_1_soldier", opp);
        game.getAction().destroy(soldier, null, true, AbilityKey.newMap());
        game.getAction().destroy(theirs, null, true, AbilityKey.newMap());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("yours stays in the graveyard", 1,
                CardLists.filter(ai.getCardsIn(ZoneType.Graveyard), Card::isToken).size());
        AssertJUnit.assertEquals("an opponent's doesn't", 0,
                CardLists.filter(opp.getCardsIn(ZoneType.Graveyard), Card::isToken).size());

        final Card fromYard = CardLists.filter(ai.getCardsIn(ZoneType.Graveyard), Card::isToken).get(0);
        game.getAction().moveToHand(fromYard, null);
        AssertJUnit.assertEquals("and can move again", 1, CardLists.filter(ai.getCardsIn(ZoneType.Hand), Card::isToken).size());

        final Card inPlay = game.getCardState(claire);
        inPlay.setSickness(false);
        SpellAbility ability = null;
        for (final SpellAbility sa : inPlay.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                ability = sa;
            }
        }
        ability.setActivatingPlayer(ai);
        AbilityUtils.resolve(ability);
        AssertJUnit.assertEquals("back onto the battlefield", 1,
                CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && c.getName().contains("Soldier")).size());
    }
}
