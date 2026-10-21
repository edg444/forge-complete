package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * How Is This a Par Three?! (UNF #49), checked against Scryfall and its ruling 2026-10-21: mill X, the words in the
 * spell's name, unless that's more than the player's library holds - then milling isn't an option.
 */
public class ParThreeTest extends AITest {

    private SpellAbility mill(final Card par, final Player p, final Card spell, final Player target) {
        final SpellAbility sa = par.getTriggers().get(0).ensureAbility();
        sa.setActivatingPlayer(p);
        sa.setTriggeringObject(AbilityKey.Card, spell);
        sa.resetTargets();
        sa.getTargets().add(target);
        return sa;
    }

    @Test
    public void testMillsTheWordsInTheName() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", opp, ZoneType.Library);
        }
        final Card par = addCard("How Is This a Par Three?!", p);
        final Card bolt = addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        AbilityUtils.resolve(mill(par, p, bolt, opp));
        AssertJUnit.assertEquals(3, opp.getCardsIn(ZoneType.Library).size());

        final Card form = addCardToZone("Form of the Approach of the Second Sun", p, ZoneType.Hand);
        AbilityUtils.resolve(mill(par, p, form, opp));
        AssertJUnit.assertEquals("eight is more than three, so it can't be chosen", 3, opp.getCardsIn(ZoneType.Library).size());
    }
}
