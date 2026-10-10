package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Steam-Powered (Unstable): "{5}: / Augment {4}" - Oracle text and the augment rulings checked against Scryfall
 * 2026-10-09. Adorable Kitten is the host: "When this creature enters, roll a six-sided die. You gain life equal to
 * the result."
 */
public class SteamPoweredTest extends AITest {

    @Test
    public void hostEffectForFive() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card kitten = addCard("Adorable Kitten", me);
        final Card steam = addCardToZone("Steam-Powered", me, ZoneType.Hand);
        for (final SpellAbility sa : steam.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Augment) {
                sa.setActivatingPlayer(me);
                sa.getTargets().add(kitten);
                AbilityUtils.resolve(sa);
            }
        }
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals("Steam-Powered Kitten", kitten.getName());
        AssertJUnit.assertEquals(1, kitten.getNetPower());
        AssertJUnit.assertEquals(5, kitten.getNetToughness());
        AssertJUnit.assertTrue(kitten.isArtifact());
        AssertJUnit.assertTrue(kitten.getType().hasCreatureType("Construct"));
        AssertJUnit.assertTrue(kitten.getOracleText().contains("{5}: Roll a six-sided die. You gain life equal to the result."));

        SpellAbility pay5 = null;
        for (final SpellAbility sa : kitten.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getPayCosts() != null && sa.getPayCosts().getTotalMana().getCMC() == 5) {
                pay5 = sa;
            }
        }
        AssertJUnit.assertNotNull(pay5);
        pay5.setActivatingPlayer(me);
        AbilityUtils.resolve(pay5);
        AssertJUnit.assertTrue("life " + me.getLife(), me.getLife() >= 21 && me.getLife() <= 26);
    }
}
