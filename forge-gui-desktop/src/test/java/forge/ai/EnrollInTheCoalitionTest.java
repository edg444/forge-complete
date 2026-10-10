package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.staticability.StaticAbilityPlayerType;
import forge.game.zone.ZoneType;

/**
 * Enroll in the Coalition (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10:
 * you're a Flagbearer but not a creature; any player's Flagbearer may be chosen ("an opponent's Shock could target
 * either creature or you, but not another player" with Coalition Honor Guards out); triggered abilities, copies and
 * changed targets are free.
 */
public class EnrollInTheCoalitionTest extends AITest {

    private static boolean legal(Card spell, Player caster, GameEntity target) {
        SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(caster);
        sa.resetTargets();
        sa.getTargets().add(target);
        return StaticAbilityMustTarget.meetsMustTargetRestriction(sa);
    }

    @Test
    public void testOpponentsMustTargetYou() {
        Game game = initAndCreateThreePlayerGame();
        Player me = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);
        Player other = game.getPlayers().get(2);
        addCard("Enroll in the Coalition", me);
        Card myBears = addCard("Grizzly Bears", me);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(StaticAbilityPlayerType.hasType(me, "Flagbearer"));
        AssertJUnit.assertFalse(StaticAbilityPlayerType.hasType(opp, "Flagbearer"));

        Card shock = addCardToZone("Shock", opp, ZoneType.Hand);
        AssertJUnit.assertTrue(legal(shock, opp, me));
        AssertJUnit.assertFalse(legal(shock, opp, myBears));
        AssertJUnit.assertFalse(legal(shock, opp, other));

        // my own spells aren't restricted
        Card mine = addCardToZone("Shock", me, ZoneType.Hand);
        AssertJUnit.assertTrue(legal(mine, me, opp));
    }

    @Test
    public void testAnyFlagbearerWithHonorGuardsOut() {
        Game game = initAndCreateThreePlayerGame();
        Player me = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);
        Player other = game.getPlayers().get(2);
        addCard("Enroll in the Coalition", me);
        Card myGuard = addCard("Coalition Honor Guard", me);
        Card theirGuard = addCard("Coalition Honor Guard", other);
        Card bears = addCard("Grizzly Bears", me);
        game.getAction().checkStateEffects(true);

        Card shock = addCardToZone("Shock", opp, ZoneType.Hand);
        AssertJUnit.assertTrue(legal(shock, opp, me));
        AssertJUnit.assertTrue(legal(shock, opp, myGuard));
        AssertJUnit.assertTrue(legal(shock, opp, theirGuard));
        AssertJUnit.assertFalse(legal(shock, opp, bears));
        AssertJUnit.assertFalse(legal(shock, opp, other));
    }

    @Test
    public void testHonorGuardAloneStillMeansCreatures() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);
        Card guard = addCard("Coalition Honor Guard", me);
        game.getAction().checkStateEffects(true);
        Card shock = addCardToZone("Shock", opp, ZoneType.Hand);
        AssertJUnit.assertTrue(legal(shock, opp, guard));
        AssertJUnit.assertFalse(legal(shock, opp, me));
    }

    @Test
    public void testCandidatesAreFilteredToFlagbearers() {
        Game game = initAndCreateThreePlayerGame();
        Player me = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);
        addCard("Enroll in the Coalition", me);
        addCard("Grizzly Bears", me);
        game.getAction().checkStateEffects(true);
        Card shock = addCardToZone("Shock", opp, ZoneType.Hand);
        SpellAbility sa = shock.getFirstSpellAbility();
        sa.setActivatingPlayer(opp);
        sa.resetTargets();
        List<Card> cards = new ArrayList<>(game.getCardsIn(ZoneType.Battlefield));
        List<Player> players = new ArrayList<>(game.getPlayers());
        AssertJUnit.assertTrue(StaticAbilityMustTarget.filterMustTargets(opp, cards, players, sa));
        AssertJUnit.assertTrue(cards.isEmpty());
        AssertJUnit.assertEquals(List.of(me), players);
    }

    @Test
    public void testAiShocksTheFlagbearer() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCard("Enroll in the Coalition", me);
        addCard("Grizzly Bears", me);
        addCard("Mountain", ai);
        Card shock = addCardToZone("Shock", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = shock.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        if (SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay()) {
            AssertJUnit.assertTrue("AI chose " + sa.getTargets(), StaticAbilityMustTarget.meetsMustTargetRestriction(sa));
        }
    }
}
