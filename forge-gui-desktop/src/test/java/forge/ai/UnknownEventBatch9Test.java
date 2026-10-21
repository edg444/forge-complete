package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardPrintings;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Delphia, Undecided, Gen, Confider of Ages, You, Magic Playtester, You, Iterative Playtester and The Unknown Wizard
 * (UNK mythics), checked against Scryfall 2026-10-21 (no rulings).
 */
public class UnknownEventBatch9Test extends AITest {

    @Test
    public void testPlaytestCards() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card playtest = addCard("Unclaimed Cat", ai);
        final Card normal = addCard("Grizzly Bears", ai);
        AssertJUnit.assertTrue(playtest.isPlaytestCard());
        AssertJUnit.assertFalse(normal.isPlaytestCard());
    }

    @Test
    public void testDelphiaOriginalPrintings() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card delphia = addCard("Delphia, Undecided", ai);
        final Card npH = addCardToZone("Birthing Pod", ai, ZoneType.Hand);
        final Card som = addCardToZone("Memnite", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("NPH", CardPrintings.originalEdition(npH).getCode());
        AssertJUnit.assertTrue(npH.isValid("Card.originallyPrintedIn_MBS_NPH_ONE_ONC_MOM_MOC", ai, delphia, null));
        AssertJUnit.assertFalse("Scars of Mirrodin is the Mirran list", som.isValid("Card.originallyPrintedIn_MBS_NPH_ONE_ONC_MOM_MOC", ai, delphia, null));
    }

    @Test
    public void testGenReleaseYears() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card gen = addCard("Gen, Confider of Ages", ai);
        final Card a = addCard("Grizzly Bears", ai);
        final Card b = addCard("Grizzly Bears", ai);
        a.setSetCode("LEA");
        b.setSetCode("M10");
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(1993, CardPrintings.releaseYear(a));
        AssertJUnit.assertEquals("1993 and 2009", 2, gen.getNetPower());
    }

    @Test
    public void testPlaytesterAdjustsWhileOnBattlefield() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card cat = addCard("Unclaimed Cat", ai);
        final Card you = addCard("You, Magic Playtester", ai);
        final SpellAbility sa = you.getSpellAbilities().stream().filter(s -> s.hasParam("WhileOnStackOrBattlefield"))
                .findFirst().orElseThrow();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(cat);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("the AI raises its own creature's power", 4, cat.getNetPower());
        game.getEndOfTurn().executeUntil();
        AssertJUnit.assertEquals("still there after the turn", 4, cat.getNetPower());
        final Card dig = addCard("You, Iterative Playtester", ai);
        AssertJUnit.assertTrue(dig.isPlaytestCard());
    }

    @Test
    public void testUnknownWizardEminence() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("The Unknown Wizard", ai, ZoneType.Command);
        final Card cat = addCardToZone("Unclaimed Cat", ai, ZoneType.Hand);
        final SpellAbility cast = cat.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        final forge.game.mana.ManaCostBeingPaid cost = new forge.game.mana.ManaCostBeingPaid(cat.getManaCost());
        forge.game.cost.CostAdjustment.adjust(cost, cast, ai, null, true, false);
        AssertJUnit.assertEquals("{2}{W} less one", 2, cost.getConvertedManaCost());
    }
}
