package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantBeChosen;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Mystery Booster 2 playtest cards (MB2), part 4, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2Playtest4Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Game game, final Player p, final Card spell) {
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    @Test
    public void testCheaFamiliars() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card chea = addCard("Chea, Friend to Maybe Too Many", ai);
        addCard("Birds of Paradise", ai);
        addCard("Savannah Lions", ai);
        addCard("Grizzly Bears", ai);
        game.getAction().checkStateEffects(true);
        final SpellAbility mana = chea.getManaAbilities().getFirst();
        AssertJUnit.assertEquals(2, AbilityUtils.calculateAmount(chea, "X", mana));
        final Card frog = addCardToZone("Spore Frog", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(frog.getFirstSpellAbility().withFlash(frog, ai));
        final Card bear = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        AssertJUnit.assertFalse(bear.getFirstSpellAbility().withFlash(bear, ai));
    }

    @Test
    public void testLutriSymbols() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card alphaAngel = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Serra Angel", "LEA"), ai);
        final Card m10Angel = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Serra Angel", "M10"), ai);
        final Card m10Bolt = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Lightning Bolt", "M10"), ai);
        AssertJUnit.assertFalse("no symbol", alphaAngel.isValid("Card.SilverGoldOrangeOrPurpleSymbol", ai, null, null));
        AssertJUnit.assertTrue("silver", m10Angel.isValid("Card.SilverGoldOrangeOrPurpleSymbol", ai, null, null));
        AssertJUnit.assertFalse("black", m10Bolt.isValid("Card.SilverGoldOrangeOrPurpleSymbol", ai, null, null));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCardToZone("Island", ai, ZoneType.Hand);
        final Card lutri = addCardToZone("Lutri, Pauper Otter", ai, ZoneType.Hand);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        cast(game, ai, lutri);
        AssertJUnit.assertEquals("discarded its hand, drew three", 3, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testAvacynsCollarShackle() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card collar = addCard("Avacyn's Collar, the Symbol of Her Church", ai);
        final Card elves = addCard("Llanowar Elves", opp);
        final SpellAbility shackle = collar.getSpellAbilities().stream().filter(SpellAbility::usesTargeting).findFirst().orElseThrow();
        shackle.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(shackle.canTarget(elves));
        AssertJUnit.assertFalse("only creatures you don't control", shackle.canTarget(addCard("Grizzly Bears", ai)));
        shackle.getTargets().add(elves);
        shackle.resolve();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(elves, collar.getAttachedTo());
        AssertJUnit.assertEquals("still yours", ai, collar.getController());
        AssertJUnit.assertFalse(CombatUtil.canAttack(elves));
        AssertJUnit.assertFalse(CombatUtil.canBlock(elves));
        final SpellAbility tap = elves.getManaAbilities().getFirst();
        tap.setActivatingPlayer(opp);
        AssertJUnit.assertFalse(tap.canPlay());
    }

    @Test
    public void testWormholeWarp() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        // on its own turn, so the AI isn't holding the creature back for its main phase
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        final Card giant = addCard("Hill Giant", opp);
        addCardToZone("Forest", opp, ZoneType.Sideboard);
        final Card bears = addCardToZone("Grizzly Bears", opp, ZoneType.Sideboard);
        final Card warp = addCardToZone("Wormhole Warp", ai, ZoneType.Hand);
        final SpellAbility sa = warp.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(giant);
        final Card onStack = game.getAction().moveToStack(warp, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
        AssertJUnit.assertTrue(game.getCardState(giant).isInZone(ZoneType.Exile));
        final Card nowBears = game.getCardState(bears);
        AssertJUnit.assertTrue("the only nonland card, cast for free: " + nowBears.getZone(), nowBears.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(opp, nowBears.getController());
    }

    @Test
    public void testYourWishExtraMode() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card command = addCardToZone("Austere Command", ai, ZoneType.Sideboard);
        final Player opp = game.getPlayers().get(0);
        for (final String name : new String[] {"Grizzly Bears", "Serra Angel", "Sol Ring", "Glorious Anthem"}) {
            for (int i = 0; i < 3; i++) {
                addCard(name, opp);
            }
        }
        // it isn't free: the spell's mana cost is still paid
        for (int i = 0; i < 6; i++) {
            addCard("Plains", ai);
        }
        for (int i = 0; i < 3; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        cast(game, ai, addCardToZone("Your Wish Is My Command", ai, ZoneType.Hand));
        AssertJUnit.assertFalse("cast from the sideboard", game.getCardState(command).isInZone(ZoneType.Sideboard));
        final java.util.Set<String> left = new java.util.HashSet<>();
        opp.getCardsIn(ZoneType.Battlefield).forEach(c -> left.add(c.getName()));
        AssertJUnit.assertTrue("three of its four modes: " + left, left.size() <= 1);
        AssertJUnit.assertEquals("the card's own ability is untouched", 0, command.getFirstSpellAbility().getExtraCharmModes());
    }

    @Test
    public void testOddricSharesKeywords() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card oddric = addCard("Oddric, Lunar Marquis", ai);
        addCard("Severed Legion", ai);
        addCard("Bog Wraith", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        game.getAction().checkStateEffects(true);
        final SpellAbility trig = oddric.getTriggers().getFirst().ensureAbility();
        trig.setActivatingPlayer(ai);
        AbilityUtils.resolve(trig);
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FEAR));
        AssertJUnit.assertTrue(bears.hasKeyword("Landwalk:Swamp"));
        AssertJUnit.assertFalse(bears.hasKeyword("Landwalk:Island"));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.SHROUD));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.BANDING));
        AssertJUnit.assertTrue(oddric.hasKeyword(Keyword.FEAR));
    }

    @Test
    public void testBrigidThoughtweft() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card brigid = addCard("Brigid, Who's Seen Some Stuff", ai);
        final Card knights = addCard("Plover Knights", ai);
        final Card harrier = addCard("Goldmeadow Harrier", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(harrier.hasKeyword(Keyword.THOUGHTWEFT));
        AssertJUnit.assertTrue(harrier.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(harrier.hasKeyword(Keyword.FIRST_STRIKE));
        AssertJUnit.assertTrue(harrier.hasKeyword(Keyword.VIGILANCE));
        AssertJUnit.assertTrue(harrier.hasKeyword(Keyword.NIMBLE));
        AssertJUnit.assertTrue(brigid.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(knights.hasKeyword(Keyword.NIMBLE));
        AssertJUnit.assertFalse("not a Kithkin", bears.hasKeyword(Keyword.FLYING));
        final Player opp = game.getPlayers().get(0);
        final Card giant = addCard("Hill Giant", opp);
        final Card elves = addCard("Llanowar Elves", opp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("nimble", CombatUtil.canBlock(brigid, giant));
        AssertJUnit.assertFalse("flying", CombatUtil.canBlock(brigid, elves));
    }

    @Test
    public void testJeskaiBallerRebound() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card baller = addCardToZone("Jeskai Baller", ai, ZoneType.Hand);
        final SpellAbility sa = baller.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final Card onStack = game.getAction().moveToStack(baller, sa);
        sa.setHostCard(onStack);
        onStack.setCastFrom(ai.getZone(ZoneType.Hand));
        onStack.setCastSA(sa);
        game.getStack().add(sa);
        resolveAll(game);
        AssertJUnit.assertEquals(1, CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && c.getType().hasCreatureType("Athlete")));
        AssertJUnit.assertTrue("rebound exiles it as it resolves: " + game.getCardState(baller).getZone(),
                game.getCardState(baller).isInZone(ZoneType.Exile));
    }

    @Test
    public void testGlimpseCantBeChosen() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card inHand = addCardToZone("Glimpse, the Unthinkable", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("only on the battlefield", StaticAbilityCantBeChosen.cantBeChosen(inHand));
        AssertJUnit.assertFalse(StaticAbilityCantBeChosen.nameCantBeChosen(game, "Glimpse, the Unthinkable"));
        final Card glimpse = addCard("Glimpse, the Unthinkable", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        glimpse.setSickness(false);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(StaticAbilityCantBeChosen.cantBeChosen(glimpse));
        AssertJUnit.assertFalse(StaticAbilityCantBeChosen.cantBeChosen(bears));
        AssertJUnit.assertTrue(StaticAbilityCantBeChosen.nameCantBeChosen(game, "Glimpse, the Unthinkable"));
        AssertJUnit.assertFalse("the sorcery's name is fine", StaticAbilityCantBeChosen.nameCantBeChosen(game, "Glimpse the Unthinkable"));
        AssertJUnit.assertTrue("attacking is a game rule, not a choice", CombatUtil.canAttack(glimpse));
    }
}
