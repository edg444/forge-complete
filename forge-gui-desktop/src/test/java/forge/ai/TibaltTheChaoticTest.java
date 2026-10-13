package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Tibalt the Chaotic (CMB1 #66, upstream), checked against Scryfall and its 5 rulings 2026-10-13: the random copy is
 * cast, without paying its mana cost, as the ability resolves - for the AI too, Ignorant Bliss included.
 */
public class TibaltTheChaoticTest extends AITest {

    @Test
    public void thePlusOneAlwaysCastsACopy() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card tibalt = addCard("Tibalt the Chaotic", ai);
        fillLibrary(ai, 40);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        SpellAbility plus = tibalt.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
        for (int i = 1; i <= 12; i++) {
            plus.setActivatingPlayer(ai);
            AbilityUtils.resolve(plus);
            playUntilStackClear(game);
            AssertJUnit.assertEquals("cast every time", i, ai.getSpellsCastThisTurn());
        }
    }
}
