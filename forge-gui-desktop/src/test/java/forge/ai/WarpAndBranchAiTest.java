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
 * AI fixes from an AI vs AI game, 2026-10-24: Starwinder was warped with no creatures to attack (so it was exiled
 * having done nothing), and Thought-Stalker Warlock's enters trigger was dropped because Branch never chose its
 * target opponent.
 */
public class WarpAndBranchAiTest extends AITest {

    private static SpellAbility warp(Card c, Player p) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isWarp()) {
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        return null;
    }

    private static AiPlayDecision decide(Player ai, SpellAbility sa) {
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    @Test
    public void starwinderIsNotWarpedWithNoAttackers() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Island", 4, ai);
        Card starwinder = addCardToZone("Starwinder", ai, ZoneType.Hand);
        // with no creatures, PlayMain1 holds it for Main 2, where it used to be warped after combat
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ai, warp(starwinder, ai)));
    }

    @Test
    public void starwinderIsWarpedAlongsideAnAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Island", 4, ai);
        Card bear = addCard("Runeclaw Bear", ai);
        bear.setSickness(false);
        Card starwinder = addCardToZone("Starwinder", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, warp(starwinder, ai)));
    }

    @Test
    public void enterTriggerStillWarpsWithNoCreatures() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Plains", 2, ai);
        Card knight = addCardToZone("Knight Luminary", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ai, warp(knight, ai)));
    }

    @Test
    public void thoughtStalkerWarlockTargetsAnOpponent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Swamp", 3, ai);
        Card warlock = addCardToZone("Thought-Stalker Warlock", ai, ZoneType.Hand);
        addCardToZone("Runeclaw Bear", opp, ZoneType.Hand);
        addCardToZone("Island", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility cast = warlock.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, cast, null));
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());

        AssertJUnit.assertEquals(1, countCardsWithName(game, "Thought-Stalker Warlock", ZoneType.Battlefield));
        AssertJUnit.assertEquals("the opponent discarded a card", 1, opp.getCardsIn(ZoneType.Hand).size());
    }
}
