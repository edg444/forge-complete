package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mana Abundance (CMB1 #58), checked against Scryfall and its 3 rulings 2026-10-13: whenever a player would add mana,
 * all players add it instead, with the same riders ("you" being whoever adds it) and from the original source.
 */
public class ManaAbundanceTest extends AITest {

    private static void activate(final Card source, final Player p) {
        SpellAbility tap = source.getManaAbilities().get(0);
        tap.setActivatingPlayer(p);
        AbilityUtils.resolve(tap);
    }

    @Test
    public void everyPlayerAddsTheManaFromTheSameSource() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCard("Mana Abundance", opp);
        Card mountain = addCard("Mountain", opp);
        Card ziggurat = addCard("Ancient Ziggurat", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        game.getAction().checkStateEffects(true);

        activate(mountain, opp);
        AssertJUnit.assertEquals(1, opp.getManaPool().getAmountOfColor(MagicColor.RED));
        AssertJUnit.assertEquals(1, ai.getManaPool().getAmountOfColor(MagicColor.RED));
        for (Mana m : ai.getManaPool()) {
            AssertJUnit.assertEquals("produced by the Mountain", mountain, m.getSourceCard());
        }
        opp.getManaPool().resetPool();
        ai.getManaPool().resetPool();

        // riders come along
        activate(ziggurat, ai);
        AssertJUnit.assertEquals(1, opp.getManaPool().totalMana());
        for (Mana m : opp.getManaPool()) {
            AssertJUnit.assertTrue("creature spells only", m.isRestricted());
            AssertJUnit.assertEquals(ziggurat, m.getSourceCard());
        }
    }

    @Test
    public void spellsThatAddManaToo() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCard("Mana Abundance", opp);
        addCard("Swamp", ai);
        Card ritual = addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = ritual.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        playUntilStackClear(game);
        AssertJUnit.assertEquals(3, ai.getManaPool().getAmountOfColor(MagicColor.BLACK));
        AssertJUnit.assertEquals("the Swamp paying for it, then the Ritual", 4, opp.getManaPool().getAmountOfColor(MagicColor.BLACK));
    }

    @Test
    public void aiStillPaysWithTheRightLands() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCard("Mana Abundance", opp);
        addCards("Forest", 2, ai);
        addCard("Island", ai);
        Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = bears.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        playUntilStackClear(game);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals("the opponent got what the AI tapped", 2, opp.getManaPool().totalMana());
    }
}
