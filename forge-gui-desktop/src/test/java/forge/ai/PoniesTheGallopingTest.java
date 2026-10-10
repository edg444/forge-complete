package forge.ai;

import java.time.LocalTime;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.ProtectEffect;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.MayPlayPermission;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Ponies: The Galloping (PTG), Oracle text and printed cards checked against Scryfall 2026-10-10 (no rulings).
 * User rulings: nighttime is the real local clock, 6 PM to 6 AM; friendship is mutual; the AI owns a pony toy half the
 * time (GameChance.50.MLPToy) with colors fixed for the game; only Tagger-confirmed moons count for Princess Luna.
 */
public class PoniesTheGallopingTest extends AITest {

    @AfterMethod
    public void restoreClock() {
        AbilityUtils.realWorldTime = LocalTime::now;
    }

    private static SpellAbility ability(Card c, ApiType api) {
        for (SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    private Card printing(String name, String edition, Player p, ZoneType zone) {
        Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, edition), p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(zone).add(c);
        return c;
    }

    @Test
    public void testNightmareMoonAtNight() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card moon = addCard("Nightmare Moon", p);
        AbilityUtils.realWorldTime = () -> LocalTime.of(14, 0);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(6, moon.getNetPower());
        AssertJUnit.assertFalse(moon.hasKeyword(Keyword.MENACE));

        AbilityUtils.realWorldTime = () -> LocalTime.of(23, 30);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(8, moon.getNetPower());
        AssertJUnit.assertEquals(8, moon.getNetToughness());
        AssertJUnit.assertTrue(moon.hasKeyword(Keyword.MENACE));

        AbilityUtils.realWorldTime = () -> LocalTime.of(6, 0);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(6, moon.getNetPower());
    }

    @Test
    public void testActivatorAndHelpersBecomeFriends() {
        Game game = initAndCreateThreePlayerGame();
        Player owner = game.getPlayers().get(0);
        Player activator = game.getPlayers().get(1);
        Player helper = game.getPlayers().get(2);
        Card moon = addCard("Nightmare Moon", owner);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = ability(moon, ApiType.SetState);
        AssertJUnit.assertTrue(sa.getRestrictions().canPlay(moon, withActivator(sa, activator)));
        sa.addPaymentHelper(helper);
        game.getStack().add(sa);

        AssertJUnit.assertTrue(owner.isFriendOf(activator) && activator.isFriendOf(owner));
        AssertJUnit.assertTrue(owner.isFriendOf(helper) && helper.isFriendOf(owner));
        AssertJUnit.assertFalse(activator.isFriendOf(helper));
        AssertJUnit.assertTrue(sa.getPaymentHelpers().isEmpty());
    }

    private static SpellAbility withActivator(SpellAbility sa, Player p) {
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void testLunaExilesOnlyTaggedMoons() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card moon = addCard("Nightmare Moon", p);
        Card specter = printing("Hypnotic Specter", "10E", p, ZoneType.Sideboard);
        Card bats = printing("Vampire Bats", "10E", p, ZoneType.Sideboard);
        Card bears = printing("Grizzly Bears", "10E", p, ZoneType.Sideboard);
        AssertJUnit.assertTrue(specter.hasKnownMoonInArt());
        AssertJUnit.assertFalse(bears.hasKnownMoonInArt());
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = ability(moon, ApiType.SetState);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals("Princess Luna", moon.getName());
        playUntilStackClear(game);

        AssertJUnit.assertTrue(game.getCardState(specter).isInZone(ZoneType.Exile));
        AssertJUnit.assertTrue(game.getCardState(bats).isInZone(ZoneType.Exile));
        AssertJUnit.assertTrue(game.getCardState(bears).isInZone(ZoneType.Sideboard));

        Card exiled = game.getCardState(specter);
        AssertJUnit.assertFalse(exiled.mayPlay(p).isEmpty());
        AssertJUnit.assertTrue(exiled.mayPlay(game.getPlayers().get(0)).isEmpty());
    }

    @Test
    public void testFriendsNeedPermission() {
        Game game = initAndCreateThreePlayerGame();
        Player owner = game.getPlayers().get(0);
        Player opp = game.getPlayers().get(1);
        Player ally = game.getPlayers().get(2);
        ally.setTeam(owner.getTeam());
        Card moon = addCard("Nightmare Moon", owner);
        Card specter = printing("Hypnotic Specter", "10E", owner, ZoneType.Sideboard);
        owner.addFriend(opp);
        owner.addFriend(ally);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, owner);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = ability(moon, ApiType.SetState);
        sa.setActivatingPlayer(owner);
        AbilityUtils.resolve(sa);
        playUntilStackClear(game);
        Card exiled = game.getCardState(specter);
        AssertJUnit.assertTrue(exiled.isInZone(ZoneType.Exile));
        AssertJUnit.assertFalse(exiled.mayPlay(opp).isEmpty());
        AssertJUnit.assertFalse(exiled.mayPlay(ally).isEmpty());

        // the AI owner lends to a teammate, not to an opponent, and a refusal sticks for the turn
        SpellAbility allyCast = castFrom(exiled, ally);
        AssertJUnit.assertTrue(MayPlayPermission.ask(allyCast));
        SpellAbility oppCast = castFrom(exiled, opp);
        AssertJUnit.assertFalse(MayPlayPermission.ask(oppCast));
        AssertJUnit.assertTrue(exiled.mayPlay(opp).isEmpty());
        AssertJUnit.assertFalse(exiled.mayPlay(ally).isEmpty());
    }

    private static SpellAbility castFrom(Card card, Player p) {
        SpellAbility spell = card.getFirstSpellAbility().copy(p);
        spell.setMayPlay(card.mayPlay(p).get(0));
        return spell;
    }

    private void controlNamed(Player p, String... names) {
        for (String name : names) {
            Card c = addCard("Grizzly Bears", p);
            c.addChangedName(name, false, p.getGame().getNextTimestamp(), 0);
        }
    }

    @Test
    public void testEveryponyWins() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card twilight = addCard("Princess Twilight Sparkle", p);
        controlNamed(p, "Applejack", "Fluttershy", "Pinkie Pie", "Rainbow Dash");
        game.getAction().checkStateEffects(true);
        SpellAbility sa = ability(twilight, ApiType.WinsGame);
        sa.setActivatingPlayer(p);
        AssertJUnit.assertFalse(sa.metConditions());
        AbilityUtils.resolve(sa);
        AssertJUnit.assertFalse(game.isGameOver());

        addCard("Rarity", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(sa.metConditions());
        AbilityUtils.resolve(sa);
        AssertJUnit.assertTrue(game.isGameOver());
        for (Player each : game.getPlayers()) {
            AssertJUnit.assertTrue(each + " won", each.getOutcome().hasWon());
        }
    }

    @Test
    public void testTwilightLordsOverPonies() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Princess Twilight Sparkle", p);
        Card rarity = addCard("Rarity", p);
        Card bears = addCard("Grizzly Bears", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(3, rarity.getNetPower());
        AssertJUnit.assertEquals(2, bears.getNetPower());
    }

    @Test
    public void testRarityCostReductionAndText() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card rarity = addCard("Rarity", p);
        AssertJUnit.assertEquals("Rare and mythic rare spells you cast cost {1} less to cast. | "
                + "{1}, {T}, Reveal a My Little Pony® toy you own: Until end of turn, another target creature gains "
                + "protection from each color in that toy's coat, mane, and outfit.",
                String.join(" | ", rarity.getAbilityText().trim().split("\\s*[\\r\\n]+")));
        Card rare = printing("Hypnotic Specter", "10E", p, ZoneType.Hand);
        Card common = printing("Vampire Bats", "10E", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        // Hypnotic Specter (rare in Tenth Edition) is {1}{B}{B}; Vampire Bats (common) is {B}
        AssertJUnit.assertEquals(2, manaToPay(rare, p));
        AssertJUnit.assertEquals(1, manaToPay(common, p));
    }

    private static int manaToPay(Card c, Player p) {
        SpellAbility spell = c.getFirstSpellAbility();
        spell.setActivatingPlayer(p);
        forge.game.mana.ManaCostBeingPaid cost = new forge.game.mana.ManaCostBeingPaid(spell.getPayCosts().getTotalMana());
        forge.game.cost.CostAdjustment.adjust(cost, spell, p, null, true, false);
        return cost.getConvertedManaCost();
    }

    @Test
    public void testAiToyIsTheSameAllGame() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Card rarity = addCard("Rarity", ai);
        SpellAbility sa = ability(rarity, ApiType.Protection);
        sa.setActivatingPlayer(ai);
        List<String> first = ProtectEffect.getProtectionList(sa);
        List<String> again = ProtectEffect.getProtectionList(sa);
        AssertJUnit.assertEquals(first, again);
        AssertJUnit.assertTrue(first.size() <= 3);
        // PTG is silver-bordered, so pink and gold are on the table
        AssertJUnit.assertTrue(game.isSilverBorderedGame());
    }
}
