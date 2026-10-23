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
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Onslaught script review and R&D's Secret Lair printed text (2026-10-23). */
public class OnslaughtAuditTest extends AITest {

    @Test
    public void testIxidorsWillCountsEveryWizard() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("Prodigal Sorcerer", p);
        addCard("Prodigal Sorcerer", opp);
        Card will = addCardToZone("Ixidor's Will", p, ZoneType.Hand);
        SpellAbility sa = will.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertEquals(4, AbilityUtils.calculateAmount(will, "Y", sa));
    }

    @Test
    public void testMiseryCharmReturnsOnlyFromYourGraveyard() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card mine = addCardToZone("Cabal Archon", p, ZoneType.Graveyard);
        Card theirs = addCardToZone("Cabal Archon", opp, ZoneType.Graveyard);
        Card charm = addCardToZone("Misery Charm", p, ZoneType.Hand);
        SpellAbility mode = charm.getFirstSpellAbility().getAdditionalAbilityList("Choices").stream()
                .filter(a -> a.getApi() == ApiType.ChangeZone).findFirst().get();
        mode.setActivatingPlayer(p);
        AssertJUnit.assertTrue(mode.canTarget(mine));
        AssertJUnit.assertFalse(mode.canTarget(theirs));
    }

    @Test
    public void testRummagingWizardDoesNotSurveilAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
        addCard("R&D's Secret Lair", p);
        Card spybug = addCard("Dimir Spybug", p);
        Card wizard = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Rummaging Wizard", "ONS"), p);
        p.getZone(ZoneType.Battlefield).add(wizard);
        game.getAction().checkStateEffects(true);
        SpellAbility printed = null;
        for (SpellAbility sa : wizard.getSpellAbilities()) {
            sa.setActivatingPlayer(p);
            if (sa.isActivatedAbility() && sa.getRestrictions().checkOtherRestrictions(wizard, sa, p)) {
                AssertJUnit.assertNull(printed);
                printed = sa;
            }
        }
        AssertJUnit.assertEquals(ApiType.Dig, printed.getApi());
        AbilityUtils.resolve(printed);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        AssertJUnit.assertEquals(0, spybug.getCounters(CounterEnumType.P1P1));
    }
}
