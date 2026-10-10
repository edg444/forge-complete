package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;

/**
 * Split Screen (Unstable) - Oracle text and its five Scryfall rulings checked 2026-10-09. User, 2026-10-09: one choice
 * per resolving ability, one per card drawn, one per permanent for continuous effects.
 */
public class SplitScreenTest extends AITest {

    /** Always picks library {@code pick} (1-based), counting what it was asked. */
    private static final class Picker {
        int pick = 1;
        int asked = 0;
        String lastPurpose = null;
    }

    private Picker picker(final Game game, final Player p) {
        final Picker k = new Picker();
        p.addController(game.getNextTimestamp(), p, new PlayerControllerAi(game, p, p.getLobbyPlayer()) {
            @Override
            public PlayerZone chooseLibrary(final List<PlayerZone> libraries, final String purpose, final SpellAbility sa) {
                k.asked++;
                k.lastPurpose = purpose;
                return libraries.get(Math.min(k.pick, libraries.size()) - 1);
            }
        }, false);
        return k;
    }

    private void runTrigger(final Card c, final String execute, final Player p) {
        for (final Trigger t : c.getTriggers()) {
            if (execute.equals(t.getParam("Execute"))) {
                final SpellAbility sa = t.ensureAbility();
                sa.setActivatingPlayer(p);
                AbilityUtils.resolve(sa);
            }
        }
    }

    private Card splitScreen(final Game game, final Player me) {
        final Card screen = addCard("Split Screen", me);
        runTrigger(screen, "TrigSplit", me);
        game.getAction().checkStateEffects(true);
        return screen;
    }

    private void library(final Player p, final int count) {
        for (int i = 0; i < count; i++) {
            addCardToZone(i % 2 == 0 ? "Forest" : "Runeclaw Bear", p, ZoneType.Library);
        }
    }

    @Test
    public void dealsIntoFourRevealedLibraries() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        final int before = me.getCardsInAllLibraries().size();
        library(me, 40);
        splitScreen(game, me);

        AssertJUnit.assertTrue(me.hasSeveralLibraries());
        final List<PlayerZone> libs = me.getLibraryZones();
        AssertJUnit.assertEquals(4, libs.size());
        int total = 0;
        for (final PlayerZone z : libs) {
            total += z.size();
            AssertJUnit.assertTrue(Math.abs(z.size() - (before + 40) / 4) <= 1);
            // "Play with your libraries' top cards revealed"
            AssertJUnit.assertTrue(z.get(0).mayPlayerLook(opp));
            AssertJUnit.assertTrue(z.get(0).getView().getMarkerText().contains("Library"));
            AssertJUnit.assertFalse(z.get(1).mayPlayerLook(opp));
        }
        AssertJUnit.assertEquals(before + 40, total);
        AssertJUnit.assertEquals(before + 40, game.getCardsIn(ZoneType.Library).size() - opp.getCardsInAllLibraries().size());
        AssertJUnit.assertEquals(before + 40, me.getView().getLibrary().size());
    }

    @Test
    public void eachDrawFromAnyLibrary() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        library(me, 40);
        splitScreen(game, me);
        final Picker k = picker(game, me);
        final List<PlayerZone> libs = me.getLibraryZones();
        final int[] sizes = new int[4];
        for (int i = 0; i < 4; i++) {
            sizes[i] = libs.get(i).size();
        }

        k.pick = 3;
        final Card top3 = libs.get(2).get(0);
        me.drawCards(2);
        AssertJUnit.assertEquals(2, k.asked);
        AssertJUnit.assertEquals("draw", k.lastPurpose);
        AssertJUnit.assertTrue(top3.isInZone(ZoneType.Hand));
        AssertJUnit.assertEquals(sizes[2] - 2, libs.get(2).size());
        AssertJUnit.assertEquals(sizes[0], libs.get(0).size());
    }

    @Test
    public void anEmptyLibraryOnlyLosesIfDrawnFrom() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        library(me, 3);
        splitScreen(game, me);
        final List<PlayerZone> libs = me.getLibraryZones();
        AssertJUnit.assertTrue(libs.get(3).isEmpty() || libs.get(3).size() <= 1);

        // the AI never picks an empty one while there's a card anywhere
        for (int i = 0; i < 3; i++) {
            me.drawCards(1);
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(me.hasLost());

        // all empty: "you may not have much choice"
        me.drawCards(1);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(me.hasLost());
    }

    @Test
    public void oneChoiceForAWholeAbility() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        library(me, 40);
        splitScreen(game, me);
        final Picker k = picker(game, me);
        final List<PlayerZone> libs = me.getLibraryZones();
        final int[] sizes = new int[4];
        for (int i = 0; i < 4; i++) {
            sizes[i] = libs.get(i).size();
        }

        // "Search your library for a basic land card ..., then shuffle" - one library for all of it
        k.pick = 2;
        final Card growth = addCardToZone("Rampant Growth", me, ZoneType.Hand);
        SpellAbility sa = null;
        for (final SpellAbility s : growth.getSpellAbilities()) {
            if (s.getApi() == ApiType.ChangeZone) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(me);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(1, k.asked);
        AssertJUnit.assertEquals("Rampant Growth", k.lastPurpose);
        AssertJUnit.assertEquals(sizes[1] - 1, libs.get(1).size());
        AssertJUnit.assertEquals(sizes[0], libs.get(0).size());
        AssertJUnit.assertEquals(sizes[2], libs.get(2).size());

        // an ability that doesn't mention a library asks nothing
        final Card shock = addCardToZone("Shock", me, ZoneType.Hand);
        final SpellAbility bolt = shock.getFirstSpellAbility();
        bolt.setActivatingPlayer(me);
        bolt.getTargets().add(game.getPlayers().get(0));
        AbilityUtils.resolve(bolt);
        AssertJUnit.assertEquals(1, k.asked);
    }

    @Test
    public void continuousEffectsGetALibraryEach() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        library(me, 40);
        splitScreen(game, me);
        final Picker k = picker(game, me);
        k.pick = 4;

        addCard("Future Sight", me);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, k.asked);
        AssertJUnit.assertEquals("Future Sight", k.lastPurpose);
        game.getAction().checkStaticAbilities();

        final List<PlayerZone> libs = me.getLibraryZones();
        AssertJUnit.assertFalse(libs.get(3).get(0).mayPlay(me).isEmpty());
        AssertJUnit.assertTrue(libs.get(0).get(0).mayPlay(me).isEmpty());
        // chosen once: not asked again
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, k.asked);
    }

    @Test
    public void secondSplitScreenMakesSevenAndLeavingShufflesThemAll() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final int before = me.getCardsInAllLibraries().size();
        library(me, 40);
        final Card first = splitScreen(game, me);
        final Picker k = picker(game, me);
        k.pick = 1;
        splitScreen(game, me);
        // "one of your four libraries will itself become four libraries"
        AssertJUnit.assertEquals(7, me.getLibraryZones().size());
        AssertJUnit.assertEquals(1, k.asked);
        AssertJUnit.assertEquals(before + 40, me.getCardsInAllLibraries().size());

        // "If one Split Screen leaves the battlefield, you'll shuffle all your libraries together"
        runTrigger(first, "TrigMerge", me);
        AssertJUnit.assertFalse(me.hasSeveralLibraries());
        AssertJUnit.assertEquals(before + 40, me.getCardsIn(ZoneType.Library).size());
        for (final Card c : me.getCardsIn(ZoneType.Library)) {
            AssertJUnit.assertNull(c.getView().getMarkerText());
        }
    }

    @Test
    public void animateLibraryIsOnOnlyOne() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        library(me, 40);
        splitScreen(game, me);
        final Picker k = picker(game, me);
        k.pick = 2;
        final Card aura = addCardToZone("Animate Library", me, ZoneType.Hand);
        final SpellAbility sa = aura.getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);

        final Card lib = forge.game.ability.effects.AnimateLibraryEffect.findLibraryPermanent(me);
        AssertJUnit.assertNotNull(lib);
        AssertJUnit.assertSame(me.getLibraryZones().get(1), lib.getRepresentedLibrary());
        AssertJUnit.assertEquals(me.getLibraryZones().get(1).size(), lib.getNetPower());
    }
}
