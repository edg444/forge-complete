package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Knight of the Kitchen Sink's six protections and Knight of the Widget, which read facts about the physical
 * printing. Every printing's border, watermark and open-mouth art below was checked against the Scryfall API.
 */
public class PrintingTraitsTest extends AITest {

    private Card printing(String name, String set, String cn, Player p, ZoneType zone) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, cn);
        AssertJUnit.assertNotNull(name + " " + set + " " + cn, pc);
        AssertJUnit.assertEquals(cn, pc.getCollectorNumber());
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(zone).add(c);
        return c;
    }

    private Card knight(String cn, Player p) {
        return printing("Knight of the Kitchen Sink", "UST", cn, p, ZoneType.Battlefield);
    }

    private boolean canTarget(String name, String set, String cn, Player caster, Card target) {
        SpellAbility sa = printing(name, set, cn, caster, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(caster);
        return sa.canTarget(target);
    }

    @Test
    public void testBlackBorders() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card knight = knight("12a", me);

        AssertJUnit.assertFalse(canTarget("Lightning Bolt", "M10", "146", opp, knight));
        AssertJUnit.assertTrue(canTarget("Lightning Bolt", "4ED", "208", opp, knight)); // white
        AssertJUnit.assertTrue(canTarget("Lightning Bolt", "2X2", "361", opp, knight)); // borderless
        AssertJUnit.assertFalse(CombatUtil.canBlock(knight, printing("Grizzly Bears", "LEA", "199", opp, ZoneType.Battlefield)));
        AssertJUnit.assertTrue(CombatUtil.canBlock(knight, printing("Grizzly Bears", "4ED", "250", opp, ZoneType.Battlefield)));
        // UST itself is silver-bordered, so one Knight doesn't protect itself from another
        AssertJUnit.assertTrue(CombatUtil.canBlock(knight, knight("12f", opp)));
    }

    @Test
    public void testCollectorNumbers() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card even = knight("12b", me);
        Card odd = knight("12d", me);

        AssertJUnit.assertFalse(canTarget("Lightning Bolt", "M10", "146", opp, even));
        AssertJUnit.assertTrue(canTarget("Lightning Bolt", "M11", "149", opp, even));
        AssertJUnit.assertTrue(canTarget("Lightning Bolt", "M10", "146", opp, odd));
        AssertJUnit.assertFalse(canTarget("Lightning Bolt", "M11", "149", opp, odd));
        // "12b" is 12
        AssertJUnit.assertFalse(CombatUtil.canBlock(even, knight("12a", opp)));
        AssertJUnit.assertTrue(CombatUtil.canBlock(odd, knight("12a", opp)));
    }

    @Test
    public void testDamageIsPrevented() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card blackBorders = knight("12a", me);
        Card even = knight("12b", me);
        Card odd = knight("12d", me);

        // black-bordered, collector number 222
        SpellAbility pyroclasm = printing("Pyroclasm", "10E", "222", opp, ZoneType.Hand).getFirstSpellAbility();
        pyroclasm.setActivatingPlayer(opp);
        AbilityUtils.resolve(pyroclasm);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(blackBorders.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(even.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(odd.isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testLooseLips() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card knight = knight("12c", me);

        AssertJUnit.assertFalse(CombatUtil.canBlock(knight, printing("Grizzly Bears", "10E", "268", opp, ZoneType.Battlefield)));
        AssertJUnit.assertTrue(CombatUtil.canBlock(knight, printing("Grizzly Bears", "LEA", "199", opp, ZoneType.Battlefield)));
    }

    @Test
    public void testTwoWordNames() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card knight = knight("12e", me);

        AssertJUnit.assertFalse(canTarget("Lightning Bolt", "M10", "146", opp, knight));
        AssertJUnit.assertTrue(canTarget("Shock", "10E", "232", opp, knight));
        // hyphenated words are one word
        AssertJUnit.assertTrue(CombatUtil.canBlock(knight, printing("Will-o'-the-Wisp", "LEA", "135", opp, ZoneType.Battlefield)));
    }

    @Test
    public void testWatermarks() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card knight = knight("12f", me);

        AssertJUnit.assertFalse(canTarget("Lightning Helix", "GK1", "90", opp, knight)); // Boros
        AssertJUnit.assertTrue(canTarget("Lightning Bolt", "M10", "146", opp, knight));
        AssertJUnit.assertFalse(CombatUtil.canBlock(knight, knight("12a", opp))); // Order of the Widget
    }

    private void castOddlyUneven(Player caster, String mode) {
        Card spell = addCardToZone("Oddly Uneven", caster, ZoneType.Hand);
        SpellAbility sa = null;
        for (SpellAbility choice : spell.getFirstSpellAbility().getAdditionalAbilityList("Choices")) {
            if (choice.getDescription().contains(mode)) {
                sa = choice;
            }
        }
        AssertJUnit.assertNotNull(mode, sa);
        sa.setActivatingPlayer(caster);
        AbilityUtils.resolve(sa);
        caster.getGame().getAction().checkStateEffects(true);
    }

    @Test
    public void testOddlyUnevenOdd() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card wisp = addCard("Will-o'-the-Wisp", me);     // 1 - hyphenated
        Card bears = addCard("Grizzly Bears", me);        // 2
        Card knight = knight("12a", me);                  // 5 - "of" and "the" count
        Card soldier = addToken("w_1_1_human_soldier", me); // "Human Soldier", not "Human Soldier Token"

        castOddlyUneven(me, "an odd number");

        AssertJUnit.assertTrue(wisp.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(bears.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(knight.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(soldier.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testOddlyUnevenEven() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card wisp = addCard("Will-o'-the-Wisp", me);
        Card bears = addCard("Grizzly Bears", me);
        Card soldier = addToken("w_1_1_human_soldier", me);

        castOddlyUneven(me, "an even number");

        AssertJUnit.assertTrue(wisp.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(bears.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertFalse(soldier.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testOldGuardTargetsOnlyCreaturesWithoutReminderText() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card guard = addCard("Old Guard", me);
        SpellAbility tap = null;
        for (SpellAbility sa : guard.getSpellAbilities()) {
            if (sa.getApi() == forge.game.ability.ApiType.Tap) {
                tap = sa;
            }
        }
        AssertJUnit.assertNotNull(tap);
        tap.setActivatingPlayer(me);

        AssertJUnit.assertTrue(tap.canTarget(addCard("Grizzly Bears", opp)));
        AssertJUnit.assertFalse(tap.canTarget(addCard("Monastery Swiftspear", opp))); // Prowess (...)
        // Knight of the Kitchen Sink's protection carries its own reminder text
        AssertJUnit.assertFalse(tap.canTarget(knight("12d", opp)));
    }

    @Test
    public void testKnightOfTheWidgetCountsWidgetWatermarks() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card widget = printing("Knight of the Widget", "UST", "13", me, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, widget.getNetPower());

        knight("12a", me);
        printing("Grizzly Bears", "LEA", "199", me, ZoneType.Battlefield);
        knight("12b", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, widget.getNetPower());
        AssertJUnit.assertEquals(2, widget.getNetToughness());
    }
}
