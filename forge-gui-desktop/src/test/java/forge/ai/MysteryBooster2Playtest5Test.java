package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.effects.BuddyListEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.CostAdjustment;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * Mystery Booster 2 playtest cards (MB2), part 5, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2Playtest5Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private void cast(final Game game, final Player p, final Card spell) {
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
    }

    private int count(final Player p, final String name) {
        return CardLists.count(p.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals(name));
    }

    @Test
    public void testFriarballCoststorm() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        cast(game, ai, addCardToZone("Llanowar Elves", ai, ZoneType.Hand));
        cast(game, ai, addCardToZone("Fyndhorn Elves", ai, ZoneType.Hand));
        final Card forest = addCardToZone("Forest", ai, ZoneType.Hand);
        final SpellAbility play = forest.getAllPossibleAbilities(ai, true).stream().filter(SpellAbility::isLandAbility).findFirst().orElseThrow();
        play.setActivatingPlayer(ai);
        play.resolve();
        AssertJUnit.assertEquals(1, ai.getLandsPlayedThisTurn());
        cast(game, ai, addCardToZone("Friarball", ai, ZoneType.Hand));
        AssertJUnit.assertEquals("itself plus a copy each for mana values 1 and 0", 3, count(ai, "Monk Token"));
    }

    @Test
    public void testKnightOfLostCauses() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card knight = addCard("Knight of Lost Causes", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(2, knight.getNetPower());
        ai.setLife(10, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(5, knight.getNetPower());
        AssertJUnit.assertTrue(knight.hasKeyword(Keyword.INDESTRUCTIBLE));
        ai.setLife(20, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("at any point during this turn", 5, knight.getNetPower());
        for (final Player p : game.getPlayers()) {
            p.onCleanupPhase();
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("a new turn", 2, knight.getNetPower());
        final Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 4; i++) {
            addCard("Grizzly Bears", opp);
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("three more creatures", 5, knight.getNetPower());
    }

    @Test
    public void testChampionOfTheHareish() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card champion = addCardToZone("Champion of the Hareish", ai, ZoneType.Hand);
        cast(game, ai, champion);
        AssertJUnit.assertEquals("written as it entered", 1, BuddyListEffect.buddyListOf(ai).size());
        cast(game, ai, addCardToZone("Grizzly Bears", ai, ZoneType.Hand));
        AssertJUnit.assertTrue(BuddyListEffect.buddyListOf(ai).contains("Bear"));
        AssertJUnit.assertEquals(0, game.getCardState(champion).getCounters(CounterEnumType.P1P1));
        cast(game, ai, addCardToZone("Grizzly Bears", ai, ZoneType.Hand));
        AssertJUnit.assertEquals(1, game.getCardState(champion).getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(2, BuddyListEffect.buddyListOf(ai).size());
    }

    @Test
    public void testMarchesasSurpriseParty() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card party = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard("Marchesa's Surprise Party"), ai);
        ai.getZone(ZoneType.Command).add(party);
        AssertJUnit.assertTrue(party.getType().hasSubtype("Secret Mission"));
        ai.setGameNote("SecretMission:" + party.getId(), 3);
        for (int i = 0; i < 9; i++) {
            addCardToZone("Island", ai, ZoneType.Graveyard);
        }
        for (int i = 0; i < 20; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, ai);
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(ai), false);
        game.getStack().addAllTriggeredAbilitiesToStack();
        resolveAll(game);
        AssertJUnit.assertTrue(party.isFaceDown());
        AssertJUnit.assertEquals("the reward", hand + 1, ai.getCardsIn(ZoneType.Hand).size());
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(ai), false);
        resolveAll(game);
        AssertJUnit.assertEquals("only once", hand + 1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testMarchesasWrongMission() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card party = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard("Marchesa's Surprise Party"), ai);
        ai.getZone(ZoneType.Command).add(party);
        ai.setGameNote("SecretMission:" + party.getId(), 1);
        for (int i = 0; i < 9; i++) {
            addCardToZone("Island", ai, ZoneType.Graveyard);
        }
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, ai);
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(ai), false);
        resolveAll(game);
        AssertJUnit.assertFalse(party.isFaceDown());
    }

    @Test
    public void testArcanumThingsSwap() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card things = addCard("Arcanum Things", ai);
        things.attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FLYING));
        final Card splitter = addCardToZone("Bonesplitter", ai, ZoneType.Hand);
        final SpellAbility swap = things.getSpellAbilities().stream().filter(s -> s.getParamOrDefault("PrecostDesc", "").equals("Equipment swap")).findFirst().orElseThrow();
        swap.setActivatingPlayer(ai);
        forge.game.ability.AbilityUtils.resolve(swap);
        game.getAction().checkStateEffects(true);
        final Card nowSplitter = game.getCardState(splitter);
        AssertJUnit.assertTrue(nowSplitter.isInPlay());
        AssertJUnit.assertEquals(bears, nowSplitter.getAttachedTo());
        AssertJUnit.assertTrue(game.getCardState(things).isInZone(ZoneType.Hand));
        AssertJUnit.assertEquals("Bonesplitter's +2/+0", 4, bears.getNetPower());
    }

    @Test
    public void testIntangibleVibes() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card vibes = addCard("Intangible Vibes", ai);
        addCard("Blood Artist", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.isToken());
        AssertJUnit.assertFalse("not a creature", vibes.isToken());
        final Player opp = game.getPlayers().get(0);
        final int life = opp.getLife();
        game.getAction().destroy(bears, null, false, null);
        resolveAll(game);
        AssertJUnit.assertEquals("it died", life - 1, opp.getLife());
        AssertJUnit.assertEquals("then ceased to exist", 0, CardLists.count(ai.getCardsIn(ZoneType.Graveyard), c -> c.getName().equals("Grizzly Bears")));
        final Card giant = addCard("Hill Giant", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(giant.isToken());
        game.getAction().destroy(vibes, null, false, null);
        resolveAll(game);
        AssertJUnit.assertFalse(giant.isToken());
    }

    @Test
    public void testTaxTakerCostIncrease() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Tax Taker", ai);
        addCard("Thalia, Guardian of Thraben", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opp);
        final Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        final SpellAbility sa = bolt.getFirstSpellAbility();
        sa.setActivatingPlayer(opp);
        sa.getTargets().add(ai);
        CostAdjustment.adjust(sa.getPayCosts(), sa, false);
        final Card onStack = game.getAction().moveToStack(bolt, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        resolveAll(game);
        AssertJUnit.assertEquals("Thalia's {1}", 1, count(ai, "Treasure Token"));
    }

    @Test
    public void testTaxTakerAttackCostOnce() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Tax Taker", ai);
        addCard("Ghostly Prison", ai);
        for (int i = 0; i < 6; i++) {
            addCard("Plains", opp);
        }
        final Card a = addCard("Grizzly Bears", opp);
        final Card b = addCard("Grizzly Bears", opp);
        a.setSickness(false);
        b.setSickness(false);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp);
        final Combat combat = new Combat(opp);
        combat.addAttacker(a, ai);
        combat.addAttacker(b, ai);
        final boolean paidA = CombatUtil.checkPropagandaEffects(game, a, combat, java.util.List.of());
        final boolean paidB = CombatUtil.checkPropagandaEffects(game, b, combat, java.util.List.of());
        game.firePendingTaxes();
        resolveAll(game);
        final int paid = (paidA ? 2 : 0) + (paidB ? 2 : 0);
        AssertJUnit.assertTrue("the AI paid something", paid > 0);
        AssertJUnit.assertEquals(paid, count(ai, "Treasure Token"));
    }

    @Test
    public void testTaxTakerUnlessCost() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Tax Taker", ai);
        for (int i = 0; i < 4; i++) {
            addCard("Plains", opp);
        }
        final Card host = addCard("Grindstone", ai);
        final SpellAbility unless = AbilityFactory.getAbility("AB$ LoseLife | Cost$ 0 | Defined$ Opponent | LifeAmount$ 5 | UnlessCost$ 2 | UnlessPayer$ Opponent", host);
        unless.setActivatingPlayer(ai);
        final int life = opp.getLife();
        forge.game.ability.AbilityUtils.resolve(unless);
        resolveAll(game);
        final boolean paid = opp.getLife() == life;
        AssertJUnit.assertEquals(paid ? 2 : 0, count(ai, "Treasure Token"));
    }
}
