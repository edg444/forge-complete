package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameAction;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Pithing Spyglass (CMB1 #108), checked against Scryfall and its 2 rulings 2026-10-15: a named keyword ability or
 * ability word is lost by every card in every zone - tokens aren't cards - along with the text an ability word
 * introduces.
 */
public class PithingSpyglassTest extends AITest {

    private Card spyglass(Game game, Player p, String word) {
        Card glass = addCard("Pithing Spyglass", p);
        glass.setChosenType(word);
        game.getAction().checkStateEffects(true);
        return glass;
    }

    @Test
    public void flyingGoesEverywhereButTokens() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card onField = addCard("Wind Drake", opp);
        Card inHand = addCardToZone("Wind Drake", opp, ZoneType.Hand);
        Card inYard = addCardToZone("Wind Drake", ai, ZoneType.Graveyard);
        Card inLibrary = addCardToZone("Wind Drake", ai, ZoneType.Library);
        Card bird = addToken("b_1_1_bat_flying", opp);
        AssertJUnit.assertTrue(onField.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(bird.hasKeyword(Keyword.FLYING));

        spyglass(game, ai, "Flying");
        AssertJUnit.assertFalse(onField.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertFalse(inHand.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertFalse(inYard.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertFalse(inLibrary.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue("a token isn't a card", bird.hasKeyword(Keyword.FLYING));
    }

    @Test
    public void flashIsNotFlashback() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card viper = addCardToZone("Ambush Viper", ai, ZoneType.Hand);
        Card twice = addCardToZone("Think Twice", ai, ZoneType.Graveyard);
        spyglass(game, ai, "Flash");
        AssertJUnit.assertFalse(viper.hasKeyword(Keyword.FLASH));
        AssertJUnit.assertTrue("only its own keyword", viper.hasKeyword(Keyword.DEATHTOUCH));
        AssertJUnit.assertTrue(twice.hasKeyword(Keyword.FLASHBACK));
    }

    @Test
    public void abilityWordTakesItsText() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card geopede = addCard("Plated Geopede", ai);
        AssertJUnit.assertEquals(1, geopede.getTriggers().size());
        Card glass = spyglass(game, ai, "Landfall");
        AssertJUnit.assertEquals("its landfall ability is gone", 0, geopede.getTriggers().size());
        AssertJUnit.assertTrue(geopede.hasKeyword(Keyword.FIRST_STRIKE));

        game.getAction().exile(glass, null, forge.game.ability.AbilityKey.newMap());
        game.getAction().checkStaticAbilities();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("back once the Spyglass is gone", 1, geopede.getTriggers().size());
    }

    @Test
    public void aiNamesWhatHurtsOpponents() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard("Wind Drake", opp);
        }
        addCard("Ambush Viper", ai);
        Card glass = addCardToZone("Pithing Spyglass", ai, ZoneType.Hand);
        SpellAbility sa = new SpellAbility.EmptySa(ApiType.ChooseType, glass, ai);
        List<String> words = GameAction.legacyWordChoices();
        AssertJUnit.assertTrue(words.contains("landfall"));
        AssertJUnit.assertEquals("Flying", ComputerUtil.chooseSomeType(ai, "KeywordOrAbilityWord", sa, words));
    }
}
