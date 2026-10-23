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

/** R&D's Secret Lair: pre-scry "look at the top card, you may bottom it" printings (2026-10-23). */
public class DarksteelAuditTest extends AITest {

    private ApiType lookAbility(final String name, final String set, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), p);
        p.getZone(ZoneType.Battlefield).add(c);
        game.getAction().checkStateEffects(true);
        ApiType found = null;
        for (SpellAbility sa : c.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if ((sa.getApi() == ApiType.Scry || sa.getApi() == ApiType.Dig)
                    && sa.getRestrictions().checkOtherRestrictions(c, sa, p)) {
                AssertJUnit.assertNull(found);
                found = sa.getApi();
            }
        }
        return found;
    }

    @Test
    public void testDarksteelPendant() {
        AssertJUnit.assertEquals(ApiType.Scry, lookAbility("Darksteel Pendant", "DST", false));
        AssertJUnit.assertEquals(ApiType.Dig, lookAbility("Darksteel Pendant", "DST", true));
    }

    @Test
    public void testSoldeviExcavations() {
        AssertJUnit.assertEquals(ApiType.Scry, lookAbility("Soldevi Excavations", "ALL", false));
        AssertJUnit.assertEquals(ApiType.Dig, lookAbility("Soldevi Excavations", "ALL", true));
    }
}
