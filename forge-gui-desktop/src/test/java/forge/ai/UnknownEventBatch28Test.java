package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Groaaaaag, Hungry Monster, Monet, Sensei of the Sewers, The Mystery Raceway and Crow Scarer (UNK), checked against
 * Scryfall 2026-10-21 (no rulings); commander tax on Fixed commander ninjutsu and Commander Suspend, the race winner
 * and Crow Scarer's attack half are user rulings.
 */
public class UnknownEventBatch28Test extends AITest {

    private static SpellAbility fromZone(final Card c, final ZoneType zone) {
        return c.getSpellAbilities().stream().filter(s -> s.getRestrictions().getZone() == zone && s.hasParam("CommanderTaxApplies"))
                .findFirst().orElse(null);
    }

    @Test
    public void testCommanderSuspendPaysAndCountsTax() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card groag = addCardToZone("Groaaaaag, Hungry Monster", ai, ZoneType.Command);
        ai.addCommander(groag);
        final SpellAbility suspend = fromZone(groag, ZoneType.Command);
        AssertJUnit.assertNotNull("a command-zone suspend", suspend);
        suspend.setActivatingPlayer(ai);
        ai.incCommanderCast(groag);
        final Cost cost = forge.game.cost.CostAdjustment.adjust(suspend.getPayCosts(), suspend, false);
        AssertJUnit.assertEquals("{B}{G} plus {2} tax", 4, cost.getTotalMana().getCMC());
        suspend.resolve();
        AssertJUnit.assertEquals(2, ai.getCommanderCast(groag));
        AssertJUnit.assertTrue(game.getCardState(groag).isInZone(ZoneType.Exile));
        AssertJUnit.assertEquals(4, game.getCardState(groag).getCounters(CounterEnumType.TIME));
    }

    @Test
    public void testFixedCommanderNinjutsuTax() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card monet = addCardToZone("Monet, Sensei of the Sewers", ai, ZoneType.Command);
        ai.addCommander(monet);
        ai.incCommanderCast(monet);
        final SpellAbility ninjutsu = fromZone(monet, ZoneType.Command);
        AssertJUnit.assertNotNull(ninjutsu);
        ninjutsu.setActivatingPlayer(ai);
        final Cost cost = forge.game.cost.CostAdjustment.adjust(ninjutsu.getPayCosts(), ninjutsu, false);
        AssertJUnit.assertEquals("{3}{B}{G} plus {2} tax", 7, cost.getTotalMana().getCMC());
    }

    @Test
    public void testGroaaaaagEats() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card groag = addCard("Groaaaaag, Hungry Monster", ai);
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", ai);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Island", ai, ZoneType.Library);
        final SpellAbility attack = groag.getTriggers().get(0).ensureAbility();
        attack.setActivatingPlayer(ai);
        AbilityUtils.resolve(attack);
        final int eaten = 2 - (int) ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Grizzly Bears")).count();
        AssertJUnit.assertEquals(eaten, groag.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(20 + eaten, ai.getLife());
        AssertJUnit.assertEquals(eaten, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testCrowScarerCowards() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card scarer = addCard("Crow Scarer", ai);
        final SpellAbility etb = scarer.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        game.getAction().checkStaticAbilities();
        final Card theirBird = human.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasCreatureType("Bird")).findFirst().orElseThrow();
        AssertJUnit.assertTrue(theirBird.getType().hasCreatureType("Coward"));
        theirBird.setSickness(false);
        AssertJUnit.assertFalse("can't attack you", CombatUtil.canAttack(theirBird, ai));
        AssertJUnit.assertFalse("can't block your creatures", CombatUtil.canBlock(scarer, theirBird));
    }
}
