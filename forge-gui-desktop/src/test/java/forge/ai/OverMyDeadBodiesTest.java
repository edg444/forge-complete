package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameAction;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardDamageTable;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Over My Dead Bodies (Unstable): "Creature cards in graveyards can attack and block as though they were on the
 * battlefield, can block or be blocked only by creature cards in graveyards, are Zombies in addition to their other
 * types, and have undeathtouch. (If they would deal damage to a creature card, exile that creature card instead.)
 * / Creature cards in your graveyard have haste." - Oracle text, rulings and the Unstable FAQ entry checked
 * 2026-10-01.
 */
public class OverMyDeadBodiesTest extends AITest {

    private static boolean anyTriggerWaiting(Game game) {
        game.getTriggerHandler().runWaitingTriggers();
        return game.getStack().hasSimultaneousStackEntries();
    }

    private Card buried(String name, Player p, Game game) {
        Card c = addCardToZone(name, p, ZoneType.Graveyard);
        // in the graveyard since an earlier turn
        c.setTurnInZone(game.getPhaseHandler().getTurn() - 1);
        return c;
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card omdb = addCard("Over My Dead Bodies", game.getPlayers().get(1));
        AssertJUnit.assertEquals("Creature cards in graveyards can attack and block as though they were on the "
                + "battlefield, can block or be blocked only by creature cards in graveyards, are Zombies in addition "
                + "to their other types, and have undeathtouch. (If they would deal damage to a creature card, exile "
                + "that creature card instead.) | Creature cards in your graveyard have haste.",
                String.join(" | ", omdb.getAbilityText().trim().split("[\\r\\n]+")));
    }

    @Test
    public void testFightingFromTheGraveyard() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Over My Dead Bodies", me);
        addCard("Lord of the Undead", me);
        addCard("Blood Artist", me);
        Card giant = addCard("Hill Giant", opp);
        Card bears = buried("Grizzly Bears", me, game);
        Card theirs = buried("Grizzly Bears", opp, game);
        Card sick = addCardToZone("Hill Giant", opp, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);
        anyTriggerWaiting(game);
        game.getStack().clearSimultaneousStack();

        // in the graveyard: Zombies with undeathtouch, and mine have haste
        AssertJUnit.assertTrue(bears.getType().hasCreatureType("Zombie"));
        AssertJUnit.assertTrue(bears.hasStartOfUnHiddenKeyword("Undeathtouch"));
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.HASTE));
        AssertJUnit.assertFalse(theirs.hasKeyword(Keyword.HASTE));

        // declaring attackers: my graveyard creature can attack, still listed in my graveyard, unnoticed
        CardCollection mine = game.getAction().enlistGraveyardCombatants(me);
        AssertJUnit.assertEquals(1, mine.size());
        AssertJUnit.assertTrue(bears.isInPlay() && GameAction.isAlsoInGraveyard(bears));
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Graveyard).contains(bears));
        AssertJUnit.assertTrue(bears.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(CombatUtil.canAttack(bears, opp));
        AssertJUnit.assertFalse(anyTriggerWaiting(game));
        // a Zombie, so Lord of the Undead pumps it
        AssertJUnit.assertEquals(3, bears.getNetPower());

        Combat combat = new Combat(me);
        combat.addAttacker(bears, opp);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().dismissGraveyardCombatants(CardLists.filter(mine, c -> !combat.isAttacking(c)));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.isInPlay());

        // only graveyard creatures can block it, and they can block nothing else
        AssertJUnit.assertFalse(CombatUtil.canBlock(bears, giant));
        CardCollection blockers = game.getAction().enlistGraveyardCombatants(opp);
        AssertJUnit.assertEquals(2, blockers.size());
        AssertJUnit.assertTrue(CombatUtil.canBlock(bears, theirs));
        AssertJUnit.assertTrue(sick.isSick());
        Card lord = findCardWithName(game, "Lord of the Undead");
        AssertJUnit.assertFalse(CombatUtil.canBlock(lord, theirs));
        combat.addBlocker(bears, theirs);
        game.getAction().dismissGraveyardCombatants(CardLists.filter(blockers, c -> !combat.isBlocking(c)));
        AssertJUnit.assertFalse(sick.isInPlay());
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Graveyard).contains(sick));

        // undeathtouch: the opponent's blocker is exiled instead of being dealt damage
        CardDamageTable damage = new CardDamageTable();
        damage.put(bears, theirs, 3);
        game.getAction().dealDamage(true, damage, new CardDamageTable(), new GameEntityCounterTable(), null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, opp.getCardsIn(ZoneType.Exile).size());
        AssertJUnit.assertEquals(1, opp.getCardsIn(ZoneType.Graveyard).size());

        // Lightning Bolt has no undeathtouch: lethal damage only removes my attacker from combat - it stays in the
        // graveyard, and Blood Artist doesn't see anything die
        SpellAbility bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand).getFirstSpellAbility();
        bolt.setActivatingPlayer(opp);
        bolt.getTargets().add(bears);
        AbilityUtils.resolve(bolt);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(combat.isAttacking(bears));
        AssertJUnit.assertEquals(0, CardLists.filter(game.getCardsIn(ZoneType.Battlefield), GameAction::isAlsoInGraveyard).size());
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertFalse(anyTriggerWaiting(game));
    }

    @Test
    public void testBackToJustDead() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card omdb = addCard("Over My Dead Bodies", me);
        Card bears = buried("Grizzly Bears", me, game);
        Card giant = buried("Hill Giant", me, game);
        game.getAction().checkStateEffects(true);

        CardCollection mine = game.getAction().enlistGraveyardCombatants(me);
        Combat combat = new Combat(me);
        combat.addAttacker(bears, opp);
        combat.addAttacker(giant, opp);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().dismissGraveyardCombatants(CardLists.filter(mine, c -> !combat.isAttacking(c)));

        // Unsummon: it's a creature, so it returns to hand - from the graveyard
        SpellAbility unsummon = addCardToZone("Unsummon", opp, ZoneType.Hand).getFirstSpellAbility();
        unsummon.setActivatingPlayer(opp);
        unsummon.getTargets().add(giant);
        AbilityUtils.resolve(unsummon);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Graveyard).size());

        // Over My Dead Bodies leaves mid-combat: just dead again
        game.getAction().destroy(omdb, null, false, AbilityKey.newMap());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(game.getCardState(bears).isInPlay());
        AssertJUnit.assertEquals(2, me.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(0, CardLists.filter(game.getCardsIn(ZoneType.Battlefield), GameAction::isAlsoInGraveyard).size());
    }

    @Test
    public void testAiAttacksFromItsGraveyard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Over My Dead Bodies", ai);
        buried("Hill Giant", ai, game);
        game.getAction().checkStateEffects(true);

        playUntilPhase(game, PhaseType.COMBAT_DECLARE_BLOCKERS);
        AssertJUnit.assertEquals(1, game.getCombat().getAttackers().size());
        AssertJUnit.assertTrue(GameAction.isAlsoInGraveyard(game.getCombat().getAttackers().get(0)));
        playUntilPhase(game, PhaseType.MAIN2);
        AssertJUnit.assertEquals(17, opp.getLife());
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(0, CardLists.filter(game.getCardsIn(ZoneType.Battlefield), GameAction::isAlsoInGraveyard).size());
    }
}
