package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Dominaria review (2026-10-23). */
public class DominariaAuditTest extends AITest {

    private void fanaticPings(final Card fanatic, final Player target) {
        for (SpellAbility sa : fanatic.getSpellAbilities()) {
            if (sa.getApi() == ApiType.DealDamage) {
                sa.setActivatingPlayer(fanatic.getController());
                sa.getTargets().add(target);
                AbilityUtils.resolve(sa);
                return;
            }
        }
        AssertJUnit.fail("no damage ability");
    }

    @Test
    public void testFlameOfKeldOnlyBoostsYourRedSources() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card flame = addCard("The Flame of Keld", p);
        Card mine = addCard("Mogg Fanatic", p);
        Card theirs = addCard("Mogg Fanatic", opp);
        game.getAction().checkStateEffects(true);
        SpellAbility chapterThree = null;
        for (Trigger t : flame.getTriggers()) {
            if (t.ensureAbility().getApi() == ApiType.Effect) {
                chapterThree = t.ensureAbility();
            }
        }
        chapterThree.setActivatingPlayer(p);
        AbilityUtils.resolve(chapterThree);
        fanaticPings(mine, opp);
        AssertJUnit.assertEquals(17, opp.getLife());
        fanaticPings(theirs, p);
        AssertJUnit.assertEquals(19, p.getLife());
    }

    private SpellAbility teferiPlusOne(final String set, final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Teferi, Hero of Dominaria", set), p);
        p.getZone(ZoneType.Battlefield).add(c);
        c.setCounters(CounterEnumType.LOYALTY, 4);
        game.getAction().checkStateEffects(true);
        SpellAbility found = null;
        for (SpellAbility sa : c.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.getApi() == ApiType.Draw && sa.getRestrictions().checkOtherRestrictions(c, sa, p)) {
                AssertJUnit.assertNull("only one +1 should be active", found);
                found = sa;
            }
        }
        return found;
    }

    private boolean untapsExactly(final SpellAbility plusOne) {
        return plusOne.getSubAbility().getAdditionalAbility("Execute").hasParam("UntapExactly");
    }

    @Test
    public void testTeferiPrintedTextUntapsExactlyTwoLands() {
        AssertJUnit.assertFalse(untapsExactly(teferiPlusOne("DOM", false)));
        AssertJUnit.assertTrue(untapsExactly(teferiPlusOne("DOM", true)));
        AssertJUnit.assertTrue(untapsExactly(teferiPlusOne("PLST", true)));
        AssertJUnit.assertFalse(untapsExactly(teferiPlusOne("SLD", true)));
    }
}
