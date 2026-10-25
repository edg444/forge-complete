package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.ability.SacrificePayoffAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Second pass, batch 108: AILogic$ SacrificeAny for Kylox's free spells and a Charm's mode (Plunge into Darkness). */
public class NoAiBatch108Test extends AITest {

    // Kylox, Visionary Inventor: its two marked-for-sacrifice creatures go when the library is mostly spells
    @Test
    public void testKyloxSacrificesJunkForSpells() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card kylox = addCard("Kylox, Visionary Inventor", ai);
        Card a = addCard("Grizzly Bears", ai);
        Card b = addCard("Grizzly Bears", ai);
        a.setSVar("SacMe", "1");
        b.setSVar("SacMe", "1");
        addCard("Serra Angel", ai);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
        }
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);
        SpellAbility trig = kylox.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        SacrificePayoffAi.decide(ai, trig);
        AssertJUnit.assertTrue("a library of lands gives nothing to cast", SacrificePayoffAi.planned(trig).isEmpty());

        for (int i = 0; i < 6; i++) {
            addCardToZone("Lightning Bolt", ai, ZoneType.Library);
        }
        SacrificePayoffAi.decide(ai, trig);
        AssertJUnit.assertEquals(2, SacrificePayoffAi.planned(trig).size());
        AssertJUnit.assertTrue(SacrificePayoffAi.planned(trig).contains(a));
    }

    private SpellAbility plungeSac(Game game, Player ai) {
        Card a = addCard("Grizzly Bears", ai);
        Card b = addCard("Grizzly Bears", ai);
        a.setSVar("SacMe", "1");
        b.setSVar("SacMe", "1");
        Card plunge = addCardToZone("Plunge into Darkness", ai, ZoneType.Hand);
        SpellAbility sac = AbilityFactory.getAbility(plunge.getSVar("DBSac"), plunge);
        sac.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        return sac;
    }

    // Plunge into Darkness: 3 life a creature only when the life is needed, and then its junk ones
    @Test
    public void testPlungeIntoDarknessLifeWhenInDanger() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setLife(3, null);
        for (int i = 0; i < 3; i++) {
            addCard("Craw Wurm", opp);
        }
        SpellAbility sac = plungeSac(game, ai);
        AssertJUnit.assertTrue(SacrificePayoffAi.decide(ai, sac).willingToPlay());
        AssertJUnit.assertEquals(2, SacrificePayoffAi.planned(sac).size());
    }

    @Test
    public void testPlungeIntoDarknessKeepsCreaturesWhenSafe() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        SpellAbility sac = plungeSac(game, ai);
        AssertJUnit.assertFalse(SacrificePayoffAi.decide(ai, sac).willingToPlay());
    }
}
