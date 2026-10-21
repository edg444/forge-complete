package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.combat.CombatUtil;
import forge.game.cost.CostAdjustment;
import forge.game.cost.CostOpenBooster;
import forge.game.cost.PaymentDecision;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * 2019 Heroes of the Realm (PH19), Oracle text checked against Scryfall 2026-10-20 (the only ruling on each is the
 * Commander RC's name-on-the-card legality ruling).
 * Collectigull // Only the Best: a Booster Fun card is a printing Scryfall gives the boosterfun promo type.
 * Keeper of the Secret Lair: a Secret Lair spell is one printed in a Secret Lair product.
 * Myntasha, Honored One: the opened pack goes to the bottom of a booster pile outside the game; booster cascade
 * cascades from that pile and puts the rest back on its bottom.
 */
public class HeroesOfTheRealm2019Test extends AITest {

    private Card printing(final String name, final String set, final String number, final Player p) {
        final PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(name, set, number);
        AssertJUnit.assertNotNull(name + " (" + set + " " + number + ")", pc);
        return Card.fromPaperCard(pc, p);
    }

    private Card onZone(final Card c, final ZoneType zone) {
        c.getOwner().getZone(zone).add(c);
        return c;
    }

    @Test
    public void testBoosterFunAndSecretLairComeFromThePrinting() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        AssertJUnit.assertTrue(printing("Faerie Guidemother", "ELD", "274", p).isValid("Card.BoosterFun", p, null, null));
        AssertJUnit.assertFalse(printing("Faerie Guidemother", "ELD", "11", p).isValid("Card.BoosterFun", p, null, null));
        AssertJUnit.assertTrue(printing("Lightning Bolt", "SLD", "83", p).isValid("Card.SecretLair", p, null, null));
        AssertJUnit.assertFalse(printing("Lightning Bolt", "M10", "146", p).isValid("Card.SecretLair", p, null, null));
    }

    @Test
    public void testCollectigullProtectionFromCommon() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card gull = addCard("Collectigull", p);
        final Card spider = onZone(printing("Giant Spider", "M10", "185", opp), ZoneType.Battlefield);
        AssertJUnit.assertFalse(CombatUtil.canBlock(gull, spider));

        final Card commonBolt = onZone(printing("Lightning Bolt", "M10", "146", opp), ZoneType.Hand);
        final SpellAbility common = commonBolt.getFirstSpellAbility();
        common.setActivatingPlayer(opp);
        AssertJUnit.assertFalse(common.canTarget(gull));

        final Card rareBolt = onZone(printing("Lightning Bolt", "SLD", "83", opp), ZoneType.Hand);
        final SpellAbility rare = rareBolt.getFirstSpellAbility();
        rare.setActivatingPlayer(opp);
        AssertJUnit.assertTrue(rare.canTarget(gull));
    }

    @Test
    public void testCollectigullAttackTakesOnlyBoosterFun() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card gull = addCard("Collectigull", p);
        final Trigger attack = gull.getTriggers().get(0);

        final Card plain = printing("Faerie Guidemother", "ELD", "11", p);
        p.getZone(ZoneType.Library).add(plain, 0);
        SpellAbility sa = attack.ensureAbility();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(game.getCardState(plain).isInZone(ZoneType.Library));
        AssertJUnit.assertEquals(game.getCardState(plain), p.getCardsIn(ZoneType.Library).get(0));

        final Card fun = printing("Faerie Guidemother", "ELD", "274", p);
        p.getZone(ZoneType.Library).add(fun, 0);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(game.getCardState(fun).isInZone(ZoneType.Hand));
    }

    @Test
    public void testOnlyTheBestTargetsBoosterFunInYourGraveyard() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card gull = addCardToZone("Collectigull", p, ZoneType.Hand);
        final Card fun = onZone(printing("Faerie Guidemother", "ELD", "274", p), ZoneType.Graveyard);
        final Card plain = onZone(printing("Faerie Guidemother", "ELD", "11", p), ZoneType.Graveyard);
        SpellAbility adventure = null;
        for (final SpellAbility sa : gull.getAllSpellAbilities()) {
            if (sa.isAdventure()) {
                adventure = sa;
            }
        }
        AssertJUnit.assertNotNull(adventure);
        adventure.setActivatingPlayer(p);
        AssertJUnit.assertTrue(adventure.canTarget(fun));
        AssertJUnit.assertFalse(adventure.canTarget(plain));
    }

    @Test
    public void testKeeperGivesSecretLairSpellsFlashAndADiscount() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        addCard("Keeper of the Secret Lair", p);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, p);

        final Card lair = onZone(printing("Ponder", "SLD", "245", p), ZoneType.Hand);
        final Card sorcery = onZone(printing("Divination", "M10", "49", p), ZoneType.Hand);
        final Card lairRing = onZone(printing("Sol Ring", "SLD", "249", p), ZoneType.Hand);
        final SpellAbility ponder = lair.getFirstSpellAbility();
        ponder.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ponder.canCastTiming(p));
        final SpellAbility divination = sorcery.getFirstSpellAbility();
        divination.setActivatingPlayer(p);
        AssertJUnit.assertFalse(divination.canCastTiming(p));

        final SpellAbility ring = lairRing.getFirstSpellAbility();
        ring.setActivatingPlayer(p);
        final ManaCostBeingPaid ringCost = new ManaCostBeingPaid(lairRing.getManaCost());
        CostAdjustment.adjust(ringCost, ring, p, null, true, false);
        AssertJUnit.assertEquals(0, ringCost.getConvertedManaCost());
        final ManaCostBeingPaid divCost = new ManaCostBeingPaid(sorcery.getManaCost());
        CostAdjustment.adjust(divCost, divination, p, null, true, false);
        AssertJUnit.assertEquals(3, divCost.getConvertedManaCost());
    }

    @Test
    public void testOpeningABoosterFillsTheBottomOfThePile() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card myntasha = addCardToZone("Myntasha, Honored One", p, ZoneType.Hand);
        addCards("Forest", 4, p);
        final Card marker = createCard("Grizzly Bears", p);
        p.getZone(ZoneType.BoosterPile).add(marker);

        final SpellAbility sa = myntasha.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        final List<OptionalCostValue> costs = GameActionUtil.getOptionalCostValues(sa);
        AssertJUnit.assertEquals(1, costs.size());
        AssertJUnit.assertEquals(1, p.getController().chooseOptionalCosts(sa, costs).size());

        final CostOpenBooster open = (CostOpenBooster) costs.get(0).getCost().getCostParts().get(0);
        AssertJUnit.assertTrue(open.payAsDecided(p, PaymentDecision.type("M10"), sa, false));
        final List<Card> pile = new java.util.ArrayList<>(p.getZone(ZoneType.BoosterPile).getCards());
        AssertJUnit.assertEquals(16, pile.size());
        AssertJUnit.assertEquals(marker, pile.get(0));
        for (final Card c : pile) {
            AssertJUnit.assertEquals(p, c.getOwner());
            AssertJUnit.assertFalse(p.getAllCards().contains(c));
            if (c != marker) {
                AssertJUnit.assertEquals("M10", c.getSetCode());
            }
        }
    }

    @Test
    public void testBoosterCascadeDigsThePile() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        addCard("Myntasha, Honored One", p);
        addCards("Mountain", 6, p);
        fillLibrary(p, 10);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        final Card forest = createCard("Forest", p);
        final Card bears = createCard("Grizzly Bears", p);
        final Card giant = createCard("Hill Giant", p);
        final Card ogre = createCard("Gray Ogre", p);
        for (final Card c : new Card[] {forest, giant, bears, ogre}) {
            p.getZone(ZoneType.BoosterPile).add(c);
        }
        final int library = p.getCardsIn(ZoneType.Library).size();

        final Card dragon = addCardToZone("Shivan Dragon", p, ZoneType.Hand);
        final SpellAbility sa = dragon.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(p, sa, null));
        AssertJUnit.assertTrue(game.getCardState(dragon).hasKeyword("Booster cascade"));
        playUntilStackClear(game);

        // the forest is passed over and the Giant cast; it's a spell cast too, so it booster-cascades into the Bears,
        // whose own cascade finds nothing below mana value 2 and puts the Forest and Ogre back
        AssertJUnit.assertTrue(game.getCardState(dragon).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(game.getCardState(giant).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Battlefield));
        final List<String> pile = new java.util.ArrayList<>();
        for (final Card c : p.getZone(ZoneType.BoosterPile).getCards()) {
            pile.add(c.getName());
        }
        java.util.Collections.sort(pile);
        AssertJUnit.assertEquals(List.of("Forest", "Gray Ogre"), pile);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Exile).isEmpty());
        AssertJUnit.assertEquals(library, p.getCardsIn(ZoneType.Library).size());
    }
}
