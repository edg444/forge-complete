package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardType;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.cost.CostAdjustment;
import forge.game.cost.CostGiveUpLandDrop;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityCoinDieSwap;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Mystery Booster 2 playtest cards (MB2), part 9, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2Playtest9Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Game game, final Player p, final Card spell, final SpellAbility sa) {
        sa.setActivatingPlayer(p);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    private void lands(final Player p, final String name, final int n) {
        for (int i = 0; i < n; i++) {
            addCard(name, p);
        }
    }

    @Test
    public void testKozilekCompleated() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card kozilek = addCardToZone("Kozilek, Compleated", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(10, kozilek.getCMC());
        int cp = 0;
        for (final ManaCostShard s : kozilek.getManaCost()) {
            cp += s == ManaCostShard.CP ? 1 : 0;
        }
        AssertJUnit.assertEquals(2, cp);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", opp, ZoneType.Hand);
        }
        cast(game, ai, kozilek, kozilek.getFirstSpellAbility());
        AssertJUnit.assertEquals(2, opp.getPoisonCounters());
        AssertJUnit.assertEquals("down to two", 2, opp.getCardsIn(ZoneType.Hand).size());
        lands(opp, "Plains", 3);
        Trigger annihinfect = null;
        for (final Trigger t : game.getCardState(kozilek).getTriggers()) {
            if (t.getParam("TriggerDescription").startsWith("Annihinfect")) {
                annihinfect = t;
            }
        }
        AssertJUnit.assertNotNull(annihinfect);
        final Card now = game.getCardState(kozilek);
        now.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);
        final Combat combat = new Combat(ai);
        combat.addAttacker(now, opp);
        game.getPhaseHandler().setCombat(combat);
        game.getTriggerHandler().runTrigger(forge.game.trigger.TriggerType.Attacks,
                forge.game.ability.AbilityKey.mapFromCard(now), false);
        final java.util.Map<forge.game.ability.AbilityKey, Object> params = forge.game.ability.AbilityKey.mapFromCard(now);
        params.put(forge.game.ability.AbilityKey.Attacker, now);
        params.put(forge.game.ability.AbilityKey.Defender, opp);
        params.put(forge.game.ability.AbilityKey.DefendingPlayer, opp);
        game.getTriggerHandler().runTrigger(forge.game.trigger.TriggerType.Attacks, params, false);
        resolveAll(game);
        AssertJUnit.assertEquals("two permanents for two poison counters", 1, opp.getCardsIn(ZoneType.Battlefield).size());
    }

    @Test
    public void testPokeySwapsOnlyForItsController() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        AssertJUnit.assertFalse(StaticAbilityCoinDieSwap.applies(ai, StaticAbilityMode.CoinFlipAsD20));
        addCard("Pokey, the Scallywagg", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(StaticAbilityCoinDieSwap.applies(ai, StaticAbilityMode.CoinFlipAsD20));
        AssertJUnit.assertTrue(StaticAbilityCoinDieSwap.applies(ai, StaticAbilityMode.D20AsCoinFlip));
        AssertJUnit.assertFalse(StaticAbilityCoinDieSwap.applies(opp, StaticAbilityMode.CoinFlipAsD20));
        StaticAbilityCoinDieSwap.setSwapping(true);
        AssertJUnit.assertFalse("a swapped event isn't swapped back", StaticAbilityCoinDieSwap.applies(ai, StaticAbilityMode.D20AsCoinFlip));
        StaticAbilityCoinDieSwap.setSwapping(false);
    }

    @Test
    public void testChatzukBands() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final Card chatzuk = addCard("Chatzuk, Mighty Guitarist", ai);
        final Card pegasus = addCard("Mesa Pegasus", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card loner = addCard("Hill Giant", ai);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);
        final Combat combat = new Combat(ai);
        combat.addAttacker(chatzuk, opp);
        final AttackingBand band = combat.getBandOfAttacker(chatzuk);
        combat.addAttacker(pegasus, opp, band);
        combat.addAttacker(bears, opp, band);
        combat.addAttacker(loner, opp);
        game.getPhaseHandler().setCombat(combat);
        final SpellAbility pump = chatzuk.getTriggers().getFirst().ensureAbility();
        pump.setActivatingPlayer(ai);
        AbilityUtils.resolve(pump);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("+3/+3 in a band of three", 5, chatzuk.getNetPower());
        AssertJUnit.assertEquals(5, bears.getNetPower());
        AssertJUnit.assertEquals("not in the band", 3, loner.getNetPower());
        final Card handPegasus = addCardToZone("Mesa Pegasus", ai, ZoneType.Hand);
        final SpellAbility sa = handPegasus.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final forge.game.mana.ManaCostBeingPaid toPay = new forge.game.mana.ManaCostBeingPaid(sa.getPayCosts().getTotalMana());
        CostAdjustment.adjust(toPay, sa, ai, null, true, false);
        AssertJUnit.assertEquals("{1}{W} less {2}: the colored part stays", 1, toPay.toManaCost().getCMC());
    }

    @Test
    public void testLuxiorIgnited() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card luxior = addCard("Luxior, Ignited", ai);
        AssertJUnit.assertTrue(luxior.isPlaneswalker() && luxior.isEquipment());
        final Card bears = addCard("Grizzly Bears", ai);
        final SpellAbility plus = luxior.getSpellAbilities().stream().filter(s -> s.getParamOrDefault("SpellDescription", "").startsWith("Attach")).findFirst().orElseThrow();
        plus.setActivatingPlayer(ai);
        plus.getTargets().add(bears);
        final forge.game.GameEntityCounterTable table = new forge.game.GameEntityCounterTable();
        luxior.addCounter(CounterEnumType.LOYALTY, 1, ai, table);
        table.replaceCounterEffect(game, null);
        AbilityUtils.resolve(plus);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(bears, luxior.getAttachedTo());
        AssertJUnit.assertEquals("one counter each", 2 + luxior.getCounters(CounterEnumType.LOYALTY), bears.getNetPower());
    }

    @Test
    public void testOrbOfOrigin() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card orb = addCard("Orb of Origin", ai);
        AssertJUnit.assertTrue(orb.getType().hasSupertype(CardType.Supertype.Poly));
        final Card stone = addCard("Mind Stone", ai);
        final Card bomb = addCard("Pyrite Spellbomb", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(stone.getType().hasSupertype(CardType.Supertype.Mono));
        AssertJUnit.assertTrue(stone.getType().hasSupertype(CardType.Supertype.Continuous));
        AssertJUnit.assertFalse("only other artifacts", orb.getType().hasSupertype(CardType.Supertype.Mono));
        final SpellAbility ping = bomb.getSpellAbilities().stream().filter(s -> s.isActivatedAbility() && !s.getPayCosts().hasTapCost()).findFirst().orElseThrow();
        ping.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("mono: an additional {T}", CostAdjustment.adjust(ping.getPayCosts(), ping, false).hasTapCost());
        AssertJUnit.assertFalse(stone.getManaAbilities().isEmpty());
        stone.setTapped(true);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("continuous and tapped: no abilities", stone.getManaAbilities().isEmpty());
    }

    @Test
    public void testBoulderJockeyLandDrop() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card jockey = addCardToZone("Boulder Jockey", ai, ZoneType.Hand);
        AssertJUnit.assertEquals("{D} isn't mana", 3, jockey.getCMC());
        final SpellAbility sa = jockey.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final CostGiveUpLandDrop drop = (CostGiveUpLandDrop) sa.getPayCosts().getCostParts().stream()
                .filter(p -> p instanceof CostGiveUpLandDrop).findFirst().orElseThrow();
        AssertJUnit.assertTrue(drop.canPay(sa, ai, false));
        AssertJUnit.assertFalse("not on another player's turn", drop.canPay(sa, opp, false));
        ai.addLandDropsGivenUp(1);
        AssertJUnit.assertFalse("no land drop left to give", drop.canPay(sa, ai, false));
        final Card forest = addCardToZone("Forest", ai, ZoneType.Hand);
        AssertJUnit.assertFalse("and none to play", ai.canPlayLand(forest, false, null));
    }

    @Test
    public void testKeeperOfTheCrown() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card keeper = addCardToZone("Keeper of the Crown", ai, ZoneType.Hand);
        AssertJUnit.assertEquals(3, keeper.getCMC());
        AssertJUnit.assertTrue("its color indicator", keeper.isGreen());
        lands(ai, "Forest", 3);
        final SpellAbility sa = keeper.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("no legendary source", ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
        addCard("Pendelhaven", ai);
        AssertJUnit.assertTrue(ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
        final Card bears = addCard("Grizzly Bears", ai);
        final Card coronation = addCardToZone("Keeper of the Crown", ai, ZoneType.Hand);
        final SpellAbility adventure = coronation.getAllPossibleAbilities(ai, true).stream()
                .filter(s -> s.isSpell() && s.getCardState() != null && s.getCardState().getName().equals("Coronation of the Wilds")).findFirst().orElseThrow();
        adventure.getTargets().add(bears);
        library(ai);
        cast(game, ai, coronation, adventure);
        AssertJUnit.assertTrue(bears.getType().isLegendary());
        AssertJUnit.assertTrue(bears.getType().hasCreatureType("Noble"));
        AssertJUnit.assertFalse(bears.getManaAbilities().isEmpty());
        AssertJUnit.assertTrue("on an adventure", game.getCardState(coronation).isInZone(ZoneType.Exile));
    }

    private void library(final Player p) {
        for (int i = 0; i < 3; i++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
    }

    @Test
    public void testAllStarKicker() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card plain = addCardToZone("All-Star Kicker", ai, ZoneType.Hand);
        cast(game, ai, plain, plain.getFirstSpellAbility());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("not kicked, no pump", 2, bears.getNetPower());
        final Card kicked = addCardToZone("All-Star Kicker", ai, ZoneType.Hand);
        final SpellAbility sa = kicked.getFirstSpellAbility().copy();
        sa.addOptionalCost(OptionalCost.Kicker1);
        cast(game, ai, kicked, sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("kicked: +1/+1", 3, bears.getNetPower());
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.HASTE));
        AssertJUnit.assertEquals(2, CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("All-Star Kicker")));
    }
}
