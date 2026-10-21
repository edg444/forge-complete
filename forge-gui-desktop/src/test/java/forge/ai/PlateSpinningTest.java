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
 * Plate Spinning (UNF #56), checked against Scryfall and its 3 rulings 2026-10-21: a human reports a fallen card (honor
 * system); the AI's cards wobble, 15% for each card it's balancing each time it balances another (user, 2026-10-21).
 */
public class PlateSpinningTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private SpellAbility fellAction(final Card plate, final Player p) {
        for (final SpellAbility sa : plate.getSpellAbilities()) {
            if (sa.hasParam("SpecialAction")) {
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        throw new AssertionError();
    }

    @Test
    public void testAFallenCardSacrificesIt() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card plate = addCard("Plate Spinning", p);
        AssertJUnit.assertFalse("nothing balanced yet", fellAction(plate, p).canPlay());
        plate.setSVar("Balanced", "Number$1");
        AssertJUnit.assertTrue(fellAction(plate, p).canPlay());
        AbilityUtils.resolve(fellAction(plate, p));
        resolveAll(game);
        AssertJUnit.assertTrue(game.getCardState(plate).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testTheAisCardsWobble() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card plate = addCard("Plate Spinning", ai);
        plate.setSVar("Balanced", "Number$2");
        final SpellAbility wobble = plate.getTriggers().get(1).ensureAbility();
        wobble.setActivatingPlayer(ai);

        plate.setSVar("D100", "Number$46");
        AbilityUtils.resolve(wobble);
        resolveAll(game);
        AssertJUnit.assertTrue("46 is over 45 (three cards at 15%)", plate.isInPlay());

        plate.setSVar("D100", "Number$45");
        AbilityUtils.resolve(wobble);
        resolveAll(game);
        AssertJUnit.assertTrue(game.getCardState(plate).isInZone(ZoneType.Graveyard));
    }
}
