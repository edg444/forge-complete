package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Entirely Normal Armchair (Unstable) - Oracle text, the three Scryfall rulings and the Unstable FAQ entry checked
 * 2026-10-09. Hidden for real: opponents don't see it and must point at the right spot to find it (user ruling).
 */
public class EntirelyNormalArmchairTest extends AITest {

    private SpellAbility ability(final Card c, final ApiType api) {
        for (final SpellAbility sa : c.getAllSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    private Card seekEffect(final Player p) {
        for (final Card c : p.getCardsIn(ZoneType.Command)) {
            if ("Something Hidden".equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    private Card hide(final Game game, final Player me) {
        // a real game fills in who's whose opponent at the start; the views read that to know who it's hidden from
        for (final Player p : game.getPlayers()) {
            p.updateOpponentsForView();
        }
        final Card chair = addCardToZone("Entirely Normal Armchair", me, ZoneType.Hand);
        final SpellAbility hide = ability(chair, ApiType.Hide);
        hide.setActivatingPlayer(me);
        // during your turn, from your hand
        AssertJUnit.assertTrue(hide.getRestrictions().canPlay(chair, hide));
        AbilityUtils.resolve(hide);
        for (final Card c : me.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Entirely Normal Armchair")) {
                return c;
            }
        }
        return null;
    }

    @Test
    public void hiddenFromOpponents() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);

        final Card chair = hide(game, me);
        AssertJUnit.assertNotNull(chair);
        // with nothing else on my side, the only spot is in plain sight
        AssertJUnit.assertEquals(me, chair.getHiddenSpot());
        AssertJUnit.assertTrue(chair.isHiddenFrom(opp));
        AssertJUnit.assertFalse(chair.isHiddenFrom(me));
        AssertJUnit.assertFalse(chair.getView().canBeShownTo(opp.getView()));
        AssertJUnit.assertTrue(chair.getView().canBeShownTo(me.getView()));

        // they can't target it or use its {0} while they don't see it
        final Card shatter = addCardToZone("Shatter", opp, ZoneType.Hand);
        final SpellAbility shatterSa = shatter.getFirstSpellAbility();
        shatterSa.setActivatingPlayer(opp);
        AssertJUnit.assertFalse(chair.canBeTargetedBy(shatterSa));
        final SpellAbility bounce = ability(chair, ApiType.ChangeZone);
        bounce.setActivatingPlayer(opp);
        AssertJUnit.assertFalse(bounce.getRestrictions().canPlay(chair, bounce));

        AssertJUnit.assertNotNull(seekEffect(opp));
    }

    @Test
    public void cantHideOnAnOpponentsTurn() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        final Card chair = addCardToZone("Entirely Normal Armchair", me, ZoneType.Hand);
        final SpellAbility hide = ability(chair, ApiType.Hide);
        hide.setActivatingPlayer(me);
        AssertJUnit.assertFalse(hide.getRestrictions().canPlay(chair, hide));
    }

    @Test
    public void whatItWasBehindLeavingLeavesItInPlainSight() {
        final Game game = initAndCreateGame();
        final Player me = game.getPlayers().get(1);
        final Card chair = hide(game, me);
        final Card forest = addCard("Forest", me);
        chair.setHiddenSpot(forest);
        AssertJUnit.assertEquals(forest, chair.getHiddenSpot());
        game.getAction().destroy(forest, null, true, null);
        AssertJUnit.assertEquals(me, chair.getHiddenSpot());
        AssertJUnit.assertTrue(chair.isHiddenOnBattlefield());
    }

    @Test
    public void foundAndSentBack() {
        final Game game = initAndCreateGame();
        final Player opp = game.getPlayers().get(0);
        final Player me = game.getPlayers().get(1);
        final Card chair = hide(game, me);

        // the only spot is plain sight, so the opponent's guess is right
        final Card effect = seekEffect(opp);
        final SpellAbility seek = ability(effect, ApiType.SeekHidden);
        seek.setActivatingPlayer(opp);
        AbilityUtils.resolve(seek);

        AssertJUnit.assertFalse(chair.isHiddenOnBattlefield());
        AssertJUnit.assertNull(seekEffect(opp));
        // now that they see it, the AI returns it with its own {0} ability, on the stack
        AssertJUnit.assertFalse(game.getStack().isEmpty());
        game.getStack().resolveStack();
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Hand).anyMatch(c -> c.getName().equals("Entirely Normal Armchair")));
    }
}
