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
 * Zombified (Unstable): "{4}{B}: Combine this card from your graveyard with target host. / {2}{B}, Exile a creature
 * card from your graveyard: / Augment {4}{B}" - Oracle text and rulings checked against Scryfall 2026-10-01.
 */
public class ZombifiedTest extends AITest {

    private static SpellAbility fromGraveyard(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Augment && "Graveyard".equals(sa.getParam("FromZone"))) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card z = addCardToZone("Zombified", game.getPlayers().get(1), ZoneType.Hand);
        AssertJUnit.assertEquals("{4}{B}: Combine this card from your graveyard with target host. | {2}{B}, Exile a "
                + "creature card from your graveyard: | Augment {4}{B} ({4}{B}, Reveal this card from your hand: "
                + "Combine it with target host. Augment only as a sorcery.)",
                String.join(" | ", z.getAbilityText().trim().split("[\\r\\n]+")));
    }

    @Test
    public void testCombineFromTheGraveyard() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        for (int i = 0; i < 8; i++) {
            addCard("Swamp", me);
        }
        Card kitten = addCard("Adorable Kitten", me);
        Card zombified = addCardToZone("Zombified", me, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", me, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = fromGraveyard(zombified);
        sa.setActivatingPlayer(me);
        AssertJUnit.assertTrue(sa.canPlay());
        sa.getTargets().add(kitten);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals("Zombified Kitten", kitten.getName());
        AssertJUnit.assertEquals(3, kitten.getNetPower());
        AssertJUnit.assertEquals(3, kitten.getNetToughness());
        AssertJUnit.assertTrue(zombified.isInZone(ZoneType.Merged));

        // the condition now has the Kitten's effect, for {2}{B} and a creature card exiled from the graveyard
        SpellAbility made = null;
        for (SpellAbility a : kitten.getSpellAbilities()) {
            if (a.getApi() == ApiType.RollDice) {
                made = a;
            }
        }
        AssertJUnit.assertNotNull(made);
        AssertJUnit.assertEquals("{2}{B}, Exile a creature card from your graveyard: Roll a six-sided die. You gain "
                + "life equal to the result.", made.getDescription().trim());
        made.setActivatingPlayer(me);
        AssertJUnit.assertTrue(made.canPlay());
        int life = me.getLife();
        AbilityUtils.resolve(made);
        AssertJUnit.assertTrue(me.getLife() > life && me.getLife() <= life + 6);
    }
}
