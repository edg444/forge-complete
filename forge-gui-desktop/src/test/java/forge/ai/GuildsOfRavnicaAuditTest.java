package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Guilds of Ravnica review (2026-10-23). */
public class GuildsOfRavnicaAuditTest extends AITest {

    @Test
    public void testTorchCourierTargetsAnotherCreature() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card c = addCard("Torch Courier", p);
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Pump) {
                AssertJUnit.assertEquals("Creature.Other", sa.getTargetRestrictions().getValidTgts()[0]);
                return;
            }
        }
        AssertJUnit.fail("no pump ability");
    }

    /** The Invert half's castable spell: the one whose restrictions pass. */
    private SpellAbility invert(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Invert // Invent", "GRN"), p);
        p.getZone(ZoneType.Hand).add(c);
        game.getAction().checkStateEffects(true);
        SpellAbility found = null;
        for (SpellAbility sa : c.getState(CardStateName.LeftSplit).getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.getRestrictions().checkOtherRestrictions(c, sa, p)) {
                AssertJUnit.assertNull("only one version should be active", found);
                found = sa;
            }
        }
        return found;
    }

    @Test
    public void testInvertPrintedSwitchLastsIndefinitely() {
        AssertJUnit.assertFalse(invert(false).hasParam("Duration"));
        AssertJUnit.assertEquals("Permanent", invert(true).getParam("Duration"));
    }
}
