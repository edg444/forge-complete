package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * CR 712.13a (2026-09-25): a double-faced spell that would enter transformed while its back face is an instant or
 * sorcery goes to its owner's graveyard instead - the rule's own Mycosynth Lattice + March of the Machines +
 * Mystic Reflection + Invasion of Kylem example. Also the AI not playing a land that would be a 0/0 there.
 */
public class EnterTransformedNonPermanentBackTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Game game, final Player p, final Card spell, final SpellAbility sa) {
        sa.setActivatingPlayer(p);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    @Test
    public void testKylemCopyingADayboundCloneAtNight() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.setDayTime(false);
        final Card admirer = addCard("Bird Admirer", ai);
        final Card cloneCard = addCardToZone("Clone", ai, ZoneType.Hand);
        cast(game, ai, cloneCard, cloneCard.getFirstSpellAbility());
        final Card clone = game.getCardState(cloneCard);
        AssertJUnit.assertEquals("Bird Admirer", clone.getName());
        AssertJUnit.assertFalse(clone.isTransformable());
        game.getAction().moveToGraveyard(admirer, null);

        addCard("Mycosynth Lattice", ai);
        addCard("March of the Machines", ai);
        game.setDayTime(true);
        resolveAll(game);
        AssertJUnit.assertTrue(game.isNight());
        AssertJUnit.assertEquals("a Clone can't transform", "Bird Admirer", clone.getName());

        final Card reflection = addCardToZone("Mystic Reflection", ai, ZoneType.Hand);
        final SpellAbility reflect = reflection.getFirstSpellAbility();
        reflect.getTargets().add(clone);
        cast(game, ai, reflection, reflect);

        final Card kylem = addCardToZone("Invasion of Kylem", ai, ZoneType.Hand);
        cast(game, ai, kylem, kylem.getFirstSpellAbility());
        final Card after = game.getCardState(kylem);
        AssertJUnit.assertTrue("712.13a: into the graveyard, it was in " + after.getZone(), after.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("Invasion of Kylem", after.getName());
        AssertJUnit.assertFalse(after.isBackSide());
    }

    @Test
    public void testDayboundSpellStillEntersTransformedAtNight() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.setDayTime(false);
        game.setDayTime(true);
        final Card admirer = addCardToZone("Bird Admirer", ai, ZoneType.Hand);
        cast(game, ai, admirer, admirer.getFirstSpellAbility());
        final Card after = game.getCardState(admirer);
        AssertJUnit.assertTrue(after.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals("Wing Shredder", after.getName());
    }

    private boolean choosesALandPlay(final Player ai) {
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).getAi().chooseSpellAbilityToPlay();
        return chosen != null && chosen.stream().anyMatch(SpellAbility::isLandAbility);
    }

    @Test
    public void testAiHoldsALandThatWouldBeAZeroZero() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        addCardToZone("Forest", ai, ZoneType.Hand);
        addCard("Mycosynth Lattice", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("Lattice alone: a land is still a land", choosesALandPlay(ai));

        addCard("March of the Machines", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("it would die as a 0/0", choosesALandPlay(ai));
        AssertJUnit.assertTrue("the prediction left the board alone", game.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> c.getName().equals("Mycosynth Lattice")).allMatch(c -> c.isCreature() && c.getNetToughness() == 6));
    }
}
