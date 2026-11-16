package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.CardFlowTiming;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Card selection and cantrip timing (report 2026-11-16).
 *
 * <p>Sigiled Starfish ("{T}: Scry 1") was tapped in the AI's own turn, leaving no blocker for the opponent's turn, and
 * a second Starfish was activated after the first one kept its card on top. Hydrolash (attacking creatures get -2/-0,
 * draw a card) was cast in the AI's own turn. Cause of the timing: Flat Monte Carlo discovery handed the rollouts the
 * plays the heuristics had turned down (ScryAi only taps a creature at the opponent's end step, PumpAllAi only shrinks
 * attackers in the opponent's combat), and the rollouts score them by the card they show or draw. A varied library is
 * needed to see it: with identical cards a scry changes nothing in the rollouts.
 */
public class CardFlowTimingTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 240_000;
    private static final String[] MIXED = { "Island", "Runeclaw Bear", "Air Elemental", "Island", "Hill Giant",
            "Counterspell", "Island", "Serra Angel", "Lightning Bolt", "Mountain" };

    private Game game;
    private Player ai;
    private Player opp;

    private void setUp(final boolean flatMc) {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = flatMc;
        game.AI_TIMEOUT = 10;
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);
        mixedLibrary(ai);
        mixedLibrary(opp);
    }

    private void mixedLibrary(final Player p) {
        for (int i = 0; i < 30; i++) {
            addCardToZone(MIXED[(i * 7) % MIXED.length], p, ZoneType.Library);
        }
    }

    private void phase(final PhaseType phase, final Player turnPlayer) {
        game.getPhaseHandler().devModeSet(phase, turnPlayer);
        game.getAction().checkStateEffects(true);
    }

    private String nextPlay() {
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
    }

    private boolean heuristicsWant(final SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay();
    }

    private Card starfish() {
        final Card c = addCard("Sigiled Starfish", ai);
        c.setSickness(false);
        return c;
    }

    private SpellAbility scryOf(final Card starfish) {
        for (final SpellAbility sa : starfish.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no scry ability on " + starfish);
    }

    /** The AI's board from the report: a Starfish, lands, a creature, and cards in hand; the opponent has attackers. */
    private void starfishBoard() {
        starfish();
        addCards("Island", 3, ai);
        addCards("Mountain", 2, ai);
        addCard("Hill Giant", ai).setSickness(false);
        addCard("Hill Giant", opp).setSickness(false);
        addCard("Grizzly Bears", opp).setSickness(false);
        addCards("Forest", 3, opp);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
    }

    @Test
    public void selectionAndCombatCantripsAreHeldForTheHeuristics() {
        setUp(false);
        AssertJUnit.assertTrue("Sigiled Starfish: a scry and nothing else", CardFlowTiming.heuristicsHoldIt(scryOf(starfish())));
        AssertJUnit.assertTrue("Hydrolash: shrinks attackers and draws",
                CardFlowTiming.heuristicsHoldIt(addCardToZone("Hydrolash", ai, ZoneType.Hand).getFirstSpellAbility()));
        AssertJUnit.assertFalse("Opt draws a card: discovery may still weigh it",
                CardFlowTiming.heuristicsHoldIt(addCardToZone("Opt", ai, ZoneType.Hand).getFirstSpellAbility()));
        AssertJUnit.assertFalse("Divination: plain card draw",
                CardFlowTiming.heuristicsHoldIt(addCardToZone("Divination", ai, ZoneType.Hand).getFirstSpellAbility()));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void starfishIsNotTappedInTheAisOwnTurn() {
        for (final PhaseType p : new PhaseType[] { PhaseType.UPKEEP, PhaseType.MAIN1, PhaseType.MAIN2 }) {
            for (int i = 0; i < 6; i++) {
                setUp(true);
                starfishBoard();
                phase(p, ai);
                final String play = nextPlay();
                AssertJUnit.assertFalse("own " + p + " (try " + i + "): the Starfish waits for the opponent's end step,"
                        + " untapped to block, but the AI chose: " + play, "Sigiled Starfish".equals(play));
            }
        }
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void starfishIsNotTappedBeforeTheOpponentsAttack() {
        for (final PhaseType p : new PhaseType[] { PhaseType.UPKEEP, PhaseType.MAIN1 }) {
            for (int i = 0; i < 3; i++) {
                setUp(true);
                starfishBoard();
                phase(p, opp);
                final String play = nextPlay();
                AssertJUnit.assertFalse("opponent's " + p + ": it is a blocker, but the AI chose: " + play,
                        "Sigiled Starfish".equals(play));
            }
        }
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void starfishScriesAtTheOpponentsEndStep() {
        setUp(true);
        starfishBoard();
        phase(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals("the opponent's end step is the moment for it", "Sigiled Starfish", nextPlay());
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void secondStarfishOnlyScriesWhenTheFirstBottomedTheCard() {
        setUp(true);
        final Card first = starfish();
        final Card second = starfish();
        addCards("Island", 4, ai);
        final Card top = addCardToZone("Runeclaw Bear", ai, ZoneType.Library);
        ai.getZone(ZoneType.Library).remove(top);
        ai.getZone(ZoneType.Library).add(top, 0);
        phase(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertFalse("precondition: the AI keeps the bear on top",
                ComputerUtil.scryWillMoveCardToBottomOfLibrary(ai, top));
        AssertJUnit.assertTrue("first Starfish at the opponent's end step", heuristicsWant(scryOf(first)));

        // the first Starfish's scry, as the game asks the AI for it
        ((PlayerControllerAi) ai.getController()).arrangeForScry(new CardCollection(top));
        first.tap(true, null, null);
        AssertJUnit.assertFalse("the second Starfish would only see the bear it just kept", heuristicsWant(scryOf(second)));
        final String play = nextPlay();
        AssertJUnit.assertNull("nothing else to do: pass, but the AI chose: " + play, play);

        // the bear was drawn (or bottomed): there is a new card on top to look at
        ai.getZone(ZoneType.Library).remove(top);
        AssertJUnit.assertTrue("a new card on top: the second Starfish has something to scry", heuristicsWant(scryOf(second)));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void hydrolashIsNotCastInTheAisOwnTurn() {
        for (final PhaseType p : new PhaseType[] { PhaseType.MAIN1, PhaseType.MAIN2 }) {
            for (int i = 0; i < 4; i++) {
                setUp(true);
                addCards("Island", 3, ai);
                addCardToZone("Hydrolash", ai, ZoneType.Hand);
                addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
                addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
                phase(p, ai);
                final String play = nextPlay();
                AssertJUnit.assertFalse("own " + p + " (try " + i + "): Hydrolash is held for the opponent's attack,"
                        + " but the AI chose: " + play, "Hydrolash".equals(play));
            }
        }
    }

    @Test
    public void hydrolashIsCastInTheOwnTurnWhenStarvedForCards() {
        setUp(false);
        addCards("Island", 3, ai);
        final Card lash = addCardToZone("Hydrolash", ai, ZoneType.Hand);
        phase(PhaseType.MAIN1, ai);
        AssertJUnit.assertTrue("empty hand otherwise and a land drop to dig for", heuristicsWant(lash.getFirstSpellAbility()));
        AssertJUnit.assertEquals("Hydrolash", nextPlay());

        ai.addLandPlayedThisTurn();
        AssertJUnit.assertFalse("the land drop is used and no mana would be left: wait for the opponent's turn",
                heuristicsWant(lash.getFirstSpellAbility()));
    }

    @Test
    public void hydrolashIsStillCastIntoTheOpponentsAttack() {
        setUp(false);
        addCards("Island", 3, ai);
        final Card lash = addCardToZone("Hydrolash", ai, ZoneType.Hand);
        final Card giant1 = addCard("Hill Giant", opp);
        final Card giant2 = addCard("Hill Giant", opp);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, false);
        final Combat combat = new Combat(opp);
        for (final Card a : new Card[] { giant1, giant2 }) {
            a.setSickness(false);
            combat.addAttacker(a, ai);
        }
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("two attackers to shrink", heuristicsWant(lash.getFirstSpellAbility()));
    }
}
