package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/** Modern Horizons, Core Set 2020 and Commander 2019 review (2026-10-23). */
public class Commander2019AuditTest extends AITest {

    @Test
    public void testLegendOfArenaPlaneswalkerEntersWithWizardLoyalty() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card arena = addCard("The Legend of Arena", p);
        addCard("Ghitu Lavarunner", p);
        addCard("Ghitu Lavarunner", p);
        Card jace = addCardToZone("Jace Beleren", p, ZoneType.Library);
        game.getAction().checkStateEffects(true);
        SpellAbility chapterThree = null;
        for (Trigger t : arena.getTriggers()) {
            if (t.ensureAbility().getApi() == ApiType.ChangeZone) {
                chapterThree = t.ensureAbility();
            }
        }
        chapterThree.setActivatingPlayer(p);
        AbilityUtils.resolve(chapterThree);
        Card onBattlefield = game.getCardState(jace);
        AssertJUnit.assertTrue(onBattlefield.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(5, onBattlefield.getCounters(CounterEnumType.LOYALTY));
    }

    @Test
    public void testEntwineReminderSaysAllForMoreThanTwoModes() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card guile = addCardToZone("Kaya's Guile", p, ZoneType.Hand);
        AssertJUnit.assertTrue(guile.getAbilityText(guile.getCurrentState()).contains("(Choose all if you pay the entwine cost.)"));
        Card party = addCardToZone("Goblin War Party", p, ZoneType.Hand);
        AssertJUnit.assertTrue(party.getAbilityText(party.getCurrentState()).contains("(Choose both if you pay the entwine cost.)"));
    }
}
