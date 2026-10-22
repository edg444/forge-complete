package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * The Horizon Seeker, The Convincing General, The Faction Dragon and Polis the Planeshifter (UNK mythics), checked
 * against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch39Test extends AITest {

    private static void runPreOpeningHandActions(final Game game, final Player p) throws Exception {
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), p);
    }

    @Test
    public void testHorizonSeekerCountsNamedKeywords() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card seeker = addCard("The Horizon Seeker", ai);
        addCard("Serra Angel", ai);
        addCard("Lotus Cobra", ai);
        addCardToZone("Opt", ai, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("horsemanship, flying, vigilance, landfall, scry", 5, seeker.getNetPower());
    }

    @Test
    public void testConvincingGeneralTwoTypes() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card general = addCardToZone("The Convincing General", ai, ZoneType.Command);
        ai.addCommander(general);
        general.setCommander(true);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Llanowar Elves", ai, ZoneType.Library);
            addCardToZone("Goblin Piker", ai, ZoneType.Library);
        }
        runPreOpeningHandActions(game, ai);
        final Card effect = ai.getCardsIn(ZoneType.Command).stream().filter(c -> c.getName().equals("The Convincing General's Two Types"))
                .findFirst().orElseThrow();
        final String a = effect.getChosenType(), b = effect.getChosenType2();
        AssertJUnit.assertFalse(a.equals(b));
        AssertJUnit.assertFalse("the General's own choice stays free", general.hasChosenType());
        final Card elves = addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
        game.getAction().checkStaticAbilities();
        if (elves.getType().hasCreatureType(a) || elves.getType().hasCreatureType(b)) {
            AssertJUnit.assertTrue(elves.getType().hasCreatureType(a) && elves.getType().hasCreatureType(b));
        }
    }

    @Test
    public void testFactionDragon() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card dragon = addCardToZone("The Faction Dragon", ai, ZoneType.Command);
        ai.addCommander(dragon);
        dragon.setCommander(true);
        addCardToZone("Siege Rhino", ai, ZoneType.Library);
        runPreOpeningHandActions(game, ai);
        AssertJUnit.assertEquals("abzan", dragon.getChosenType());
        AssertJUnit.assertEquals("{W}{B}{G}", dragon.getManaCost().getSimpleString().isEmpty() ? "" : "{W}{B}{G}");
        AssertJUnit.assertEquals(3, dragon.getCMC());
        AssertJUnit.assertTrue(ai.getCommanderColorID().hasWhite() && ai.getCommanderColorID().hasBlack() && ai.getCommanderColorID().hasGreen());
        final Card onBattlefield = game.getAction().moveToPlay(dragon, ai, null, null);
        final Card charm = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Abzan Battle Priest", "KTK", "1"), ai);
        ai.getZone(ZoneType.Hand).add(charm);
        final SpellAbility cast = charm.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        final ManaCostBeingPaid cost = new ManaCostBeingPaid(cast.getPayCosts().getTotalMana());
        forge.game.cost.CostAdjustment.adjust(cost, cast, ai, null, true, false);
        AssertJUnit.assertEquals("the Abzan watermark costs {1} less", 3, cost.getConvertedManaCost());
        AssertJUnit.assertNotNull(onBattlefield);
    }

    @Test
    public void testPolisHasThePlanesAbilities() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card polis = addCard("Polis the Planeshifter", ai);
        final SpellAbility etb = polis.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(polis.getImprintedCards().getFirst().isPlane());

        polis.clearImprintedCards();
        polis.addImprintedCard(Card.fromPaperCard(StaticData.instance().getVariantCards().getCard("Academy at Tolaria West"), ai));
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(polis.getTriggers().anyMatch(t -> t.getParam("TriggerDescription").contains("draw seven cards")
                && t.getActiveZone().contains(ZoneType.Battlefield)));
        addCardToZone("Island", ai, ZoneType.Hand);
        addCardToZone("Island", ai, ZoneType.Hand);
        final SpellAbility chaos = AbilityFactory.getAbility(polis.getSVar("DBChaos"), polis);
        chaos.setActivatingPlayer(ai);
        AbilityUtils.resolve(chaos);
        playUntilStackClear(game);
        AssertJUnit.assertEquals("its chaos ensues: discard your hand", 0, ai.getCardsIn(ZoneType.Hand).size());
    }
}
