package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Seek Bolas's Counsel (CMB1 #98), checked against Scryfall and its 13 rulings 2026-10-15: one of six modes at
 * random; Pools of Becoming is reached even outside Planechase and isn't a real plane card.
 */
public class SeekBolassCounselTest extends AITest {

    private static SpellAbility withMode(Card card, Player p, int mode) {
        SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.setSubAbility(null);
        CharmEffect.chainAbilities(sa, Lists.newArrayList(CharmEffect.makePossibleOptions(sa).get(mode)));
        return sa;
    }

    private static Card emblem(Player p, String part) {
        return p.getCardsIn(ZoneType.Command).stream().filter(c -> c.getName().contains(part)).findFirst().orElse(null);
    }

    @Test
    public void oneModeAtRandom() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card counsel = addCardToZone("Seek Bolas's Counsel", ai, ZoneType.Hand);
        SpellAbility sa = counsel.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(6, CharmEffect.makePossibleOptions(sa).size());
        String text = CharmEffect.makeFormatedDescription(sa);
        AssertJUnit.assertTrue(text, text.startsWith("Choose one at random —"));
        AssertJUnit.assertTrue(CharmEffect.makeChoices(sa));
        AssertJUnit.assertNotNull(sa.getSubAbility());
        AssertJUnit.assertNull(sa.getSubAbility().getSubAbility());
    }

    @Test
    public void upkeepEmblemLosesUnlessPaid() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AbilityUtils.resolve(withMode(addCardToZone("Seek Bolas's Counsel", ai, ZoneType.Hand), ai, 0));
        Card emblem = emblem(ai, "upkeep");
        AssertJUnit.assertNotNull(emblem);
        Trigger t = emblem.getTriggers().iterator().next();
        SpellAbility upkeep = t.ensureAbility();
        upkeep.setActivatingPlayer(ai);
        AbilityUtils.resolve(upkeep);
        AssertJUnit.assertTrue("no {U}{B}{R} to pay", ai.hasLost());
    }

    @Test
    public void discardDestroyAndDig() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        addCardToZone("Island", ai, ZoneType.Hand);
        addCardToZone("Shock", opp, ZoneType.Hand);
        AbilityUtils.resolve(withMode(addCardToZone("Seek Bolas's Counsel", ai, ZoneType.Hand), ai, 1));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Hand).stream().noneMatch(c -> c.getName().equals("Island")));
        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Hand).isEmpty());

        Card bears = addCard("Grizzly Bears", opp);
        Card bolas = addCard("Nicol Bolas, Dragon-God", ai);
        bolas.addCounterInternal(CounterEnumType.LOYALTY, 4, ai, false, null, null);
        Card jace = addCard("Jace Beleren", opp);
        jace.addCounterInternal(CounterEnumType.LOYALTY, 3, opp, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(jace.isInZone(ZoneType.Battlefield));
        AbilityUtils.resolve(withMode(addCardToZone("Seek Bolas's Counsel", ai, ZoneType.Hand), ai, 4));
        AssertJUnit.assertTrue(bears.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue(jace.isInZone(ZoneType.Graveyard));
        AssertJUnit.assertTrue("a Bolas planeswalker survives", bolas.isInZone(ZoneType.Battlefield));

        // the library helper adds to the bottom, so the Forest is on top
        Card forest = addCardToZone("Forest", opp, ZoneType.Library);
        Card giant = addCardToZone("Hill Giant", opp, ZoneType.Library);
        Card island = addCardToZone("Island", opp, ZoneType.Library);
        AbilityUtils.resolve(withMode(addCardToZone("Seek Bolas's Counsel", ai, ZoneType.Hand), ai, 3));
        game.getStack().resolveStack();
        AssertJUnit.assertTrue(forest.isInZone(ZoneType.Exile));
        AssertJUnit.assertTrue(island.isInZone(ZoneType.Library));
        Card cast = game.getCardState(giant);
        AssertJUnit.assertTrue("cast for free: " + cast.getZone(), cast.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(ai, cast.getController());
    }

    @Test
    public void poolsOfBecomingOutsidePlanechase() {
        Game game = initAndCreateGame();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertNull(game.getActivePlanes());
        AbilityUtils.resolve(withMode(addCardToZone("Seek Bolas's Counsel", ai, ZoneType.Hand), ai, 2));
        Card pools = emblem(ai, "Pools of Becoming");
        AssertJUnit.assertNotNull(pools);
        AssertJUnit.assertTrue(game.getActivePlanes().contains(pools));
        AssertJUnit.assertEquals(ai, pools.getController());

        // leaving it (a planar die roll after Fractured Powerstone) leaves no plane card behind
        opp.leaveCurrentPlane();
        ai.leaveCurrentPlane();
        AssertJUnit.assertNull(emblem(ai, "Pools of Becoming"));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.PlanarDeck).isEmpty());
        ai.planeswalk(null);
        AssertJUnit.assertTrue("totally lost", game.getActivePlanes().isEmpty());
    }

    @Test
    public void sideboardEmblemCastsNonlandsOnly() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Sideboard);
        Card forest = addCardToZone("Forest", ai, ZoneType.Sideboard);
        AssertJUnit.assertTrue(bolt.mayPlay(ai).isEmpty());
        AbilityUtils.resolve(withMode(addCardToZone("Seek Bolas's Counsel", ai, ZoneType.Hand), ai, 5));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNotNull(emblem(ai, "sideboard"));
        AssertJUnit.assertFalse(bolt.mayPlay(ai).isEmpty());
        AssertJUnit.assertTrue("not lands", forest.mayPlay(ai).isEmpty());
    }
}
