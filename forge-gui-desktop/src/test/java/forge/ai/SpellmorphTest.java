package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Spellmorph (Spellmorph Raise Dead, CMB1 #46), checked against Scryfall and its 12 rulings 2026-10-13: cast face down
 * as a 2/2 for {3}, then cast the face-down permanent as its own spell any time for its spellmorph cost. It can't be
 * turned face up, and once it has lost its abilities it can't be cast.
 */
public class SpellmorphTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    private static SpellAbility find(Card c, Player p, boolean faceDownCast) {
        for (SpellAbility sa : c.getAllPossibleAbilities(p, true)) {
            if (faceDownCast ? sa.isCastFaceDown() : sa.hasParam("Spellmorph")) {
                return sa;
            }
        }
        return null;
    }

    private Card castFaceDown(Game game, Player ai) {
        Card card = addCardToZone("Spellmorph Raise Dead", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        SpellAbility down = find(card, ai, true);
        AssertJUnit.assertNotNull(down);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, down, null));
        resolveAll(game);
        Card morph = game.getCardState(card);
        AssertJUnit.assertTrue(morph.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(morph.isFaceDown());
        AssertJUnit.assertEquals(2, morph.getNetPower());
        return morph;
    }

    @Test
    public void castFromTheBattlefieldAtInstantSpeed() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Swamp", 4, ai);
        Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        Card morph = castFaceDown(game, ai);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_END, opp);
        for (Card land : ai.getCardsIn(ZoneType.Battlefield)) {
            land.setTapped(false);
        }
        game.getAction().checkStateEffects(true);
        SpellAbility up = find(morph, ai, false);
        AssertJUnit.assertNotNull("castable from the battlefield on an opponent's turn", up);
        up.resetTargets();
        up.getTargets().add(bears);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, up, null));
        Card onStack = game.getCardState(morph);
        AssertJUnit.assertTrue(onStack.isInZone(ZoneType.Stack));
        AssertJUnit.assertFalse(onStack.isFaceDown());
        resolveAll(game);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Hand));
        AssertJUnit.assertTrue(game.getCardState(morph).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void staysFaceDownIfSomethingTriesToTurnItUp() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Swamp", 3, ai);
        Card morph = castFaceDown(game, ai);
        AssertJUnit.assertFalse(morph.turnFaceUp(null));
        AssertJUnit.assertTrue(morph.isFaceDown());
        AssertJUnit.assertTrue(morph.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void cantBeCastOnceItLostItsAbilities() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Swamp", 4, ai);
        addCards("Island", 2, ai);
        addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        Card morph = castFaceDown(game, ai);
        for (Card land : ai.getCardsIn(ZoneType.Battlefield)) {
            land.setTapped(false);
        }
        Card frog = addCardToZone("Turn to Frog", ai, ZoneType.Hand);
        SpellAbility frogSa = frog.getFirstSpellAbility();
        frogSa.setActivatingPlayer(ai);
        frogSa.getTargets().add(morph);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, frogSa, null));
        resolveAll(game);
        AssertJUnit.assertNull(find(game.getCardState(morph), ai, false));
    }

    @Test
    public void rulesTextShowsTheKeyword() {
        Game game = initAndCreateGame();
        Card card = addCardToZone("Spellmorph Raise Dead", game.getPlayers().get(1), ZoneType.Hand);
        String text = card.getAbilityText();
        AssertJUnit.assertTrue(text, text.contains("Spellmorph {B} (You may cast this card face down as a 2/2 creature for {3}. Cast it any time for its spellmorph cost.)"));
        AssertJUnit.assertTrue(text, text.contains("Return target creature card from your graveyard to your hand."));
    }
}
