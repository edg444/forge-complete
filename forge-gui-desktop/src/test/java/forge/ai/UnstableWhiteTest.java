package forge.ai;

import java.util.HashMap;
import java.util.Map;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/** Unstable white, ust/17-25: Ordinary Pony, Rhino-, Sacrifice Play, Side Quest, Success!, Teacher's Pet. */
public class UnstableWhiteTest extends AITest {

    private static SpellAbility abilityWithApi(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    private static SpellAbility etb(Card c) {
        for (Trigger t : c.getTriggers()) {
            if (t.getMode() == TriggerType.ChangesZone) {
                return t.ensureAbility();
            }
        }
        return null;
    }

    private static void resolve(SpellAbility sa, Player activator, Object... targets) {
        sa.setActivatingPlayer(activator);
        sa.resetTargets();
        for (Object t : targets) {
            if (t instanceof Card) {
                sa.getTargets().add((Card) t);
            } else {
                sa.getTargets().add((Player) t);
            }
        }
        AbilityUtils.resolve(sa);
        activator.getGame().getAction().checkStateEffects(true);
    }

    private void augment(Player p, Card augmentCard, Card host) {
        resolve(abilityWithApi(augmentCard, ApiType.Augment), p, host);
    }

    @Test
    public void testOrdinaryPonyFlickersEachCreatureOncePerTurn() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card pony = addCard("Ordinary Pony", me);
        Card bears = addCard("Grizzly Bears", me);
        SpellAbility flicker = etb(pony);
        flicker.setActivatingPlayer(me);

        AssertJUnit.assertFalse(flicker.canTarget(pony)); // a Horse
        AssertJUnit.assertTrue(flicker.canTarget(bears));
        bears.addCounterInternal(CounterEnumType.P1P1, 1, me, false, null, null);
        resolve(flicker, me, bears);

        Card back = me.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Grizzly Bears")).findFirst().orElse(null);
        AssertJUnit.assertNotNull(back);
        AssertJUnit.assertEquals(0, back.getCounters(CounterEnumType.P1P1)); // a new object
        flicker.setActivatingPlayer(me);
        AssertJUnit.assertFalse(flicker.canTarget(back));

        // flickered again by something else, it's a new object that this ability didn't return
        Card again = game.getAction().moveToPlay(game.getAction().exile(back, null, null), null, null);
        AssertJUnit.assertTrue(flicker.canTarget(again));
    }

    @Test
    public void testRhinoCompletesItsBlockConditionWithTheHostEffect() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card kitten = addCard("Adorable Kitten", me);
        augment(me, addCardToZone("Rhino-", me, ZoneType.Hand), kitten);

        AssertJUnit.assertEquals("Rhino-Kitten", kitten.getName());
        AssertJUnit.assertEquals(2, kitten.getNetPower());
        AssertJUnit.assertEquals(5, kitten.getNetToughness());
        Trigger blocks = null;
        for (Trigger t : kitten.getTriggers()) {
            if (t.getMode() == TriggerType.Blocks) {
                blocks = t;
            }
        }
        AssertJUnit.assertNotNull(blocks);
        AssertJUnit.assertEquals(ApiType.RollDice, blocks.ensureAbility().getApi());
    }

    @Test
    public void testSuccessGivesLifelinkOnlyToHostsAndAugmentedCreatures() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card kitten = addCard("Adorable Kitten", me);
        Card bears = addCard("Grizzly Bears", me);
        Card augmented = addCard("Adorable Kitten", me);
        augment(me, addCardToZone("Half-Kitten, Half-", me, ZoneType.Hand), augmented);

        resolve(addCardToZone("Success!", me, ZoneType.Hand).getFirstSpellAbility(), me, kitten);
        resolve(addCardToZone("Success!", me, ZoneType.Hand).getFirstSpellAbility(), me, bears);
        resolve(addCardToZone("Success!", me, ZoneType.Hand).getFirstSpellAbility(), me, augmented);

        AssertJUnit.assertEquals(3, kitten.getNetPower());
        AssertJUnit.assertTrue(kitten.hasKeyword(Keyword.LIFELINK));
        AssertJUnit.assertEquals(4, bears.getNetPower());
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.LIFELINK));
        AssertJUnit.assertTrue(augmented.hasKeyword(Keyword.LIFELINK));
    }

    @Test
    public void testTeachersPetCombinesAnAugmentFromTheLibrary() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card kitten = addCard("Adorable Kitten", me);
        fillLibrary(me, 5);
        Card rhino = addCardToZone("Rhino-", me, ZoneType.Library);
        Card pet = addCard("Teacher's Pet", me);
        SpellAbility sa = abilityWithApi(pet, ApiType.Augment);

        AssertJUnit.assertFalse(sa.canTarget(addCard("Grizzly Bears", me))); // not a host
        resolve(sa, me, kitten);

        AssertJUnit.assertEquals("Rhino-Kitten", kitten.getName());
        AssertJUnit.assertTrue(rhino.isInZone(ZoneType.Merged));
        AssertJUnit.assertEquals(5, me.getCardsIn(ZoneType.Library).size());
    }

    @Test
    public void testSacrificePlayFavorsTheExtremes() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card small = addCard("Llanowar Elves", opp);
        Card mid1 = addCard("Grizzly Bears", opp);
        Card mid2 = addCard("Hill Giant", opp);
        Card big = addCard("Serra Angel", opp);
        addCard("Craw Wurm", opp); // not attacking - never chosen

        Map<String, Integer> picks = new HashMap<>();
        for (int i = 0; i < 400; i++) {
            game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp);
            Combat combat = new Combat(opp);
            for (Card c : new Card[] {small, mid1, mid2, big}) {
                combat.addAttacker(c, me);
            }
            game.getPhaseHandler().setCombat(combat);

            SpellAbility sa = addCardToZone("Sacrifice Play", me, ZoneType.Hand).getFirstSpellAbility();
            sa.setActivatingPlayer(me);
            sa.resetTargets();
            sa.getTargets().add(opp);
            AbilityUtils.resolve(sa);
            Card chosen = null;
            for (Card c : new Card[] {small, mid1, mid2, big}) {
                if (!c.isInZone(ZoneType.Battlefield)) {
                    chosen = c;
                }
            }
            AssertJUnit.assertNotNull(chosen);
            picks.merge(chosen.getName(), 1, Integer::sum);
            // put it back for the next trial
            game.getAction().moveToPlay(chosen, opp, null, null);
            small = refresh(opp, small);
            mid1 = refresh(opp, mid1);
            mid2 = refresh(opp, mid2);
            big = refresh(opp, big);
        }
        // 40% / 40% / 20% split between the two middles: generous bounds for 400 trials
        int s = picks.getOrDefault("Llanowar Elves", 0), b = picks.getOrDefault("Serra Angel", 0);
        int m = 400 - s - b;
        AssertJUnit.assertTrue("smallest " + s, s > 120 && s < 200);
        AssertJUnit.assertTrue("biggest " + b, b > 120 && b < 200);
        AssertJUnit.assertTrue("middle " + m, m > 40 && m < 120);
    }

    private static Card refresh(Player p, Card c) {
        return p.getCardsIn(ZoneType.Battlefield).stream().filter(x -> x.getName().equals(c.getName())).findFirst().orElse(c);
    }

    @Test
    public void testSideQuestRemovesTheCreatureUntilYourNextTurn() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(me, 10);
        fillLibrary(opp, 10);
        Card bears = addCard("Grizzly Bears", me);

        resolve(addCardToZone("Side Quest", me, ZoneType.Hand).getFirstSpellAbility(), me, bears);
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Battlefield).stream().noneMatch(c -> c.getName().equals("Grizzly Bears")));
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Exile).isEmpty());
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.None).size());

        playUntilNextTurn(game); // opponent's turn: still gone
        AssertJUnit.assertTrue(game.getPhaseHandler().isPlayerTurn(opp));
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Battlefield).stream().noneMatch(c -> c.getName().equals("Grizzly Bears")));

        playUntilNextTurn(game); // my turn: back, then two counters in the upkeep
        AssertJUnit.assertTrue(game.getPhaseHandler().isPlayerTurn(me));
        playUntilPhase(game, PhaseType.DRAW);
        Card back = refresh(me, bears);
        AssertJUnit.assertTrue(back.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(2, back.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.None).isEmpty());
    }
}
