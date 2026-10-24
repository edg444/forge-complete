package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.cost.CostPaySpeed;
import forge.game.cost.PaymentDecision;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Chicago Loop, ______, The Playful Winners, Huge Truck and Split into Three Parts (UNK), checked against Scryfall
 * 2026-10-22 (no rulings).
 */
public class UnknownEventBatch54Test extends AITest {

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void testChicagoLoopSpeedAndOrangeBear() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card loop = addCard("Chicago Loop", ai);
        ai.setSpeed(3);
        final SpellAbility sa = loop.getManaAbilities().stream()
                .filter(m -> m.getPayCosts().toString().contains("speed")).findFirst().orElseThrow();
        sa.setActivatingPlayer(ai);
        final CostPaySpeed part = (CostPaySpeed) sa.getPayCosts().getCostParts().stream()
                .filter(p -> p instanceof CostPaySpeed).findFirst().orElseThrow();
        AssertJUnit.assertEquals(Integer.valueOf(3), part.getMaxAmountX(sa, ai, false));
        part.payAsDecided(ai, PaymentDecision.number(2), sa, false);
        AssertJUnit.assertEquals(1, ai.getSpeed());
        part.payAsDecided(ai, PaymentDecision.number(5), sa, false);
        AssertJUnit.assertEquals("no lower than no speed", 0, ai.getSpeed());

        AbilityUtils.resolve(svar(loop, "DBBear", ai));
        final Card bear = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Bear")).get(0);
        AssertJUnit.assertTrue(bear.getColor().hasAnyColor(MagicColor.Color.ORANGE));
        AssertJUnit.assertFalse(bear.getColor().hasAnyColor(MagicColor.Color.RED));
        AssertJUnit.assertTrue(bear.hasKeyword(Keyword.HASTE));
        AbilityUtils.resolve(svar(loop, "DBDinosaur", ai));
        final Card dino = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Dinosaur")).get(0);
        AssertJUnit.assertTrue(dino.getType().hasCreatureType("Skeleton"));
        AssertJUnit.assertEquals(3, dino.getNetPower());
        AssertJUnit.assertTrue(dino.hasKeyword(Keyword.TRAMPLE));
    }

    @Test
    public void testBlankNameIsFilledIn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card blank = addCard("______", ai);
        addCard("Grizzly Bears", ai);
        addCard("Runeclaw Bear", ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        AbilityUtils.resolve(svar(blank, "TrigName", ai));
        final String first = ai.getName().trim().split("\\s+")[0];
        AssertJUnit.assertEquals(first + "'s Bear and Instants", blank.getName());
        AbilityUtils.resolve(svar(blank, "TrigName", ai));
        AssertJUnit.assertEquals("only the first time", first + "'s Bear and Instants", blank.getName());

        final Card bear = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        final Card shock = addCardToZone("Shock", ai, ZoneType.Hand);
        final Card divination = addCardToZone("Divination", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(bear.hasProperty("writtenNameAllows", ai, blank, null));
        AssertJUnit.assertTrue(shock.hasProperty("writtenNameAllows", ai, blank, null));
        AssertJUnit.assertFalse(divination.hasProperty("writtenNameAllows", ai, blank, null));
        blank.setWrittenName("Gavin's Dragon and Sorceries");
        AssertJUnit.assertTrue(divination.hasProperty("writtenNameAllows", ai, blank, null));
        AssertJUnit.assertTrue("starts with G", bear.hasProperty("writtenNameAllows", ai, blank, null));
        AssertJUnit.assertFalse(shock.hasProperty("writtenNameAllows", ai, blank, null));
    }

    @Test
    public void testPlayfulWinnersCopiesAPartner() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card winners = addCard("The Playful Winners", ai);
        AbilityUtils.resolve(svar(winners, "DBCopy", ai));
        // the partner is picked at random; a planeswalker partner (Will or Rowan Kenrith) has no power to check
        for (int i = 0; i < 20 && !winners.isCreature(); i++) {
            AbilityUtils.resolve(svar(winners, "DBCopy", ai));
        }
        AssertJUnit.assertFalse(winners.getName().equals("The Playful Winners"));
        AssertJUnit.assertTrue(winners.getName(), winners.getKeywords().stream().anyMatch(k ->
                k.getOriginal().startsWith("Partner") || k.getOriginal().startsWith("Friends forever")));
        AssertJUnit.assertEquals("TallyOnWin kept", "True", winners.getSVar("TallyOnWin"));
        final int power = winners.getNetPower();
        winners.setTallyMarks(2);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(power + 4, winners.getNetPower());
    }

    @Test
    public void testHugeTruckKeepsBackupAbilities() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card truck = addCard("Huge Truck", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card valkyrie = addCard("Boon-Bringer Valkyrie", ai);
        final Trigger backup = valkyrie.getTriggers().stream().filter(t -> t.isKeyword(Keyword.BACKUP)).findFirst().orElseThrow();
        final SpellAbility backupSA = backup.getOverridingAbility();
        AssertJUnit.assertTrue(backupSA.isBackup());
        final SpellAbility share = svar(truck, "TrigShare", ai);
        share.setTriggeringObject(AbilityKey.SourceSA, backupSA);
        share.setTriggeringObject(AbilityKey.Target, bears);
        AbilityUtils.resolve(share);
        AssertJUnit.assertTrue(truck.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(truck.hasKeyword(Keyword.FIRST_STRIKE));
        AssertJUnit.assertTrue(truck.hasKeyword(Keyword.LIFELINK));
        game.getEndOfTurn().executeUntil();
        game.getCleanup().executeUntil();
        AssertJUnit.assertTrue("permanently", truck.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.FLYING));
    }

    @Test
    public void testSplitIntoThreeParts() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        final Card split = addCardToZone("Split into Three Parts", ai, ZoneType.Hand);
        final SpellAbility sa = split.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(human.getCardsIn(ZoneType.Battlefield).isEmpty());
        final java.util.List<Card> exiled = CardLists.filter(game.getCardsIn(ZoneType.Exile), c -> c.getName().equals("Grizzly Bears"));
        AssertJUnit.assertEquals(3, exiled.size());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("the tokens stay in exile", 3,
                CardLists.filter(game.getCardsIn(ZoneType.Exile), c -> c.getName().equals("Grizzly Bears")).size());
        final java.util.Set<Integer> counters = new java.util.TreeSet<>();
        for (final Card c : exiled) {
            counters.add(c.getCounters(CounterEnumType.TIME));
            AssertJUnit.assertTrue(c + " has suspend", c.hasKeyword(Keyword.SUSPEND));
        }
        AssertJUnit.assertEquals(java.util.Set.of(2, 3, 4), counters);
        AssertJUnit.assertEquals("the caster's tokens", 2, CardLists.filter(ai.getCardsIn(ZoneType.Exile), Card::isToken).size());
    }
}
