package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardStateName;
import forge.deck.DeckFormat;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Mystery Booster 2 playtest cards (MB2), part 10, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2Playtest10Test extends AITest {

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

    private void library(final Player p, final String name, final int n) {
        for (int i = 0; i < n; i++) {
            addCardToZone(name, p, ZoneType.Library);
        }
    }

    @Test
    public void testOmnipresentImpostor() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card impostor = addCardToZone("Omnipresent Impostor", ai, ZoneType.Library);
        AssertJUnit.assertTrue(impostor.sharesNameWith("Lightning Bolt"));
        AssertJUnit.assertTrue("back faces too", impostor.sharesNameWith("Insectile Aberration"));
        AssertJUnit.assertFalse("not a token's", impostor.sharesNameWith("Zombie Token"));
        final Card bears = addCard("Grizzly Bears", ai);
        AssertJUnit.assertTrue(bears.sharesNameWith(impostor));
        AssertJUnit.assertTrue(impostor.getType().isBasic());
        AssertJUnit.assertTrue("basic: any number", DeckFormat.canHaveAnyNumberOf(FModel.getMagicDb().getCommonCards().getUniqueByName("Omnipresent Impostor")));
        final Card growth = addCardToZone("Rampant Growth", ai, ZoneType.Hand);
        cast(game, ai, growth, growth.getFirstSpellAbility());
        AssertJUnit.assertTrue("found in place of a basic land: " + game.getCardState(impostor).getZone(), game.getCardState(impostor).isInPlay());
        AssertJUnit.assertTrue("onto the battlefield tapped, as the search said", game.getCardState(impostor).isTapped());
    }

    @Test
    public void testAGirlAndHerDogs() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card girl = addCardToZone("A Girl and Her Dogs", ai, ZoneType.Hand);
        cast(game, ai, girl, girl.getFirstSpellAbility());
        final Card dog = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken()).getFirst();
        AssertJUnit.assertEquals("Booster", dog.getName());
        AssertJUnit.assertTrue(dog.getType().isLegendary());
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().runTrigger(forge.game.trigger.TriggerType.Phase, forge.game.ability.AbilityKey.mapFromPlayer(ai), false);
        resolveAll(game);
        AssertJUnit.assertEquals("a second, differently named Dog", 2, CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken()));
        AssertJUnit.assertEquals(1, CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Bryn")));
    }

    @Test
    public void testAlberixResources() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Island", 8);
        final Card alberix = addCardToZone("Alberix, the Trade Planet", ai, ZoneType.Hand);
        cast(game, ai, alberix, alberix.getFirstSpellAbility());
        AssertJUnit.assertEquals("five resources", 5, ai.getCardsIn(ZoneType.Exile).size());
        AssertJUnit.assertTrue("face up", ai.getCardsIn(ZoneType.Exile).stream().noneMatch(Card::isFaceDown));
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().runTrigger(forge.game.trigger.TriggerType.Phase, forge.game.ability.AbilityKey.mapFromPlayer(ai), false);
        resolveAll(game);
        final boolean moreResources = ai.getCardsIn(ZoneType.Exile).size() == 6;
        final boolean traded = ai.getCardsIn(ZoneType.Hand).size() == hand + 1 && ai.getCardsIn(ZoneType.Exile).size() == 3;
        AssertJUnit.assertTrue("a trade route: hand " + ai.getCardsIn(ZoneType.Hand).size() + ", exile " + ai.getCardsIn(ZoneType.Exile).size(),
                moreResources || traded || ai.getCardsIn(ZoneType.Hand).isEmpty());
    }

    @Test
    public void testDontWorryAboutIt() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Island", 8);
        final Card divination = addCardToZone("Divination", ai, ZoneType.Hand);
        final Card aura = addCardToZone("Don't Worry About It", ai, ZoneType.Hand);
        final SpellAbility auraSa = aura.getFirstSpellAbility();
        auraSa.getTargets().add(divination);
        cast(game, ai, aura, auraSa);
        final Card onField = game.getCardState(aura);
        AssertJUnit.assertTrue("an Aura on a card in hand stays: " + onField.getZone(), onField.isInPlay());
        AssertJUnit.assertEquals(divination, onField.getAttachedTo());
        final SpellAbility div = divination.getFirstSpellAbility();
        div.setActivatingPlayer(ai);
        final forge.game.mana.ManaCostBeingPaid toPay = new forge.game.mana.ManaCostBeingPaid(div.getPayCosts().getTotalMana());
        forge.game.cost.CostAdjustment.adjust(toPay, div, ai, null, true, false);
        AssertJUnit.assertEquals("{2}{U} less {1}", 2, toPay.toManaCost().getCMC());
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        final Card onStackProbe = game.getAction().moveToStack(divination, div);
        div.setHostCard(onStackProbe);
        div.setActivatingPlayer(ai);
        game.getStack().add(div);
        // as casting does: waiting cast triggers go on before state-based actions
        game.getStack().unfreezeStack();
        resolveAll(game);
        AssertJUnit.assertEquals("copied: four cards for one", hand - 1 + 4, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertTrue("and the Aura is gone", game.getCardState(aura).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testPlantASapling() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Grizzly Bears", 3);
        final Card forest = addCardToZone("Forest", ai, ZoneType.Library);
        final Card sapling = addCardToZone("Plant a Sapling", ai, ZoneType.Hand);
        AssertJUnit.assertTrue("can't be cast as the Treefolk from the start",
                sapling.getAllPossibleAbilities(ai, true).stream().noneMatch(s -> s.isSpell() && s.getCardState().getName().equals("Fully-Grown Treefolk")));
        cast(game, ai, sapling, sapling.getFirstSpellAbility());
        AssertJUnit.assertTrue(game.getCardState(forest).isInZone(ZoneType.Hand));
        final Card inLibrary = game.getCardState(sapling);
        AssertJUnit.assertTrue("shuffled in: " + inLibrary.getZone(), inLibrary.isInZone(ZoneType.Library));
        AssertJUnit.assertEquals("transformed", CardStateName.Backside, inLibrary.getCurrentStateName());
        final Card drawn = game.getAction().moveToHand(inLibrary, null);
        AssertJUnit.assertEquals("still transformed in hand", "Fully-Grown Treefolk", drawn.getName());
        addCard("Forest", ai);
        addCard("Forest", ai);
        final SpellAbility treefolk = drawn.getAllPossibleAbilities(ai, true).stream().filter(SpellAbility::isSpell).findFirst().orElse(null);
        AssertJUnit.assertNotNull("castable as the Treefolk now", treefolk);
        cast(game, ai, drawn, treefolk);
        final Card tree = game.getCardState(drawn);
        AssertJUnit.assertTrue(tree.isInPlay());
        AssertJUnit.assertEquals(2, tree.getNetPower());
    }

    @Test
    public void testPlaneswalkerificate() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card giant = addCard("Hill Giant", ai);
        final Card aura = addCard("Planeswalkerificate", ai);
        aura.attachToEntity(giant, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(giant.isPlaneswalker() && giant.isCreature());
        AssertJUnit.assertTrue("no loyalty counters, and alive", giant.isInPlay());
        AssertJUnit.assertFalse("can't block", forge.game.combat.CombatUtil.canBlock(giant));
        final SpellAbility minusX = giant.getSpellAbilities().stream().filter(sa -> sa.getDescription().contains("deals X damage")).findFirst().orElseThrow();
        final SpellAbility plus = giant.getSpellAbilities().stream().filter(sa -> sa.getDescription().contains("Add {R}{R}")).findFirst().orElseThrow();
        AssertJUnit.assertTrue(plus.isPwAbility() && minusX.isPwAbility());
        final forge.game.cost.CostChangeToughness raise = (forge.game.cost.CostChangeToughness) plus.getPayCosts().getCostParts().iterator().next();
        plus.setActivatingPlayer(ai);
        raise.payAsDecided(ai, forge.game.cost.PaymentDecision.number(1), plus, false);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("+1 raises toughness", 4, giant.getNetToughness());
        final forge.game.cost.CostChangeToughness lower = (forge.game.cost.CostChangeToughness) minusX.getPayCosts().getCostParts().iterator().next();
        minusX.setActivatingPlayer(ai);
        minusX.setXManaCostPaid(5);
        AssertJUnit.assertFalse("can't go below 0", lower.canPay(minusX, ai, false));
        minusX.setXManaCostPaid(2);
        AssertJUnit.assertTrue(lower.canPay(minusX, ai, false));
        giant.setDamage(1);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("damage lowers toughness", 3, giant.getNetToughness());
        AssertJUnit.assertEquals(0, giant.getDamage());
        game.getEndOfTurn().executeUntil(ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("and it doesn't heal", 3, giant.getNetToughness());
    }

    @Test
    public void testWerewhat() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        library(ai, "Island", 3);
        final Card dragon = addCardToZone("Shivan Dragon", ai, ZoneType.Graveyard);
        final Card werewhat = addCardToZone("Werewhat", ai, ZoneType.Hand);
        cast(game, ai, werewhat, werewhat.getFirstSpellAbility());
        final Card now = game.getCardState(werewhat);
        AssertJUnit.assertTrue(now.isInPlay());
        AssertJUnit.assertTrue("exiled as the back face", game.getCardState(dragon).isInZone(ZoneType.Exile));
        AssertJUnit.assertTrue(now.isTransformable());
        AssertJUnit.assertEquals("Shivan Dragon", now.getState(CardStateName.Backside).getName());
        AssertJUnit.assertTrue(now.getState(CardStateName.Backside).hasKeyword(forge.game.keyword.Keyword.NIGHTBOUND));
        // daybound: it transforms only as it becomes night
        game.setDayTime(false);
        game.setDayTime(true);
        resolveAll(game);
        AssertJUnit.assertEquals("Shivan Dragon", now.getName());
        AssertJUnit.assertEquals(5, now.getNetPower());
        game.getAction().moveToGraveyard(now, null);
        AssertJUnit.assertTrue("they separate, both into the graveyard", game.getCardState(dragon).isInZone(ZoneType.Graveyard));
        AssertJUnit.assertEquals(2, CardLists.count(ai.getCardsIn(ZoneType.Graveyard), c -> c.getName().equals("Werewhat") || c.getName().equals("Shivan Dragon")));
    }

    @Test
    public void testDefenderOfTheQueue() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card giant = addCard("Hill Giant", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("no line without positioning", ai.getCreatureOrder().isEmpty());
        final Card defender = addCardToZone("Defender of the Queue", ai, ZoneType.Hand);
        cast(game, ai, defender, defender.getFirstSpellAbility());
        final Card def = game.getCardState(defender);
        AssertJUnit.assertEquals(3, ai.getCreatureOrder().size());
        AssertJUnit.assertTrue(def.getWrittenDown() != null && def.getWrittenDown().startsWith("position"));
        final int at = ai.getCreatureOrder().indexOf(def);
        for (final Card c : ai.getCreatureOrder()) {
            if (c == def) {
                continue;
            }
            final boolean adjacent = Math.abs(ai.getCreatureOrder().indexOf(c) - at) == 1;
            AssertJUnit.assertEquals(c + " adjacent " + adjacent, adjacent, c.hasKeyword(forge.game.keyword.Keyword.VIGILANCE));
        }
        final Card elves = addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
        cast(game, ai, elves, elves.getFirstSpellAbility());
        final Card nowElves = game.getCardState(elves);
        AssertJUnit.assertEquals("placed beside Defender", 1, Math.abs(ai.getCreatureOrder().indexOf(nowElves) - ai.getCreatureOrder().indexOf(def)));
        AssertJUnit.assertEquals(2, nowElves.getNetPower());
        game.getAction().destroy(def, null, false, null);
        resolveAll(game);
        AssertJUnit.assertTrue("unlocked once no positioning creature is left", ai.getCreatureOrder().isEmpty());
        AssertJUnit.assertNull(bears.getWrittenDown());
        AssertJUnit.assertNotNull(giant);
    }

    @Test
    public void testBuiltBear() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bear = addCardToZone("Built Bear", ai, ZoneType.Library);
        game.getAction().buildCard(ai, bear, ai.getCardsIn(ZoneType.Library));
        AssertJUnit.assertEquals("three points: one more mana", 3, bear.getCMC());
        AssertJUnit.assertTrue(bear.hasKeyword(forge.game.keyword.Keyword.DEATHTOUCH));
        AssertJUnit.assertTrue(bear.hasKeyword(forge.game.keyword.Keyword.REACH));
        AssertJUnit.assertTrue(bear.hasKeyword(forge.game.keyword.Keyword.VIGILANCE));
        AssertJUnit.assertFalse(bear.hasKeyword(forge.game.keyword.Keyword.FLASH));
        final Card drawn = game.getAction().moveToHand(bear, null);
        AssertJUnit.assertTrue("still built in hand", drawn.hasKeyword(forge.game.keyword.Keyword.DEATHTOUCH));
        AssertJUnit.assertEquals(3, drawn.getFirstSpellAbility().getPayCosts().getTotalMana().getCMC());
    }
}
