package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Infinity;

/**
 * Infinity Elemental (Unstable), "(This creature has INFINITE POWER.)", and infinity everywhere it can reach. Oracle
 * text, the five Scryfall rulings (2020-02-29) and the Unstable FAQ entry checked 2026-10-02.
 */
public class InfinityTest extends AITest {

    private Card elemental(Player p) {
        Card c = addCard("Infinity Elemental", p);
        c.setSickness(false);
        p.getGame().getAction().checkStateEffects(true);
        return c;
    }

    @Test
    public void testInfinitePower() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card e = elemental(me);

        AssertJUnit.assertEquals(Infinity.VALUE, e.getNetPower());
        AssertJUnit.assertEquals(5, e.getNetToughness());
        AssertJUnit.assertEquals("∞", CardView.get(e).getCurrentState().getPowerString());
        AssertJUnit.assertEquals("(This creature has INFINITE POWER.)", e.getAbilityText().trim());
    }

    @Test
    public void testGainingOrLosingPowerIsMeaninglessButSettingWorks() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card e = elemental(me);

        e.addPTBoost(3, 3, game.getNextTimestamp(), 0);
        AssertJUnit.assertEquals(Infinity.VALUE, e.getNetPower());
        AssertJUnit.assertEquals(8, e.getNetToughness());
        e.addPTBoost(-100, 0, game.getNextTimestamp(), 0);
        AssertJUnit.assertEquals(Infinity.VALUE, e.getNetPower());
        e.addCounterInternal(CounterEnumType.P1P1, 2, me, false, null, null);
        AssertJUnit.assertEquals(Infinity.VALUE, e.getNetPower());

        // "base power becomes 1": a set value wins, and the boosts apply to it from there
        e.addNewPT(1, null, game.getNextTimestamp(), 0);
        AssertJUnit.assertEquals(1 + 3 - 100 + 2, e.getNetPower());
    }

    @Test
    public void testTiesWithAnotherInfinityElemental() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card a = elemental(me);
        Card b = elemental(me);
        Card bears = addCard("Grizzly Bears", me);
        b.addPTBoost(5, 0, game.getNextTimestamp(), 0);

        AssertJUnit.assertEquals(a.getNetPower(), b.getNetPower());
        AssertJUnit.assertTrue(a.isValid("Creature.greatestPower", me, a, null));
        AssertJUnit.assertTrue(b.isValid("Creature.greatestPower", me, b, null));
        AssertJUnit.assertFalse(bears.isValid("Creature.greatestPower", me, bears, null));
    }

    @Test
    public void testInfiniteDamageAndInfiniteLife() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card e = elemental(me);

        // lifelink: infinite damage, infinite life
        // damage to a player becomes life loss when the damage event finishes; testLifelinkInCombat plays that out
        opp.loseLife(e.getNetPower(), true, false, null);
        me.gainLife(e.getNetPower(), e, null);
        AssertJUnit.assertTrue(Infinity.isNegativeInfinite(opp.getLife()));
        AssertJUnit.assertEquals(Infinity.VALUE, me.getLife());
        AssertJUnit.assertEquals("∞", PlayerView.get(me).getLifeString());

        // "At infinite life and gain 2 life? ... lose 29 life? You're at infinite life." Even against ∞ damage.
        me.gainLife(2, e, null);
        me.loseLife(29, false, false, null);
        me.loseLife(Infinity.VALUE, true, false, null);
        AssertJUnit.assertEquals(Infinity.VALUE, me.getLife());
        // any amount of life can be paid
        AssertJUnit.assertTrue(me.canPayLife(1000000, false, null));
        // setting a life total works even from infinity
        me.setLife(7, null);
        AssertJUnit.assertEquals(7, me.getLife());

        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(opp.hasLost());
    }

    @Test
    public void testInfiniteDamageKillsCreatures() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card e = elemental(me);
        Card wall = addCard("Wall of Stone", opp);
        wall.addDamageAfterPrevention(e.getNetPower(), e, null, false, null);
        AssertJUnit.assertEquals(Infinity.VALUE, wall.getDamage());
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(wall.getZone() == null || !wall.isInPlay());
    }

    @Test
    public void testDrawingInfinitelyManyLoses() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Mountain", me, ZoneType.Library);
        }
        me.drawCards(Infinity.VALUE);
        AssertJUnit.assertEquals(0, me.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(5, me.getCardsIn(ZoneType.Hand).size());
        // even at infinite life ("You're not immortal")
        me.gainLife(Infinity.VALUE, null, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(me.hasLost());
    }

    @Test
    public void testInfiniteCounters() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", me);
        bears.addCounterInternal(CounterEnumType.P1P1, Infinity.VALUE, me, false, null, null);
        bears.addCounterInternal(CounterEnumType.P1P1, 3, me, false, null, null);
        AssertJUnit.assertEquals(Infinity.VALUE, bears.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(Infinity.VALUE, bears.getNetPower());
        AssertJUnit.assertEquals(Infinity.VALUE, bears.getNetToughness());

        // removing some leaves infinitely many, but they were removed (a cost paid)
        AssertJUnit.assertEquals(2, bears.subtractCounter(CounterEnumType.P1P1, 2, me));
        AssertJUnit.assertEquals(Infinity.VALUE, bears.getCounters(CounterEnumType.P1P1));
        // removing all of them does empty it
        bears.subtractCounter(CounterEnumType.P1P1, Infinity.VALUE, me);
        AssertJUnit.assertEquals(0, bears.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void testInfiniteTokensBecomeAChosenNumber() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card source = addCard("Grizzly Bears", me);
        SpellAbility make = forge.game.ability.AbilityFactory.getAbility(
                "DB$ Token | TokenAmount$ X | TokenScript$ r_1_1_goblin", source);
        make.setSVar("X", "Number$" + Infinity.VALUE);
        make.setActivatingPlayer(me);
        AbilityUtils.resolve(make);
        // the AI picks a finite number in place of infinity
        AssertJUnit.assertEquals(PlayerControllerAi.FINITE_FOR_INFINITE,
                forge.game.card.CardLists.filter(me.getCardsIn(ZoneType.Battlefield), Card::isToken).size());
    }

    @Test
    public void testInfiniteManaOfAColorPaysAnInfiniteColoredX() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card source = addCard("Mountain", me);
        SpellAbility tap = source.getManaAbilities().get(0);
        me.getManaPool().addInfinite(MagicColor.RED, source, tap.getManaPart());
        AssertJUnit.assertTrue(me.getManaPool().hasInfinite(MagicColor.RED));

        ManaCostBeingPaid cost = new ManaCostBeingPaid(new ManaCost(new ManaCostParser("X R")));
        cost.setXManaCostPaid(Infinity.VALUE, "R");
        // the X in red and the {R} are one red shard, and ∞ + 1 is ∞
        AssertJUnit.assertEquals("{R}×∞", cost.toString().replace(" ", ""));
        SpellAbility spell = addCardToZone("Lightning Bolt", me, ZoneType.Hand).getFirstSpellAbility();
        spell.setActivatingPlayer(me);
        AssertJUnit.assertTrue(me.getManaPool().payManaCostFromPool(cost, spell, false, new java.util.ArrayList<>()));
        AssertJUnit.assertTrue(cost.isPaid());
        // still unbounded afterwards
        AssertJUnit.assertTrue(me.getManaPool().hasInfinite(MagicColor.RED));
    }

    @Test
    public void testMoxLotusPaysXEqualsInfinity() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Card mox = addCard("Mox Lotus", me);
        Card fireball = addCardToZone("Fireball", me, ZoneType.Hand);
        SpellAbility spell = fireball.getFirstSpellAbility();
        spell.setActivatingPlayer(me);
        game.getAction().checkStateEffects(true);

        // offered before paying: Mox Lotus is tapped during payment, after X is announced
        AssertJUnit.assertTrue(me.getManaPool().canPayInfiniteX(spell));
        for (SpellAbility ma : mox.getManaAbilities()) {
            if ("Infinity".equals(ma.getManaPart().getOrigProduced())) {
                ma.setActivatingPlayer(me);
                AbilityUtils.resolve(ma);
            }
        }
        AssertJUnit.assertTrue(me.getManaPool().hasInfiniteColorless());

        // Fireball is {X}{R}: the {R} still needs real red mana, the X is infinite colorless
        ManaCostBeingPaid cost = new ManaCostBeingPaid(new ManaCost(new ManaCostParser("X R")));
        cost.setXManaCostPaid(Infinity.VALUE, null);
        AssertJUnit.assertEquals("{∞}{R}", cost.toString().replace(" ", ""));
        me.getManaPool().payManaCostFromPool(cost, spell, false, new java.util.ArrayList<>());
        AssertJUnit.assertEquals(0, cost.getGenericManaAmount());
        AssertJUnit.assertFalse(cost.isPaid());
        AssertJUnit.assertEquals("{R}", cost.toString().replace(" ", ""));
    }

    @Test
    public void testAiPlaysATurnWithIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        elemental(ai);
        addCard("Grizzly Bears", opp);
        addCard("Wall of Stone", opp);
        // the AI's own attack and block planning adds and multiplies power all over; none of it may overflow or hang
        playUntilPhase(game, forge.game.phase.PhaseType.MAIN2);
        AssertJUnit.assertTrue(game.isGameOver() || game.getPhaseHandler().is(forge.game.phase.PhaseType.MAIN2));
    }

    @Test
    public void testLifelinkInCombat() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card e = elemental(me);
        SpellAbility lifelink = forge.game.ability.AbilityFactory.getAbility("DB$ Pump | Defined$ Self | KW$ Lifelink", e);
        lifelink.setActivatingPlayer(me);
        AbilityUtils.resolve(lifelink);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(e.hasKeyword(Keyword.LIFELINK));

        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.COMBAT_DECLARE_BLOCKERS, me, false);
        forge.game.combat.Combat combat = new forge.game.combat.Combat(me);
        combat.addAttacker(e, opp);
        combat.setBlocked(e, false);
        game.getPhaseHandler().setCombat(combat);
        int guard = 0;
        while (!game.isGameOver() && !game.getPhaseHandler().is(forge.game.phase.PhaseType.MAIN2)) {
            game.getPhaseHandler().mainLoopStep();
            AssertJUnit.assertTrue("ran away", ++guard < 500);
        }
        AssertJUnit.assertEquals(Infinity.VALUE, me.getLife());
        AssertJUnit.assertTrue(opp.hasLost());
    }
}
