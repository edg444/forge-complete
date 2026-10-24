package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** New Capenna Commander and Streets of New Capenna review (2026-10-23). */
public class StreetsOfNewCapennaAuditTest extends AITest {

    private String text(final String name) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        return addCard(name, p).getAbilityText();
    }

    @Test
    public void testCostAndCounterText() {
        String halo = text("Halo Fountain");
        AssertJUnit.assertTrue(halo, halo.contains("Untap two tapped creatures you control:"));
        AssertJUnit.assertTrue(halo, halo.contains("Untap fifteen tapped creatures you control:"));
        String brute = text("Brambleback Brute");
        AssertJUnit.assertTrue(brute, brute.contains("Remove a counter from this creature:"));
        String falco = text("Falco Spara, Pactweaver");
        AssertJUnit.assertTrue(falco, falco.contains("Falco Spara enters with a shield counter on it."));
        String depths = text("Dark Depths");
        AssertJUnit.assertTrue(depths, depths.contains("Dark Depths enters with ten ice counters on it."));
    }

    @Test
    public void testFalseFloorTapsSelfAndCreatures() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card floor = addCard("False Floor", p);
        AssertJUnit.assertTrue(floor.getSVar("ETBTapped").contains("Defined$ Self"));
        AssertJUnit.assertTrue(floor.getSVar("ETBTappedCreature").contains("Defined$ ReplacedCard"));
    }

    private ApiType activeApi(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Sanguine Spy", "SNC"), p);
        p.getZone(ZoneType.Battlefield).add(c);
        game.getAction().checkStateEffects(true);
        ApiType found = null;
        for (SpellAbility sa : c.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if ((sa.getApi() == ApiType.Surveil || sa.getApi() == ApiType.Dig)
                    && sa.getRestrictions().checkOtherRestrictions(c, sa, p)) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = sa.getApi();
            }
        }
        AssertJUnit.assertNotNull(found);
        return found;
    }

    @Test
    public void testSanguineSpyPrintedLooks() {
        AssertJUnit.assertEquals(ApiType.Surveil, activeApi(false));
        AssertJUnit.assertEquals(ApiType.Dig, activeApi(true));
    }
}
