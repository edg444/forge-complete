package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Assembled Ensemble (UNF #3), checked against Scryfall and its 5 rulings 2026-10-20: only the Robot creature type
 * counts toward its power, itself included; the spell needn't be an artifact creature, just have one in its art.
 * Which arts show one (user ruling): Tagger's robot/construct/golem/thopter/servo/automaton/juggernaut/animated-object
 * tags, and any art that is an artifact creature card's own; gargoyle, scarecrow, cyborg, clockwork, vehicle and
 * homunculus art only when it's an artifact creature's own. Untagged art is asked about, and the AI says no.
 */
public class AssembledEnsembleTest extends AITest {

    private static Card printing(final String name, final String set, final String number, final Player owner) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set, number), owner);
        AssertJUnit.assertNotNull(name, c);
        return c;
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

    private static int clownRobots(final Player p) {
        return p.getCreaturesInPlay().filter(c -> c.isToken() && c.getType().hasCreatureType("Robot")).size();
    }

    @Test
    public void testPowerCountsRobotsItselfIncluded() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card ensemble = addCard("Assembled Ensemble", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, ensemble.getNetPower());
        AssertJUnit.assertEquals(6, ensemble.getNetToughness());
        addCard("Grizzly Bears", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("a non-Robot doesn't count", 1, ensemble.getNetPower());
        addToken("w_1_1_a_clown_robot", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, ensemble.getNetPower());
    }

    @Test
    public void testArtifactCreatureArtMakesARobot() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Mountain", ai);
        addCard("Assembled Ensemble", ai);
        game.getAction().checkStateEffects(true);

        final Card thopter = printing("Ornithopter", "AER", "167", ai);
        AssertJUnit.assertTrue(thopter.hasKnownArtifactCreatureInArt());
        ai.getZone(ZoneType.Hand).add(thopter);
        final SpellAbility cast = thopter.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, cast, null));
        resolveAll(game);
        AssertJUnit.assertEquals(1, clownRobots(ai));

        final Card bolt = printing("Lightning Bolt", "M10", "146", ai);
        AssertJUnit.assertFalse(bolt.hasKnownArtifactCreatureInArt());
        AssertJUnit.assertFalse(bolt.isArtUntagged());
        ai.getZone(ZoneType.Hand).add(bolt);
        final SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(ai);
        boltSa.getTargets().add(opp);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, boltSa, null));
        resolveAll(game);
        AssertJUnit.assertEquals("tagged art without one", 1, clownRobots(ai));
    }

    @Test
    public void testUntaggedArtIsAskedAndTheAiSaysNo() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card ensemble = addCard("Assembled Ensemble", ai);
        final Card untagged = printing("Pyroblast", "SLZ", "308", ai);
        AssertJUnit.assertTrue(untagged.isArtUntagged());
        AssertJUnit.assertFalse(untagged.hasKnownArtifactCreatureInArt());
        AssertJUnit.assertTrue(untagged.isValid("Card.ArtUntagged", ai, ensemble, null));

        final SpellAbility trig = ensemble.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        trig.setTriggeringObject(AbilityKey.Card, untagged);
        AbilityUtils.resolve(trig);
        AssertJUnit.assertEquals(0, clownRobots(ai));

        // the same trigger with tagged artifact-creature art makes one without asking
        trig.setTriggeringObject(AbilityKey.Card, printing("Ornithopter", "AER", "167", ai));
        AbilityUtils.resolve(trig);
        AssertJUnit.assertEquals(1, clownRobots(ai));
    }
}
