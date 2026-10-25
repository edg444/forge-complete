package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Bombardment (CMB1 #53), checked against Scryfall and its 6 rulings 2026-10-13: until end of turn, the cards you own
 * that weren't on the battlefield as it resolved are red sorceries named Missile that cost {R} with "Missile deals 2
 * damage to any target"; a Missile stops being one when it changes zones other than by being cast as a Missile, or
 * when its library is shuffled.
 */
public class BombardmentTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    private static SpellAbility castable(Card c, Player p) {
        for (SpellAbility sa : c.getAllPossibleAbilities(p, true)) {
            if (sa.isSpell()) {
                return sa;
            }
        }
        return null;
    }

    private Game bombard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Mountain", 4, ai);
        fillLibrary(ai, 5);
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        addCardToZone("Lightning Bolt", ai, ZoneType.Graveyard);
        Card bomb = addCardToZone("Bombardment", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = bomb.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        resolveAll(game);
        return game;
    }

    @Test
    public void handCardsAreMissilesThatDealTwo() {
        Game game = bombard();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wurm = ai.getCardsIn(ZoneType.Hand).get(0);
        AssertJUnit.assertEquals("Missile", wurm.getName());
        AssertJUnit.assertTrue(wurm.isSorcery());
        AssertJUnit.assertFalse(wurm.isCreature());
        AssertJUnit.assertTrue(wurm.getColor().hasRed());
        AssertJUnit.assertEquals(1, wurm.getCMC());
        AssertJUnit.assertEquals("Missile", ai.getCardsIn(ZoneType.Graveyard).getFirst().getName().equals("Bombardment")
                ? ai.getCardsIn(ZoneType.Graveyard).get(1).getName() : ai.getCardsIn(ZoneType.Graveyard).getFirst().getName());

        SpellAbility missile = castable(wurm, ai);
        AssertJUnit.assertNotNull("a Missile can be cast", missile);
        AssertJUnit.assertTrue(missile.usesTargeting());
        missile.resetTargets();
        missile.getTargets().add(opp);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, missile, null));
        AssertJUnit.assertEquals("still a Missile on the stack", "Missile", game.getCardState(wurm).getName());
        resolveAll(game);
        AssertJUnit.assertEquals(18, opp.getLife());
        Card done = game.getCardState(wurm);
        AssertJUnit.assertTrue(done.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals("left the stack: no longer a Missile", "Craw Wurm", done.getName());
    }

    @Test
    public void afterTheTurnCardsCostWhatTheyDidAndArentHighlighted() {
        Game game = bombard();
        Player ai = game.getPlayers().get(1);
        Card wurm = ai.getCardsIn(ZoneType.Hand).get(0);
        // a card's first spell is resynced to its mana cost when it changes, which used to put a {0} part in front
        AssertJUnit.assertEquals("{R}", castable(wurm, ai).getPayCosts().getTotalMana().toString());
        playUntilNextTurn(game);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        wurm = game.getCardState(wurm);
        AssertJUnit.assertEquals("Craw Wurm", wurm.getName());
        AssertJUnit.assertEquals("{4}{G}{G}", castable(wurm, ai).getPayCosts().getTotalMana().toString());
        // four Mountains, one tapped for Bombardment: the actionable highlight read the {0} and marked the whole hand
        AssertJUnit.assertFalse(AvailableActions.collectActionable(ai, 10000).contains(wurm.getView()));
    }

    @Test
    public void drawnAndShuffledCardsArentMissiles() {
        Game game = bombard();
        Player ai = game.getPlayers().get(1);
        AssertJUnit.assertEquals("Missile", ai.getCardsIn(ZoneType.Library).get(0).getName());
        ai.drawCards(1);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("drawn: a zone change", 1, ai.getCardsIn(ZoneType.Hand).stream().filter(c -> c.getName().equals("Runeclaw Bear")).count());
        ai.shuffle(null);
        game.getAction().checkStateEffects(true);
        for (Card c : ai.getCardsIn(ZoneType.Library)) {
            AssertJUnit.assertEquals("Runeclaw Bear", c.getName());
        }
    }

    @Test
    public void aiBombardsOnlyForLethal() {
        for (int oppLife : new int[] {4, 5}) {
            Game game = initAndCreateGame();
            Player ai = game.getPlayers().get(1);
            Player opp = game.getPlayers().get(0);
            opp.setLife(oppLife, null);
            addCards("Mountain", 3, ai);
            fillLibrary(ai, 5);
            addCardToZone("Craw Wurm", ai, ZoneType.Hand);
            addCardToZone("Craw Wurm", ai, ZoneType.Hand);
            Card bomb = addCardToZone("Bombardment", ai, ZoneType.Hand);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
            game.getAction().checkStateEffects(true);
            SpellAbility sa = bomb.getFirstSpellAbility();
            sa.setActivatingPlayer(ai);
            boolean will = ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa) == AiPlayDecision.WillPlay;
            // two Missiles with two Mountains left over: 4 damage
            AssertJUnit.assertEquals("opponent at " + oppLife, oppLife <= 4, will);
        }
    }

    @Test
    public void legendaryStaysLegendaryAndCopiesOfAMissileAreMissiles() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Mountain", 3, ai);
        addCards("Island", 2, ai);
        fillLibrary(ai, 3);
        Card isamaru = addCardToZone("Isamaru, Hound of Konda", ai, ZoneType.Hand);
        Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        Card twincast = addCardToZone("Twincast", ai, ZoneType.Hand);
        Card bomb = addCardToZone("Bombardment", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = bomb.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        resolveAll(game);

        Card legend = game.getCardState(isamaru);
        AssertJUnit.assertEquals("Missile", legend.getName());
        AssertJUnit.assertTrue("supertypes stay (ruling 4)", legend.getType().isLegendary());
        AssertJUnit.assertTrue("a copiable value (ruling 6)", legend.isCloned());

        SpellAbility missile = castable(game.getCardState(wurm), ai);
        missile.resetTargets();
        missile.getTargets().add(opp);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, missile, null));
        // Twincast is a Missile too; copy the Missile spell with a real copy effect instead
        SpellAbility copy = forge.game.ability.AbilityFactory.getAbility(
                "DB$ CopySpellAbility | Defined$ ValidStack Spell", bomb);
        copy.setActivatingPlayer(ai);
        forge.game.ability.AbilityUtils.resolve(copy);
        resolveAll(game);
        AssertJUnit.assertEquals("the Missile and its copy", 16, opp.getLife());
    }

    @Test
    public void aSpellAlreadyOnTheStackBecomesAMissileAndDoesntResolve() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        fillLibrary(ai, 3);
        Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        Card bomb = addCardToZone("Bombardment", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(ai);
        boltSa.getTargets().add(opp);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, boltSa, null));
        // Bombardment resolving with the Bolt still on the stack (as if cast with flash)
        SpellAbility sa = bomb.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        forge.game.ability.AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("Missile", game.getCardState(bolt).getName());
        resolveAll(game);
        AssertJUnit.assertEquals("it didn't resolve", 20, opp.getLife());
        AssertJUnit.assertTrue(game.getCardState(bolt).isInZone(ZoneType.Graveyard));
    }
}
