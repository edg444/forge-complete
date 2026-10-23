package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Ravnica: City of Guilds script review and R&D's Secret Lair printed text (2026-10-23). */
public class RavnicaAuditTest extends AITest {

    private Card addPrinting(final String name, final Player p, final ZoneType zone) {
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, "RAV"), p);
        p.getZone(zone).add(c);
        return c;
    }

    private static SpellAbility playableSpell(final Card c) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            sa.setActivatingPlayer(c.getController());
            if (sa.isSpell() && sa.getRestrictions().checkOtherRestrictions(c, sa, c.getController())) {
                return sa;
            }
        }
        return null;
    }

    private static void castAndResolve(final Game game, final Card inHand, final Object target) {
        SpellAbility sa = playableSpell(inHand);
        sa.setHostCard(game.getAction().moveToStack(inHand, sa));
        if (target instanceof SpellAbility) {
            sa.getTargets().add((SpellAbility) target);
        } else {
            sa.getTargets().add((Card) target);
        }
        game.getStack().add(sa);
        game.getStack().resolveStack();
        game.getAction().checkStateEffects(true);
    }

    private int remandIntoVulturousZombie(final boolean lair) {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card zombie = addCard("Vulturous Zombie", p);
        addCardToZone("Island", p, ZoneType.Library);
        Card bears = game.getAction().moveToStack(addCardToZone("Grizzly Bears", opp, ZoneType.Hand), null);
        SpellAbility bearsSpell = bears.getFirstSpellAbility();
        bearsSpell.setActivatingPlayer(opp);
        game.getStack().add(bearsSpell);
        castAndResolve(game, addPrinting("Remand", p, ZoneType.Hand), bearsSpell);
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Hand).stream().anyMatch(c -> c.getName().equals("Grizzly Bears")));
        AssertJUnit.assertEquals(1, p.getCardsIn(ZoneType.Hand).size());
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        if (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        return zombie.getCounters(CounterEnumType.P1P1);
    }

    @Test
    public void testRemandCountersIntoTheGraveyardFirstAsPrinted() {
        AssertJUnit.assertEquals(0, remandIntoVulturousZombie(false));
        AssertJUnit.assertEquals(1, remandIntoVulturousZombie(true));
    }

    private boolean inciteHysteriaLeavesBlocker(final boolean lair, final String targetName) {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player p = game.getPlayers().get(1);
        if (lair) {
            addCard("R&D's Secret Lair", p);
        }
        Card target = addCard(targetName, opp);
        castAndResolve(game, addPrinting("Incite Hysteria", p, ZoneType.Hand), target);
        return CombatUtil.canBlock(target);
    }

    @Test
    public void testInciteHysteriaMissesAColorlessTargetAsPrinted() {
        AssertJUnit.assertFalse(inciteHysteriaLeavesBlocker(false, "Ornithopter"));
        AssertJUnit.assertTrue(inciteHysteriaLeavesBlocker(true, "Ornithopter"));
        AssertJUnit.assertFalse(inciteHysteriaLeavesBlocker(true, "Hill Giant"));
    }
}
