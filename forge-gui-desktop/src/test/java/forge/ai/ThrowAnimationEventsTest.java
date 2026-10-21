package forge.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.eventbus.Subscribe;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.event.GameEventFlipOntoBattlefield;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * The funny cards that throw something send the throw animation event (upstream's Chaos Orb animation on mobile), each
 * showing what really flies: Landfill's dropped lands, Chaos Confetti's torn pieces, Ol' Buzzbark's dice, and Orcish
 * Paratroopers / Clay Pigeon tossed in place.
 */
public class ThrowAnimationEventsTest extends AITest {

    public static final class Throws {
        final List<GameEventFlipOntoBattlefield> started = new ArrayList<>();
        @Subscribe
        public void on(final GameEventFlipOntoBattlefield ev) {
            if (!ev.finished()) {
                started.add(ev);
            }
        }
    }

    private static Throws listen(final Game game) {
        final Throws t = new Throws();
        game.subscribeToEvents(t);
        return t;
    }

    private static SpellAbility first(final Card c) {
        final SpellAbility sa = c.isPermanent() ? c.getSpellAbilities().stream().filter(x -> !x.isSpell()).findFirst().orElseThrow() : c.getFirstSpellAbility();
        sa.setActivatingPlayer(c.getController());
        return sa;
    }

    @Test
    public void landfillDropsTheLandsThemselves() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        List<Card> forests = addCards("Forest", 2, ai);
        addCard("Grizzly Bears", opp);
        Card landfill = addCardToZone("Landfill", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        Throws t = listen(game);
        AbilityUtils.resolve(first(landfill));
        AssertJUnit.assertEquals("chose " + game.getCardState(landfill).getChosenType() + " log " + game.getGameLog().getLogEntries(null), 2, t.started.size());
        for (GameEventFlipOntoBattlefield ev : t.started) {
            AssertJUnit.assertTrue("a Forest flies, not Landfill",
                    forests.stream().anyMatch(f -> f.getId() == ev.flipped().getId()));
            AssertJUnit.assertEquals(-1, ev.style().piece());
        }
    }

    @Test
    public void chaosConfettiThrowsPieces() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card confetti = addCard("Chaos Confetti", ai);
        addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);
        Throws t = listen(game);
        AbilityUtils.resolve(first(confetti));
        AssertJUnit.assertEquals("log " + game.getGameLog().getLogEntries(null), 4, t.started.size());
        for (GameEventFlipOntoBattlefield ev : t.started) {
            AssertJUnit.assertEquals(confetti.getId(), ev.flipped().getId());
            AssertJUnit.assertTrue(ev.style().piece() >= 0 && ev.style().piece() < 4);
        }
    }

    @Test
    public void paratroopersTossInPlace() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card orcs = addCard("Orcish Paratroopers", ai);
        game.getAction().checkStateEffects(true);
        Throws t = listen(game);
        for (Trigger tr : orcs.getTriggers()) {
            if (tr.getMode() == TriggerType.ChangesZone) {
                SpellAbility sa = tr.ensureAbility();
                sa.setActivatingPlayer(ai);
                AbilityUtils.resolve(sa);
            }
        }
        AssertJUnit.assertEquals(1, t.started.size());
        GameEventFlipOntoBattlefield ev = t.started.get(0);
        AssertJUnit.assertTrue(ev.style().returnHome());
        AssertJUnit.assertEquals("a bad drop lands face down", orcs.isInPlay() ? false : true, ev.style().landFaceDown());
    }

    @Test
    public void buzzbarksDiceLandWhereTheyTouch() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wurm = addCard("Craw Wurm", opp);
        Card buzzbark = addCard("Ol' Buzzbark", me);
        game.getAction().checkStateEffects(true);
        Throws t = listen(game);
        Random before = MyRandom.getRandom();
        MyRandom.setRandom(new Random() {
            @Override public double nextDouble() { return 0.5; }
            @Override public synchronized double nextGaussian() { return 0; }
        });
        try {
            for (Trigger tr : buzzbark.getTriggers()) {
                if (tr.getMode() == TriggerType.ChangesZone) {
                    SpellAbility sa = tr.ensureAbility();
                    sa.setActivatingPlayer(me);
                    sa.setSVar("X", "Number$2");
                    AbilityUtils.resolve(sa);
                }
            }
        } finally {
            MyRandom.setRandom(before);
        }
        AssertJUnit.assertEquals(2, t.started.size());
        for (GameEventFlipOntoBattlefield ev : t.started) {
            AssertJUnit.assertTrue(ev.style().dieFace() >= 1 && ev.style().dieFace() <= 6);
            AssertJUnit.assertTrue(ev.style().keepHits());
            AssertJUnit.assertEquals("hit " + ev.hit() + " log " + game.getGameLog().getLogEntries(null), 1, ev.hit().size());
            // dead-center dice land on whatever the AI aimed at (wherever the log says they came to rest)
            final int id = ev.hit().get(0).getId();
            AssertJUnit.assertTrue(id == wurm.getId() || id == buzzbark.getId());
            AssertJUnit.assertEquals(id, ev.target().getId());
        }
    }
}
