package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mystery Booster 2 playtest cards (MB2), part 11, checked against Scryfall 2026-10-23 with their rulings.
 */
public class MysteryBooster2Playtest11Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Game game, final Player p, final Card spell, final SpellAbility sa) {
        sa.setActivatingPlayer(p);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    @Test
    public void testPanglacialShinobi() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 2; i++) {
            addCard("Island", ai);
        }
        final Card shinobi = addCardToZone("Panglacial Shinobi", ai, ZoneType.Library);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
        final SpellAbility ninjutsu = shinobi.getSpellAbilities().getFirst();
        ninjutsu.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("only while searching", ninjutsu.canPlay());

        final Card lions = addCard("Savannah Lions", ai);
        lions.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false);
        final Combat combat = new Combat(ai);
        combat.addAttacker(lions, opp);
        combat.setBlocked(lions, false);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);

        final Card growth = addCardToZone("Rampant Growth", ai, ZoneType.Hand);
        cast(game, ai, growth, growth.getFirstSpellAbility());
        final Card ninja = game.getCardState(shinobi);
        AssertJUnit.assertTrue("put onto the battlefield: " + ninja.getZone(), ninja.isInPlay());
        AssertJUnit.assertTrue(ninja.isTapped());
        AssertJUnit.assertTrue("attacking", combat.isAttacking(ninja));
        AssertJUnit.assertEquals(opp, combat.getDefenderByAttacker(ninja));
        AssertJUnit.assertTrue("the attacker was shuffled into the library", game.getCardState(lions).isInZone(ZoneType.Library));
    }

    @Test
    public void testThereTheyreTheir() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card card = addCardToZone("There // They're // Their", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("three thirds combine while not on the stack", 6, card.getCMC());
        SpellAbility their = null;
        for (final SpellAbility sa : card.getAllPossibleAbilities(ai, true)) {
            if (sa.getDescription().contains("gains control")) {
                their = sa;
            }
        }
        AssertJUnit.assertNotNull(their);
        their.setActivatingPlayer(ai);
        their.getTargets().add(opp);
        their.getSubAbility().getTargets().add(bears);
        cast(game, ai, card, their);
        AssertJUnit.assertEquals(opp, game.getCardState(bears).getController());
        AssertJUnit.assertTrue(game.getCardState(card).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testBlurryVisionary() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Library);
        addCardToZone("Delver of Secrets", ai, ZoneType.Library);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
        final Card visionary = addCardToZone("Blurry Visionary", ai, ZoneType.Hand);
        cast(game, ai, visionary, visionary.getFirstSpellAbility());
        AssertJUnit.assertEquals("the other card ceased to exist", 3, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
        final Card sleeved = ai.getCardsIn(ZoneType.Hand).getFirst();
        AssertJUnit.assertEquals("Lightning Bolt", sleeved.getName());
        AssertJUnit.assertTrue(sleeved.isModal());
        AssertJUnit.assertFalse("a modal double-faced card, not a transforming one", sleeved.isTransformable());
        AssertJUnit.assertEquals("Delver of Secrets", sleeved.getState(forge.card.CardStateName.Backside).getName());
        SpellAbility delver = null;
        for (final SpellAbility sa : sleeved.getAllPossibleAbilities(ai, false)) {
            if (sa.isSpell() && sa.getCardState() != null && sa.getCardState().getStateName() == forge.card.CardStateName.Backside) {
                delver = sa;
            }
        }
        AssertJUnit.assertNotNull("either face can be cast", delver);
        cast(game, ai, sleeved, delver);
        final Card onField = game.getCardState(sleeved);
        AssertJUnit.assertTrue(onField.isInPlay());
        AssertJUnit.assertEquals("Delver of Secrets", onField.getName());
        AssertJUnit.assertTrue(onField.isCreature());
        game.getAction().destroy(onField, null, false, null);
        final Card inGrave = game.getCardState(sleeved);
        AssertJUnit.assertTrue(inGrave.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("front face up again", "Lightning Bolt", inGrave.getName());
        AssertJUnit.assertTrue("still sleeved for the rest of the game", inGrave.isModal());
        AssertJUnit.assertEquals("Delver of Secrets", inGrave.getState(forge.card.CardStateName.Backside).getName());
    }

    @Test
    public void testStartingTownNPC() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final Card npc = addCard("Starting Town NPC", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.isAdventureCard());
        AssertJUnit.assertEquals("Fetch Herbs", bears.getState(forge.card.CardStateName.Secondary).getName());
        AssertJUnit.assertFalse("creature cards only", bolt.isAdventureCard());
        SpellAbility herbs = null;
        for (final SpellAbility sa : bears.getAllPossibleAbilities(ai, false)) {
            if (sa.isAdventure()) {
                herbs = sa;
            }
        }
        AssertJUnit.assertNotNull(herbs);
        final int life = ai.getLife();
        cast(game, ai, bears, herbs);
        AssertJUnit.assertEquals(life + 2, ai.getLife());
        final Card exiled = game.getCardState(bears);
        AssertJUnit.assertTrue("sent on an Adventure: " + exiled.getZone(), exiled.isInZone(ZoneType.Exile));
        AssertJUnit.assertTrue(exiled.isOnAdventure());

        // the NPC leaving doesn't strand it (rulings)
        game.getAction().destroy(npc, null, false, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(bears).isOnAdventure());
        AssertJUnit.assertFalse("gone from cards in hand with no NPC", game.getCardState(bolt).isAdventureCard());

        final Card npc2 = addCard("Starting Town NPC", ai);
        SpellAbility creature = null;
        for (final SpellAbility sa : game.getCardState(bears).getAllPossibleAbilities(ai, false)) {
            if (sa.isSpell() && !sa.isAdventure()) {
                creature = sa;
            }
        }
        AssertJUnit.assertNotNull("castable from exile", creature);
        cast(game, ai, game.getCardState(bears), creature);
        final Card onField = game.getCardState(bears);
        AssertJUnit.assertTrue(onField.isInPlay());
        AssertJUnit.assertEquals("cast from exile: an additional counter", 1, onField.getCounters(forge.game.card.CounterEnumType.P1P1));
        AssertJUnit.assertNull("no Adventure on the battlefield", onField.getGrantedAdventure());
        AssertJUnit.assertNotNull(npc2);
    }

    @Test
    public void testCleaverBlow() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
            addCardToZone("Grizzly Bears", opp, ZoneType.Library);
        }
        final Card ownBlack = addCard("Vampire Nighthawk", ai);
        final Card oppBlack = addCard("Vampire Nighthawk", opp);
        final Card oppBears = addCard("Grizzly Bears", opp);

        final Card whole = addCardToZone("Cleaver Blow", ai, ZoneType.Hand);
        final SpellAbility base = whole.getFirstSpellAbility();
        base.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("nonblack", base.canTarget(oppBlack));
        AssertJUnit.assertFalse("an opponent controls", base.canTarget(ownBlack));
        AssertJUnit.assertTrue(base.canTarget(oppBears));
        final int aiLife = ai.getLife();
        final int oppLife = opp.getLife();
        base.getTargets().add(oppBears);
        cast(game, ai, whole, base);
        AssertJUnit.assertTrue(game.getCardState(oppBears).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(1, opp.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(aiLife - 2, ai.getLife());
        AssertJUnit.assertEquals(oppLife - 2, opp.getLife());
        final Card spirit = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isToken).getFirst();
        AssertJUnit.assertTrue("tapped", spirit.isTapped());

        // nonblack, an opponent controls, and its controller each, tapped: removed
        final Card cleaved = addCardToZone("Cleaver Blow", ai, ZoneType.Hand);
        final SpellAbility sa = cleaved.getFirstSpellAbility().copy();
        sa.setActivatingPlayer(ai);
        sa.setCleaved(new java.util.TreeSet<>(java.util.List.of(1, 3, 4, 6)));
        AssertJUnit.assertTrue(sa.canTarget(ownBlack));
        sa.getTargets().add(ownBlack);
        cast(game, ai, cleaved, sa);
        AssertJUnit.assertTrue(game.getCardState(ownBlack).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("only you draw", 2, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(1, opp.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals("you still lose 2", aiLife - 4, ai.getLife());
        AssertJUnit.assertEquals(oppLife - 2, opp.getLife());
        AssertJUnit.assertEquals(1, CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && !c.isTapped()));
        AssertJUnit.assertTrue(oppBlack.isInPlay());
    }
}
