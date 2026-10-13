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
}
