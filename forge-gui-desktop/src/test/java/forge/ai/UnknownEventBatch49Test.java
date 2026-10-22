package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Sue, Everlasting Dinosaur, Awoken Nephilim, Seventh Stage of Magic Design, Saga of Krark Losing His Thumb, Niv-Mizzet's
 * Diary and Simic, Value Engine (UNK rares), checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch49Test extends AITest {

    @Test
    public void testSueFossilize() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Sue, Everlasting Dinosaur", ai);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FOSSILIZE));
        final SpellAbility fossilize = bears.getSpellAbilities().stream()
                .filter(s -> s.isActivatedAbility() && "Fossilize".equals(s.getParam("PrecostDesc"))).findFirst().orElseThrow();
        AssertJUnit.assertEquals("{1}{G} minus {2} is {G}", 1, fossilize.getPayCosts().getTotalMana().getCMC());
        fossilize.setActivatingPlayer(ai);
        AbilityUtils.resolve(fossilize);
        game.getAction().checkStaticAbilities();
        final Card back = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Grizzly Bears")).get(0);
        AssertJUnit.assertEquals(1, back.getCounters(CounterEnumType.FINALITY));
        AssertJUnit.assertTrue(back.isArtifact());
        AssertJUnit.assertFalse(back.isCreature());
    }

    @Test
    public void testAwokenNephilim() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card awoken = addCardToZone("Awoken Nephilim", ai, ZoneType.Library);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Swamp", ai, ZoneType.Library);
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), ai);
        AssertJUnit.assertTrue(awoken.getName().endsWith("Nephilim") && !awoken.getName().startsWith("Awoken"));
        AssertJUnit.assertTrue(awoken.getType().isLegendary());
        final Card cast = game.getAction().moveToPlay(awoken, ai, null, AbilityKey.newMap());
        AssertJUnit.assertEquals("still the copy in another zone", awoken.getName(), cast.getName());
        AssertJUnit.assertTrue(cast.getType().isLegendary());
    }

    @Test
    public void testSeventhStageDoublesMana() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Seventh Stage of Magic Design", ai);
        final Card forest = addCard("Forest", ai);
        final SpellAbility mana = forest.getManaAbilities().get(0);
        mana.setActivatingPlayer(ai);
        mana.resolve();
        AssertJUnit.assertEquals(2, ai.getManaPool().totalMana());
    }

    @Test
    public void testKrarkQuarterOfTheirLife() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card saga = addCard("Saga of Krark Losing His Thumb", ai);
        human.setLife(19, null);
        final SpellAbility lose = AbilityFactory.getAbility("DB$ LoseLife | ValidTgts$ Opponent | LifeAmount$ TheirQuarter", saga);
        lose.setActivatingPlayer(ai);
        lose.getTargets().add(human);
        AbilityUtils.resolve(lose);
        AssertJUnit.assertEquals("19 / 4 rounded up is 5", 14, human.getLife());
        final SpellAbility tokens = AbilityFactory.getAbility(saga.getSVar("DBKrark"), saga);
        tokens.setActivatingPlayer(ai);
        AbilityUtils.resolve(tokens);
        AssertJUnit.assertEquals(2, CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isToken).size());
    }

    @Test
    public void testDiaryImprintsAsItResolves() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card diary = addCard("Niv-Mizzet's Diary", ai);
        final Card bolt = game.getAction().moveTo(ZoneType.Stack, addCardToZone("Lightning Bolt", human, ZoneType.Hand), null, AbilityKey.newMap());
        final SpellAbility trig = diary.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        trig.setTriggeringObject(AbilityKey.Card, bolt);
        AbilityUtils.resolve(trig);
        final java.util.Map<AbilityKey, Object> resolving = AbilityKey.newMap();
        resolving.put(AbilityKey.Fizzle, false);
        game.getAction().moveTo(ZoneType.Graveyard, bolt, null, resolving);
        AssertJUnit.assertEquals(1, human.getCardsIn(ZoneType.Exile).size());
        AssertJUnit.assertEquals(1, diary.getImprintedCards().size());
    }

    @Test
    public void testSimicValueEngine() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card simic = addCard("Simic, Value Engine", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, human);
        game.getAction().moveToPlay(addCardToZone("Forest", human, ZoneType.Hand), human, null, AbilityKey.newMap());
        human.onCleanupPhase();
        AssertJUnit.assertEquals(1, human.getLandsEnteredOwnLastTurn());
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().moveToPlay(addCardToZone("Island", ai, ZoneType.Hand), ai, null, AbilityKey.newMap());
        final SpellAbility trig = simic.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("one land against one", 0, AbilityUtils.calculateAmount(simic, "LandGap", trig));
        game.getAction().moveToPlay(addCardToZone("Forest", ai, ZoneType.Hand), ai, null, AbilityKey.newMap());
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(simic, "LandGap", trig));
    }
}
