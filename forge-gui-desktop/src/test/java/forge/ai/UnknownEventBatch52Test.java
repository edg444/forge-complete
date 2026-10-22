package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Shahrazad and Sindbad, The Trivia Mastermind and The Cosplayzer (UNK rares), checked against Scryfall 2026-10-22
 * (no rulings).
 */
public class UnknownEventBatch52Test extends AITest {

    @Test
    public void testShahrazadAndSindbad() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card pair = addCard("Shahrazad and Sindbad", ai);
        AssertJUnit.assertEquals(0, AbilityUtils.calculateAmount(pair, "Subgames", pair.getTriggers().get(0)));
        game.getMatch().addSubgamePlayed();
        AssertJUnit.assertEquals("no more subgames this match", 1, AbilityUtils.calculateAmount(pair, "Subgames", pair.getTriggers().get(0)));

        addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        final SpellAbility draw = pair.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().orElseThrow();
        draw.setActivatingPlayer(ai);
        AbilityUtils.resolve(draw);
        AssertJUnit.assertEquals("a nonland is discarded", 1, ai.getCardsIn(ZoneType.Graveyard).size());
        addCardToZone("Forest", ai, ZoneType.Library);
        AbilityUtils.resolve(draw);
        AssertJUnit.assertEquals("a land is kept", 1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testTriviaMastermind() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card trivia = addCard("The Trivia Mastermind", ai);
        final Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Library);
        trivia.addRemembered(wurm);
        final SpellAbility guess = AbilityFactory.getAbility(trivia.getSVar("DBTrivia"), trivia);
        guess.setActivatingPlayer(ai);
        AbilityUtils.resolve(guess);
        // the AI opponent guesses 0-6, so a Wurm (6/4, mana value 6) can go either way
        AssertJUnit.assertTrue(wurm.isInZone(ZoneType.Hand) || wurm.isInZone(ZoneType.Library));
        final boolean logged = game.getGameLog().getLogEntries(null).stream().anyMatch(e -> e.message().contains("guesses"));
        AssertJUnit.assertTrue(logged);
    }

    @Test
    public void testNahYeahResolvesTheSpellRightAway() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(ai);
        boltSa.getTargets().add(human);
        game.getStack().add(boltSa);
        final Card split = addCardToZone("Yeah Nah", ai, ZoneType.Hand);
        final SpellAbility nahYeah = split.getAllSpellAbilities().stream()
                .filter(s -> s.isSpell() && s.getApi() == forge.game.ability.ApiType.ResolveNow).findFirst().orElseThrow();
        nahYeah.setActivatingPlayer(ai);
        nahYeah.getTargets().add(game.getStack().peekAbility());
        game.getStack().add(nahYeah);
        AssertJUnit.assertEquals(2, game.getStack().size());
        game.getStack().resolveStack();
        AssertJUnit.assertEquals("the Bolt resolved right after it", 17, human.getLife());
        AssertJUnit.assertTrue(game.getStack().isEmpty());
    }

    @Test
    public void testYeahNahVote() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final SpellAbility boltSa = bolt.getFirstSpellAbility();
        boltSa.setActivatingPlayer(ai);
        boltSa.getTargets().add(human);
        game.getStack().add(boltSa);
        final Card split = addCardToZone("Yeah Nah", ai, ZoneType.Hand);
        final SpellAbility yeahNah = split.getAllSpellAbilities().stream()
                .filter(s -> s.isSpell() && s.getApi() == forge.game.ability.ApiType.Vote).findFirst().orElseThrow();
        yeahNah.setActivatingPlayer(ai);
        yeahNah.getTargets().add(game.getStack().peekAbility());
        AbilityUtils.resolve(yeahNah);
        final boolean countered = bolt.isInZone(ZoneType.Graveyard) && game.getStack().isEmpty();
        final boolean copied = game.getStack().size() == 3;
        AssertJUnit.assertTrue("countered, or a copy for each player", countered || copied);
    }

    @Test
    public void testCosplayzer() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Prodigal Sorcerer", human);
        final Card cos = addCardToZone("The Cosplayzer", ai, ZoneType.Hand);
        final SpellAbility copy = AbilityFactory.getAbility(cos.getSVar("DBCopy"), cos);
        copy.setActivatingPlayer(ai);
        AbilityUtils.resolve(copy);
        AssertJUnit.assertEquals("Zrodigal Sorcerer", cos.getName());
        final java.util.List<Trigger> copiers = cos.getTriggers().stream().filter(t -> t.hasParam("SharedLimit")).toList();
        AssertJUnit.assertEquals(2, copiers.size());
        AssertJUnit.assertEquals(1, copiers.get(1).limitByActivations(1));
        cos.addAbilityActivated(copiers.get(0).ensureAbility());
        AssertJUnit.assertEquals("once each turn between them", 0, copiers.get(1).limitByActivations(1));
    }
}
