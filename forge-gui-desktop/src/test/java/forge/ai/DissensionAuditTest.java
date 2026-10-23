package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Dissension script review and R&D's Secret Lair printed text (2026-10-23). */
public class DissensionAuditTest extends AITest {

    private static void resolveTriggers(final Game game) {
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
    }

    private static void run(final String ability, final Card host) {
        SpellAbility sa = AbilityFactory.getAbility(ability, host);
        sa.setActivatingPlayer(host.getController());
        AbilityUtils.resolve(sa);
    }

    @Test
    public void testPalliationAccordUsesShieldCountersAsPrinted() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card accord = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Palliation Accord", "DIS"), p);
        p.getZone(ZoneType.Battlefield).add(accord);
        Card bear = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);

        run("DB$ Tap | Defined$ Self", bear);
        resolveTriggers(game);
        AssertJUnit.assertEquals(1, accord.getCounters(CounterEnumType.SHIELD));
        AssertJUnit.assertEquals(0, accord.getCounters(CounterEnumType.PALLIATION));

        run("DB$ Destroy | Defined$ Self", accord);
        AssertJUnit.assertTrue(accord.isInPlay());
        AssertJUnit.assertEquals(0, accord.getCounters(CounterEnumType.SHIELD));
    }

    @Test
    public void testPalliationAccordUsesPalliationCountersOtherwise() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player p = game.getPlayers().get(1);
        Card accord = addCard("Palliation Accord", p);
        Card bear = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);

        run("DB$ Tap | Defined$ Self", bear);
        resolveTriggers(game);
        AssertJUnit.assertEquals(1, accord.getCounters(CounterEnumType.PALLIATION));
        AssertJUnit.assertEquals(0, accord.getCounters(CounterEnumType.SHIELD));
    }
}
