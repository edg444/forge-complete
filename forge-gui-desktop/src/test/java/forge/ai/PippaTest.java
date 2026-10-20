package forge.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

/**
 * Pippa, Duchess of Dice (UND #68), checked against Scryfall and its 4 rulings 2026-10-20.
 */
public class PippaTest extends AITest {

    /** Die faces in order (1-based), then 1s. */
    private static final class Faces extends Random {
        private final Deque<Integer> faces = new ArrayDeque<>();
        Faces(final int... f) { for (final int x : f) { faces.add(x); } }
        @Override public int nextInt(final int bound) { return faces.isEmpty() ? 0 : faces.poll() - 1; }
    }

    private final Random before = MyRandom.getRandom();

    @AfterMethod
    public void restoreRandom() {
        MyRandom.setRandom(before);
    }

    private static SpellAbility ability(final Card c, final ApiType api, final Player p) {
        SpellAbility sa = c.getSpellAbilities().stream().filter(s -> s.getApi() == api).findFirst().orElseThrow();
        sa.setActivatingPlayer(p);
        return sa;
    }

    private static void resolveAll(final Game game) {
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private Card dieCreature(final Game game) {
        return game.getCardsIn(forge.game.zone.ZoneType.Battlefield).stream().filter(Card::isDie).findFirst().orElse(null);
    }

    @Test
    public void theRollBecomesADieCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card pippa = addCard("Pippa, Duchess of Dice", ai);
        pippa.setSickness(false);
        addCards("Forest", 3, ai);
        game.getAction().checkStateEffects(true);
        MyRandom.setRandom(new Faces(4));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, ability(pippa, ApiType.RollDice, ai), null));
        resolveAll(game);
        Card die = dieCreature(game);
        AssertJUnit.assertNotNull(die);
        AssertJUnit.assertTrue(die.getType().hasCreatureType("Die"));
        AssertJUnit.assertTrue(die.getColor().hasGreen());
        AssertJUnit.assertEquals(4, die.getNetPower());
        AssertJUnit.assertEquals(4, die.getNetToughness());
    }

    @Test
    public void theAiRerollsItsLowDie() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card pippa = addCard("Pippa, Duchess of Dice", ai);
        pippa.setSickness(false);
        addCards("Island", 3, ai);
        Card die = addToken("g_x_x_die", ai);
        die.setDieFace(1, 6);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, die.getNetPower());

        SpellAbility reroll = ability(pippa, ApiType.RerollDie, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(reroll));
        MyRandom.setRandom(new Faces(5));
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, reroll, null));
        resolveAll(game);
        AssertJUnit.assertEquals(5, die.getNetPower());
    }

    @Test
    public void theAiRerollsAnOpponentsHighRoll() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card pippa = addCard("Pippa, Duchess of Dice", ai);
        pippa.setSickness(false);
        addCards("Island", 3, ai);
        game.getAction().checkStateEffects(true);
        MyRandom.setRandom(new Faces(6, 2));
        int result = RollDiceEffect.rollDiceForPlayer(null, opp, 1, 6);
        AssertJUnit.assertEquals(2, result);
        AssertJUnit.assertTrue(pippa.isTapped());
    }
}
