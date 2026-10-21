package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.PrintingTraits;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Vorthos, Steward of Myth (UNF #126), checked against Scryfall and its rulings 2026-10-21: every name a character has
 * gone by counts (Lim-Dûl on The Raven Man, Urza on Blind Seer), possessives and derived words count (Teferi's
 * Protection, Avacynian Priest), and only colored mana is reduced.
 */
public class VorthosStewardOfMythTest extends AITest {

    private Card vorthosNaming(final Player p, final String character) {
        final Card vorthos = addCard("Vorthos, Steward of Myth", p);
        vorthos.setChosenType(PrintingTraits.findCharacter(character).name());
        p.getGame().getAction().checkStateEffects(true);
        return vorthos;
    }

    private boolean canCast(final Player p, final String name) {
        final Card c = addCardToZone(name, p, ZoneType.Hand);
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        return ComputerUtilMana.canPayManaCost(sa, p, 0, false);
    }

    @Test
    public void testEveryNameCounts() {
        AssertJUnit.assertEquals("Teferi Akosa", PrintingTraits.findCharacter("teferi").name());
        AssertJUnit.assertEquals("lim-dul", PrintingTraits.findCharacter("The Raven Man").slug());
        AssertJUnit.assertEquals("lim-dul", PrintingTraits.findCharacter("lim-dul").slug());
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertTrue(addCardToZone("The Raven Man", p, ZoneType.Hand).hasKnownCharacter("Lim-Dûl"));
        AssertJUnit.assertTrue(addCardToZone("Blind Seer", p, ZoneType.Hand).hasKnownCharacter("Urza"));
        AssertJUnit.assertTrue(addCardToZone("Avacynian Priest", p, ZoneType.Hand).hasKnownCharacter("Avacyn"));
        AssertJUnit.assertFalse(addCardToZone("Grizzly Bears", p, ZoneType.Hand).hasKnownCharacter("Urza"));
    }

    @Test
    public void testColoredManaOnly() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        vorthosNaming(p, "Teferi");
        addCard("Mountain", p);
        addCard("Mountain", p);
        AssertJUnit.assertTrue("{2}{W} costs {2}", canCast(p, "Teferi's Protection"));
        AssertJUnit.assertFalse("generic isn't reduced, nor other spells", canCast(p, "Lightning Helix"));
    }

    @Test
    public void testAiNamesItsMostCommonCharacter() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Serra Angel", ai, ZoneType.Library);
        }
        addCardToZone("Teferi's Protection", ai, ZoneType.Library);
        final Card vorthos = addCardToZone("Vorthos, Steward of Myth", ai, ZoneType.Hand);
        final SpellAbility choose = forge.game.ability.AbilityFactory.getAbility(vorthos.getSVar("ChooseCharacter"), vorthos);
        choose.setActivatingPlayer(ai);
        final List<String> all = PrintingTraits.allCharacterNames();
        AssertJUnit.assertEquals("Serra", ComputerUtil.chooseSomeType(ai, "Character", choose, all));
    }
}
