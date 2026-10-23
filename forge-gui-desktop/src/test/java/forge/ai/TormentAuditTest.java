package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Torment R&D's Secret Lair printed text (2026-10-23). */
public class TormentAuditTest extends AITest {

    @Test
    public void testBalthorPumpsAndTargetsItselfAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card balthor = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Balthor the Stout", "TOR"), p);
        p.getZone(ZoneType.Battlefield).add(balthor);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, balthor.getNetPower());
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, balthor.getNetPower());
        boolean canTargetSelf = false;
        for (SpellAbility sa : balthor.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.usesTargeting() && sa.getRestrictions().checkOtherRestrictions(balthor, sa, p)
                    && sa.canTarget(balthor)) {
                canTargetSelf = true;
            }
        }
        AssertJUnit.assertTrue(canTargetSelf);
    }
}
