package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mystery Booster 2 playtest cards (MB2), part 7, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2Playtest7Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Game game, final Player p, final Card spell) {
        cast(game, p, spell, spell.getFirstSpellAbility());
    }

    private void cast(final Game game, final Player p, final Card spell, final SpellAbility sa) {
        sa.setActivatingPlayer(p);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    private void library(final Player p, final String name, final int n) {
        for (int i = 0; i < n; i++) {
            addCardToZone(name, p, ZoneType.Library);
        }
    }

    private int count(final Player p, final ZoneType zone, final String name) {
        return CardLists.count(p.getCardsIn(zone), c -> c.getName().equals(name));
    }

    @Test
    public void testLichsDuelMastery() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Swamp", 10);
        final Card lich = addCardToZone("Lich's Duel Mastery", ai, ZoneType.Hand);
        cast(game, ai, lich);
        AssertJUnit.assertEquals("five shields", 5, ai.getCardsIn(ZoneType.Exile).size());
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Exile).stream().allMatch(Card::isFaceDown));
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        ai.loseLife(7, false, false, null);
        resolveAll(game);
        AssertJUnit.assertEquals("no life lost", 20, ai.getLife());
        AssertJUnit.assertEquals("a shield instead", hand + 1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(4, ai.getCardsIn(ZoneType.Exile).size());
        for (final Card shield : new forge.game.card.CardCollection(ai.getCardsIn(ZoneType.Exile))) {
            game.getAction().moveToGraveyard(shield, null);
        }
        ai.loseLife(1, false, false, null);
        resolveAll(game);
        AssertJUnit.assertFalse("sacrificed", game.getCardState(lich).isInPlay());
        AssertJUnit.assertTrue("and you lose", ai.hasLost());
    }

    @Test
    public void testLifeningElementalSplice() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card elemental = addCardToZone("Lifening Elemental", ai, ZoneType.Hand);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final SpellAbility sa = bolt.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(opp);
        final Card onStack = game.getAction().moveToStack(bolt, sa);
        sa.setHostCard(onStack);
        AbilityUtils.addSpliceEffect(sa, elemental);
        AssertJUnit.assertTrue(onStack.hasKeyword(Keyword.LIFELINK));
        AssertJUnit.assertTrue("still an instant", onStack.isInstant() && !onStack.isCreature());
        game.getStack().add(sa);
        resolveAll(game);
        AssertJUnit.assertEquals(17, opp.getLife());
        AssertJUnit.assertEquals("lifelink", 23, ai.getLife());
        AssertJUnit.assertTrue("the spliced card stays in hand", elemental.isInZone(ZoneType.Hand));
        final Card inYard = game.getCardState(bolt);
        AssertJUnit.assertFalse("only while it was a spell", inYard.hasKeyword(Keyword.LIFELINK));
    }

    @Test
    public void testManyDeedsOfBelzenlok() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        // the AI exiles from an opponent's graveyard, as it does with graveyard hate
        final Card benalia = addCardToZone("History of Benalia", game.getPlayers().get(0), ZoneType.Graveyard);
        cast(game, ai, addCardToZone("The Many Deeds of Belzenlok", ai, ZoneType.Hand));
        AssertJUnit.assertTrue("exiled", game.getCardState(benalia).isInZone(ZoneType.Exile));
        AssertJUnit.assertEquals("its chapter I, copied", 1, CardLists.count(ai.getCardsIn(ZoneType.Battlefield),
                c -> c.isToken() && c.getType().hasCreatureType("Knight")));
    }

    @Test
    public void testTooferCardAdvantage() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Island", 6);
        addCard("Toofer, Keeper of the Full Grip", ai);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        bears.setCastFrom(ai.getZone(ZoneType.Hand));
        cast(game, ai, bears);
        AssertJUnit.assertEquals("a creature from hand is no card advantage", 20, opp.getLife());
        final Card divination = addCardToZone("Divination", ai, ZoneType.Hand);
        divination.setCastFrom(ai.getZone(ZoneType.Hand));
        cast(game, ai, divination);
        AssertJUnit.assertEquals("one card up", 18, opp.getLife());
    }

    @Test
    public void testYawgmothsDayPlanner() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card planner = addCard("Yawgmoth's Day Planner", ai);
        final Card forest = addCard("Forest", ai);
        final Card reef = addCard("Shivan Reef", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(AbilityManaPart.causesLifeLoss(planner.getManaAbilities().getFirst()));
        AssertJUnit.assertFalse(AbilityManaPart.causesLifeLoss(forest.getManaAbilities().getFirst()));
        int pained = 0;
        for (final SpellAbility ma : reef.getManaAbilities()) {
            pained += AbilityManaPart.causesLifeLoss(ma) ? 1 : 0;
        }
        AssertJUnit.assertEquals("the colored ability deals damage to you, the colorless one doesn't", 1, pained);
        final Card bears = addCard("Grizzly Bears", ai);
        game.getAction().destroy(bears, null, false, null);
        resolveAll(game);
        AssertJUnit.assertTrue("exiled instead", game.getCardState(bears).isInZone(ZoneType.Exile));
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);
        final SpellAbility fromYard = bolt.getAllPossibleAbilities(ai, true).stream().filter(SpellAbility::isSpell).findFirst().orElse(null);
        AssertJUnit.assertNotNull("castable from the graveyard", fromYard);
        AssertJUnit.assertNotNull(fromYard.getMayPlay());
        AssertJUnit.assertFalse(forest.getManaAbilities().getFirst().getManaPart().meetsManaRestrictions(fromYard));
        AssertJUnit.assertTrue(planner.getManaAbilities().getFirst().getManaPart().meetsManaRestrictions(fromYard));
    }

    @Test
    public void testYouCompleatMe() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        cast(game, ai, addCardToZone("You Compleat Me", ai, ZoneType.Hand));
        AssertJUnit.assertEquals(10, ai.getLife());
        ai.gainLife(5, null, null);
        AssertJUnit.assertEquals("never above 10", 10, ai.getLife());
        ai.loseLife(2, false, false, null);
        ai.gainLife(4, null, null);
        AssertJUnit.assertEquals(10, ai.getLife());
        AssertJUnit.assertEquals("the emblem", 1, count(ai, ZoneType.Command, "Emblem — You Compleat Me"));
    }

    @Test
    public void testBoltfireFlashforward() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Mountain", 3);
        final Card boltfire = addCardToZone("Boltfire", ai, ZoneType.Exile);
        final SpellAbility forward = boltfire.getAllPossibleAbilities(ai, true).stream()
                .filter(s -> s.isAlternativeCost(forge.game.spellability.AlternativeCost.Flashforward)).findFirst().orElse(null);
        AssertJUnit.assertNotNull("castable from exile", forward);
        forward.getTargets().add(opp);
        cast(game, ai, boltfire, forward);
        AssertJUnit.assertEquals(18, opp.getLife());
        final Card now = game.getCardState(boltfire);
        AssertJUnit.assertTrue("bottom of the library: " + now.getZone(), now.isInZone(ZoneType.Library));
        AssertJUnit.assertEquals(now, ai.getCardsIn(ZoneType.Library).getLast());
        final Card inHand = addCardToZone("Boltfire", ai, ZoneType.Hand);
        AssertJUnit.assertTrue("not from hand", inHand.getAllPossibleAbilities(ai, true).stream()
                .noneMatch(s -> s.isAlternativeCost(forge.game.spellability.AlternativeCost.Flashforward)));
    }

    @Test
    public void testToddlersRageTantrum() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card elves = addCard("Llanowar Elves", opp);
        elves.setSickness(false);
        final Card giant = addCard("Hill Giant", ai);
        final Card rage = addCard("Toddler's Rage", ai);
        rage.attachToEntity(giant, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(5, giant.getNetPower());
        AssertJUnit.assertTrue(giant.hasKeyword(Keyword.TANTRUM));
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false);
        final Combat combat = new Combat(opp);
        combat.addAttacker(elves, ai);
        combat.addBlocker(elves, giant);
        combat.setBlocked(elves, true);
        combat.orderBlockersForDamageAssignment();
        combat.orderAttackersForDamageAssignment();
        game.getPhaseHandler().setCombat(combat);
        combat.assignCombatDamage(false);
        combat.dealAssignedDamage();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(game.getCardState(elves).isInPlay());
        AssertJUnit.assertEquals("the excess to the attacking player", 16, opp.getLife());
    }
}
