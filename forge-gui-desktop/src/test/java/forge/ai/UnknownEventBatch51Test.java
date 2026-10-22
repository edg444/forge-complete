package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Interrogation Robot, The Zonian Brawler and The Karst, Enchanted (UNK cards), checked against Scryfall 2026-10-22
 * (no rulings).
 */
public class UnknownEventBatch51Test extends AITest {

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
    public void testInterrogationRobotNamesEachOnce() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card robot = addCard("Interrogation Robot", ai);
        final SpellAbility name = AbilityFactory.getAbility("DB$ NameCard | Defined$ You | ChooseFromList$ Who,What,When,Where,Why | ExcludeChosen$ True", robot);
        name.setActivatingPlayer(ai);
        for (int i = 0; i < 6; i++) {
            AbilityUtils.resolve(name);
        }
        AssertJUnit.assertEquals("each name once", 5, robot.getNamedCards().size());
        AssertJUnit.assertEquals(5, robot.getNamedCards().stream().distinct().count());
        final Card split = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getUniqueByName(robot.getNamedCard()), ai);
        final long castable = split.getAllSpellAbilities().stream()
                .filter(s -> s.isSpell() && s.isValid("Spell.LastNamedSpell", ai, robot, null)).count();
        AssertJUnit.assertEquals("only the half just named", 1, castable);
    }

    @Test
    public void testZonianBrawlerDeckSize() {
        initAndCreateGame();
        final Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add(FModel.getMagicDb().getCommonCards().getCard("The Zonian Brawler"));
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Forest"), 30);
        deck.getMain().add(FModel.getMagicDb().getCommonCards().getCard("Mountain"), 29);
        AssertJUnit.assertNull(DeckFormat.Commander.getDeckConformanceProblem(deck));
        deck.getMain().remove(FModel.getMagicDb().getCommonCards().getCard("Mountain"), 1);
        AssertJUnit.assertNotNull("59 cards with it", DeckFormat.Commander.getDeckConformanceProblem(deck));
    }

    @Test
    public void testZonianBrawlerBite() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("The Zonian Brawler", ai);
        fillLibrary(ai, 3);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card elf = addCard("Llanowar Elves", human);
        final Card wurm = addCard("Craw Wurm", human);
        final SpellAbility bite = AbilityFactory.getAbility("DB$ DealDamage | Defined$ Remembered | DamageSource$ Self | NumDmg$ 2", bears);
        bite.setActivatingPlayer(ai);
        bears.addRemembered(elf);
        AbilityUtils.resolve(bite);
        resolveAll(game);
        AssertJUnit.assertEquals("lethal bite: a Food and a card", 1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(1, CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Food")).size());
        bears.clearRemembered();
        bears.addRemembered(wurm);
        AbilityUtils.resolve(bite);
        resolveAll(game);
        AssertJUnit.assertEquals("not lethal", 1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testZonianBrawlerStartingLife() throws Exception {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card brawler = addCardToZone("The Zonian Brawler", ai, ZoneType.Command);
        brawler.setCommander(true);
        ai.addCommander(brawler);
        final java.lang.reflect.Method pre = game.getAction().getClass().getDeclaredMethod("runPreOpeningHandActions", Player.class);
        pre.setAccessible(true);
        pre.invoke(game.getAction(), ai);
        AssertJUnit.assertEquals(30, ai.getLife());
        AssertJUnit.assertEquals(30, ai.getStartingLife());
    }

    @Test
    public void testKarstSearchesUnsharedCards() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card karst = addCard("The Karst, Enchanted", ai);
        for (final String name : new String[] {"Grizzly Bears", "Runeclaw Bear", "Forest", "Lightning Bolt", "Craw Wurm", "Ornithopter"}) {
            addCardToZone(name, ai, ZoneType.Library);
        }
        final SpellAbility search = AbilityFactory.getAbility("DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Card"
                + " | ChangeNum$ 4 | DifferentCMC$ True | DifferentPT$ True | DifferentCardTypes$ True", karst);
        search.setActivatingPlayer(ai);
        AbilityUtils.resolve(search);
        final forge.game.card.CardCollectionView found = ai.getCardsIn(ZoneType.Hand);
        for (final Card a : found) {
            for (final Card b : found) {
                if (a != b) {
                    AssertJUnit.assertFalse(a + " / " + b, a.getCMC() == b.getCMC() || a.sharesCardTypeWith(b));
                }
            }
        }
        AssertJUnit.assertFalse(found.isEmpty());
        AssertJUnit.assertNotNull(human);
    }
}
