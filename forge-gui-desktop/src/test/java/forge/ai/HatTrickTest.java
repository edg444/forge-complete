package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Hat Trick (UNF #13), checked against Scryfall and its 10 rulings 2026-10-21. A hat Forge knows of counts; otherwise
 * the caster is asked, and "you have a hat" is honor system. The AI can't see the art and has no head.
 */
public class HatTrickTest extends AITest {

    private Card castOn(final Game game, final Player p, final Card target) {
        final Card trick = addCardToZone("Hat Trick", p, forge.game.zone.ZoneType.Hand);
        final SpellAbility sa = trick.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
        return trick;
    }

    @Test
    public void testAKnownHatGivesDoubleStrikeButTheAiHasNoHat() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        bears.setHatInArtClaim(true);
        final Card trick = castOn(game, p, bears);
        AssertJUnit.assertEquals(3, bears.getNetPower());
        AssertJUnit.assertEquals(3, bears.getNetToughness());
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FIRST_STRIKE));
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.DOUBLE_STRIKE));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.TRIPLE_STRIKE));
        AssertJUnit.assertEquals(0, trick.getRememberedCount());
    }

    @Test
    public void testNoKnownHatAsksAndTheAiSaysNo() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        castOn(game, p, bears);
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FIRST_STRIKE));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.DOUBLE_STRIKE));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.TRIPLE_STRIKE));
    }

    @Test
    public void testYouHavingAHatGivesTripleStrike() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", p);
        bears.setHatInArtClaim(true);
        final Card trick = addCardToZone("Hat Trick", p, forge.game.zone.ZoneType.Hand);
        final SpellAbility sa = trick.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(bears);
        // stand in for a human who says yes to both questions
        SpellAbility yourHatAsk = sa;
        while (!yourHatAsk.getParamOrDefault("ChoicePrompt", "").startsWith("Do you have")) {
            yourHatAsk = yourHatAsk.getSubAbility();
        }
        for (final SpellAbility choice : yourHatAsk.getAdditionalAbilityList("Choices")) {
            if (choice.hasParam("KW")) {
                AbilityUtils.resolve(choice);
            }
        }
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.TRIPLE_STRIKE));
    }
}
