package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCantBeCast;
import forge.game.zone.ZoneType;

/**
 * X (Unstable) - Oracle text and the four Scryfall rulings checked 2026-10-06 (no FAQ entry).
 */
public class XTest extends AITest {

    private static SpellAbility ability(Card x, ApiType api) {
        for (SpellAbility sa : x.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    private Card sneak(Game game, Player me, Player opp) {
        Card x = addCard("X", me);
        game.getAction().checkStateEffects(true);
        SpellAbility put = ability(x, ApiType.ChangeZone);
        put.setActivatingPlayer(me);
        put.resetTargets();
        put.getTargets().add(opp);
        AbilityUtils.resolve(put);
        game.getAction().checkStateEffects(true);
        return game.getCardState(x);
    }

    @Test
    public void testInTheOpponentsHand() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        AssertJUnit.assertFalse(bolt.getView().canBeShownTo(me.getView()));

        Card x = sneak(game, me, opp);
        AssertJUnit.assertTrue(opp.getZone(ZoneType.Hand).contains(x));
        AssertJUnit.assertEquals(me, x.getOwner());
        // that opponent plays with their hand revealed
        AssertJUnit.assertTrue(bolt.getView().canBeShownTo(me.getView()));

        // that opponent can't cast X; its owner can
        SpellAbility spell = x.getFirstSpellAbility();
        spell.setActivatingPlayer(opp);
        AssertJUnit.assertTrue(StaticAbilityCantBeCast.cantBeCastAbility(spell, x, opp));
        List<SpellAbility> mine = x.getAllPossibleAbilities(me, true);
        AssertJUnit.assertTrue(mine.stream().anyMatch(SpellAbility::isSpell));
        AssertJUnit.assertTrue(x.getAllPossibleAbilities(opp, true).stream().noneMatch(SpellAbility::isSpell));

        // the play ability: its owner, from there; not the opponent
        AssertJUnit.assertTrue(mine.stream().anyMatch(sa -> sa.getApi() == ApiType.Play));
        AssertJUnit.assertTrue(x.getAllPossibleAbilities(opp, true).stream().noneMatch(sa -> sa.getApi() == ApiType.Play));

        // back in its owner's hand none of it applies
        game.getAction().moveToHand(x, null);
        game.getAction().checkStateEffects(true);
        Card home = game.getCardState(x);
        AssertJUnit.assertTrue(me.getZone(ZoneType.Hand).contains(home));
        AssertJUnit.assertFalse(bolt.getView().canBeShownTo(me.getView()));
        AssertJUnit.assertTrue(home.getAllPossibleAbilities(me, true).stream().noneMatch(sa -> sa.getApi() == ApiType.Play));
    }

    @Test
    public void testCastingFromTheirHandForFree() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        // a card worth more than X itself, which is in that hand too and fair game
        Card dragon = addCardToZone("Shivan Dragon", opp, ZoneType.Hand);
        Card x = sneak(game, me, opp);
        SpellAbility play = ability(x, ApiType.Play);
        play.setActivatingPlayer(me);
        AbilityUtils.resolve(play);
        playUntilStackClear(game);
        Card cast = game.getCardState(dragon);

        AssertJUnit.assertTrue(cast.isInZone(ZoneType.Battlefield));
        // you control it but don't own it (ruling)
        AssertJUnit.assertEquals(me, cast.getController());
        AssertJUnit.assertEquals(opp, cast.getOwner());
    }

    @Test
    public void testALandOnlyOnYourTurnWithALandPlay() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card island = addCardToZone("Island", opp, ZoneType.Hand);
        Card x = sneak(game, me, opp);
        SpellAbility play = ability(x, ApiType.Play);

        // the opponent's turn: no land
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        play.setActivatingPlayer(me);
        AbilityUtils.resolve(play);
        AssertJUnit.assertTrue(opp.getZone(ZoneType.Hand).contains(game.getCardState(island)));

        // my turn with a land play left
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
        play.setActivatingPlayer(me);
        AbilityUtils.resolve(play);
        Card land = game.getCardState(island);
        AssertJUnit.assertTrue(land.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(me, land.getController());
    }
}
