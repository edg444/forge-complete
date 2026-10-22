package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.cost.Cost;
import forge.game.cost.CostSacrifice;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilitySacrificeInterchangeably;
import forge.game.zone.ZoneType;

/**
 * Need for Speed (Not the Odyssey One), Blu, Mansion Prince, Slobad, Actually Just Fine, The Octopus Official, Syx,
 * Robot Overlord, The Actualizer, The Good Time Sleuth, The Meld Hound and X, Mystery Racer (UNK rares), checked against Scryfall
 * 2026-10-22 (no rulings).
 */
public class UnknownEventBatch55Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private static SpellAbility svar(final Card c, final String name, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(c.getSVar(name), c);
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void testNeedForSpeed() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Need for Speed (Not the Odyssey One)", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card inHand = addCardToZone("Hill Giant", ai, ZoneType.Hand);
        ai.setSpeed(1);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.HASTE));
        AssertJUnit.assertFalse(bears.hasKeyword(Keyword.TRAMPLE));
        AssertJUnit.assertFalse(inHand.getSpellAbilities().stream().anyMatch(s -> s.toString().contains("Super haste")));
        ai.setSpeed(4);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.TRAMPLE));
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.FIRST_STRIKE));
        AssertJUnit.assertTrue("super haste for the creature card in hand",
                inHand.getSpellAbilities().stream().anyMatch(s -> s.toString().contains("Super haste")));
    }

    @Test
    public void testBluRoomHalf() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card blu = addCard("Blu, Mansion Prince", ai);
        AbilityUtils.resolve(svar(blu, "TrigRoom", ai));
        final java.util.List<Card> rooms = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isRoom);
        AssertJUnit.assertEquals(1, rooms.size());
        final Card room = rooms.get(0);
        AssertJUnit.assertTrue(room.isToken());
        AssertJUnit.assertEquals(1, room.getUnlockedRooms().size());
        AssertJUnit.assertEquals("the other door is gone", 1, room.getSealedRooms().size());
        AssertJUnit.assertTrue(room.getLockedRooms().isEmpty());
        AssertJUnit.assertFalse(room.unlockRoom(ai, room.getSealedRooms().iterator().next()));
        AssertJUnit.assertEquals(room.getUnlockedRoomNames().get(0), room.getName());
    }

    @Test
    public void testSlobadInterchangeably() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card ring = addCard("Sol Ring", ai);
        final SpellAbility edictSource = AbilityFactory.getAbility("DB$ Sacrifice | Defined$ You | SacValid$ Creature", ring);
        edictSource.setActivatingPlayer(ai);
        final Cost sacCreature = new Cost("Sac<1/Creature>", true);
        final CostSacrifice part = (CostSacrifice) sacCreature.getCostParts().get(0);
        AssertJUnit.assertEquals(Integer.valueOf(0), part.getMaxAmountX(edictSource, ai, false));
        final Card slobad = addCard("Slobad, Actually Just Fine", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("Creature,Artifact", StaticAbilitySacrificeInterchangeably.expand("Creature", ai, ","));
        AssertJUnit.assertEquals("Creature", StaticAbilitySacrificeInterchangeably.expand("Creature", human, ","));
        AssertJUnit.assertEquals("Sol Ring and Slobad", Integer.valueOf(2), part.getMaxAmountX(edictSource, ai, false));
        AbilityUtils.resolve(edictSource);
        AssertJUnit.assertTrue("an artifact for a creature", ring.isInZone(ZoneType.Battlefield) != slobad.isInZone(ZoneType.Battlefield));

        final Card slobad2 = addCard("Slobad, Actually Just Fine", human);
        slobad2.addCounterInternal(forge.game.card.CounterType.getType("REBUILDING"), 3, human, false, null, null);
        final SpellAbility bosh = slobad2.getSpellAbilities().stream().filter(s -> s.getApi() == forge.game.ability.ApiType.CopyPermanent).findFirst().orElseThrow();
        bosh.setActivatingPlayer(human);
        AbilityUtils.resolve(bosh);
        AssertJUnit.assertEquals(1, CardLists.filter(human.getCardsIn(ZoneType.Battlefield), c -> c.getName().equals("Bosh, Iron Golem") && c.isToken()).size());
    }

    @Test
    public void testOctopusTournament() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 8; i++) {
            addCardToZone(i % 2 == 0 ? "Grizzly Bears" : "Shock", ai, ZoneType.Library);
            if (i == 3 || i == 5) {
                addCardToZone("Island", ai, ZoneType.Library);
            }
        }
        addCardToZone("Forest", ai, ZoneType.Library);
        final Card octopus = addCard("The Octopus Official", ai);
        AbilityUtils.resolve(svar(octopus, "TrigAnnounce", ai));
        AssertJUnit.assertEquals(4, ai.getTournament().size());
        AssertJUnit.assertEquals(8, ai.getCardsIn(ZoneType.Exile).size());
        final CardCollection library = new CardCollection(ai.getCardsIn(ZoneType.Library));
        AssertJUnit.assertEquals(3, library.size());
        AssertJUnit.assertTrue(library.get(1).isLand() && library.get(2).isLand());
        AbilityUtils.resolve(svar(octopus, "TrigAnnounce", ai));
        AssertJUnit.assertEquals("one tournament at a time", 8, ai.getCardsIn(ZoneType.Exile).size());

        final SpellAbility round = AbilityFactory.getAbility("DB$ Tournament | Mode$ Advance | RememberChampion$ True", octopus);
        round.setActivatingPlayer(ai);
        AbilityUtils.resolve(round);
        AssertJUnit.assertEquals(4, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(2, ai.getTournament().size());
        AbilityUtils.resolve(round);
        AssertJUnit.assertEquals(6, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(1, ai.getTournament().size());
        AbilityUtils.resolve(round);
        AssertJUnit.assertEquals(7, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertNull("the tournament is over", ai.getTournament());
        AssertJUnit.assertEquals(1, octopus.getRememberedCount());
        AssertJUnit.assertTrue(((Card) octopus.getRemembered().iterator().next()).isInZone(ZoneType.Exile));
    }

    @Test
    public void testSyxInvaders() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card syx = addCard("Syx, Robot Overlord", ai);
        ai.getInvaderNames().add("Grizzly Bears");
        final Card bears = addCard("Grizzly Bears", ai);
        // the guesser calls the creature it most wants gone, so leave only Bears
        game.getAction().exile(syx, null, null);
        final SpellAbility g1 = AbilityFactory.getAbility("DB$ GuessInvader | ValidTgts$ Opponent", bears);
        g1.setActivatingPlayer(ai);
        g1.getTargets().add(human);
        AbilityUtils.resolve(g1);
        AssertJUnit.assertTrue("a right guess exiles the invader", bears.isInZone(ZoneType.Exile) || game.getCardState(bears).isInZone(ZoneType.Exile));
        AssertJUnit.assertEquals(20, human.getLife());
        final Card giant = addCard("Hill Giant", ai);
        final SpellAbility g2 = AbilityFactory.getAbility("DB$ GuessInvader | ValidTgts$ Opponent", giant);
        g2.setActivatingPlayer(ai);
        g2.getTargets().add(human);
        AbilityUtils.resolve(g2);
        AssertJUnit.assertTrue(giant.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals("a wrong guess costs 3 life", 17, human.getLife());

        final Card bears2 = addCard("Grizzly Bears", ai);
        AssertJUnit.assertTrue(bears2.hasProperty("Invader", ai, giant, null));
        AssertJUnit.assertFalse(giant.hasProperty("Invader", ai, giant, null));
        final Card syx2 = addCard("Syx, Robot Overlord", ai);
        syx2.setChosenCards(new CardCollection(bears2));
        AbilityUtils.resolve(svar(syx2, "DBCounters", ai));
        AssertJUnit.assertEquals(2, bears2.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(bears2.isArtifact() && bears2.isCreature());
        AssertJUnit.assertTrue(bears2.getType().hasCreatureType("Robot"));
        AssertJUnit.assertFalse(bears2.getType().hasCreatureType("Bear"));
    }

    @Test
    public void testActualizer() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 3);
        final Card actualizer = addCard("The Actualizer", ai);
        final Card shock = addCardToZone("Shock", ai, ZoneType.Hand);
        final SpellAbility read = AbilityFactory.getAbility("DB$ Actualize", actualizer);
        read.setActivatingPlayer(ai);
        AbilityUtils.resolve(read);
        AssertJUnit.assertTrue(actualizer.getChosenCards().contains(shock));
        AssertJUnit.assertTrue(human.getLife() == 20 || human.getLife() == 17);
    }

    @Test
    public void testMeldHound() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, ai);
        addCardToZone("Island", ai, ZoneType.Library);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        addCardToZone("Forest", ai, ZoneType.Library);
        addCard("The Meld Hound", ai);
        Card giant = addCardToZone("Hill Giant", ai, ZoneType.Hand);
        final SpellAbility cast = giant.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        giant = game.getAction().moveToStack(giant, cast);
        cast.setHostCard(giant);
        game.getStack().add(cast);
        resolveAll(game);
        final Card melded = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().contains("Hill Giant")).get(0);
        AssertJUnit.assertEquals("Hill Giant and Grizzly Bears", melded.getName());
        AssertJUnit.assertEquals(5, melded.getNetPower());
        AssertJUnit.assertEquals(5, melded.getNetToughness());
        AssertJUnit.assertTrue(melded.getType().hasCreatureType("Giant") && melded.getType().hasCreatureType("Bear"));
        AssertJUnit.assertEquals(6, melded.getCMC());
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Merged).contains(bears));
        game.getAction().destroy(melded, null, true, null);
        final java.util.List<String> graveyard = ai.getCardsIn(ZoneType.Graveyard).stream().map(Card::getName).toList();
        AssertJUnit.assertTrue(graveyard.toString(), graveyard.contains("Hill Giant") && graveyard.contains("Grizzly Bears"));
    }

    @Test
    public void testGoodTimeSleuth() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Runeclaw Bear", ai, ZoneType.Library);
        addCard("Grizzly Bears", ai);
        addCard("Hill Giant", ai);
        final Card sleuth = addCard("The Good Time Sleuth", ai);
        AssertJUnit.assertTrue(sleuth.getType().hasCreatureType("Human?"));
        AbilityUtils.resolve(svar(sleuth, "TrigExileTop", ai));
        game.getAction().checkStateEffects(true);
        final java.util.List<Card> faceDown = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isFaceDown);
        AssertJUnit.assertEquals("Bears, Giant, the Sleuth and the top card", 4, faceDown.size());
        AssertJUnit.assertEquals(4, CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSubtype("Blood")).size());
        for (final Card c : faceDown) {
            AssertJUnit.assertTrue(c.getSpellAbilities().stream().anyMatch(s -> s.getPayCosts().toString().contains("Blood")));
        }
        final Card effect = CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.getRememberedCount() == 1).get(0);
        final Card chosen = (Card) effect.getFirstRemembered();
        final Card onBattlefield = game.getCardState(chosen);
        final SpellAbility faceUp = onBattlefield.getSpellAbilities().stream().filter(s -> s.getPayCosts().toString().contains("Blood")).findFirst().orElseThrow();
        faceUp.setActivatingPlayer(ai);
        AbilityUtils.resolve(faceUp);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(onBattlefield.isFaceDown());
        AssertJUnit.assertEquals(5, onBattlefield.getNetPower());
        AssertJUnit.assertEquals(5, onBattlefield.getNetToughness());
        AssertJUnit.assertTrue(onBattlefield.getColor().hasBlack());
        AssertJUnit.assertTrue(onBattlefield.getType().hasCreatureType("Demon"));
        AssertJUnit.assertEquals(1, onBattlefield.getType().getCreatureTypes().size());
    }

    @Test
    public void testXMysteryRacer() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.MAIN1, ai);
        final Card copter = addCard("Smuggler's Copter", human);
        copter.setTapped(true);
        addCard("Grizzly Bears", ai);
        final Card x = addCard("X, Mystery Racer", ai);
        final SpellAbility crew = copter.getSpellAbilities().stream().filter(SpellAbility::isCrew).findFirst().orElseThrow();
        crew.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("not before X is in their hand", crew.getRestrictions().checkActivatorRestrictions(copter, crew));
        final SpellAbility give = svar(x, "TrigGive", ai);
        give.getTargets().add(human);
        AbilityUtils.resolve(give);
        final Card inHand = game.getCardState(x);
        AssertJUnit.assertTrue(human.getCardsIn(ZoneType.Hand).contains(inHand));
        AssertJUnit.assertEquals(ai, inHand.getOwner());
        final Card effect = CardLists.filter(ai.getCardsIn(ZoneType.Command), c -> c.isRemembered(inHand)).get(0);
        AssertJUnit.assertTrue(crew.getRestrictions().checkActivatorRestrictions(copter, crew));
        final SpellAbility cast = inHand.getFirstSpellAbility();
        cast.setActivatingPlayer(human);
        AssertJUnit.assertTrue("they can't cast X", forge.game.staticability.StaticAbilityCantBeCast.cantBeCastAbility(cast, inHand, human));
        final Card humanCard = addCardToZone("Shock", human, ZoneType.Hand);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue("their hand is revealed", humanCard.getView().mayPlayerLook(ai.getView()) || humanCard.mayPlayerLook(ai));

        final SpellAbility steal = AbilityFactory.getAbility(effect.getSVar("TrigSteal"), effect);
        steal.setActivatingPlayer(ai);
        steal.setTriggeringObject(forge.game.ability.AbilityKey.Card, copter);
        AbilityUtils.resolve(steal);
        AssertJUnit.assertEquals(ai, copter.getController());
        AssertJUnit.assertFalse(copter.isTapped());
        AssertJUnit.assertTrue(copter.hasKeyword(Keyword.HASTE));

        game.getAction().moveToGraveyard(inHand, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse("the effect ends once X leaves their hand", ai.getCardsIn(ZoneType.Command).contains(effect));
    }
}
