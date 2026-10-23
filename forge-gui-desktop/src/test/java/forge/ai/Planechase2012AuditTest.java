package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Planechase 2012 R&D's Secret Lair printed text (2026-10-23). */
public class Planechase2012AuditTest extends AITest {

    /** Returns the minimum number of targets of the Mass Mutiny mode that can be cast. */
    private int massMutinyMinTargets(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        addCard("Grizzly Bears", opp);
        Card mutiny = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Mass Mutiny", "PC2"), p);
        p.getZone(ZoneType.Hand).add(mutiny);
        game.getAction().checkStateEffects(true);
        for (SpellAbility sa : mutiny.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.getRestrictions().checkOtherRestrictions(mutiny, sa, p)) {
                return sa.getTargetRestrictions().getMinTargets(mutiny, sa);
            }
        }
        return -1;
    }

    @Test
    public void testMassMutinyTargetsUpToOne() {
        AssertJUnit.assertEquals(0, massMutinyMinTargets(false));
    }

    @Test
    public void testPrintedMassMutinyMustTargetEachOpponentsCreature() {
        AssertJUnit.assertEquals(1, massMutinyMinTargets(true));
    }
}
