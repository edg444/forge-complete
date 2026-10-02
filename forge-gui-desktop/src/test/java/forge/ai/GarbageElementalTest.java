package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Garbage Elemental (Unstable 82a-f), six functional variants. Oracle text checked against Scryfall 2026-10-01; the
 * rulings (art menace's "figure", last strike) and the Unstable FAQ's art menace entry too. "Wordy" and art figures
 * are facts about the printed card, read from PrintingTraits: each printing used below was checked by eye.
 */
public class GarbageElementalTest extends AITest {

    private Card printing(String name, String set, String cn, Player p) {
        PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, cn);
        AssertJUnit.assertNotNull(name + " " + set + " " + cn, pc);
        Card c = Card.fromPaperCard(pc, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }

    private Card elemental(String variant, Player p) {
        return printing("Garbage Elemental", "UST", "82" + variant, p);
    }

    private static Trigger trigger(Card c, TriggerType mode) {
        for (Trigger t : c.getTriggers()) {
            if (t.getMode() == mode) {
                return t;
            }
        }
        throw new AssertionError("no " + mode + " trigger on " + c);
    }

    @Test
    public void testNewVariantsReadLikeOracle() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card a = elemental("a", me);
        Card e = elemental("e", me);
        Card f = elemental("f", me);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(2, a.getNetPower());
        AssertJUnit.assertEquals(4, a.getNetToughness());
        AssertJUnit.assertEquals("Frenzy 2 (Whenever this creature attacks and isn't blocked, it gets +2/+0 until end of turn.) | "
                + "This creature can't be blocked by wordy creatures. (A creature is wordy if it has four or more lines of rules text.)",
                String.join(" | ", a.getAbilityText().trim().split("\\s*[\\r\\n]+")));
        AssertJUnit.assertEquals(4, e.getNetPower());
        AssertJUnit.assertEquals(3, e.getNetToughness());
        AssertJUnit.assertTrue(e.getAbilityText().contains("Each creature you control with any kind of counter on it has art menace. "
                + "(They can't be blocked except by creatures with two or more visible figures in their art.)"));
        AssertJUnit.assertEquals(6, f.getNetPower());
        AssertJUnit.assertEquals(5, f.getNetToughness());
        AssertJUnit.assertTrue(f.hasLastStrike());
        AssertJUnit.assertTrue(f.getAbilityText().contains("Battalion — Whenever this creature and at least two other creatures attack, "
                + "target creature can't block this turn."));
    }

    @Test
    public void testFrenzy() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card a = elemental("a", me);
        game.getAction().checkStateEffects(true);

        SpellAbility pump = trigger(a, TriggerType.AttackerUnblocked).ensureAbility().copy(a, me, false);
        pump.setActivatingPlayer(me);
        AbilityUtils.resolve(pump);
        AssertJUnit.assertEquals(4, a.getNetPower());
        AssertJUnit.assertEquals(4, a.getNetToughness());
    }

    @Test
    public void testBattalionStopsABlocker() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card f = elemental("f", me);
        Card bears = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(CombatUtil.canBlock(bears));

        SpellAbility cantBlock = trigger(f, TriggerType.Attacks).ensureAbility().copy(f, me, false);
        cantBlock.setActivatingPlayer(me);
        cantBlock.getTargets().add(bears);
        AbilityUtils.resolve(cantBlock);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(CombatUtil.canBlock(bears));
    }

    @Test
    public void testWordyCreaturesCantBlock() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card a = elemental("a", me);
        // four printed lines of rules text (reminder text included), three
        Card druid = printing("Paradise Druid", "WOC", "129", opp);
        Card servant = printing("Air Servant", "W16", "4", opp);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(druid.isWordy());
        AssertJUnit.assertFalse(servant.isWordy());
        AssertJUnit.assertFalse(CombatUtil.canBlock(a, druid));
        AssertJUnit.assertTrue(CombatUtil.canBlock(a, servant));
        // every Garbage Elemental is wordy itself (6 to 9 lines)
        AssertJUnit.assertFalse(CombatUtil.canBlock(a, elemental("b", opp)));
    }

    @Test
    public void testArtMenaceBlockers() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        elemental("e", me);
        Card attacker = addCard("Grizzly Bears", me);
        attacker.addCounterInternal(CounterEnumType.P1P1, 1, me, false, null, null);
        // one fish; goblins in the background; an untagged cloud elemental
        Card skyfish = printing("Mystic Skyfish", "M21", "326", opp);
        Card prospector = printing("Skirk Prospector", "DOM", "144", opp);
        Card servant = printing("Air Servant", "W16", "4", opp);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(1, skyfish.getArtFigures());
        AssertJUnit.assertEquals(2, prospector.getArtFigures());
        AssertJUnit.assertEquals(0, servant.getArtFigures());
        AssertJUnit.assertFalse(CombatUtil.canBlock(attacker, skyfish));
        AssertJUnit.assertTrue(CombatUtil.canBlock(attacker, prospector));
        // left to the players, but an AI can't look at the art, so it doesn't block with it
        AssertJUnit.assertTrue(opp.getController().isAI());
        AssertJUnit.assertFalse(CombatUtil.canBlock(attacker, servant));
    }

    @Test
    public void testSoloArtWithAnimalsIsUnknown() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        // Tagger calls it solo, but the butterflies are figures too (Unstable FAQ: any living being that can move)
        AssertJUnit.assertEquals(0, printing("Paradise Druid", "WOC", "129", opp).getArtFigures());
    }

    @Test
    public void testArtMenaceGoesToCreaturesWithCounters() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        elemental("e", me);
        Card bears = addCard("Grizzly Bears", me);
        Card theirs = addCard("Grizzly Bears", opp);
        theirs.addCounterInternal(CounterEnumType.P1P1, 1, opp, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.ART_MENACE));

        // any kind of counter
        bears.addCounterInternal(CounterEnumType.CHARGE, 1, me, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.ART_MENACE));
        AssertJUnit.assertFalse(theirs.hasKeyword(Keyword.ART_MENACE));
    }
}
