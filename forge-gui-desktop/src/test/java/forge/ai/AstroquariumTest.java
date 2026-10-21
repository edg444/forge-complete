package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.sticker.AppliedSticker;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerSheet;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Astroquarium (UNF #34), checked against Scryfall and its 4 rulings 2026-10-21: a blue 4/4 Shark, or a blue 8/8
 * Octopus while its cracks are covered - here, while an art sticker is on it. Only colors and creature types change.
 */
public class AstroquariumTest extends AITest {

    private SpellAbility aim(final Card aquarium, final Player p, final Card target) {
        for (final SpellAbility sa : aquarium.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                sa.setActivatingPlayer(p);
                sa.getTargets().add(target);
                return sa;
            }
        }
        throw new AssertionError();
    }

    @Test
    public void testSharkOrOctopus() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card aquarium = addCard("Astroquarium", p);
        final Card ornithopter = addCard("Ornithopter", p);
        AbilityUtils.resolve(aim(aquarium, p, ornithopter));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, ornithopter.getNetPower());
        AssertJUnit.assertTrue(ornithopter.getType().hasCreatureType("Shark"));
        AssertJUnit.assertFalse(ornithopter.getType().hasCreatureType("Thopter"));
        AssertJUnit.assertTrue("other card types stay", ornithopter.isArtifact());
        AssertJUnit.assertTrue(ornithopter.getColor().hasBlue());

        final Card sheet = Card.fromPaperCard(StaticData.instance().getVariantCards().getCard("Ancestral Hot Dog Minotaur"), p);
        p.getZone(ZoneType.StickerSheets).add(sheet);
        Sticker art = null;
        for (final Sticker s : StickerSheet.getStickers(sheet)) {
            if (s.getSlot().equals("STK4")) {
                art = s;
            }
        }
        aquarium.addSticker(new AppliedSticker(art, game.getNextTimestamp()));
        final Card bears = addCard("Grizzly Bears", p);
        AbilityUtils.resolve(aim(aquarium, p, bears));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(8, bears.getNetPower());
        AssertJUnit.assertTrue(bears.getType().hasCreatureType("Octopus"));
        AssertJUnit.assertFalse("instead, not as well", bears.getType().hasCreatureType("Shark"));
    }
}
