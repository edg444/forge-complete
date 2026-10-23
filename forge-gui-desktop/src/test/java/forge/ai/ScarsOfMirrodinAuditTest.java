package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Scars of Mirrodin script review (2026-10-23). */
public class ScarsOfMirrodinAuditTest extends AITest {

    @Test
    public void testStolenArgentSphinxReturnsUnderYourControl() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        Card sphinx = addCard("Argent Sphinx", opp);
        for (int i = 0; i < 3; i++) {
            addCard("Ornithopter", p);
        }
        SpellAbility steal = AbilityFactory.getAbility("DB$ GainControl | Defined$ Self | NewController$ You", sphinx);
        steal.setActivatingPlayer(p);
        AbilityUtils.resolve(steal);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(p, sphinx.getController());

        for (SpellAbility sa : sphinx.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZone) {
                sa.setActivatingPlayer(p);
                AbilityUtils.resolve(sa);
                break;
            }
        }
        AssertJUnit.assertTrue(opp.getZone(ZoneType.Exile).contains(c -> c.getName().equals("Argent Sphinx")));
        playUntilPhase(game, PhaseType.CLEANUP);
        AssertJUnit.assertTrue(p.getZone(ZoneType.Battlefield).contains(c -> c.getName().equals("Argent Sphinx")));
    }
}
