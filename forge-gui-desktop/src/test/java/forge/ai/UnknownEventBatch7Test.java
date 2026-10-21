package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.ChooseTeamEffect;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Azra Matchthrower through Literal Delver of Secrets (UNK CR04a-CU06), checked against Scryfall 2026-10-21 (no
 * rulings).
 */
public class UnknownEventBatch7Test extends AITest {

    @Test
    public void testAzraMatchthrowerBattlebond() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card battle = addCard("Invasion of Kaldheim", ai);
        battle.addCounterInternal(CounterEnumType.DEFENSE, 4, ai, false, null, null);
        battle.setProtectingPlayer(game.getPlayers().get(0));
        final Card azra = addCard("Azra Matchthrower", ai);
        final SpellAbility bond = forge.game.ability.AbilityFactory.getAbility(
                "DB$ Bond | Defined$ Self | ValidCards$ Battle.YouCtrl+!Paired | Battlebond$ True", azra);
        bond.setActivatingPlayer(ai);
        AbilityUtils.resolve(bond);
        AssertJUnit.assertEquals(battle, azra.getPairedWith());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("a battle partner stays paired", azra.isPaired());
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue("the battle has the damage trigger", battle.getTriggers().stream()
                .anyMatch(t -> t.getMode() == forge.game.trigger.TriggerType.DamageDoneOnce));
        AssertJUnit.assertTrue(azra.getTriggers().stream()
                .anyMatch(t -> t.getMode() == forge.game.trigger.TriggerType.DamageDoneOnce));
    }

    @Test
    public void testEstablishingShot() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card shot = addCardToZone("Establishing Shot", ai, ZoneType.Hand);
        final SpellAbility sa = shot.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("no spells cast yet this game", 2, AbilityUtils.calculateAmount(shot, "X", sa));
    }

    @Test
    public void testAetherRacingModesByTeam() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card racing = addCardToZone("Now THIS Is Aether Racing", ai, ZoneType.Hand);
        addCard("Mountain", ai);
        addCard("Mountain", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final SpellAbility cloud = racing.getSpellAbilities().get(0);
        final SpellAbility demons = racing.getSpellAbilities().get(1);
        cloud.setActivatingPlayer(ai);
        demons.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("no team yet: either line", cloud.canPlay() && demons.canPlay());
        ai.setMatchNote(ChooseTeamEffect.RACE_NOTE, ChooseTeamEffect.SPEED_DEMONS);
        AssertJUnit.assertFalse(cloud.canPlay());
        AssertJUnit.assertTrue(demons.canPlay());
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Island", ai, ZoneType.Library);
        AbilityUtils.resolve(demons);
        AssertJUnit.assertEquals(2, ai.getCardsIn(ZoneType.Exile).size());
    }

    @Test
    public void testAetherRacingJoinsTheTeamItsLineIsFor() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card racing = addCardToZone("Now THIS Is Aether Racing", ai, ZoneType.Hand);
        final SpellAbility cloud = racing.getSpellAbilities().get(0);
        cloud.setActivatingPlayer(ai);
        cloud.getTargets().add(human);
        AbilityUtils.resolve(cloud);
        AssertJUnit.assertEquals(17, human.getLife());
        AssertJUnit.assertEquals(ChooseTeamEffect.CLOUDSPIRE, ChooseTeamEffect.teamOf(ai, true));
    }

    @Test
    public void testMyDeckIsAboutASeven() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card target = addCard("Colossal Dreadmaw", human);
        final Card seven = addCardToZone("My Deck is About a Seven", ai, ZoneType.Hand);
        final SpellAbility sa = seven.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(target);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("6 or more damage kills a 6/6", game.getCardState(target).isInZone(ZoneType.Graveyard));
    }

    @Test
    public void testIrrefutableEvidence() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card evidence = addCardToZone("Irrefutable Evidence", ai, ZoneType.Graveyard);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("mana value 10 in your graveyard", 10, evidence.getCMC());
        addCardToZone("Island", ai, ZoneType.Library);
        final Card detective = addCard("Surveillance Monitor", ai);
        final SpellAbility collect = new SpellAbility.EmptySa(detective, ai);
        final forge.game.cost.CostCollectEvidence cost = new forge.game.cost.CostCollectEvidence("8");
        cost.payAsDecided(ai, forge.game.cost.PaymentDecision.card(evidence), collect, true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
        }
        AssertJUnit.assertEquals("drew for being collected", 1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testUnclaimedBirdMirran() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.MIRRAN);
        final Card theirs = addCard("Grizzly Bears", human);
        theirs.addCounterInternal(CounterEnumType.P1P1, 2, human, false, null, null);
        theirs.addCounterInternal(CounterEnumType.CHARGE, 1, human, false, null, null);
        final Card bird = addCard("Unclaimed Bird", ai);
        final SpellAbility etb = bird.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AbilityUtils.resolve(etb);
        AssertJUnit.assertEquals("one +1/+1 counter off", 1, theirs.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals("and one charge counter", 0, theirs.getCounters(CounterEnumType.CHARGE));
    }

    @Test
    public void testShadowmoorDrawSpellUntapConvoke() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card spell = addCardToZone("Shadowmoor Draw Spell", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(spell.hasKeyword(Keyword.UNTAP_CONVOKE));
        // a blue creature pays the {U}; the bears pay {3}
        addCard("Merfolk of the Pearl Trident", ai).setTapped(true);
        for (int i = 0; i < 3; i++) {
            final Card bear = addCard("Grizzly Bears", ai);
            bear.setTapped(true);
        }
        addCardToZone("Island", ai, ZoneType.Library);
        addCardToZone("Island", ai, ZoneType.Library);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        final forge.game.mana.ManaCostBeingPaid cost = new forge.game.mana.ManaCostBeingPaid(spell.getManaCost());
        forge.game.cost.CostAdjustment.adjust(cost, sa, ai, null, false, false);
        AssertJUnit.assertTrue("paid by untapping four tapped creatures: " + cost, cost.isPaid());
        AssertJUnit.assertTrue(ai.getCreaturesInPlay().stream().noneMatch(Card::isTapped));
    }

    @Test
    public void testWistfulPuppeteerAnimatedRole() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card rock = addCard("Mind Stone", ai);
        final Card puppeteer = addCard("Wistful Puppeteer", ai);
        final SpellAbility etb = puppeteer.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        etb.getTargets().add(rock);
        AbilityUtils.resolve(etb);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(rock.isCreature());
        AssertJUnit.assertTrue(rock.isArtifact());
        AssertJUnit.assertEquals(4, rock.getNetPower());
        AssertJUnit.assertEquals(4, rock.getNetToughness());
    }

    @Test
    public void testSimpleOnes() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final int base = ai.getPoisonCountersToLose();
        addCard("Drake with Set's Mechanic", ai);
        addCard("Burn the Phyresis", ai);
        AssertJUnit.assertEquals(base + 4, ai.getPoisonCountersToLose());
        final Card delver = addCardToZone("Literal Delver of Secrets", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(delver.hasKeyword(Keyword.DELVE));
        final Card detective = addCard("Educated Detective", ai);
        AssertJUnit.assertEquals(1, AbilityUtils.calculateAmount(detective, "X", detective.getFirstSpellAbility()));
    }
}
