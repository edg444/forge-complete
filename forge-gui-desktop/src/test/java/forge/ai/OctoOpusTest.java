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
 * Octo Opus (UNF #54), checked against Scryfall and its 7 rulings 2026-10-21: Contortionist folds 4/4, 2/2, 1/1, 1/2,
 * stopping there (user, 2026-10-21); sacrificing Octo Opus draws one plus one per fold of a Contortionist you choose.
 */
public class OctoOpusTest extends AITest {

    private SpellAbility fold(final Card contortionist, final Player p) {
        final SpellAbility sa = contortionist.getTriggers().get(0).ensureAbility();
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void testFoldsAndDraws() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
        final Card octo = addCard("Octo Opus", p);
        final SpellAbility etb = octo.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(p);
        AbilityUtils.resolve(etb);
        game.getAction().checkStateEffects(true);
        final Card contortionist = p.getCreaturesInPlay().stream().filter(c -> c.getName().equals("Contortionist")).findFirst().get();
        AssertJUnit.assertEquals(4, contortionist.getNetPower());

        AbilityUtils.resolve(fold(contortionist, p));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, contortionist.getNetToughness());
        AbilityUtils.resolve(fold(contortionist, p));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, contortionist.getNetToughness());
        AbilityUtils.resolve(fold(contortionist, p));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("half a power", 0, contortionist.getNetPower());
        AssertJUnit.assertTrue(contortionist.isInPlay());
        AssertJUnit.assertFalse("no more folds than half values allow",
                contortionist.getTriggers().get(0).requirementsCheck(game));

        SpellAbility sac = null;
        for (final SpellAbility sa : octo.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                sac = sa;
            }
        }
        sac.setActivatingPlayer(p);
        final int hand = p.getCardsIn(ZoneType.Hand).size();
        AbilityUtils.resolve(sac);
        AssertJUnit.assertEquals("one and three folds", hand + 4, p.getCardsIn(ZoneType.Hand).size());
    }
}
