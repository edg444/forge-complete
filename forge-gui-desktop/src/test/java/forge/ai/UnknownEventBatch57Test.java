package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
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
 * Occupation of Kulrath, Occupation of Llanowar, Sort of _____ and _____, The Whiteboarder, The Chaos Keeper, The
 * Charitable Drafter, The Rhystic Storyteller, The Game-Changing Host, Clear, Fair Magic and The Commander Recommender
 * (UNK rares), checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch57Test extends AITest {

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

    private void castAndResolve(final Game game, final Player p, final String name, final Card target) {
        Card spell = addCardToZone(name, p, ZoneType.Hand);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add(target);
        }
        spell = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(spell);
        game.getStack().add(sa);
        resolveAll(game);
    }

    @Test
    public void testControlPointCaptured() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card kulrath = addCard("Occupation of Kulrath", human);
        kulrath.addCounterInternal(CounterEnumType.DEFENSE, 2, human, false, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("its controller protects it", human, kulrath.getProtectingPlayer());
        final Card bears = addCard("Grizzly Bears", ai);
        final SpellAbility hit = AbilityFactory.getAbility("DB$ DealDamage | Defined$ CardUID_" + kulrath.getId() + " | NumDmg$ 2", bears);
        hit.setActivatingPlayer(ai);
        AbilityUtils.resolve(hit);
        resolveAll(game);
        final Card captured = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Occupation of Kulrath")).get(0);
        AssertJUnit.assertEquals(ai, captured.getController());
        AssertJUnit.assertEquals(2, captured.getCounters(CounterEnumType.DEFENSE));
        AssertJUnit.assertEquals(ai, captured.getProtectingPlayer());

        final Card llanowar = addCard("Occupation of Llanowar", ai);
        llanowar.addCounterInternal(CounterEnumType.DEFENSE, 4, ai, false, null, null);
        final SpellAbility own = AbilityFactory.getAbility("DB$ DealDamage | Defined$ CardUID_" + llanowar.getId() + " | NumDmg$ 4", bears);
        own.setActivatingPlayer(ai);
        AbilityUtils.resolve(own);
        resolveAll(game);
        AssertJUnit.assertTrue("its own controller doesn't capture it", ai.getCardsIn(ZoneType.Battlefield).stream().noneMatch(c -> c.getName().equals("Occupation of Llanowar")));
    }

    @Test
    public void testSortOf() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card sort = addCard("Sort of _____ and _____", ai);
        final int before = sort.getTriggers().size();
        AbilityUtils.resolve(svar(sort, "TrigChoose", ai));
        AssertJUnit.assertEquals(before + 1, sort.getTriggers().size());
        final String text = sort.getTriggers().get(before).toString();
        AssertJUnit.assertTrue(text, text.startsWith("Whenever equipped creature deals combat damage to a player, ") && text.contains(", then "));
    }

    @Test
    public void testWhiteboarder() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card board = addCard("The Whiteboarder", ai);
        final Card angel = addCardToZone("Serra Angel", ai, ZoneType.Hand);
        final SpellAbility note = AbilityFactory.getAbility("DB$ Whiteboard | Mode$ Note | Defined$ CardUID_" + angel.getId(), board);
        note.setActivatingPlayer(ai);
        AbilityUtils.resolve(note);
        AssertJUnit.assertEquals("Flying", board.getWhiteboardNotes().get(0).label());
        board.getWhiteboardNotes().add(new Card.WhiteboardNote("Power", "Power 3", 3));
        board.getWhiteboardNotes().add(new Card.WhiteboardNote("Toughness", "Toughness 4", 4));
        final SpellAbility create = board.getSpellAbilities().stream().filter(s -> s.getApi() == forge.game.ability.ApiType.Whiteboard).findFirst().orElseThrow();
        create.setActivatingPlayer(ai);
        AbilityUtils.resolve(create);
        game.getAction().checkStateEffects(true);
        final Card token = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Elemental")).get(0);
        AssertJUnit.assertTrue(token.hasKeyword(Keyword.FLYING));
        AssertJUnit.assertTrue(token.hasProperty("WhiteBordered", ai, board, null));
        AssertJUnit.assertEquals("3/4, +1/+1 for being white-bordered", 4, token.getNetPower());
        AssertJUnit.assertEquals(5, token.getNetToughness());
        AssertJUnit.assertTrue("erased", board.getWhiteboardNotes().isEmpty());
    }

    @Test
    public void testChaosKeeper() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card keeper = addCard("The Chaos Keeper", ai);
        AbilityUtils.resolve(svar(keeper, "DBPack", ai));
        final int size = keeper.getPackUnder().size();
        AssertJUnit.assertTrue(size > 0);
        AbilityUtils.resolve(svar(keeper, "TrigTake", ai));
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(size - 1, keeper.getPackUnder().size());
    }

    @Test
    public void testCharitableDrafter() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Shock", human, ZoneType.Hand);
        addCardToZone("Shock", ai, ZoneType.Hand);
        final Card drafter = addCard("The Charitable Drafter", ai);
        AbilityUtils.resolve(svar(drafter, "TrigDog", ai));
        AssertJUnit.assertEquals(1, human.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(1, ai.getCardsIn(ZoneType.Library).size());
        final int ais = ai.getCardsIn(ZoneType.Hand).size();
        final int humans = human.getCardsIn(ZoneType.Hand).size();
        AssertJUnit.assertTrue(ais > 0 && (ais == humans || ais == humans + 1));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Hand).allMatch(c -> !c.isStartingDeckCard() && c.getOwner().equals(ai)));
        AssertJUnit.assertEquals(1, CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasCreatureType("Dog")).size());
    }

    @Test
    public void testRhysticStoryteller() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card teller = addCard("The Rhystic Storyteller", ai);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        int words = 0;
        for (final String w : bears.getFlavorText().split("\\s+")) {
            if (w.codePoints().anyMatch(Character::isLetterOrDigit)) {
                words++;
            }
        }
        AssertJUnit.assertEquals(words >= 10, bears.hasProperty("flavorWordsGE10", ai, teller, null));
        ai.setForcedUnsleeved(true);
        AssertJUnit.assertFalse(ai.isSleeved());
    }

    @Test
    public void testGameChangingHostRetroactive() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card bears = addCard("Grizzly Bears", ai);
        castAndResolve(game, ai, "Giant Growth", bears);
        castAndResolve(game, ai, "Overrun", null);
        final Card host = addCardToZone("The Game-Changing Host", ai, ZoneType.Hand);
        final Card inPlay = game.getAction().moveToPlay(host, ai, null, forge.game.ability.AbilityKey.newMap());
        resolveAll(game);
        AssertJUnit.assertEquals("4 + Giant Growth's 3 + Overrun's 3", 10, inPlay.getNetPower());
        AssertJUnit.assertTrue(inPlay.hasKeyword(Keyword.TRAMPLE));
        castAndResolve(game, ai, "Pyroclasm", null);
        game.getAction().exile(game.getCardState(inPlay), null, null);
        final Card late = addCardToZone("The Game-Changing Host", ai, ZoneType.Hand);
        final Card second = game.getAction().moveToPlay(late, ai, null, forge.game.ability.AbilityKey.newMap());
        resolveAll(game);
        AssertJUnit.assertEquals("Pyroclasm's 2 damage", 2, game.getCardState(second).getDamage());
    }

    @Test
    public void testClearFairMagic() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card magic = addCardToZone("Clear, Fair Magic", ai, ZoneType.Hand);
        human.createMonarchEffect("CN2");
        game.setMonarch(human);
        AbilityUtils.resolve(svar(magic, "DBDesignation", ai));
        AssertJUnit.assertNull(game.getMonarch());
        final Card treasure = addToken("c_a_treasure_sac", human);
        final SpellAbility obj = svar(magic, "DBObject", ai);
        obj.getTargets().add(treasure);
        AbilityUtils.resolve(obj);
        AssertJUnit.assertFalse(treasure.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void testCommanderRecommenderGuessedWrong() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 6);
        final Card rec = addCard("The Commander Recommender", ai);
        game.getAction().checkStateEffects(true);
        final SpellAbility wrong = AbilityFactory.getAbility("DB$ GuessedWrong | Defined$ Opponent", rec);
        wrong.setActivatingPlayer(ai);
        AbilityUtils.resolve(wrong);
        resolveAll(game);
        AssertJUnit.assertEquals(2, ai.getCardsIn(ZoneType.Hand).size());
        final SpellAbility mine = AbilityFactory.getAbility("DB$ GuessedWrong | Defined$ You", rec);
        mine.setActivatingPlayer(ai);
        AbilityUtils.resolve(mine);
        resolveAll(game);
        AssertJUnit.assertEquals("only opponents' wrong guesses", 2, ai.getCardsIn(ZoneType.Hand).size());

        final forge.deck.Deck deck = new forge.deck.Deck("Test precon");
        deck.getOrCreate(forge.deck.DeckSection.Commander).add(forge.StaticData.instance().getCommonCards().getCard("Omnath, Locus of Mana"));
        for (int i = 0; i < 20; i++) {
            deck.getMain().add(forge.StaticData.instance().getCommonCards().getCard("Forest"));
        }
        forge.game.ability.effects.RecommendDeckEffect.setDeckSource(new forge.game.ability.effects.RecommendDeckEffect.DeckSource() {
            @Override
            public java.util.List<forge.deck.Deck> commanderDecks() {
                return java.util.List.of(deck);
            }
            @Override
            public forge.deck.Deck randomPrecon() {
                return deck;
            }
        });
        AbilityUtils.resolve(svar(rec, "TrigRecommend", ai));
        resolveAll(game);
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        AssertJUnit.assertTrue("a right guess draws nothing, a wrong one two", hand == 2 || hand == 4);
        AssertJUnit.assertNotNull(human);
    }
}
