package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantBeCast;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * The Grand Calcutron (Unstable) - Oracle text, the five Scryfall rulings and the Unstable FAQ entry checked
 * 2026-10-06. The user ruled "can play only the first card" exclusive: nothing else can be played from anywhere.
 */
public class GrandCalcutronTest extends AITest {

    private Card enter(Game game, Player me) {
        Card calc = addCard("The Grand Calcutron", me);
        game.getAction().checkStateEffects(true);
        for (Trigger t : calc.getTriggers()) {
            if (t.getMode() == TriggerType.ChangesZone) {
                SpellAbility sa = t.ensureAbility();
                sa.setActivatingPlayer(me);
                AbilityUtils.resolve(sa);
            }
        }
        game.getAction().checkStateEffects(true);
        return calc;
    }

    private static boolean cantCast(Card c, Player p) {
        SpellAbility spell = c.getFirstSpellAbility();
        spell.setActivatingPlayer(p);
        return StaticAbilityCantBeCast.cantBeCastAbility(spell, c, p);
    }

    @Test
    public void testOnlyTheFirstCardCanBePlayed() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bolt = addCardToZone("Lightning Bolt", me, ZoneType.Hand);
        Card shock = addCardToZone("Shock", me, ZoneType.Hand);
        Card think = addCardToZone("Think Twice", me, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
        addCards("Mountain", 2, me);

        AssertJUnit.assertFalse(cantCast(shock, me));
        Card calc = enter(game, me);
        AssertJUnit.assertTrue(me.hasProgram());
        AssertJUnit.assertTrue(opp.hasProgram());
        AssertJUnit.assertTrue(me.getView().hasProgram());
        // revealed: the opponent can see the program
        AssertJUnit.assertTrue(bolt.getView().canBeShownTo(opp.getView()));

        Card first = me.getCardsIn(ZoneType.Hand).get(0);
        Card second = me.getCardsIn(ZoneType.Hand).get(1);
        AssertJUnit.assertFalse(cantCast(first, me));
        AssertJUnit.assertTrue(cantCast(second, me));
        // exclusive: a card in the graveyard (Think Twice has flashback) isn't the first card of the program either
        AssertJUnit.assertTrue(cantCast(think, me));

        // once on the stack, the first card is still judged by where it came from
        SpellAbility spell = first.getFirstSpellAbility();
        spell.setActivatingPlayer(me);
        Card onStack = game.getAction().moveToStack(first, spell);
        AssertJUnit.assertTrue(me.isFirstOfProgram(onStack));
        AssertJUnit.assertFalse(StaticAbilityCantBeCast.cantBeCastAbility(spell, onStack, me));

        // it leaves: hands are hands again
        game.getAction().exile(calc, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(me.hasProgram());
        AssertJUnit.assertFalse(me.getView().hasProgram());
        AssertJUnit.assertFalse(cantCast(shock, me));
        AssertJUnit.assertFalse(shock.getView().canBeShownTo(opp.getView()));
    }

    @Test
    public void testOnlyTheFirstLandCanBePlayed() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        addCardToZone("Island", me, ZoneType.Hand);
        addCardToZone("Plains", me, ZoneType.Hand);
        enter(game, me);
        Card first = me.getCardsIn(ZoneType.Hand).get(0);
        Card second = me.getCardsIn(ZoneType.Hand).get(1);
        AssertJUnit.assertTrue(me.canPlayLand(first, false, first.getFirstSpellAbility()));
        AssertJUnit.assertFalse(me.canPlayLand(second, false, second.getFirstSpellAbility()));
    }

    @Test
    public void testDrawnCardsArePlacedAndEndStepFillsToFive() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCardToZone("Island", me, ZoneType.Hand);
        addCardToZone("Plains", me, ZoneType.Hand);
        fillLibrary(me, 10);
        fillLibrary(opp, 10);
        enter(game, me);

        me.drawCard();
        AssertJUnit.assertEquals(3, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue(game.getGameLog().getLogEntries(null).stream()
                .anyMatch(e -> e.message().contains("in their program")));

        // at my end step I have 3 (maybe fewer after the AI plays a land): draw up to five
        playUntilPhase(game, PhaseType.END_OF_TURN);
        playUntilStackClear(game);
        AssertJUnit.assertEquals(5, me.getCardsIn(ZoneType.Hand).size());
        // only the player whose end step it is
        AssertJUnit.assertEquals(0, opp.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testAiOrderLeadsWithALandWhenItCanPlayOne() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        CardCollection cards = new CardCollection();
        cards.add(createCard("Shivan Dragon", me));
        cards.add(createCard("Lightning Bolt", me));
        cards.add(createCard("Mountain", me));
        CardCollection order = AiProgram.idealOrder(me, cards);
        // no lands out: Mountain, then Bolt fits the one mana, the Dragon waits
        AssertJUnit.assertEquals("Mountain", order.get(0).getName());
        AssertJUnit.assertEquals("Lightning Bolt", order.get(1).getName());
        AssertJUnit.assertEquals("Shivan Dragon", order.get(2).getName());
    }
}
