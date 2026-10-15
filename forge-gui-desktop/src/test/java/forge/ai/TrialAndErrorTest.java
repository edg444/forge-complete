package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Trial and Error (CMB1 #68), checked against Scryfall and its 2 rulings 2026-10-14: Elemental is a card type and Fire
 * an elemental type, unrelated to the creature type; countered or fizzled, it may be copied with new targets.
 */
public class TrialAndErrorTest extends AITest {

    private static SpellAbility castOnto(final Player p, final Card c, final GameObject target) {
        Card onStack = p.getGame().getAction().moveToStack(c, null);
        SpellAbility sa = onStack.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add(target);
        }
        p.getGame().getStack().add(sa);
        return sa;
    }

    private static void resolveAll(final Game game) {
        for (int i = 0; i < 10 && !game.getStack().isEmpty(); i++) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void elementalIsACardTypeNotTheCreatureType() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card trial = addCardToZone("Trial and Error", ai, ZoneType.Hand);

        AssertJUnit.assertTrue(trial.getType().hasType(CardType.CoreType.ElementalCard));
        AssertJUnit.assertTrue(trial.isInstant());
        AssertJUnit.assertTrue(trial.getType().hasSubtype("Fire"));
        AssertJUnit.assertEquals("Elemental Instant - Fire", trial.getType().toString());
        AssertJUnit.assertEquals(2, trial.getType().getCoreTypes().size());
        AssertJUnit.assertFalse("an Elemental means the creature type", trial.getType().hasStringType("Elemental"));
        AssertJUnit.assertFalse(trial.isValid("Card.Elemental", ai, trial, null));
        AssertJUnit.assertTrue(trial.isValid("Card.ElementalCard", ai, trial, null));
    }

    @Test
    public void counteredItCopiesItself() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card trial = addCardToZone("Trial and Error", ai, ZoneType.Hand);
        Card cancel = addCardToZone("Cancel", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility trialSa = castOnto(ai, trial, opp);
        castOnto(opp, cancel, trialSa);
        resolveAll(game);

        AssertJUnit.assertEquals("the copy hit the opponent", 17, opp.getLife());
        AssertJUnit.assertTrue(game.getCardState(trialSa.getHostCard()).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void fizzledItCopiesItselfAtANewTarget() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", opp);
        Card trial = addCardToZone("Trial and Error", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility trialSa = castOnto(ai, trial, bears);
        game.getAction().moveToHand(bears, null);
        resolveAll(game);

        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertEquals("the copy found a new target", 17, opp.getLife());
        AssertJUnit.assertTrue(game.getCardState(trialSa.getHostCard()).isInZone(ZoneType.Graveyard));
    }
}
