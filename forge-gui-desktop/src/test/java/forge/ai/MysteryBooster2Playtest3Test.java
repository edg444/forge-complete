package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardRules;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.combat.GlobalAttackRestrictions;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Mystery Booster 2 playtest cards (MB2), part 3, checked against Scryfall 2026-10-22 with their rulings.
 */
public class MysteryBooster2Playtest3Test extends AITest {

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

    @Test
    public void testMadlandsIsPlayedOffMadness() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card madlands = addCardToZone("Madlands", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(madlands.hasKeyword(Keyword.MADNESS));
        ai.discard(madlands, null, false, forge.game.ability.AbilityKey.newMap());
        resolveAll(game);
        final Card now = game.getCardState(madlands);
        AssertJUnit.assertTrue("played as its land play: " + now.getZone(), now.isInZone(ZoneType.Battlefield) || now.isInZone(ZoneType.Graveyard));
        if (now.isInZone(ZoneType.Battlefield)) {
            AssertJUnit.assertTrue(now.isTapped());
            AssertJUnit.assertEquals(1, ai.getLandsPlayedThisTurn());
        }
    }

    @Test
    public void testHideoutLandCasualty() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card giant = addCard("Hill Giant", ai);
        final Card hideout = addCardToZone("Maestros' Totally Safe Hideout", ai, ZoneType.Hand);
        final SpellAbility play = hideout.getAllPossibleAbilities(ai, true).stream().filter(SpellAbility::isLandAbility).findFirst().orElse(null);
        AssertJUnit.assertNotNull(play);
        play.setActivatingPlayer(ai);
        play.resolve();
        resolveAll(game);
        final long hideouts = CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Maestros' Totally Safe Hideout"));
        if (game.getCardState(giant).isInPlay()) {
            AssertJUnit.assertEquals("no sacrifice, no copy", 1, hideouts);
        } else {
            AssertJUnit.assertEquals("a copy for the sacrifice", 2, hideouts);
        }
        AssertJUnit.assertTrue(CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Maestros' Totally Safe Hideout")).stream().allMatch(Card::isTapped));
    }

    @Test
    public void testSnapJudgment() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(game.isWinnerWinsMatch());
        cast(game, ai, addCardToZone("Snap Judgment", ai, ZoneType.Hand));
        AssertJUnit.assertTrue(game.isWinnerWinsMatch());
    }

    @Test
    public void testTerryPin() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Terry Pin, Turboturtle", ai);
        final Card scroll = addCard("Grindstone", ai);
        final SpellAbility sorcery = AbilityFactory.getAbility("AB$ Draw | Cost$ 0 | SorcerySpeed$ True", scroll);
        sorcery.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(sorcery.isValid("Activated.SorcerySpeed", ai, scroll, null));
        final SpellAbility instant = AbilityFactory.getAbility("AB$ Draw | Cost$ 0", scroll);
        instant.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(instant.isValid("Activated.SorcerySpeed", ai, scroll, null));
    }

    @Test
    public void testEssenceOfAjani() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card essence = addCardToZone("Essence of Ajani", ai, ZoneType.Hand);
        cast(game, ai, essence);
        final Card now = game.getCardState(essence);
        AssertJUnit.assertTrue("an emblem in the command zone: " + now.getZone(), now.isInZone(ZoneType.Command));
        AssertJUnit.assertTrue(now.isValid("Emblem", ai, now, null));
        final int life = ai.getLife();
        cast(game, ai, addCardToZone("Llanowar Elves", ai, ZoneType.Hand));
        AssertJUnit.assertEquals(life + 1, ai.getLife());
    }

    @Test
    public void testRuleWithAnEvenHand() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card conspiracy = Card.fromPaperCard(forge.StaticData.instance().getVariantCards().getCard("Rule with an Even Hand"), ai);
        ai.getZone(ZoneType.Command).add(conspiracy);
        game.getAction().checkStateEffects(true);
        final GlobalAttackRestrictions restrictions = GlobalAttackRestrictions.getGlobalRestrictions(ai,
                new forge.util.collect.FCollection<forge.game.GameEntity>(game.getPlayers().get(0)));
        AssertJUnit.assertTrue(restrictions.isEvenOnly());
        AssertJUnit.assertNull("no maximum from it", restrictions.getMax());
        final Card a = addCard("Grizzly Bears", ai);
        final Card b = addCard("Hill Giant", ai);
        final java.util.Map<Card, forge.game.GameEntity> one = new java.util.HashMap<>();
        one.put(a, game.getPlayers().get(0));
        AssertJUnit.assertFalse(restrictions.isLegal(one));
        one.put(b, game.getPlayers().get(0));
        AssertJUnit.assertTrue(restrictions.isLegal(one));
        AssertJUnit.assertTrue("zero is even", restrictions.isLegal(new java.util.HashMap<>()));
    }

    @Test
    public void testPhyrexianSeedling() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player human = game.getPlayers().get(0);
        final Card seedling = addCard("Phyrexian Seedling", ai);
        seedling.addCounterInternal(CounterEnumType.P1P1, 1, ai, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(seedling.hasKeyword(Keyword.PROLIFERATELINK));
        final SpellAbility hit = AbilityFactory.getAbility("DB$ DealDamage | Defined$ Opponent | NumDmg$ 2 | DamageSource$ Self", seedling);
        hit.setActivatingPlayer(ai);
        AbilityUtils.resolve(hit);
        resolveAll(game);
        AssertJUnit.assertEquals("proliferated twice", 3, seedling.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testMothersYamazaki() {
        initAndCreateGame();
        final CardRules mothers = FModel.getMagicDb().getCommonCards().getCard("Mothers Yamazaki").getRules();
        AssertJUnit.assertTrue(mothers.canBePartnerCommanders(mothers));
        final Deck deck = new Deck("mothers");
        deck.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("Mothers Yamazaki"), 2);
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Mountain"), 49);
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Plains"), 49);
        AssertJUnit.assertNull(DeckFormat.Commander.getDeckConformanceProblem(deck), DeckFormat.Commander.getDeckConformanceProblem(deck));

        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card one = addCard("Mothers Yamazaki", ai);
        final Card two = addCard("Mothers Yamazaki", ai);
        final Card samurai = addCard("Isamaru, Hound of Konda", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("no legend rule for exactly two", one.isInPlay() && two.isInPlay());
        AssertJUnit.assertEquals("a Dog isn't a Samurai", 2, samurai.getNetPower());
        AssertJUnit.assertEquals("+2/+2 from each of the two", 6, one.getNetPower());
        AssertJUnit.assertTrue(one.hasKeyword(Keyword.VIGILANCE));
    }
}
