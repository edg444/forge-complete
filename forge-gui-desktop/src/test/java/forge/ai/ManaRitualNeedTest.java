package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Live games, 2026-11-07. The AI cast Pyretic Ritual, then a second Pyretic Ritual off the first one's mana, then a
 * single 1R spell its lands could have cast alone: a ritual only counted whether something was castable afterwards.
 */
public class ManaRitualNeedTest extends AITest {

    private Game game;
    private Player ai;

    private Player setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getPhaseHandler().onStackResolved();
        return ai;
    }

    private AiPlayDecision decide(Card card) {
        final SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    @Test
    public void ritualForASpellTheLandsCanCastAlone() {
        setUp();
        addCards("Mountain", 4, ai);
        final Card ritual = addCardToZone("Pyretic Ritual", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ritual));
    }

    @Test
    public void ritualForASpellOneManaOutOfReach() {
        setUp();
        addCards("Mountain", 3, ai);
        final Card ritual = addCardToZone("Pyretic Ritual", ai, ZoneType.Hand);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ritual));
    }

    @Test
    public void secondRitualWithManaAlreadyFloating() {
        setUp();
        addCards("Mountain", 2, ai);
        final Card ritual = addCardToZone("Pyretic Ritual", ai, ZoneType.Hand);
        addCardToZone("Goblin Piker", ai, ZoneType.Hand);
        for (int i = 0; i < 3; i++) {
            ai.getManaPool().addManaNoEvent(new Mana(MagicColor.RED, ritual, null, ai));
        }
        AssertJUnit.assertNotSame(AiPlayDecision.WillPlay, decide(ritual));
    }

    @Test
    public void ritualThatCastsASecondSpell() {
        setUp();
        addCards("Mountain", 3, ai);
        final Card ritual = addCardToZone("Pyretic Ritual", ai, ZoneType.Hand);
        addCardToZone("Goblin Piker", ai, ZoneType.Hand);
        addCardToZone("Goblin Piker", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, decide(ritual));
    }
}
