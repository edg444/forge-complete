package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Acornelia, Fashionable Filcher (UND #31), checked against Scryfall and its 2 rulings 2026-10-20.
 */
public class AcorneliaTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner) {
        return Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private static int acorns(final Player p) {
        return p.getCounters(CounterEnumType.ACORN);
    }

    private static void cast(final Player p, final Card c) {
        p.getZone(ZoneType.Hand).add(c);
        p.getGame().getAction().checkStateEffects(true);
        SpellAbility spell = c.getFirstSpellAbility();
        spell.setActivatingPlayer(p);
        if (spell.usesTargeting()) {
            spell.getTargets().add(p.getWeakestOpponent());
        }
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, spell, null));
    }

    @Test
    public void squirrelArtAndSquirrelsEntering() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCards("Forest", 2, ai);
        addCards("Mountain", 1, ai);
        addCard("Acornelia, Fashionable Filcher", ai);
        game.getAction().checkStateEffects(true);

        // ruling: a Squirrel spell that depicts a squirrel gets two - one cast, one entering
        Card chatter = printing("Chatter of the Squirrel", "2XM", ai);
        AssertJUnit.assertTrue(chatter.hasKnownSquirrelInArt());
        cast(ai, chatter);
        resolveAll(game);
        AssertJUnit.assertEquals(2, acorns(ai));

        Card bolt = printing("Lightning Bolt", "M10", ai);
        AssertJUnit.assertFalse(bolt.hasKnownSquirrelInArt());
        AssertJUnit.assertFalse(bolt.isArtUntagged());
        cast(ai, bolt);
        resolveAll(game);
        AssertJUnit.assertEquals(2, acorns(ai));

        Card squirrel = ai.getCreaturesInPlay().filter(c -> c.getType().hasCreatureType("Squirrel") && c.isToken()).getFirst();
        game.getAction().destroy(squirrel, null, false, null);
        resolveAll(game);
        AssertJUnit.assertEquals("a Squirrel dying", 3, acorns(ai));
    }

    @Test
    public void spendAcornsForMinusX() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCards("Swamp", 3, ai);
        Card acornelia = addCard("Acornelia, Fashionable Filcher", ai);
        Card bears = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);
        ai.addCounterInternal(CounterEnumType.ACORN, 2, ai, false, null, null);

        SpellAbility minus = acornelia.getSpellAbilities().stream()
                .filter(s -> s.getApi() == ApiType.Pump && "-X".equals(s.getParam("NumAtt"))).findFirst().orElseThrow();
        minus.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(minus));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, minus, null));
        resolveAll(game);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(0, acorns(ai));
    }
}
