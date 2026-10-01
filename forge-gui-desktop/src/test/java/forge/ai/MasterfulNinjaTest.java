package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameAction;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Masterful Ninja (Unstable): "Haste / Reveal this card from your hand: Masterful Ninja is on the battlefield and in
 * your hand until end of turn. / {1}{B}: This creature gets +1/+1 until end of turn." - Oracle text and rulings
 * checked against Scryfall (and the Unstable FAQ) 2026-09-30.
 */
public class MasterfulNinjaTest extends AITest {

    private static SpellAbility reveal(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.AlsoOnBattlefield) {
                return sa;
            }
        }
        return null;
    }

    private Card ninjaInBothZones(Game game, Player me) {
        Card ninja = addCardToZone("Masterful Ninja", me, ZoneType.Hand);
        SpellAbility sa = reveal(ninja);
        sa.setActivatingPlayer(me);
        AssertJUnit.assertTrue(sa.getRestrictions().checkZoneRestrictions(ninja, sa));
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        return game.getCardState(ninja);
    }

    private static boolean anyTriggerWaiting(Game game) {
        game.getTriggerHandler().runWaitingTriggers();
        return game.getStack().hasSimultaneousStackEntries();
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card ninja = addCardToZone("Masterful Ninja", me, ZoneType.Hand);
        AssertJUnit.assertEquals("Haste | Reveal this card from your hand: Masterful Ninja is on the battlefield and in "
                + "your hand until end of turn. | {1}{B}: This creature gets +1/+1 until end of turn.",
                String.join(" | ", ninja.getAbilityText().trim().split("[\\r\\n]+")));
    }

    @Test
    public void testOnTheBattlefieldAndInYourHandUnnoticed() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        addCard("Soul Warden", me);
        addCard("Extractor Demon", me);
        addCardToZone("Grizzly Bears", me, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        anyTriggerWaiting(game);
        AssertJUnit.assertFalse(game.getStack().hasSimultaneousStackEntries());

        Card ninja = ninjaInBothZones(game, me);
        AssertJUnit.assertTrue(ninja.isInPlay());
        AssertJUnit.assertTrue(GameAction.isAlsoInHand(ninja));
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Hand).contains(ninja));
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Battlefield).contains(ninja));
        AssertJUnit.assertEquals(2, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue(ninja.hasKeyword(Keyword.HASTE));
        // Soul Warden doesn't see it enter
        AssertJUnit.assertFalse(anyTriggerWaiting(game));

        // pumped in play, then back to just a card in hand at end of turn; Extractor Demon doesn't see it leave
        SpellAbility pump = null;
        for (SpellAbility sa : ninja.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Pump) {
                pump = sa;
            }
        }
        pump.setActivatingPlayer(me);
        AbilityUtils.resolve(pump);
        AssertJUnit.assertEquals(2, ninja.getNetPower());
        game.getEndOfTurn().executeUntil();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(anyTriggerWaiting(game));

        Card inHand = game.getCardState(ninja);
        AssertJUnit.assertTrue(inHand.isInZone(ZoneType.Hand));
        AssertJUnit.assertNull(inHand.getShadowZone());
        AssertJUnit.assertEquals(0, countCardsWithName(game, "Masterful Ninja"));
        AssertJUnit.assertEquals(2, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(1, inHand.getNetPower());

        // a creature that really enters and leaves does trigger both, so the checks above mean something
        Card bears = me.getCardsIn(ZoneType.Hand).get(0) == inHand ? me.getCardsIn(ZoneType.Hand).get(1) : me.getCardsIn(ZoneType.Hand).get(0);
        Card bearsInPlay = game.getAction().moveToPlay(bears, me, null, AbilityKey.newMap());
        AssertJUnit.assertTrue(anyTriggerWaiting(game));
        game.getStack().clearSimultaneousStack();
        game.getAction().moveToHand(bearsInPlay, null);
        AssertJUnit.assertTrue(anyTriggerWaiting(game));
    }

    @Test
    public void testBounceDoesNothingButDiscardAndDestroyEndBoth() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        // Unsummon: it's already in its owner's hand, so it stays in both
        Card ninja = ninjaInBothZones(game, me);
        SpellAbility unsummon = addCardToZone("Unsummon", opp, ZoneType.Hand).getFirstSpellAbility();
        unsummon.setActivatingPlayer(opp);
        unsummon.getTargets().add(ninja);
        AbilityUtils.resolve(unsummon);
        AssertJUnit.assertTrue(ninja.isInPlay());
        AssertJUnit.assertTrue(GameAction.isAlsoInHand(ninja));
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Hand).size());

        // discarded: just in the graveyard
        me.discard(ninja, null, true, AbilityKey.newMap());
        AssertJUnit.assertEquals(0, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(0, countCardsWithName(game, "Masterful Ninja"));
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Graveyard).size());

        // destroyed: just in the graveyard
        Card second = ninjaInBothZones(game, me);
        game.getAction().destroy(second, null, true, AbilityKey.newMap());
        AssertJUnit.assertEquals(0, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(0, countCardsWithName(game, "Masterful Ninja"));
        AssertJUnit.assertEquals(2, me.getCardsIn(ZoneType.Graveyard).size());

        // nothing left to end at end of turn
        game.getEndOfTurn().executeUntil();
        AssertJUnit.assertEquals(2, me.getCardsIn(ZoneType.Graveyard).size());
    }

    @Test
    public void testStolenItStaysInItsOwnersHand() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card ninja = ninjaInBothZones(game, me);

        ninja.addTempController(opp, game.getNextTimestamp());
        game.getAction().controllerChangeZoneCorrection(ninja);
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Battlefield).contains(ninja));
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Hand).contains(ninja));

        game.getEndOfTurn().executeUntil();
        game.getAction().checkStateEffects(true);
        Card inHand = game.getCardState(ninja);
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Hand).contains(inHand));
        AssertJUnit.assertEquals(0, countCardsWithName(game, "Masterful Ninja"));
    }

    @Test
    public void testAiBringsItInToAttackOrBlock() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card ninja = addCardToZone("Masterful Ninja", ai, ZoneType.Hand);
        SpellAbility sa = reveal(ninja);
        sa.setActivatingPlayer(ai);
        SpellAbilityAi logic = SpellApiToAi.Converter.get(sa);

        // its own main phase, nothing to stop a hasty attacker
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertTrue(logic.canPlayWithSubs(ai, sa).willingToPlay());
        // after combat there's nothing to gain
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertFalse(logic.canPlayWithSubs(ai, sa).willingToPlay());
        // on the opponent's turn, not before they attack
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        AssertJUnit.assertFalse(logic.canPlayWithSubs(ai, sa).willingToPlay());

        // a Hill Giant attack at 20 life: a 1/1 can't beat it and doesn't need to chump it
        Card giant = addCard("Hill Giant", opp);
        giant.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, false);
        Combat combat = new Combat(opp);
        combat.addAttacker(giant, ai);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertFalse(logic.canPlayWithSubs(ai, sa).willingToPlay());
        // at 3 life it comes in to chump
        ai.setLife(3, null);
        AssertJUnit.assertTrue(logic.canPlayWithSubs(ai, sa).willingToPlay());
    }
}
