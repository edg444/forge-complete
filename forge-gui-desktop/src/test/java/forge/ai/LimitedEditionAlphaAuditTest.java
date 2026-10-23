package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Fixes from the Limited Edition Alpha script review (2026-10-23). */
public class LimitedEditionAlphaAuditTest extends AITest {

    private static SpellAbility ability(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    private void assertOnlyOnOwnUpkeep(Game game, Card c, SpellAbility sa, Player owner, Player opp) {
        sa.setActivatingPlayer(owner);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, owner);
        AssertJUnit.assertTrue(c.getName() + " on its controller's upkeep", sa.getRestrictions().canPlay(c, sa));
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, opp);
        AssertJUnit.assertFalse(c.getName() + " on an opponent's upkeep", sa.getRestrictions().canPlay(c, sa));
    }

    @Test
    public void testRockHydraCounterOnlyDuringYourUpkeep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card hydra = addCard("Rock Hydra", p);
        assertOnlyOnOwnUpkeep(game, hydra, ability(hydra, ApiType.PutCounter), p, opp);
    }

    @Test
    public void testCyclopeanTombOnlyDuringYourUpkeep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card tomb = addCard("Cyclopean Tomb", p);
        assertOnlyOnOwnUpkeep(game, tomb, ability(tomb, ApiType.PutCounter), p, opp);
    }

    @Test
    public void testChaosOrbDoesNothingOnceItHasLeft() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card orb = addCard("Chaos Orb", p);
        addCards("Grizzly Bears", 4, opp);
        addCards("Plains", 4, opp);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = ability(orb, ApiType.FlipOntoBattlefield);
        sa.setActivatingPlayer(p);
        // destroyed in response: "If this artifact is on the battlefield" no longer holds
        game.getAction().moveToGraveyard(orb, null);
        for (int i = 0; i < 10; i++) {
            AbilityUtils.resolve(sa);
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(8, opp.getCardsIn(ZoneType.Battlefield).size());
    }

    @Test
    public void testAiPlaysMeekstoneAgainstBigCreatures() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card stone = addCardToZone("Meekstone", p, ZoneType.Hand);
        addCard("Hill Giant", opp);
        addCard("Grizzly Bears", p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = stone.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ComputerUtilCard.checkNeedsToPlayReqs(stone, sa));
        addCard("Craw Wurm", p);
        AssertJUnit.assertEquals(AiPlayDecision.NeedsToPlayCriteriaNotMet, ComputerUtilCard.checkNeedsToPlayReqs(stone, sa));
    }
}
