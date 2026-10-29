package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardDamageTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Fixes from the Legends script review (2026-10-23). */
public class LegendsAuditTest extends AITest {

    @Test
    public void testGlyphOfDestructionPreventsDamageToTheWall() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card wall = addCard("Wall of Stone", p);
        Card bears = addCard("Grizzly Bears", opp);
        Card glyph = addCardToZone("Glyph of Destruction", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = glyph.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(wall);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(10, wall.getNetPower());

        CardDamageTable damage = new CardDamageTable();
        damage.put(bears, wall, 2);
        damage.put(wall, bears, 10);
        game.getAction().dealDamage(true, damage, new CardDamageTable(), new GameEntityCounterTable(), null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("damage to the Wall is prevented", 0, wall.getDamage());
        AssertJUnit.assertFalse("the Wall's own damage isn't", bears.isInPlay());
    }

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(forge.model.FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
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
    public void testKoboldTaskmasterPumpsItselfAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card master = printing("Kobold Taskmaster", "LEG", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(1, master.getNetPower());
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, master.getNetPower());
    }

    @Test
    public void testGreedLosesLifeInsteadOfPayingAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card greed = printing("Greed", "LEG", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        addCardToZone("Island", p, ZoneType.Library);
        p.setLife(1, null);
        SpellAbility printed = null;
        for (SpellAbility sa : greed.getSpellAbilities()) {
            if (sa.getPayCosts().hasOnlySpecificCostType(forge.game.cost.CostPartMana.class)) {
                printed = sa;
            }
        }
        printed.setActivatingPlayer(p);
        AssertJUnit.assertTrue(printed.getRestrictions().canPlay(greed, printed));
        int hand = p.getCardsIn(ZoneType.Hand).size();
        AbilityUtils.resolve(printed);
        AssertJUnit.assertEquals(hand + 1, p.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(-1, p.getLife());
    }

    @Test
    public void testBlightDestroysAtEndOfTurnAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("R&D's Secret Lair", p);
        Card land = addCard("Forest", opp);
        Card blight = printing("Blight", "LEG", p, ZoneType.Battlefield);
        blight.attachToEntity(land, null);
        game.getAction().checkStateEffects(true);
        land.tap(null, null);
        resolveAll(game);
        AssertJUnit.assertTrue("still there until the end step", land.isInPlay());
    }
}
