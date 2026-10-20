package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Infernius Spawnington III, Esq. (UND #40), checked against Scryfall and its ruling 2026-10-20: only the reveal keyword
 * action counts.
 */
public class InfernusSpawningtonTest extends AITest {

    @Test
    public void costsThreeLessPerCardRevealed() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card esq = addCardToZone("Infernius Spawnington III, Esq.", ai, ZoneType.Hand);
        Card a = addCardToZone("Island", ai, ZoneType.Hand);
        Card b = addCardToZone("Swamp", ai, ZoneType.Hand);
        SpellAbility spell = esq.getFirstSpellAbility();
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(esq, "X", spell));

        game.getAction().reveal(new CardCollection(java.util.List.of(a, b)), ai);
        AssertJUnit.assertEquals(6, AbilityUtils.calculateAmount(esq, "X", spell));
        game.getAction().reveal(new CardCollection(a), ai);
        AssertJUnit.assertEquals("the same card once a turn", 6, AbilityUtils.calculateAmount(esq, "X", spell));
        game.getAction().revealForDisplay(new CardCollection(esq), ai, true, null);
        AssertJUnit.assertEquals("showing isn't revealing", 6, AbilityUtils.calculateAmount(esq, "X", spell));

        ai.onCleanupPhase();
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(esq, "X", spell));
    }

    @Test
    public void imHereDealsThree() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        Card esq = addCardToZone("Infernius Spawnington III, Esq.", ai, ZoneType.Hand);
        game.getAction().moveToPlay(esq, ai, null, null);
        playUntilStackClear(game);
        AssertJUnit.assertEquals(17, opp.getLife());
    }
}
