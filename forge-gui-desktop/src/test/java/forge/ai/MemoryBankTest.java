package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Memory Bank (Mystery Booster playtest card), Oracle text and rulings checked against Scryfall 2026-10-10: cast from
 * your hand and resolved, it's exiled until the end of the match (not if it's countered); banked cards begin later
 * games of the match in exile, castable from there, and once cast they're ordinary spells.
 */
public class MemoryBankTest extends AITest {

    private void resolveAll(final Game game) {
        do {
            playUntilStackClear(game);
            game.getAction().checkStateEffects(true);
        } while (game.getStack().addAllTriggeredAbilitiesToStack());
    }

    private static SpellAbility spell(final Card c, final Player p) {
        for (final SpellAbility sa : c.getAllPossibleAbilities(p, true)) {
            if (sa.isSpell()) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testBankedThenCastNextGame() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCards("Island", 2, ai);
        fillLibrary(ai, 3);
        Card bank = addCardToZone("Memory Bank", ai, ZoneType.Hand);

        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, spell(bank, ai), null));
        resolveAll(game);
        Card banked = game.getCardState(bank);
        AssertJUnit.assertTrue(banked.isInZone(ZoneType.Exile));
        AssertJUnit.assertEquals(1, banked.getBank());
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
        // not castable in the game it was banked in
        AssertJUnit.assertNull(spell(banked, ai));

        // the next game of the match: it begins in exile, castable
        game.getMatch().recordBank(game);
        Game next = game.getMatch().createGame();
        Player ai2 = next.getPlayers().get(1);
        next.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai2);
        next.getPhaseHandler().onStackResolved();
        addCardToZone("Memory Bank", ai2, ZoneType.Library);
        fillLibrary(ai2, 3);
        game.getMatch().startBanked(next, ai2, ai2.getRegisteredPlayer());
        Card again = ai2.getCardsIn(ZoneType.Exile).getFirst();
        AssertJUnit.assertEquals("Memory Bank", again.getName());
        AssertJUnit.assertEquals(2, again.getBank());
        addCards("Island", 2, ai2);
        next.getAction().checkStateEffects(true);
        SpellAbility fromExile = spell(again, ai2);
        AssertJUnit.assertNotNull(fromExile);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai2, fromExile, null));
        resolveAll(next);
        // cast from exile, not from hand: an ordinary spell now
        Card done = next.getCardState(again);
        AssertJUnit.assertTrue(done.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(0, done.getBank());
    }

    @Test
    public void testCounteredIsNotBanked() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCards("Island", 2, ai);
        fillLibrary(ai, 3);
        Card bank = addCardToZone("Memory Bank", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, spell(bank, ai), null));

        Card counter = addCardToZone("Counterspell", opp, ZoneType.Hand);
        SpellAbility csa = counter.getFirstSpellAbility();
        csa.setActivatingPlayer(opp);
        csa.getTargets().add(game.getStack().peekAbility());
        AbilityUtils.resolve(csa);

        Card gone = game.getCardState(bank);
        AssertJUnit.assertTrue(gone.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(0, gone.getBank());
    }
}
