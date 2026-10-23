package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** R&D's Secret Lair with Antiquities printings (printed text checked against MTGJSON originalText, 2026-10-23). */
public class SecretLairAntiquitiesPrintedTextTest extends AITest {

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
    public void testOnuletGainsLifeFromAnyZoneAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card first = printing("Onulet", "ATQ", p, ZoneType.Hand);
        game.getAction().moveToGraveyard(first, null);
        resolveAll(game);
        AssertJUnit.assertEquals(20, p.getLife());

        addCard("R&D's Secret Lair", p);
        Card second = printing("Onulet", "ATQ", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        game.getAction().moveToGraveyard(second, null);
        resolveAll(game);
        AssertJUnit.assertEquals(22, p.getLife());
    }

    @Test
    public void testGateToPhyrexiaHasNoTurnLimitAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card gate = printing("Gate to Phyrexia", "ATQ", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = gate.getFirstSpellAbility();
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(gate, "GateLimit", sa));
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(AbilityUtils.calculateAmount(gate, "GateLimit", sa) > 1);
    }

    @Test
    public void testGolgothianSylexTakesOnlyAntiquitiesPrintingsAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card sylex = printing("Golgothian Sylex", "ATQ", p, ZoneType.Battlefield);
        Card oldMine = printing("Urza's Mine", "ATQ", p, ZoneType.Battlefield);
        Card newMine = printing("Urza's Mine", "2XM", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = null;
        for (SpellAbility s : sylex.getSpellAbilities()) {
            if (s.getApi() == ApiType.SacrificeAll) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(oldMine.isInPlay());
        AssertJUnit.assertFalse(sylex.isInPlay());
        AssertJUnit.assertTrue("a later printing isn't from the Antiquities expansion", newMine.isInPlay());
    }
}
