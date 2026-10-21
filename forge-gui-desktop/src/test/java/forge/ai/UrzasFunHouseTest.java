package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Urza's Fun House (UNF #199), checked against Scryfall and its rulings 2026-10-21: {∞} only with all three Urza lands;
 * the Fun House button picks one of Urza, Academy Headmaster's outcome lists at 75/15/10, then an outcome.
 */
public class UrzasFunHouseTest extends AITest {

    private SpellAbility ability(final Card c, final int index, final Player p) {
        final SpellAbility sa = c.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).toList().get(index);
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void testFunHouse() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
            addCardToZone("Island", opp, ZoneType.Library);
        }
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", opp);
        final Card fun = addCard("Urza's Fun House", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, fun.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).count());
        AssertJUnit.assertFalse("no {∞} without the Urza lands", ability(fun, 1, ai).canPlay());
        addCard("Urza's Mine", ai);
        addCard("Urza's Power Plant", ai);
        addCard("Urza's Tower", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("with Mine, Power-Plant and Tower", ability(fun, 1, ai).canPlay());

        final SpellAbility button = ability(fun, 2, ai);
        AssertJUnit.assertEquals("15 Common, 3 Uncommon and 2 Rare entries", 20,
                button.getAdditionalAbilityList("Choices").size());
        for (int i = 0; i < 10; i++) {
            AbilityUtils.resolve(button);
            while (!game.getStack().isEmpty()) {
                game.getStack().resolveStack();
            }
            game.getAction().checkStateEffects(true);
        }
    }
}
