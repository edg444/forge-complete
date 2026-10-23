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

/** Innistrad script review (2026-10-23). */
public class InnistradAuditTest extends AITest {

    @Test
    public void testStolenMirrorMadPhantasmRevealsFromItsOwnersLibrary() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        for (int i = 0; i < 4; i++) {
            addCardToZone("Island", p, ZoneType.Library);
            addCardToZone("Swamp", opp, ZoneType.Library);
        }
        Card phantasm = addCard("Mirror-Mad Phantasm", opp);
        SpellAbility steal = AbilityFactory.getAbility("DB$ GainControl | Defined$ Self | NewController$ You", phantasm);
        steal.setActivatingPlayer(p);
        AbilityUtils.resolve(steal);
        game.getAction().checkStateEffects(true);

        for (SpellAbility sa : phantasm.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZone) {
                sa.setActivatingPlayer(p);
                AbilityUtils.resolve(sa);
                break;
            }
        }
        AssertJUnit.assertTrue(opp.getZone(ZoneType.Battlefield).contains(c -> c.getName().equals("Mirror-Mad Phantasm")));
        AssertJUnit.assertEquals(4, p.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(0, p.getCardsIn(ZoneType.Graveyard).size());
    }
}
