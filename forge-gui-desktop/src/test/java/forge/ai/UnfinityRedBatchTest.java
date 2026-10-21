package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.sticker.StickerKind;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Art Appreciation (UNF #99) and Carnival Barker (UNF #102), checked against Scryfall and their rulings 2026-10-21.
 */
public class UnfinityRedBatchTest extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testArtAppreciation() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        ai.getZone(ZoneType.StickerSheets).add(Card.fromPaperCard(
                StaticData.instance().getVariantCards().getCard("Ancestral Hot Dog Minotaur"), ai));
        final Card mine = addCard("Grizzly Bears", ai);
        final Card blocker = addCard("Hill Giant", opp);
        final Card spell = addCardToZone("Art Appreciation", ai, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        resolveAll(game);
        AssertJUnit.assertTrue("the art sticker went on its own permanent",
                mine.getStickers().stream().anyMatch(s -> s.getKind() == StickerKind.ART));
        final int rating = spell.getChosenNumber();
        AssertJUnit.assertTrue(rating >= 1 && rating <= 5);
        mine.setSickness(false);
        AssertJUnit.assertFalse("the AI picks the opposing blocker", CombatUtil.canBlock(mine, blocker));
    }

    @Test
    public void testDevilKNevilJumps() {
        int cleared = 0;
        int failed = 0;
        for (int i = 0; i < 20; i++) {
            final Game game = initAndCreateGame();
            final Player ai = game.getPlayers().get(1);
            for (int j = 0; j < 5; j++) {
                addCard("Grizzly Bears", game.getPlayers().get(j % 2));
            }
            final Card devil = addCard("Devil K. Nevil", ai);
            final SpellAbility jump = devil.getTriggers().get(0).ensureAbility();
            jump.setActivatingPlayer(ai);
            AbilityUtils.resolve(jump);
            final int counters = devil.getCounters(forge.game.card.CounterEnumType.P1P1);
            if (counters > 0) {
                AssertJUnit.assertEquals("the AI jumps four, the best expected count", 4, counters);
                cleared++;
            } else {
                failed++;
            }
        }
        AssertJUnit.assertTrue("a 49% jump both clears and fails: " + cleared + "/" + failed, cleared > 0 && failed > 0);
    }

    @Test
    public void testDontTryThisAtHome() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Don't Try This at Home", ai);
        final Card spike = addCardToZone("Lava Spike", ai, ZoneType.Hand);
        AssertJUnit.assertTrue("Lava Spike's art is tagged hot", spike.isValid("Card.HotInArt", ai, spike, null));
        final SpellAbility sa = spike.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(opp);
        final int life = opp.getLife();
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("3 plus 1", life - 4, opp.getLife());
    }

    @Test
    public void testCarnivalBarker() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card barker = addCard("Carnival Barker", ai);
        barker.setSickness(false);
        SpellAbility laud = null;
        for (final SpellAbility s : barker.getSpellAbilities()) {
            if (s.isActivatedAbility()) {
                laud = s;
            }
        }
        laud.setActivatingPlayer(ai);
        AbilityUtils.resolve(laud);
        final int applause = barker.getChosenNumber();
        AssertJUnit.assertTrue("a simulated audience of up to three", applause >= 0 && applause <= 3);
        AssertJUnit.assertEquals(3 + applause, barker.getNetPower());
        AssertJUnit.assertTrue(barker.hasKeyword(Keyword.TRAMPLE) && barker.hasKeyword(Keyword.HASTE));
    }
}
