package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Prophecy script review and R&D's Secret Lair printed text (2026-10-23). */
public class ProphecyAuditTest extends AITest {

    private SpellAbility playableManaAbility(final boolean lair) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card cave = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Rhystic Cave", "PCY"), p);
        p.getZone(ZoneType.Battlefield).add(cave);
        game.getAction().checkStateEffects(true);
        for (SpellAbility sa : cave.getManaAbilities()) {
            sa.setActivatingPlayer(p);
            // mirrors SpellAbility.isManaAbilityFor, which won't pay with an "activate only as an instant" ability
            if (sa.getRestrictions().canPlay(cave, sa) && !sa.getRestrictions().isInstantSpeed()) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testRhysticCaveIsAManaAbilityAsPrinted() {
        SpellAbility sa = playableManaAbility(true);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertTrue(sa.isManaAbility());
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(sa.getActivatingPlayer().getManaPool().totalMana() <= 1);
    }

    @Test
    public void testRhysticCaveIsNoManaAbilityByOracle() {
        AssertJUnit.assertNull(playableManaAbility(false));
    }
}
