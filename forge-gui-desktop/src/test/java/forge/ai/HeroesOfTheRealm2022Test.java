package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.CardRules;
import forge.deck.DeckFormat;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * 2022 Heroes of the Realm (PH22), Ormacar (PH23) and Convention Maro (PF24), checked against Scryfall 2026-10-22 (the
 * only ruling is the Commander RC's name-on-the-card one). User rulings 2026-10-22: nostalgic legends are legendary
 * cards and retro frames are 1993/1997 frames; a player donates when their own spell or ability gives a permanent to
 * another player; Heroes of Kamigawa's pack is of the Kamigawa expansion the player picks; Maro's [A] and [B] are chosen
 * once per match as the game begins.
 */
public class HeroesOfTheRealm2022Test extends AITest {

    private static CardRules rules(final String name) {
        return FModel.getMagicDb().getCommonCards().getCard(name).getRules();
    }

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    private static SpellAbility ability(final Card c, final ApiType api) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api && sa.isActivatedAbility()) {
                return sa;
            }
        }
        return null;
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

    private Card cast(final Game game, final Player p, final Card spell, final Card target) {
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        if (target != null) {
            sa.getTargets().add(target);
        }
        final Card onStack = game.getAction().moveToStack(spell, sa);
        sa.setHostCard(onStack);
        game.getStack().add(sa);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        return onStack;
    }

    @Test
    public void testPreciousDeckRules() {
        initAndCreateGame();
        final CardRules ormacar = rules("Ormacar, Relic Wraith");
        AssertJUnit.assertTrue(ormacar.canBePartnerCommanders(rules("The One Ring")));
        AssertJUnit.assertTrue(rules("The One Ring").canBePartnerCommanders(ormacar));
        AssertJUnit.assertTrue("a legendary Vehicle is a noncreature artifact", ormacar.canBePartnerCommanders(rules("Esika's Chariot")));
        AssertJUnit.assertFalse("an artifact creature isn't", ormacar.canBePartnerCommanders(rules("Karn, Silver Golem")));
        AssertJUnit.assertFalse(ormacar.canBePartnerCommanders(rules("Sol Ring")));
        AssertJUnit.assertFalse(ormacar.canBePartnerCommanders(rules("Thrasios, Triton Hero")));
        final PaperCard wraith = FModel.getMagicDb().getCommonCards().getCard("Ormacar, Relic Wraith");
        final PaperCard ring = FModel.getMagicDb().getCommonCards().getCard("The One Ring");
        AssertJUnit.assertTrue(DeckFormat.Commander.isLegalCommanderWith(ring.getRules(), List.of(wraith, ring)));
        AssertJUnit.assertFalse(DeckFormat.Commander.isLegalCommanderWith(ring.getRules(), List.of(ring)));
    }

    @Test
    public void testOrmacarGetsItsPrecious() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card ormacar = addCard("Ormacar, Relic Wraith", p);
        final Card ring = addCard("The One Ring", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("not its Precious unless both are commanders", 1, ormacar.getNetPower());
        p.addCommander(ormacar);
        p.addCommander(ring);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(5, ormacar.getNetPower());
        AssertJUnit.assertEquals(5, ormacar.getNetToughness());
        AssertJUnit.assertTrue(ormacar.getAbilityText().contains("Precious (You can have two commanders"));
    }

    @Test
    public void testCreateACharacter() {
        final Game game = initAndCreateGame();
        final CardRules wizard = rules("Wizard from Beyond");
        AssertJUnit.assertTrue(wizard.canBePartnerCommanders(rules("Grizzly Bears")));
        AssertJUnit.assertTrue(rules("Grizzly Bears").canBePartnerCommanders(wizard));
        AssertJUnit.assertFalse("a legend needs Choose a Background", wizard.canBePartnerCommanders(rules("Thrasios, Triton Hero")));
        AssertJUnit.assertTrue(wizard.canBePartnerCommanders(rules("Abdel Adrian, Gorion's Ward")));
        AssertJUnit.assertFalse(wizard.canBePartnerCommanders(rules("Sol Ring")));
        final PaperCard wiz = FModel.getMagicDb().getCommonCards().getCard("Wizard from Beyond");
        final PaperCard bearsCard = FModel.getMagicDb().getCommonCards().getCard("Grizzly Bears");
        AssertJUnit.assertTrue(DeckFormat.Commander.isLegalCommanderWith(bearsCard.getRules(), List.of(wiz, bearsCard)));

        final Player p = game.getPlayers().get(1);
        final Card background = addCard("Wizard from Beyond", p);
        final Card bears = addCard("Grizzly Bears", p);
        p.addCommander(background);
        p.addCommander(bears);
        p.promoteLegendaryPartners();
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("it becomes legendary", bears.getType().isLegendary());
        for (final String type : new String[] {"Cleric", "Rogue", "Warrior", "Wizard", "Bear"}) {
            AssertJUnit.assertTrue(type, bears.getType().hasCreatureType(type));
        }
        // one creature fills only one party slot; the commander is the Cleric here
        addCard("Merfolk Looter", p);
        addCard("Prodigal Sorcerer", p);
        addCard("Goblin Piker", p);
        game.getAction().checkStateEffects(true);
        final SpellAbility party = background.getFirstSpellAbility();
        party.setActivatingPlayer(p);
        AssertJUnit.assertEquals("a full party", 4, AbilityUtils.calculateAmount(background, "X", party));
        AssertJUnit.assertTrue(background.getAbilityText().contains("Create a Character (Any nonlegendary creature"));

        fillLibrary(p, 5);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        final int hand = p.getCardsIn(ZoneType.Hand).size();
        game.getAction().moveToPlay(addCardToZone("Elvish Visionary", p, ZoneType.Hand), p, null, forge.game.ability.AbilityKey.newMap());
        resolveAll(game);
        AssertJUnit.assertEquals("its enters trigger triggers twice", hand + 2, p.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testElusenDonates() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 5);
        fillLibrary(human, 5);
        final Card elusen = addCard("Elusen, the Giving", ai);
        game.getAction().checkStateEffects(true);
        final int life = ai.getLife();
        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        AbilityUtils.resolve(svar(elusen, "TrigDonate", ai));
        resolveAll(game);
        AssertJUnit.assertEquals("the player to its left", human, elusen.getController());
        AssertJUnit.assertEquals(hand + 1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(life + 1, ai.getLife());
        AssertJUnit.assertEquals(3, CardLists.count(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Treasure")));

        // taking someone's permanent isn't donating it
        final Card bears = addCard("Grizzly Bears", ai);
        final SpellAbility steal = AbilityFactory.getAbility("DB$ GainControl | Defined$ CardUID_" + bears.getId()
                + " | NewController$ You", elusen);
        steal.setActivatingPlayer(human);
        AbilityUtils.resolve(steal);
        resolveAll(game);
        AssertJUnit.assertEquals(human, bears.getController());
        AssertJUnit.assertEquals(0, CardLists.count(human.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Treasure")));
        AssertJUnit.assertEquals(hand + 1, ai.getCardsIn(ZoneType.Hand).size());

        // but giving your own away is - human now controls Elusen and gets the reward
        final SpellAbility give = AbilityFactory.getAbility("DB$ GainControl | Defined$ CardUID_" + bears.getId()
                + " | NewController$ Opponent", elusen);
        give.setActivatingPlayer(human);
        AbilityUtils.resolve(give);
        resolveAll(game);
        AssertJUnit.assertEquals(ai, bears.getController());
        AssertJUnit.assertEquals(3, CardLists.count(human.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Treasure")));
    }

    @Test
    public void testHeroesOfKamigawa() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card heroes = addCard("Heroes of Kamigawa", ai);
        final Card mystic = addCardToZone("Kitsune Mystic", ai, ZoneType.Graveyard);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        AssertJUnit.assertTrue(mystic.isValid("Card.NamePrintedIn CHK BOK SOK NEO", ai, heroes, null));
        AssertJUnit.assertTrue("Neon Dynasty", addCardToZone("Satoru Umezawa", ai, ZoneType.Hand)
                .isValid("Card.NamePrintedIn CHK BOK SOK NEO", ai, heroes, null));
        AssertJUnit.assertFalse(bears.isValid("Card.NamePrintedIn CHK BOK SOK NEO", ai, heroes, null));
        AssertJUnit.assertTrue(heroes.getRules().canBeCommander());

        // before the +2, which can mill other Kamigawa creatures
        final SpellAbility minus = ability(heroes, ApiType.ChangeZone);
        minus.setActivatingPlayer(ai);
        AbilityUtils.resolve(minus);
        AssertJUnit.assertTrue(game.getCardState(mystic).isInZone(ZoneType.Battlefield));
        AssertJUnit.assertTrue(bears.isInZone(ZoneType.Graveyard));

        final int hand = ai.getCardsIn(ZoneType.Hand).size();
        final int yard = ai.getCardsIn(ZoneType.Graveyard).size();
        final SpellAbility plus = ability(heroes, ApiType.OpenBooster);
        plus.setActivatingPlayer(ai);
        AbilityUtils.resolve(plus);
        AssertJUnit.assertEquals(hand + 2, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(yard + 2, ai.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertTrue("the rest of the pack stays in the booster pile", ai.getCardsIn(ZoneType.BoosterPile).size() >= 6);
    }

    @Test
    public void testTreizeciNostalgia() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card treizeci = addCard("Treizeci, Sun of Serra", ai);
        final Card retro = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Llanowar Elves", "4ED"), ai);
        final Card modern = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Llanowar Elves", "M19"), ai);
        AssertJUnit.assertTrue("a 1997 frame", retro.isValid("Card.Nostalgic", ai, treizeci, null));
        AssertJUnit.assertFalse(modern.isValid("Card.Nostalgic", ai, treizeci, null));
        AssertJUnit.assertTrue("artifacts", addCardToZone("Sol Ring", ai, ZoneType.Hand).isValid("Card.Nostalgic", ai, treizeci, null));
        AssertJUnit.assertTrue("legends", addCardToZone("Thrasios, Triton Hero", ai, ZoneType.Hand).isValid("Card.Nostalgic", ai, treizeci, null));

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        cast(game, ai, addCardToZone("Sol Ring", ai, ZoneType.Hand), null);
        AssertJUnit.assertEquals("the spell and Treizeci's trigger", 2, game.getStack().size());
        game.getStack().clear();

        AbilityUtils.resolve(svar(treizeci, "TrigCounter", ai));
        AbilityUtils.resolve(svar(treizeci, "TrigCounter", ai));
        AssertJUnit.assertEquals(2, treizeci.getCounters(forge.game.card.CounterEnumType.P1P1));
        AssertJUnit.assertEquals("one, then two", 3, CardLists.count(ai.getCardsIn(ZoneType.Battlefield),
                c -> c.isToken() && c.getName().equals("Serra Angel")));
    }

    @Test
    public void testConventionMaro() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Fire // Ice", ai, ZoneType.Library);
        }
        final Card maro = addCard("Convention Maro", ai);
        game.getAction().chooseDeckOptions(ai, maro, ai.getCardsIn(ZoneType.Library));
        AssertJUnit.assertEquals("the AI's deck has landscape cards", List.of("A2", "B1"), forge.game.GameAction.deckChoicesOf(maro));
        AssertJUnit.assertTrue(maro.isValid("Card.DeckChoice_A2", ai, maro, null));
        AssertJUnit.assertFalse(maro.isValid("Card.DeckChoice_A1", ai, maro, null));
        AssertJUnit.assertTrue(maro.getAbilityText().contains("[A]: \"has a watermark or landscape orientation\""));

        ai.setMatchNote("DeckChoices:Convention Maro", List.of("A2", "B3"));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card fire = addCardToZone("Fire // Ice", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(fire.isValid("Card.Landscape", ai, maro, null));
        AssertJUnit.assertFalse(addCardToZone("Commit // Memory", ai, ZoneType.Hand).isValid("Card.Landscape", ai, maro, null));
        cast(game, ai, addCardToZone("Grizzly Bears", ai, ZoneType.Hand), null);
        AssertJUnit.assertEquals("no trigger for a portrait, unwatermarked spell", 1, game.getStack().size());
        game.getStack().clear();
        AbilityUtils.resolve(svar(maro, "TrigToken", ai));
        game.getAction().checkStateEffects(true);
        final Card token = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && c.getName().equals("Maro")).get(0);
        AssertJUnit.assertTrue(token.getType().hasCreatureType("Elemental"));
        final int artists = (int) ai.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> !c.isLand() && !c.isToken()).map(c -> c.getArtist().toLowerCase()).distinct().count();
        AssertJUnit.assertEquals(artists, token.getNetPower());
    }
}
