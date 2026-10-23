package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.PlanarDice;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Planechase (2009) R&D's Secret Lair printed text (2026-10-23). */
public class PlanechaseAuditTest extends AITest {

    private static void resolveAll(Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    /** Returns Goats made by {rolled chaos, chaos ensuing from an effect}. */
    private int[] goldmeadowGoats(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card plane = Card.fromPaperCard(FModel.getMagicDb().getVariantCards().getCard("Goldmeadow", "OHOP"), p);
        p.getZone(ZoneType.PlanarDeck).add(plane);
        p.planeswalkTo(null, new CardCollection(plane));
        resolveAll(game);

        PlanarDice.roll(p, PlanarDice.Chaos);
        resolveAll(game);
        int rolled = goats(p);

        Card source = addCard("Grizzly Bears", p);
        SpellAbility sa = AbilityFactory.getAbility("DB$ ChaosEnsues", source);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        resolveAll(game);
        return new int[] { rolled, goats(p) - rolled };
    }

    private static int goats(final Player p) {
        return CardLists.filter(p.getCardsIn(ZoneType.Battlefield), c -> c.getName().contains("Goat")).size();
    }

    @Test
    public void testChaosEnsuesTriggersEitherWay() {
        int[] g = goldmeadowGoats(false);
        AssertJUnit.assertEquals(1, g[0]);
        AssertJUnit.assertEquals(1, g[1]);
    }

    @Test
    public void testPrintedPlaneTriggersOnlyOnChaosRoll() {
        int[] g = goldmeadowGoats(true);
        AssertJUnit.assertEquals(1, g[0]);
        AssertJUnit.assertEquals(0, g[1]);
    }
}
