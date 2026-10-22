package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.ColorSet;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.ChooseTeamEffect;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Tax Keeper, Tax Bolt, Devoted Sultai, Arcbound Mamba, You and Ooze Army?, Artist Alley, Mana Conference, Must Be
 * Knights, Unclaimed Battle Axe, Nevermind and Across the Multiverse (UNK uncommons), checked against Scryfall
 * 2026-10-22 (no rulings).
 */
public class UnknownEventBatch30Test extends AITest {

    private static Card printing(final String name, final String set, final String number, final Player owner) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set, number), owner);
        AssertJUnit.assertNotNull(name, c);
        return c;
    }

    @Test
    public void testHighestCommanderTax() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card commander = addCardToZone("Grizzly Bears", ai, ZoneType.Command);
        ai.addCommander(commander);
        ai.incCommanderCast(commander);
        ai.incCommanderCast(commander);
        final Card keeper = addCard("Tax Keeper", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("cast twice: +4/+4", 5, keeper.getNetPower());
        final Card bolt = addCardToZone("Tax Bolt", ai, ZoneType.Hand);
        final SpellAbility cast = bolt.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(4, AbilityUtils.calculateAmount(bolt, "X", cast));
    }

    @Test
    public void testDevotionToSultai() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card sultai = addCard("Devoted Sultai", ai);
        addCard("Sedraxis Specter", ai);
        final SpellAbility etb = sultai.getTriggers().get(0).ensureAbility();
        etb.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("Sultai's {G} and the Specter's {U}{B}, not its {R}", 3, AbilityUtils.calculateAmount(sultai, "X", etb));
    }

    @Test
    public void testArcboundMambaPoisons() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card mamba = addCard("Arcbound Mamba", ai);
        mamba.addCounterInternal(forge.game.card.CounterEnumType.P1P1, 2, ai, false, null, null);
        final SpellAbility dies = mamba.getTriggers().get(0).ensureAbility();
        dies.setActivatingPlayer(ai);
        dies.setTriggeringObject(AbilityKey.Card, mamba);
        dies.getTargets().add(human);
        AbilityUtils.resolve(dies);
        AssertJUnit.assertEquals(2, human.getPoisonCounters());
        AssertJUnit.assertEquals("a player gets no +1/+1 counters", 0, human.getCounters(forge.game.card.CounterEnumType.P1P1));

        final Card myr = addCard("Ornithopter", ai);
        dies.resetTargets();
        dies.getTargets().add(myr);
        AbilityUtils.resolve(dies);
        AssertJUnit.assertEquals(2, myr.getCounters(forge.game.card.CounterEnumType.P1P1));
        AssertJUnit.assertEquals(2, human.getPoisonCounters());
    }

    @Test
    public void testOozeArmyIgnoresChangelings() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", ai);
        addCard("Llanowar Elves", human);
        addCard("Chameleon Colossus", human);
        final Card ooze = addCardToZone("You and Ooze Army?", ai, ZoneType.Hand);
        final SpellAbility cast = ooze.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("Bear, Elf, Druid", 3, AbilityUtils.calculateAmount(ooze, "X", cast));
        AbilityUtils.resolve(cast);
        final Card army = ai.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasCreatureType("Army")).findFirst().orElseThrow();
        AssertJUnit.assertTrue(army.getType().hasCreatureType("Ooze"));
        AssertJUnit.assertEquals(3, army.getNetPower());
    }

    @Test
    public void testArtistAlleySignedColors() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card alley = addCard("Artist Alley", ai);
        final SpellAbility signed = alley.getManaAbilities().get(1);
        signed.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("unsigned: only {C}", signed.getRestrictions().canPlay(alley, signed));
        alley.setMarkedColors(ColorSet.fromNames("blue", "red"));
        AssertJUnit.assertTrue(signed.getRestrictions().canPlay(alley, signed));
        AssertJUnit.assertEquals("U R", signed.getManaPart().getComboColors(signed));
    }

    @Test
    public void testManaConferenceGainsVotedTypes() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card conference = addCard("Mana Conference", ai);
        final SpellAbility vote = conference.getTriggers().get(0).ensureAbility();
        vote.setActivatingPlayer(ai);
        AbilityUtils.resolve(vote);
        AssertJUnit.assertTrue(conference.getType().getLandTypes().iterator().hasNext());
        for (final String type : conference.getType().getLandTypes()) {
            AssertJUnit.assertTrue(type, forge.card.CardType.isABasicLandType(type));
        }
    }

    @Test
    public void testEnchantingTalesByPrinting() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card knights = addCard("Must Be Knights", ai);
        final Card tale = printing("Intangible Virtue", "WOT", "6", ai);
        final Card reprint = printing("Intangible Virtue", "CMR", "24", ai);
        AssertJUnit.assertTrue(tale.isValid("Card.printingFrom_WOT", ai, knights, null));
        AssertJUnit.assertFalse(reprint.isValid("Card.printingFrom_WOT", ai, knights, null));

        final Card multiverse = addCard("Across the Multiverse", ai);
        final Card mom = printing("Phyrexian Gargantua", "MOM", "121", ai);
        ai.getZone(ZoneType.Battlefield).add(mom);
        final Card bears = addCard("Grizzly Bears", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, bears.getNetPower());
        AssertJUnit.assertTrue(bears.hasKeyword(Keyword.HASTE));
        AssertJUnit.assertEquals("a MOM printing gets nothing", 4, mom.getNetPower());
        AssertJUnit.assertNotNull(multiverse);
    }

    @Test
    public void testUnclaimedBattleAxeTeams() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.PHYREXIAN);
        final Card axe = addCard("Unclaimed Battle Axe", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(axe.hasKeyword(Keyword.LIVING_WEAPON));
        AssertJUnit.assertFalse(axe.hasKeyword(Keyword.FOR_MIRRODIN));
        ai.setMatchNote(ChooseTeamEffect.NOTE, ChooseTeamEffect.MIRRAN);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(axe.hasKeyword(Keyword.FOR_MIRRODIN));
        AssertJUnit.assertFalse(axe.hasKeyword(Keyword.LIVING_WEAPON));
    }

    @Test
    public void testNevermindSplicesOntoAnything() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card nevermind = addCardToZone("Nevermind", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(nevermind.hasKeyword(Keyword.SPLICE));
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(bolt.isValid("Card", ai, nevermind, null));
    }
}
