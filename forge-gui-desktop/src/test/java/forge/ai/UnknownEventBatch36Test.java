package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.SecretIdentityEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mysterious Hero, Ring Out, Adventurer Beguiler, Takesies // Backsies, You're Toast! and Trash Panda (UNK uncommons),
 * checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch36Test extends AITest {

    @Test
    public void testMysteriousHeroSecretIdentity() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card hero = addCardToZone("Mysterious Hero", ai, ZoneType.Library);
        final Card angel = addCardToZone("Serra Angel", ai, ZoneType.Library);
        SecretIdentityEffect.cover(hero, angel, ai);
        AssertJUnit.assertFalse("the cover is out of the deck", ai.getCardsIn(ZoneType.Library).contains(angel));
        AssertJUnit.assertEquals("Serra Angel", hero.getName());
        final Card inPlay = game.getAction().moveToPlay(hero, ai, null, null);
        AssertJUnit.assertEquals("still the cover on the battlefield", "Serra Angel", inPlay.getName());
        final SpellAbility unmask = inPlay.getSpellAbilities().stream().filter(s -> s.getApi() == forge.game.ability.ApiType.SecretIdentity)
                .findFirst().orElseThrow();
        unmask.setActivatingPlayer(ai);
        AbilityUtils.resolve(unmask);
        AssertJUnit.assertEquals("Mysterious Hero", game.getCardState(inPlay).getName());
        AssertJUnit.assertFalse(SecretIdentityEffect.isCovered(game.getCardState(inPlay)));
    }

    @Test
    public void testRingOutTargetsOnlyThatSolRing() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card ring = addCardToZone("Sol Ring", human, ZoneType.Hand);
        final Card other = addCardToZone("Mind Stone", human, ZoneType.Hand);
        final SpellAbility ringSa = ring.getFirstSpellAbility();
        ringSa.setActivatingPlayer(human);
        final SpellAbility otherSa = other.getFirstSpellAbility();
        otherSa.setActivatingPlayer(human);
        final Card ringOut = addCardToZone("Ring Out", ai, ZoneType.Library);
        final SpellAbility counter = ringOut.getFirstSpellAbility();
        counter.setActivatingPlayer(ai);
        counter.setForcedTargets(java.util.List.of(ringSa));
        AssertJUnit.assertFalse(counter.canTarget(otherSa));
        AssertJUnit.assertTrue(ringOut.getTriggers().anyMatch(t -> t.getParam("TriggerZones").equals("Library")));
    }

    @Test
    public void testAdventurerBeguilerTakesTheAdventure() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card onAdventure = addCardToZone("Bonecrusher Giant", human, ZoneType.Exile);
        // as if Stomp had resolved: exiled by its own adventure, and its owner may cast the creature from exile
        final Card stompLki = forge.game.card.CardCopyService.getLKICopy(onAdventure);
        stompLki.setState(forge.card.CardStateName.Secondary, false);
        onAdventure.setExiledWith(stompLki);
        onAdventure.setExiledBy(human);
        final SpellAbility adventure = forge.game.ability.AbilityFactory.getAbility(
                "DB$ Effect | RememberObjects$ Self | StaticAbilities$ Play | ForgetOnMoved$ Exile | Duration$ Permanent | Adventure$ True", onAdventure);
        adventure.setSVar("Play", "Mode$ Continuous | MayPlay$ True | EffectZone$ Command | Affected$ Card.IsRemembered+!Adventure | AffectedZone$ Exile | Description$ You may cast the card.");
        adventure.setActivatingPlayer(human);
        adventure.resolve();
        AssertJUnit.assertTrue(onAdventure.isOnAdventure());
        final Card beguiler = addCard("Adventurer Beguiler", ai);
        final SpellAbility etb = beguiler.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.getTargets().add(onAdventure);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertFalse("the owner's adventure is gone", human.getCardsIn(ZoneType.Command).anyMatch(c -> c.isRemembered(onAdventure)));
        AssertJUnit.assertTrue(ai.getCardsIn(ZoneType.Command).anyMatch(c -> c.isRemembered(onAdventure)));
    }

    @Test
    public void testTakesiesAndBacksies() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card theirs = addCard("Grizzly Bears", human);
        theirs.addCounterInternal(CounterEnumType.P1P1, 2, human, false, null, null);
        final Card mine = addCard("Llanowar Elves", ai);
        final Card split = addCardToZone("Takesies", ai, ZoneType.Hand);
        final SpellAbility takesies = split.getFirstSpellAbility();
        takesies.setActivatingPlayer(ai);
        takesies.getTargets().add(mine);
        AbilityUtils.resolve(takesies);
        AssertJUnit.assertEquals("one counter from each", 1, mine.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, theirs.getCounters(CounterEnumType.P1P1));

        final SpellAbility backsies = split.getState(forge.card.CardStateName.RightSplit).getFirstAbility();
        backsies.setActivatingPlayer(ai);
        AbilityUtils.resolve(backsies);
        AssertJUnit.assertEquals("its +1/+1 counter is now a -1/-1 counter", 1, theirs.getNetPower());
        AssertJUnit.assertEquals(0, theirs.getCounters(CounterEnumType.P1P1));
        game.setCountersTreatedAs(null);
        AssertJUnit.assertEquals(3, theirs.getNetPower());
    }

    @Test
    public void testYoureToastManifestsBread() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card bears = addCard("Grizzly Bears", human);
        addCardToZone("Serra Angel", human, ZoneType.Library);
        addCardToZone("Island", human, ZoneType.Library);
        final Card toast = addCardToZone("You're Toast!", ai, ZoneType.Hand);
        final SpellAbility cast = toast.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        cast.getTargets().add(bears);
        AbilityUtils.resolve(cast);
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Exile));
        final Card bread = human.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isFaceDown).findFirst().orElseThrow();
        AssertJUnit.assertTrue(bread.getType().hasSubtype("Food"));
        AssertJUnit.assertTrue(bread.isArtifact());
        AssertJUnit.assertFalse(bread.isCreature());
        AssertJUnit.assertFalse("it's bread", bread.canBeTurnedFaceUp());
        AssertJUnit.assertEquals(1, human.getCardsIn(ZoneType.Graveyard).size());
    }

    @Test
    public void testTrashPanda() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCardToZone("Grizzly Bears", human, ZoneType.Graveyard);
        addCardToZone("Llanowar Elves", human, ZoneType.Graveyard);
        addCardToZone("Island", human, ZoneType.Graveyard);
        final Card panda = addCard("Trash Panda", ai);
        final SpellAbility etb = panda.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.getTargets().add(human);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertTrue(human.getCardsIn(ZoneType.Graveyard).isEmpty());
        AssertJUnit.assertEquals("a Food for each creature card", 2,
                ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Food")).count());

        final Card dead = game.getAction().moveToGraveyard(panda, null);
        final SpellAbility dies = panda.getTriggers().get(1).ensureAbility();
        dies.setActivatingPlayer(ai);
        dies.setTriggeringObject(AbilityKey.NewCard, dead);
        dies.setTriggeringObject(AbilityKey.CardLKI, dead);
        dies.getTargets().add(human);
        AbilityUtils.resolve(dies);
        final Card inTheirs = game.getCardState(dead);
        AssertJUnit.assertTrue("into the opponent's graveyard", human.getCardsIn(ZoneType.Graveyard).contains(inTheirs));
        AssertJUnit.assertEquals(ai, inTheirs.getOwner());

        for (int i = 0; i < 4; i++) {
            addCardToZone("Island", human, ZoneType.Library);
        }
        AssertJUnit.assertTrue(human.isValid("Player.Opponent+OpponentDredge4", ai, inTheirs, null));
        AssertJUnit.assertFalse(ai.isValid("Player.OpponentDredge4", ai, inTheirs, null));
    }
}
