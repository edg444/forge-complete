package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * An instant-speed sweeper (Perilous Vault: "{5}, {T}, Exile this artifact: Exile all nonland permanents.") waits
 * until after combat on the AI's own turn. Only Main 1 used to be held back, so the AI exiled its own attackers
 * in upkeep of an extra turn.
 */
public class PerilousVaultTimingTest extends AITest {

    private static SpellAbility vaultAbility(Card vault) {
        for (SpellAbility sa : vault.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZoneAll) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testWaitsForMain2OnOwnTurn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card vault = addCard("Perilous Vault", ai);
        addCards("Island", 5, ai);
        addCards("Atmosphere Surgeon", 2, ai);
        addCard("Boneclad Necromancer", opp);
        addCard("Ugin's Conjurant", opp);
        addCard("Ravine Raider", opp);
        addCard("Obsessive Pursuit", opp);
        addCards("Serra Angel", 2, opp);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = vaultAbility(vault);
        sa.setActivatingPlayer(ai);
        for (PhaseType phase : new PhaseType[] {PhaseType.UPKEEP, PhaseType.DRAW, PhaseType.MAIN1,
                PhaseType.COMBAT_BEGIN, PhaseType.COMBAT_DECLARE_ATTACKERS}) {
            game.getPhaseHandler().devModeSet(phase, ai);
            AssertJUnit.assertEquals(phase.toString(), AiPlayDecision.WaitForMain2,
                    SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).decision());
        }

        // the opponent's board is still worth more, so after combat (or on their turn) it's a go;
        // the final roll is random, so allow a few tries
        for (PhaseType phase : new PhaseType[] {PhaseType.MAIN2, PhaseType.UPKEEP}) {
            game.getPhaseHandler().devModeSet(phase, phase == PhaseType.MAIN2 ? ai : opp);
            boolean willing = false;
            for (int i = 0; i < 20 && !willing; i++) {
                willing = SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay();
            }
            AssertJUnit.assertTrue(phase.toString(), willing);
        }
    }
}
