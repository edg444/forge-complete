package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardRulesPredicates;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Jester's Sombrero (Unglued): "{2}, {T}, Sacrifice this artifact: Look at target player's sideboard, choose three cards
 * from it, and remove those cards from the match." The AI cast it and activated it at players with no sideboard; it
 * now does neither, and generated decks without sideboards leave it out (AI:RemoveDeck:NoSideboard).
 */
public class JestersSombreroTest extends AITest {

    private static SpellAbility raid(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChooseCard) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testLeftOutOfDecksWithoutSideboards() {
        initAndCreateGame();
        forge.card.CardRules rules = FModel.getMagicDb().getCommonCards().getCard("Jester's Sombrero").getRules();
        AssertJUnit.assertTrue(rules.getAiHints().getRemNoSideboardDecks());
        AssertJUnit.assertFalse(rules.getAiHints().getRemRandomDecks());
        AssertJUnit.assertFalse(CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS.test(rules));
        // an ordinary card is unaffected
        AssertJUnit.assertTrue(CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS.test(
                FModel.getMagicDb().getCommonCards().getCard("Grizzly Bears").getRules()));
    }

    @Test
    public void testNoActivationAtAnEmptySideboard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card sombrero = addCard("Jester's Sombrero", ai);
        for (int i = 0; i < 2; i++) {
            addCard("Mountain", ai);
        }
        game.getAction().checkStateEffects(true);
        AiController aic = ((PlayerControllerAi) ai.getController()).getAi();

        SpellAbility sa = raid(sombrero);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, aic.canPlaySa(sa));

        addCardToZone("Lightning Bolt", opp, ZoneType.Sideboard);
        sa.resetTargets();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, aic.canPlaySa(sa));
        AssertJUnit.assertEquals(opp, sa.getTargets().getFirstTargetedPlayer());
    }

    @Test
    public void testNotCastWithoutASideboardToRaid() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 2; i++) {
            addCard("Mountain", ai);
        }
        Card sombrero = addCardToZone("Jester's Sombrero", ai, ZoneType.Hand);
        // the AI casts a permanent with nothing to do on entering in its second main phase
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        AiController aic = ((PlayerControllerAi) ai.getController()).getAi();

        SpellAbility cast = sombrero.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, aic.canPlaySa(cast));

        addCardToZone("Lightning Bolt", opp, ZoneType.Sideboard);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, aic.canPlaySa(cast));
    }
}
