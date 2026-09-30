package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Scripts that disagreed with current Oracle (Scryfall, checked 2026-09-30), found by the Oracle: field sync.
 */
public class OracleDriftFixesTest extends AITest {

    private static SpellAbility byApi(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void testDragonbornImmolatorPumpsByOne() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card c = addCard("Dragonborn Immolator", me);
        SpellAbility pump = byApi(c, ApiType.Pump);
        pump.setActivatingPlayer(me);
        AbilityUtils.resolve(pump);
        AssertJUnit.assertEquals(3, c.getNetPower());
    }

    @Test
    public void testDiscipleOfPerditionExilesAnOpponentsGraveyardAndDrainsThem() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card disciple = addCard("Disciple of Perdition", me);
        addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);
        SpellAbility exile = AbilityFactory.getAbility(disciple, "DBExile");
        exile.setActivatingPlayer(me);
        AssertJUnit.assertFalse(exile.canTarget(me));
        AssertJUnit.assertTrue(exile.canTarget(opp));
        int life = opp.getLife();
        exile.getTargets().add(opp);
        AbilityUtils.resolve(exile);
        AssertJUnit.assertEquals(0, opp.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(life - 1, opp.getLife());
    }

    @Test
    public void testPsychicWhorlTargetsOnlyOpponents() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        SpellAbility sa = addCardToZone("Psychic Whorl", me, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        AssertJUnit.assertFalse(sa.canTarget(me));
        AssertJUnit.assertTrue(sa.canTarget(opp));
    }

    @Test
    public void testCharredGraverobberEscapesForFiveMana() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card c = addCardToZone("Charred Graverobber", me, ZoneType.Graveyard);
        SpellAbility escape = null;
        for (SpellAbility sa : GameActionUtil.getAlternativeCosts(c.getFirstSpellAbility(), me, true)) {
            if (sa.isEscape()) {
                escape = sa;
            }
        }
        AssertJUnit.assertNotNull(escape);
        AssertJUnit.assertEquals(5, escape.getPayCosts().getTotalMana().getCMC());
    }

    @Test
    public void testTheForgottenPlaceEntersTapped() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card c = addCardToZone("The Forgotten Place", me, ZoneType.Hand);
        Card played = game.getAction().moveToPlay(c, me, null, null);
        AssertJUnit.assertTrue(played.isTapped());
    }

    @Test
    public void testSupernaturalRescueEnchantsOnlyYourCreatures() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card mine = addCard("Grizzly Bears", me);
        Card theirs = addCard("Grizzly Bears", opp);
        SpellAbility sa = addCardToZone("Supernatural Rescue", me, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        AssertJUnit.assertTrue(sa.canTarget(mine));
        AssertJUnit.assertFalse(sa.canTarget(theirs));
    }

    @Test
    public void testObscuraPolymorphistMustTarget() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card c = addCard("Obscura Polymorphist", me);
        SpellAbility exile = AbilityFactory.getAbility(c, "TrigExile");
        AssertJUnit.assertEquals(1, exile.getMinTargets());
    }
}
