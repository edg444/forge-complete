package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * P-Pick Your Poison (CMB1 #97), checked against Scryfall and its 2 rulings 2026-10-15: modes worth [1], [2] and [4]
 * totaling exactly 4, repeatable, performed in printed order.
 */
public class PickYourPoisonTest extends AITest {

    private static int total(List<AbilitySub> chosen) {
        return chosen.stream().mapToInt(CharmEffect::pawprintValue).sum();
    }

    @Test
    public void exactTotals() {
        AssertJUnit.assertTrue(CharmEffect.canTotalExactly(List.of(1, 2, 4), 4, true));
        AssertJUnit.assertTrue(CharmEffect.canTotalExactly(List.of(2, 4), 2, true));
        AssertJUnit.assertFalse(CharmEffect.canTotalExactly(List.of(2, 4), 3, true));
        AssertJUnit.assertFalse(CharmEffect.canTotalExactly(List.of(1, 2), 4, false));
        AssertJUnit.assertTrue(CharmEffect.canTotalExactly(List.of(1, 2), 4, true));
        AssertJUnit.assertFalse(CharmEffect.canTotalExactly(List.of(1), -1, true));
    }

    @Test
    public void modesResolveInPrintedOrder() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card pyp = addCardToZone("P-Pick Your Poison", ai, ZoneType.Hand);
        SpellAbility sa = pyp.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        String text = CharmEffect.makeFormatedDescription(sa);
        AssertJUnit.assertTrue(text, text.startsWith("Choose any number of modes that total exactly 4. You may choose the same mode more than once."));
        AssertJUnit.assertTrue(text, text.contains("[2] Create a 1/1 black Snake creature token with deathtouch."));

        // chosen snake first, then the counter twice: the counters still come first (ruling 1)
        List<AbilitySub> opts = CharmEffect.makePossibleOptions(sa);
        CharmEffect.chainAbilities(sa, Lists.newArrayList(opts.get(1), opts.get(0), opts.get(0)));
        for (SpellAbility sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() == ApiType.PutCounter) {
                sub.getTargets().add(bears);
            }
        }
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(2, bears.getCounters(CounterEnumType.P1P1));
        List<Card> snakes = ai.getCreaturesInPlay().stream().filter(c -> c.getType().hasCreatureType("Snake")).toList();
        AssertJUnit.assertEquals(1, snakes.size());
        AssertJUnit.assertEquals(0, snakes.get(0).getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(snakes.get(0).hasKeyword("Deathtouch"));
    }

    @Test
    public void aiSweepsTheirBoard() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        for (int i = 0; i < 3; i++) {
            addCard("Grizzly Bears", opp);
            addCard("Swamp", ai);
        }
        addCard("Forest", ai);
        addCard("Forest", ai);
        Card pyp = addCardToZone("P-Pick Your Poison", ai, ZoneType.Hand);
        SpellAbility sa = pyp.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertEquals(4, total(sa.getChosenList()));
        AssertJUnit.assertTrue(sa.getChosenList().stream().anyMatch(s -> s.getApi() == ApiType.PumpAll));

        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(opp.getCreaturesInPlay().isEmpty());
    }

    @Test
    public void aiBuildsUpWithoutAWipe() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Hill Giant", ai);
        for (int i = 0; i < 3; i++) {
            addCard("Swamp", ai);
        }
        addCard("Forest", ai);
        addCard("Forest", ai);
        Card pyp = addCardToZone("P-Pick Your Poison", ai, ZoneType.Hand);
        SpellAbility sa = pyp.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertEquals(4, total(sa.getChosenList()));
        AssertJUnit.assertTrue("no -2/-2 to its own board", sa.getChosenList().stream().noneMatch(s -> s.getApi() == ApiType.PumpAll));
    }
}
