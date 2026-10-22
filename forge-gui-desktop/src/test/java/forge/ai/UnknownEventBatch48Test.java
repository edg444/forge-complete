package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardDamageTable;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Maeve, Wearer of Many Hats, Halana and Alena and Gisa and Geralf, The Vast Scrier, The Salubrious Philosopher, Falko,
 * Showoff Pilot and The Traveling Postman (UNK rares), checked against Scryfall 2026-10-22 (no rulings).
 */
public class UnknownEventBatch48Test extends AITest {

    private void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private static SpellAbility ability(final String text, final Card host, final Player p) {
        final SpellAbility sa = AbilityFactory.getAbility(text, host);
        sa.setActivatingPlayer(p);
        return sa;
    }

    @Test
    public void testMaeveKeepsEveryRole() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card maeve = addCard("Maeve, Wearer of Many Hats", ai);
        fillLibrary(ai, 5);
        AbilityUtils.resolve(ability("DB$ Token | TokenScript$ role_royal | AttachedTo$ Self", maeve, ai));
        AbilityUtils.resolve(ability("DB$ Token | TokenScript$ role_young_hero | AttachedTo$ Self", maeve, ai));
        resolveAll(game);
        AssertJUnit.assertEquals(2, CardLists.filter(maeve.getAttachedCards(), c -> c.getType().hasSubtype("Role")).size());
        AssertJUnit.assertEquals("1 + a Role each + the Royal role's own +1/+1", 4, maeve.getNetPower());
        AssertJUnit.assertEquals("created attached isn't attaching (CR 701.3a)", 0, ai.getCardsIn(ZoneType.Hand).size());
        final Card bears = addCard("Grizzly Bears", ai);
        AbilityUtils.resolve(ability("DB$ Token | TokenScript$ role_cursed | AttachedTo$ Remembered", addRemembering(maeve, bears), ai));
        final Card cursed = CardLists.filter(bears.getAttachedCards(), c -> c.getType().hasSubtype("Role")).get(0);
        maeve.clearRemembered();
        final SpellAbility move = ability("DB$ Attach | Object$ Remembered | Defined$ Self", maeve, ai);
        maeve.addRemembered(cursed);
        AbilityUtils.resolve(move);
        resolveAll(game);
        AssertJUnit.assertEquals("attaching one draws", 1, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(3, CardLists.filter(maeve.getAttachedCards(), c -> c.getType().hasSubtype("Role")).size());
    }

    private static Card addRemembering(final Card host, final Card c) {
        host.addRemembered(c);
        return host;
    }

    @Test
    public void testHalanaCastCreatureEnters() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card halana = addCard("Halana and Alena and Gisa and Geralf", ai);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Graveyard);
        halana.addRemembered(bears);
        final SpellAbility effect = AbilityFactory.getAbility(halana.getSVar("DBZombie"), halana);
        effect.setActivatingPlayer(ai);
        AbilityUtils.resolve(effect);
        final Card entered = game.getAction().moveToPlay(bears, ai, null, AbilityKey.newMap());
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("X is its power", 3, entered.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(entered.getType().hasCreatureType("Zombie") && entered.getType().hasCreatureType("Bear"));
        AssertJUnit.assertTrue(entered.hasKeyword(Keyword.HASTE));
    }

    @Test
    public void testVastScrierAttackTriggers() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card scrier = addCard("The Vast Scrier", ai);
        addCardToZone("Brimaz, King of Oreskos", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, ai);
        final Combat combat = new Combat(ai);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(scrier, human);
        final SpellAbility put = scrier.getTriggers().get(0).ensureAbility();
        put.setActivatingPlayer(ai);
        put.setTriggeringObject(AbilityKey.Defender, human);
        AbilityUtils.resolve(put);
        final Card brimaz = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.getName().startsWith("Brimaz")).get(0);
        AssertJUnit.assertTrue(combat.isAttacking(brimaz) && brimaz.isTapped());
        resolveAll(game);
        AssertJUnit.assertEquals("Brimaz's own attack trigger", 1,
                CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), c -> c.isToken() && c.getType().hasCreatureType("Cat")).size());
    }

    @Test
    public void testPhilosopherKinds() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card philosopher = addCard("The Salubrious Philosopher", ai);
        final String[][] cases = {
            {"Forest", "addsMana"}, {"Llanowar Elves", "addsMana"}, {"Dark Ritual", "addsMana"},
            {"Lightning Bolt", "removalSpell"}, {"Murder", "removalSpell"}, {"Swords to Plowshares", "removalSpell"},
        };
        for (final String[] c : cases) {
            AssertJUnit.assertTrue(c[0], addCardToZone(c[0], ai, ZoneType.Library).isValid("Card." + c[1], ai, philosopher, null));
        }
        AssertJUnit.assertFalse("only hits players", addCardToZone("Sizzle", ai, ZoneType.Library).isValid("Card.removalSpell", ai, philosopher, null));
        AssertJUnit.assertFalse(addCardToZone("Grizzly Bears", ai, ZoneType.Library).isValid("Card.addsMana", ai, philosopher, null));
    }

    @Test
    public void testFalkoSpectacle() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        addCard("Falko, Showoff Pilot", ai);
        final Card vehicle = addCard("Smuggler's Copter", ai);
        final Card wurm = addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertFalse(wurm.hasKeyword(Keyword.SPECTACLE));
        final CardDamageTable damage = new CardDamageTable();
        damage.put(vehicle, human, 3);
        game.getAction().dealDamage(true, damage, new CardDamageTable(), new GameEntityCounterTable(), null);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(wurm.hasKeyword(Keyword.SPECTACLE));
        final SpellAbility spectacle = wurm.getSpellAbilities().stream().filter(SpellAbility::isSpectacle).findFirst().orElseThrow();
        AssertJUnit.assertEquals("{4}{G}{G} minus {2}", 4, spectacle.getPayCosts().getTotalMana().getCMC());
    }

    @Test
    public void testTravelingPostman() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card postman = addCard("The Traveling Postman", ai);
        final SpellAbility trig = postman.getTriggers().get(0).ensureAbility();
        trig.setActivatingPlayer(ai);
        AbilityUtils.resolve(trig);
        final List<Card> made = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isToken);
        AssertJUnit.assertEquals(1, made.size());
    }
}
