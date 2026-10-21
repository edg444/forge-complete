package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.sticker.AppliedSticker;
import forge.game.card.sticker.StickerKind;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Animate Object (UNF #33), checked against Scryfall and its 5 rulings 2026-10-21: seven tickets, then a blank object
 * from outside the game gets a power/toughness sticker (and maybe others) before entering as a creature token.
 */
public class AnimateObjectTest extends AITest {

    private void sheet(final Player p) {
        p.getZone(ZoneType.StickerSheets).add(Card.fromPaperCard(
                StaticData.instance().getVariantCards().getCard("Ancestral Hot Dog Minotaur"), p));
    }

    @Test
    public void testAStickeredObjectEntersAsACreatureToken() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        sheet(ai);
        final Card spell = addCardToZone("Animate Object", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);

        final Card object = ai.getCreaturesInPlay().stream().filter(Card::isToken).findFirst().orElse(null);
        AssertJUnit.assertNotNull("it survived as a creature", object);
        boolean pt = false;
        for (final AppliedSticker s : object.getStickers()) {
            pt |= s.getKind() == StickerKind.PT;
        }
        AssertJUnit.assertTrue("it has a power/toughness sticker", pt);
        AssertJUnit.assertTrue(object.getNetToughness() > 0);
        AssertJUnit.assertTrue("only the stickers' characteristics", object.getType().getCreatureTypes().isEmpty());
        AssertJUnit.assertTrue(object.getColor().isColorless());
        final boolean named = object.getStickers().stream().anyMatch(s -> s.getKind() == StickerKind.NAME);
        if (!named) {
            AssertJUnit.assertEquals("no name without a name sticker", "", object.getName());
        }
        AssertJUnit.assertTrue("seven tickets, some spent", ai.getCounters(CounterEnumType.TICKET) < 7);
    }

    @Test
    public void testTheAiNeedsAStickerSheet() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCard("Island", ai);
        }
        final Card spell = addCardToZone("Animate Object", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final AiController brains = ((PlayerControllerAi) ai.getController()).getAi();
        AssertJUnit.assertFalse("no sticker sheet, so it would be a 0/0", brains.canPlaySa(sa) == AiPlayDecision.WillPlay);
        sheet(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, brains.canPlaySa(sa));
    }
}
