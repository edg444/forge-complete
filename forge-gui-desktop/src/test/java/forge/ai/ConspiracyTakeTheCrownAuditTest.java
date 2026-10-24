package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCopyService;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Conspiracy: Take the Crown R&D's Secret Lair printed text (2026-10-23). */
public class ConspiracyTakeTheCrownAuditTest extends AITest {

    private static boolean hasChooseCard(final Iterable<SpellAbility> abilities) {
        for (SpellAbility sa : abilities) {
            if (sa.getApi() == ApiType.ChooseCard) {
                return true;
            }
        }
        return false;
    }

    /** Activates the Chimera into a Grizzly Bears; returns it. */
    private Card chimera(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card chimera = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Volatile Chimera", "CN2"), p);
        p.getZone(ZoneType.Battlefield).add(chimera);
        Card bears = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Grizzly Bears"), p);
        p.getZone(ZoneType.Exile).add(bears);
        p.addNoteForName("VolatileChimera", "Id:" + bears.getId());
        game.getAction().checkStateEffects(true);
        SpellAbility live = null;
        for (SpellAbility sa : chimera.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.getApi() == ApiType.ChooseCard && sa.getRestrictions().checkOtherRestrictions(chimera, sa, p)) {
                AssertJUnit.assertNull("only one version should be live", live);
                live = sa;
            }
        }
        AbilityUtils.resolve(live);
        AssertJUnit.assertEquals("Grizzly Bears", chimera.getName());
        AssertJUnit.assertTrue(hasChooseCard(chimera.getSpellAbilities()));
        return chimera;
    }

    /** A copy of the Chimera gets only its copiable values. */
    private static boolean copyHasAbility(final Card c) {
        return hasChooseCard(CardCopyService.copyStats(c, c.getController(), true).getSpellAbilities());
    }

    @Test
    public void testOracleAbilityIsPartOfTheCopy() {
        AssertJUnit.assertTrue(copyHasAbility(chimera(false)));
    }

    @Test
    public void testPrintedAbilityIsGainedAfterTheCopy() {
        AssertJUnit.assertFalse(copyHasAbility(chimera(true)));
    }
}
