package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.CostExile;
import forge.game.cost.CostPart;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * "Rumors of My Death . . ." (Unstable): "{3}{B}, Exile a permanent you control with a League of Dastardly Doom
 * watermark: Return a permanent card with a League of Dastardly Doom watermark from your graveyard to the
 * battlefield." - Oracle text checked against Scryfall 2026-10-01 (no rulings, no FAQ entry; the FAQ's watermark
 * section counts Contraption faction symbols as watermarks).
 */
public class RumorsOfMyDeathTest extends AITest {

    private static SpellAbility ability(Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZone) {
                return sa;
            }
        }
        return null;
    }

    // watermarks belong to the printing: Hoisted Hireling's Unsanctioned reprint has none, its Unstable one does
    private Card unstable(String name, Player p, ZoneType zone) {
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, "UST"), p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(zone).add(c);
        return c;
    }

    private Card setUp(Game game, Player ai, String onBattlefield, String inGraveyard) {
        for (int i = 0; i < 4; i++) {
            addCard("Swamp", ai);
        }
        Card rumors = unstable("\"Rumors of My Death . . .\"", ai, ZoneType.Battlefield);
        if (onBattlefield != null) {
            unstable(onBattlefield, ai, ZoneType.Battlefield);
        }
        if ("Grizzly Bears".equals(inGraveyard)) {
            addCardToZone(inGraveyard, ai, ZoneType.Graveyard);
        } else {
            unstable(inGraveyard, ai, ZoneType.Graveyard);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        return rumors;
    }

    @Test
    public void testTextReadsLikeOracle() {
        Game game = initAndCreateGame();
        Card rumors = addCard("\"Rumors of My Death . . .\"", game.getPlayers().get(1));
        AssertJUnit.assertEquals("{3}{B}, Exile a permanent you control with a League of Dastardly Doom watermark: "
                + "Return a permanent card with a League of Dastardly Doom watermark from your graveyard to the "
                + "battlefield.", rumors.getAbilityText().trim());
    }

    @Test
    public void testOnlyLeagueCardsCount() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        // Grizzly Bears has no watermark: nothing to return
        Card rumors = setUp(game, ai, "Hangman", "Grizzly Bears");
        SpellAbility sa = ability(rumors);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
    }

    @Test
    public void testAiTradesUp() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card rumors = setUp(game, ai, "Hangman", "Hoisted Hireling");
        SpellAbility sa = ability(rumors);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(sa.canPlay());
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));

        // it pays with Hangman, not with itself (both have the watermark)
        CostExile exile = null;
        for (CostPart part : sa.getPayCosts().getCostParts()) {
            if (part instanceof CostExile ce) {
                exile = ce;
            }
        }
        CardCollection paid = ComputerUtil.chooseExileFrom(ai, exile, rumors, 1, sa, false);
        AssertJUnit.assertEquals("Hangman", paid.getFirst().getName());

        // with only itself to exile, a returning creature is still worth more than this enchantment's next use
        game.getAction().exile(paid.getFirst(), null, null);
        AssertJUnit.assertEquals(rumors, ComputerUtil.chooseExileFrom(ai, exile, rumors, 1, sa, false).getFirst());
    }

    @Test
    public void testAiDoesNotTradeDown() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card rumors = setUp(game, ai, "Hoisted Hireling", "Hangman");
        // it would have to exile Hoisted Hireling (keeping itself) to get back the smaller Hangman
        SpellAbility sa = ability(rumors);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        // and not before main 2
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card rumors2 = setUp(game, ai, "Hangman", "Hoisted Hireling");
        SpellAbility early = ability(rumors2);
        early.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(early));
    }
}
