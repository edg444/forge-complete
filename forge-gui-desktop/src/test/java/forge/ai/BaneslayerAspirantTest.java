package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Baneslayer Aspirant (Mystery Booster playtest card): "gets +3/+3 and has flying, first strike, and lifelink as long
 * as you have one or more emblems." - Oracle text checked against Scryfall 2026-10-10 (no rulings).
 */
public class BaneslayerAspirantTest extends AITest {

    private void giveEmblem(Player p) {
        Card elspeth = addCard("Elspeth, Knight-Errant", p);
        for (SpellAbility sa : elspeth.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Effect) {
                sa.setActivatingPlayer(p);
                AbilityUtils.resolve(sa);
            }
        }
        p.getGame().getAction().moveToGraveyard(elspeth, null, null);
        p.getGame().getAction().checkStateEffects(true);
    }

    @Test
    public void testOnlyYourEmblemsCount() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card aspirant = addCard("Baneslayer Aspirant", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, aspirant.getNetPower());
        AssertJUnit.assertFalse(aspirant.hasKeyword(Keyword.FLYING));

        giveEmblem(opp);
        AssertJUnit.assertEquals(2, aspirant.getNetPower());

        giveEmblem(p);
        AssertJUnit.assertEquals(5, aspirant.getNetPower());
        AssertJUnit.assertEquals(5, aspirant.getNetToughness());
        AssertJUnit.assertTrue(aspirant.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(aspirant.hasKeyword(Keyword.FIRST_STRIKE));
        AssertJUnit.assertTrue(aspirant.hasKeyword(Keyword.LIFELINK));
    }
}
