package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Invasion script review and R&D's Secret Lair printed text (2026-10-23). */
public class InvasionAuditTest extends AITest {

    private Card addPrinting(final String name, final Player p, final ZoneType zone) {
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, "INV"), p);
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

    @Test
    public void testElvishChampionPumpsItselfAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card champion = addPrinting("Elvish Champion", p, ZoneType.Battlefield);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, champion.getNetPower());
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, champion.getNetPower());
    }

    @Test
    public void testKangeePumpsItselfAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card kangee = addPrinting("Kangee, Aerie Keeper", p, ZoneType.Battlefield);
        kangee.setCounters(CounterType.getType("FEATHER"), 2);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, kangee.getNetPower());
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, kangee.getNetPower());
    }

    @Test
    public void testOptDoesNotScryAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card opt = addPrinting("Opt", p, ZoneType.Hand);
        AssertJUnit.assertEquals(ApiType.Scry, playableSpell(opt).getApi());
        addCard("R&D's Secret Lair", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(ApiType.Dig, playableSpell(opt).getApi());
    }

    @Test
    public void testCauldronDancePutsTheHandCreatureIntoTheGraveyardAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("R&D's Secret Lair", p);
        Card dead = addCardToZone("Grizzly Bears", p, ZoneType.Graveyard);
        Card inHand = addCardToZone("Hill Giant", p, ZoneType.Hand);
        Card dance = addPrinting("Cauldron Dance", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = playableSpell(dance);
        AssertJUnit.assertTrue(sa.getDescription().startsWith("(As printed)"));
        sa.setHostCard(game.getAction().moveToStack(dance, sa));
        sa.getTargets().add(dead);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c -> c.getName().equals("Hill Giant")));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, opp);
        playUntilPhase(game, PhaseType.CLEANUP);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Hand).stream().anyMatch(c -> c.getName().equals("Grizzly Bears")));
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Graveyard).stream().anyMatch(c -> c.getName().equals("Hill Giant")));
        AssertJUnit.assertFalse(inHand.isInZone(ZoneType.Hand));
    }
}
