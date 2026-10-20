package forge.ai;

import java.util.Random;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Flavor Judge (UND #4), checked against Scryfall and its 2 rulings 2026-10-20. User ruling: a human asks a real
 * person; the AI's simulated person says no half the time.
 */
public class FlavorJudgeTest extends AITest {

    private static final class Fixed extends Random {
        private final int value;
        Fixed(final int value) { this.value = value; }
        @Override public int nextInt(final int bound) { return value; }
    }

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

    private static SpellAbility judgeAbility(final Card judge, final Player ai) {
        SpellAbility sa = judge.getSpellAbilities().stream().filter(s -> s.getApi() == ApiType.GenericChoice).findFirst().orElse(null);
        AssertJUnit.assertNotNull(sa);
        sa.setActivatingPlayer(ai);
        return sa;
    }

    private void judge(final boolean storyMakesSense) {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card judge = addCard("Flavor Judge", ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);

        SpellAbility boltSa = castOnto(opp, bolt, bears);
        SpellAbility sa = judgeAbility(judge, ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertSame(boltSa, sa.getTargets().getFirstTargetedSpell());
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));

        final Random before = MyRandom.getRandom();
        MyRandom.setRandom(new Fixed(storyMakesSense ? 99 : 0));
        try {
            game.getStack().resolveStack();
        } finally {
            MyRandom.setRandom(before);
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("stack: " + game.getStack().size() + " judge in " + game.getCardState(judge).getZone(), storyMakesSense, game.getCardState(judge).isInPlay());
        AssertJUnit.assertEquals("the Bolt is still coming", storyMakesSense, !game.getStack().isEmpty());
        if (!storyMakesSense) {
            AssertJUnit.assertTrue(game.getCardState(judge).isInZone(ZoneType.Graveyard));
            AssertJUnit.assertTrue(game.getCardState(bolt).isInZone(ZoneType.Graveyard));
            AssertJUnit.assertTrue(bears.isInPlay());
        }
    }

    @Test
    public void theyFindItNonsense() {
        judge(false);
    }

    @Test
    public void theStoryMakesSense() {
        judge(true);
    }

    @Test
    public void onlySpellsTargetingYourPermanents() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Card judge = addCard("Flavor Judge", ai);
        Card theirs = addCard("Grizzly Bears", opp);
        Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        Card shock = addCardToZone("Shock", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);
        AiController brains = ((PlayerControllerAi) ai.getController()).getAi();
        SpellAbility sa = judgeAbility(judge, ai);

        castOnto(opp, bolt, theirs);
        AssertJUnit.assertFalse(brains.canPlaySa(sa) == AiPlayDecision.WillPlay);
        game.getStack().resolveStack();
        castOnto(opp, shock, ai);
        AssertJUnit.assertFalse("a player isn't a permanent", brains.canPlaySa(sa) == AiPlayDecision.WillPlay);
    }
}
