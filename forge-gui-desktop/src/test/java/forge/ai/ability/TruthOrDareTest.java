package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Truth or Dare (CMB1 #32): "Target opponent chooses one". The mode is chosen before targets (CR 601.2b), so the caster
 * picks which opponent chooses (CR 700.2e) and that opponent is the only legal target.
 */
public class TruthOrDareTest extends AITest {

    private Game setUp(int oppLibrary) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCardToZone("Truth or Dare", ai, ZoneType.Hand);
        fillLibrary(ai, 20);
        fillLibrary(opp, oppLibrary);
        addCardToZone("Grizzly Bears", opp, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        return game;
    }

    private static SpellAbility spell(Player ai) {
        Card card = ai.getCardsIn(ZoneType.Hand).get(0);
        SpellAbility sa = card.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        return sa;
    }

    private static List<AbilitySub> chooserPicks(Player ai, Player opp) {
        SpellAbility sa = spell(ai);
        sa.setChoosingPlayer(opp);
        List<AbilitySub> modes = sa.getAdditionalAbilityList("Choices");
        return opp.getController().chooseModeForAbility(sa, modes, 1, 1, false);
    }

    @Test
    public void aiCastsItAndTheOpponentTakesTruth() {
        Game game = setUp(40);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        playUntilPhase(game, PhaseType.END_OF_TURN);
        AssertJUnit.assertEquals("the AI should have cast it", 1, ai.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(40, opp.getCardsIn(ZoneType.Library).size());
        Card inHand = opp.getCardsIn(ZoneType.Hand).get(0);
        AssertJUnit.assertTrue(inHand.getView().canBeShownTo(ai.getView()));
    }

    @Test
    public void chooserTakesDareOnlyWhenItIsFree() {
        Game game = setUp(40);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        AssertJUnit.assertEquals("Truth", chooserPicks(ai, opp).get(0).getDescription().substring(0, 5));

        game = setUp(10);
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        AssertJUnit.assertEquals("Dare ", chooserPicks(ai, opp).get(0).getDescription().substring(0, 5));
    }

    @Test
    public void casterHoldsItWhenDareWouldBeFree() {
        Game game = setUp(10);
        Player ai = game.getPlayers().get(1);
        SpellAbility sa = spell(ai);
        AssertJUnit.assertFalse(SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
    }

    @Test
    public void dareMillsAllButTheBottomTen() {
        Game game = setUp(25);
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card bottom = opp.getCardsIn(ZoneType.Library).get(15);
        SpellAbility sa = spell(ai);
        sa.setChoosingPlayer(opp);
        AbilitySub dare = sa.getAdditionalAbilityList("Choices").get(1);
        dare.setActivatingPlayer(ai);
        dare.setParent(sa);
        dare.getTargets().add(opp);
        dare.resolve();
        AssertJUnit.assertEquals(10, opp.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(15, opp.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Library).contains(bottom));
    }

    @Test
    public void onlyTheChoosingOpponentCanBeTargeted() {
        Game game = initAndCreateThreePlayerGame();
        Player ai = game.getPlayers().get(1);
        Player chooser = game.getPlayers().get(0);
        Player other = game.getPlayers().get(2);
        addCardToZone("Truth or Dare", ai, ZoneType.Hand);
        SpellAbility sa = spell(ai);
        sa.setChoosingPlayer(chooser);
        for (AbilitySub mode : sa.getAdditionalAbilityList("Choices")) {
            mode.setActivatingPlayer(ai);
            mode.setParent(sa);
            AssertJUnit.assertTrue(mode.canTarget(chooser));
            AssertJUnit.assertFalse(mode.canTarget(other));
            AssertJUnit.assertFalse(mode.canTarget(ai));
        }
    }
}
