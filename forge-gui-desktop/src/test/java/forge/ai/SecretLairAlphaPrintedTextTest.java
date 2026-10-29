package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.card.Card;
import forge.game.card.CardDamageTable;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * R&D's Secret Lair with Limited Edition Alpha printings: printed text checked against MTGJSON originalText
 * for every printing of each card (2026-10-23).
 */
public class SecretLairAlphaPrintedTextTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testLordOfAtlantisPumpsItselfOnlyAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card alpha = printing("Lord of Atlantis", "LEA", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, alpha.getNetPower());

        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, alpha.getNetPower());
        AssertJUnit.assertTrue(alpha.hasKeyword(Keyword.LANDWALK));

        // Time Spiral's reprint already says "Other Merfolk"
        Card timeshifted = printing("Lord of Atlantis", "TSB", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, alpha.getNetPower());
        AssertJUnit.assertEquals(3, timeshifted.getNetPower());
    }

    @Test
    public void testKormusBellSwampsAreColorlessAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        printing("Kormus Bell", "LEA", p, ZoneType.Battlefield);
        Card swamp = addCard("Swamp", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(swamp.isCreature());
        AssertJUnit.assertTrue(swamp.isBlack());

        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(swamp.isCreature());
        AssertJUnit.assertTrue(swamp.isColorless());
    }

    @Test
    public void testWhiteWardKnocksItselfOffAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card bears = addCard("Grizzly Bears", p);
        Card ward = printing("White Ward", "LEA", p, ZoneType.Battlefield);
        ward.attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Graveyard).contains(ward));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.PROTECTION));
    }

    @Test
    public void testManabarbsHitsAnyTappedLandAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        printing("Manabarbs", "LEA", p, ZoneType.Battlefield);
        Card land = addCard("Mountain", opp);
        game.getAction().checkStateEffects(true);
        land.tap(null, null);
        resolveAll(game);
        AssertJUnit.assertEquals("Oracle: only tapping for mana", 20, opp.getLife());

        addCard("R&D's Secret Lair", p);
        land.untap();
        game.getAction().checkStateEffects(true);
        land.tap(null, null);
        resolveAll(game);
        AssertJUnit.assertEquals(19, opp.getLife());
    }

    @Test
    public void testCloneNeedsACreatureAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        addCards("Island", 4, p);
        Card clone = printing("Clone", "LEA", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = clone.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        // checkRestrictions is where casting consults CantBeCast
        AssertJUnit.assertFalse(sa.checkRestrictions(clone, p));

        addCard("Grizzly Bears", game.getPlayers().get(0));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(sa.checkRestrictions(clone, p));
    }

    @Test
    public void testEarthbindTargetsOnlyFlyersAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bears = addCard("Grizzly Bears", opp);
        Card angel = addCard("Serra Angel", opp);
        Card bind = printing("Earthbind", "LEA", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = bind.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(sa.canTarget(bears));

        // casting puts the spell on the stack before targets are chosen
        sa.setHostCard(game.getAction().moveToStack(bind, sa));
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(sa.canTarget(bears));
        AssertJUnit.assertTrue(sa.canTarget(angel));
    }

    @Test
    public void testLichDestroysForDamageAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("R&D's Secret Lair", p);
        Card lich = printing("Lich", "LEA", p, ZoneType.Battlefield);
        addCard("Grizzly Bears", p);
        Card pinger = addCard("Prodigal Sorcerer", opp);
        game.getAction().checkStateEffects(true);

        CardDamageTable damage = new CardDamageTable();
        damage.put(pinger, p, 1);
        game.getAction().dealDamage(false, damage, new CardDamageTable(), new GameEntityCounterTable(), null);
        resolveAll(game);
        // one card destroyed for the point of damage; destroying Lich itself loses the game
        AssertJUnit.assertEquals(1, p.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(p.getCardsIn(ZoneType.Graveyard).contains(lich), p.hasLost());
    }

    @Test
    public void testLichLosesOnlyWhenDestroyedAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card lich = printing("Lich", "LEA", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);

        final java.util.Map<forge.game.ability.AbilityKey, Object> params = forge.game.ability.AbilityKey.newMap();
        params.put(forge.game.ability.AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
        params.put(forge.game.ability.AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
        game.getAction().sacrifice(new forge.game.card.CardCollection(lich), null, true, params);
        resolveAll(game);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Graveyard).contains(lich));
        AssertJUnit.assertFalse("sacrificing isn't destroying", p.hasLost());

        Card second = printing("Lich", "LEA", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        params.put(forge.game.ability.AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
        params.put(forge.game.ability.AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
        game.getAction().destroy(second, null, true, params);
        resolveAll(game);
        AssertJUnit.assertTrue(p.hasLost());
    }
}
