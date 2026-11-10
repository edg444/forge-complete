package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Alchemy: Reality Fracture (YFRA) cards the fork scripted before upstream, checked against Scryfall 2026-11-10 (no
 * rulings on any of them). Actualize counts spells that weren't from the starting deck; cards added by these tests
 * aren't from it unless marked.
 */
public class AlchemyRealityFractureTest extends AITest {

    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getPhaseHandler().onStackResolved();
    }

    private SpellAbility cast(Card c, Object target) {
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        if (target instanceof Card t) {
            sa.getTargets().add(t);
        } else if (target instanceof Player p) {
            sa.getTargets().add(p);
        }
        final Card moved = game.getAction().moveToStack(c, sa);
        sa.setHostCard(moved);
        game.getStack().add(sa);
        return sa;
    }

    private Card starting(Card c) {
        c.setStartingDeckCard(true);
        return c;
    }

    @Test
    public void actualizeCountsOnlySpellsNotFromTheStartingDeck() {
        setUp();
        final Card goyf = addCard("Echogoyfs", ai);
        final Card etali = addCardToZone("Etali, Primal Garden", ai, ZoneType.Hand);
        cast(addCardToZone("Grizzly Bears", ai, ZoneType.Hand), null);
        cast(starting(addCardToZone("Grizzly Bears", ai, ZoneType.Hand)), null);
        cast(addCardToZone("Grizzly Bears", ai, ZoneType.Hand), null);
        playUntilStackClear(game);
        AssertJUnit.assertEquals(2, ai.getSpellsCastNotFromStartingDeckThisGame());
        AssertJUnit.assertEquals(2, goyf.getNetPower());
        AssertJUnit.assertEquals(3, goyf.getNetToughness());

        final SpellAbility etaliSa = etali.getFirstSpellAbility();
        etaliSa.setActivatingPlayer(ai);
        final ManaCostBeingPaid cost = ComputerUtilMana.calculateManaCost(etaliSa.getPayCosts(), etaliSa, ai, true, 0, false);
        AssertJUnit.assertEquals("{6}{G} less 2", 5, cost.getConvertedManaCost());
    }

    @Test
    public void flourishingForcesCountsLifeGainedAllGame() {
        setUp();
        ai.gainLife(4, null, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        ai.gainLife(3, null, null);
        AssertJUnit.assertEquals(7, ai.getLifeGainedThisGame());
        AbilityUtils.resolve(cast(addCardToZone("Flourishing Forces", ai, ZoneType.Hand), null));
        final List<Card> cadets = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Cadet"));
        AssertJUnit.assertEquals("one plus one for every 3 life", 3, cadets.size());
    }

    @Test
    public void remnantsPutsTheEmpowerCountersOnARealJace() {
        setUp();
        final Card jace = addCard("Jace Beleren", ai);
        jace.addCounterInternal(CounterEnumType.LOYALTY, 3, ai, false, null, null);
        final Card remnants = addCard("Remnants of Beleren", ai);
        final SpellAbility empower = remnants.getSpellAbilities().stream()
                .filter(s -> s.getApi() == forge.game.ability.ApiType.Empower).findFirst().orElseThrow();
        empower.setActivatingPlayer(ai);
        AbilityUtils.resolve(empower);
        AssertJUnit.assertEquals(4, jace.getCounters(CounterEnumType.LOYALTY));
        AssertJUnit.assertTrue("no Jace token", CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isToken).isEmpty());
    }

    @Test
    public void remnantsWithoutAJaceStillEmpowers() {
        setUp();
        final Card remnants = addCard("Remnants of Beleren", ai);
        final SpellAbility empower = remnants.getSpellAbilities().stream()
                .filter(s -> s.getApi() == forge.game.ability.ApiType.Empower).findFirst().orElseThrow();
        empower.setActivatingPlayer(ai);
        AbilityUtils.resolve(empower);
        final List<Card> tokens = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isToken);
        AssertJUnit.assertEquals(1, tokens.size());
        AssertJUnit.assertEquals(1, tokens.get(0).getCounters(CounterEnumType.LOYALTY));
    }

    @Test
    public void jacesSagaEmpowersThree() {
        setUp();
        final Card saga = addCard("Jace's Saga", ai);
        AssertJUnit.assertEquals("3", saga.getSVar("EmpowerJace").replaceAll(".*Num\\$ (\\w+).*", "$1"));
    }

    @Test
    public void enterTheEternitiesMakesThePlaneswalker() {
        setUp();
        final Card bears = addCard("Grizzly Bears", ai);
        bears.addCounterInternal(CounterEnumType.P1P1, 2, ai, false, null, null);
        AbilityUtils.resolve(cast(addCardToZone("Enter the Eternities", ai, ZoneType.Hand), bears));
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Exile));
        final List<Card> tokens = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isToken);
        AssertJUnit.assertEquals(1, tokens.size());
        final Card walker = tokens.get(0);
        AssertJUnit.assertEquals("Grizzly Bears", walker.getName());
        AssertJUnit.assertTrue(walker.isPlaneswalker() && walker.getType().isLegendary());
        AssertJUnit.assertTrue("green", walker.getColor().hasGreen() && walker.getColor().countColors() == 1);
        AssertJUnit.assertEquals("power as it last existed", 4, walker.getCounters(CounterEnumType.LOYALTY));
        AssertJUnit.assertEquals(3, walker.getSpellAbilities().stream().filter(SpellAbility::isPwAbility).count());

        // every pooled ability has to work on the token, sub-abilities and counts included
        for (final String prefix : new String[] {"Plus", "Minus", "Ult"}) {
            for (int i = 1; i <= 10; i++) {
                AssertJUnit.assertNotNull(prefix + i, AbilityFactory.getAbility(walker, prefix + i));
            }
        }
        final int life = ai.getLife();
        final SpellAbility gain = AbilityFactory.getAbility(walker, "Plus3");
        gain.setActivatingPlayer(ai);
        AbilityUtils.resolve(gain);
        AssertJUnit.assertEquals(life + 2, ai.getLife());
        AssertJUnit.assertEquals(5, walker.getCounters(CounterEnumType.LOYALTY));
    }

    @Test
    public void valgavothPumpsCardsAndOpensTheGraveyard() {
        setUp();
        addCard("Valgavoth, the Lifeline", ai);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final Card giant = addCardToZone("Hill Giant", ai, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("no life gained yet", giant.mayPlay(ai).isEmpty());
        ai.gainLife(1, null, null);
        playUntilStackClear(game);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, bears.getNetPower());
        AssertJUnit.assertEquals(4, giant.getNetPower());
        AssertJUnit.assertFalse("life gained this turn", giant.mayPlay(ai).isEmpty());
    }

    @Test
    public void nyleasOracleConjuresItsDevotion() {
        setUp();
        addCard("Llanowar Elves", ai);
        cast(addCardToZone("Nylea's Oracle", ai, ZoneType.Hand), null);
        playUntilStackClear(game);
        // the enters trigger is stacked only after the loop has seen an empty stack
        playUntilStackClear(game);
        final int conjured = CardLists.filter(ai.getCardsIn(ZoneType.Library), c -> c.getName().equals("Nylea's Oracle")).size();
        AssertJUnit.assertEquals("devotion GG + G", 3, conjured);
        final Card oracle = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Nylea's Oracle")).get(0);
        // the test deck is empty, so the library is bigger than it
        AssertJUnit.assertEquals(6, oracle.getNetPower());
        AssertJUnit.assertTrue(oracle.hasKeyword(forge.game.keyword.Keyword.HASTE));
    }

    @Test
    public void willboundQuillCopiesOnlySpellsNotFromTheStartingDeck() {
        setUp();
        addCard("Willbound Quill", ai);
        final int life = opp.getLife();
        cast(starting(addCardToZone("Lightning Bolt", ai, ZoneType.Hand)), opp);
        playUntilStackClear(game);
        AssertJUnit.assertEquals(life - 3, opp.getLife());
        cast(addCardToZone("Lightning Bolt", ai, ZoneType.Hand), opp);
        playUntilStackClear(game);
        AssertJUnit.assertEquals("original and copy", life - 9, opp.getLife());
    }

    @Test
    public void cruxEmpowersOnActualize() {
        setUp();
        addCard("Crux of the Theory", ai);
        cast(addCardToZone("Grizzly Bears", ai, ZoneType.Hand), null);
        playUntilStackClear(game);
        final List<Card> jaces = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && c.getType().hasSubtype("Jace"));
        AssertJUnit.assertEquals(1, jaces.size());
        AssertJUnit.assertEquals(2, jaces.get(0).getCounters(CounterEnumType.LOYALTY));
    }
}
