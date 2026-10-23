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

/** Fallen Empires script review and R&D's Secret Lair printed text (2026-10-23). */
public class FallenEmpiresAuditTest extends AITest {

    @Test
    public void testTheloniteMonkMakesABasicForestAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("R&D's Secret Lair", p);
        Card monk = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Thelonite Monk", "FEM"), p);
        monk.setGameTimestamp(game.getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(monk);
        Card land = addCard("Tundra", opp);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = null;
        for (SpellAbility s : monk.getSpellAbilities()) {
            if (s.getApi() == ApiType.Animate) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(p);
        sa.getTargets().add(land);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(land.getType().hasSubtype("Forest"));
        AssertJUnit.assertFalse(land.getType().hasSubtype("Island"));
        AssertJUnit.assertTrue(land.isBasicLand());
    }
}
