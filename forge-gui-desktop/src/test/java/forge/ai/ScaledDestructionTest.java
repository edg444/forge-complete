package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Scaled Destruction (Mystery Booster playtest card, scripted upstream), checked against Scryfall's ruling 2026-10-10:
 * "creatures are destroyed in the order specified. This may cause some creatures to escape destruction."
 */
public class ScaledDestructionTest extends AITest {

    @Test
    public void testModesInOrderLetShrunkCreaturesEscape() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card lord = addCard("Lord of Atlantis", p);      // 2/2: small
        Card merfolk = addCard("Coral Merfolk", p);      // 2/1, 3/2 with the lord: medium
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, merfolk.getNetPower());

        Card spell = addCardToZone("Scaled Destruction", p, ZoneType.Hand);
        SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        // small, then medium
        sa.setSubAbility(null);
        AbilitySub small = null, medium = null;
        for (AbilitySub sub : sa.getAdditionalAbilityList("Choices")) {
            if (sub.getDescription().contains("small")) small = sub;
            if (sub.getDescription().contains("medium")) medium = sub;
        }
        AssertJUnit.assertNotNull(small);
        AssertJUnit.assertNotNull(medium);
        forge.game.ability.effects.CharmEffect.chainAbilities(sa, new java.util.ArrayList<>(java.util.List.of(small, medium)));
        AbilityUtils.resolve(sa);

        AssertJUnit.assertTrue(game.getCardState(lord).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue("the merfolk is small once the lord is gone", game.getCardState(merfolk).isInZone(ZoneType.Battlefield));
    }
}
