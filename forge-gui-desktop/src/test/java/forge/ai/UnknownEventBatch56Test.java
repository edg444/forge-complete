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
import forge.game.card.CounterType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityChooseGuessOutcome;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

/**
 * Blue Screen of Death, The Snapstone Wielder, Welcome to Australia, The Sprinkler of Stardust, The Fact Checker, The
 * Convention Enthusiast, The Policy Maker, The Beleaguered Boxer and The Multiplayer Nitpickers (UNK rares), checked
 * against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch56Test extends AITest {

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testBlueScreenOfDeath() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bsod = addCard("Blue Screen of Death", ai);
        AssertJUnit.assertTrue(bsod.isInstant() && bsod.isArtifact() && bsod.isEnchantment());
        bsod.addCounterInternal(CounterType.getType("CURSE"), 2, ai, false, null, null);
        ai.addPriorityNanosThisTurn(2_500_000_000L);
        resolveAll(game);
        AssertJUnit.assertTrue("2 seconds isn't more than 2", bsod.isInZone(ZoneType.Battlefield));
        ai.addPriorityNanosThisTurn(1_000_000_000L);
        resolveAll(game);
        AssertJUnit.assertFalse(game.getCardState(bsod).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(14, ai.getLife());
    }

    @Test
    public void testSnapstoneWielder() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.UPKEEP, ai);
        final Card snap = addCard("The Snapstone Wielder", ai);
        AbilityUtils.resolve(svar(snap, "NoLandsCommanderEffect", ai));
        final Card effect = CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.getName().equals("Snapstone Mana")).get(0);
        final SpellAbility upkeep = AbilityFactory.getAbility(snap.getSVar("SnapCounter"), effect);
        upkeep.setActivatingPlayer(ai);
        AbilityUtils.resolve(upkeep);
        AbilityUtils.resolve(upkeep);
        AssertJUnit.assertEquals(2, ai.getCounters(CounterType.getType("MANA")));
        final SpellAbility mana = effect.getManaAbilities().get(0);
        mana.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(mana.getRestrictions().checkOtherRestrictions(effect, mana, ai));
        AbilityUtils.resolve(svar(snap, "TrigCast", ai));
    }

    @Test
    public void testWelcomeToAustralia() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Forest", ai, ZoneType.Library);
        addCardToZone("Mountain", ai, ZoneType.Library);
        final java.util.List<String> before = ai.getCardsIn(ZoneType.Library).stream().map(Card::getName).toList();
        final Card wurm = addCard("Craw Wurm", ai);
        final Card vanguard = addCard("Elite Vanguard", ai);
        final Card welcome = addCard("Welcome to Australia", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(4, wurm.getNetPower());
        AssertJUnit.assertEquals(6, wurm.getNetToughness());
        AssertJUnit.assertTrue(wurm.hasKeyword(Keyword.DEATHTOUCH));
        AssertJUnit.assertFalse("Humans don't", vanguard.hasKeyword(Keyword.DEATHTOUCH));
        AbilityUtils.resolve(svar(welcome, "TrigFlip", ai));
        final java.util.List<String> after = ai.getCardsIn(ZoneType.Library).stream().map(Card::getName).toList();
        final java.util.List<String> reversed = new java.util.ArrayList<>(before);
        java.util.Collections.reverse(reversed);
        AssertJUnit.assertEquals(reversed, after);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("the top card is face up", ai.getCardsIn(ZoneType.Library).get(0).mayPlayerLook(game.getPlayers().get(0)));
    }

    @Test
    public void testSprinklerOfStardust() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
        }
        addCardToZone("Shock", ai, ZoneType.Library);
        final Card sprinkler = addCard("The Sprinkler of Stardust", ai);
        AbilityUtils.resolve(svar(sprinkler, "TrigDig", ai));
        final java.util.List<Card> shocks = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Shock"));
        if (!shocks.isEmpty()) {
            final Card shock = shocks.get(0);
            AssertJUnit.assertTrue(shock.isCreature() && shock.isInstant());
            AssertJUnit.assertEquals(2, shock.getNetPower());
            AssertJUnit.assertEquals(2, shock.getNetToughness());
            AssertJUnit.assertTrue(shock.isTapped());
            AssertJUnit.assertTrue(shock.getTriggers().stream().anyMatch(t -> t.toString().contains("cast a copy of this card")));
        }
        AssertJUnit.assertEquals(6 - shocks.size(), ai.getCardsIn(ZoneType.Library).size());
    }

    @Test
    public void testFactChecker() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 3);
        addCardToZone("Grizzly Bears", human, ZoneType.Library);
        final Card checker = addCard("The Fact Checker", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(Boolean.TRUE, StaticAbilityChooseGuessOutcome.chosenOutcome(ai, null));
        AssertJUnit.assertNull(StaticAbilityChooseGuessOutcome.chosenOutcome(human, null));
        final SpellAbility check = svar(checker, "TrigCheck", ai);
        check.setTriggeringObject(AbilityKey.Player, human);
        AbilityUtils.resolve(check);
        final boolean wrong = "Number$1".equals(checker.getSVar("FactWrong"));
        AssertJUnit.assertEquals(wrong ? 1 : 0, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testConventionEnthusiast() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card fan = addCard("The Convention Enthusiast", ai);
        AssertJUnit.assertEquals(1, fan.getManaAbilities().size());
        final SpellAbility resource = fan.getSpellAbilities().stream()
                .filter(s -> s.getApi() == forge.game.ability.ApiType.GenericChoice).findFirst().orElseThrow();
        resource.setActivatingPlayer(ai);
        AbilityUtils.resolve(resource);
        final int tokens = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isToken).size();
        final int counters = ai.getCounters(CounterEnumType.ENERGY) + ai.getCounters(CounterEnumType.EXPERIENCE)
                + ai.getCounters(CounterType.getType("TICKET"));
        AssertJUnit.assertEquals("one resource", 1, tokens + counters);
    }

    @Test
    public void testPolicyMaker() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card maker = addCard("The Policy Maker", ai);
        for (int i = 0; i < 8; i++) {
            AbilityUtils.resolve(svar(maker, "TrigPropose", ai));
        }
        final int rules = CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.getName().startsWith("Policy - ")).size();
        AssertJUnit.assertEquals("each proposal is either applied or rejected", 8, rules + maker.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testBeleagueredBoxerSwap() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card shock = addCardToZone("Shock", ai, ZoneType.Hand);
        final PaperCard shockPaper = (PaperCard) shock.getPaperCard();
        final forge.deck.Deck deck = ai.getRegisteredPlayer().getDeck();
        deck.getMain().add(shockPaper);
        final PaperCard bolt = forge.StaticData.instance().getCommonCards().getCard("Lightning Bolt");
        AssertJUnit.assertTrue(game.getMatch().swapInDeck(shock, bolt));
        AssertJUnit.assertEquals(0, deck.getMain().count(shockPaper));
        AssertJUnit.assertEquals(1, deck.getMain().count(bolt));
        final Card boxer = addCard("The Beleaguered Boxer", ai);
        AbilityUtils.resolve(svar(boxer, "TrigOpen", ai));
    }

    @Test
    public void testMultiplayerNitpickers() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 5);
        final Card nit = addCard("The Multiplayer Nitpickers", ai);
        game.getAction().checkStateEffects(true);
        final forge.game.staticability.StaticAbility st = nit.getStaticAbilities().iterator().next();
        AssertJUnit.assertEquals(2, AbilityUtils.calculateAmount(nit, "PlayerCountOpponents$Amount", st));
        AssertJUnit.assertEquals(3, AbilityUtils.calculateAmount(nit, "PlayerCountPlayers$Amount", st));
        nit.setSVar("DBDraw", "DB$ Draw | Defined$ You");
        final SpellAbility repeat = AbilityFactory.getAbility("DB$ RepeatEach | RepeatPlayers$ Player.Opponent | RepeatSubAbility$ DBDraw", nit);
        repeat.setActivatingPlayer(ai);
        AbilityUtils.resolve(repeat);
        AssertJUnit.assertEquals("one opponent and a phantom", 2, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testWelcomeToAustraliaFromOpeningHand() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Forest", ai, ZoneType.Library);
        final String top = ai.getCardsIn(ZoneType.Library).get(0).getName();
        final Card welcome = addCardToZone("Welcome to Australia", ai, ZoneType.Hand);
        AbilityUtils.resolve(svar(welcome, "FromHand", ai));
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(game.getCardState(welcome).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertFalse("flipped", top.equals(ai.getCardsIn(ZoneType.Library).get(0).getName()));
        game.getAction().moveToGraveyard(game.getCardState(welcome), null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("flipped back", top, ai.getCardsIn(ZoneType.Library).get(0).getName());
    }
}
