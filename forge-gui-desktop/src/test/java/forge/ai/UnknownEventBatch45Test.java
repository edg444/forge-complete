package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Kami Knight, Bobby, Another Dwarf, Fioran Reformist, Azorius Traffic Enforcement, The Tokenator, Third Stage of
 * Magic Design, Naming Screen, Hosting Season and The Miniaturizer (UNK white rares), checked against Scryfall
 * 2026-10-22 (no rulings).
 */
public class UnknownEventBatch45Test extends AITest {

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

    private static List<Card> tokens(final Player p) {
        return CardLists.filter(p.getCardsIn(ZoneType.Battlefield), Card::isToken);
    }

    @Test
    public void testSecondStageAbilityWordAfterTheFirstLine() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card stage = addCard("Second Stage of Magic Design", ai);
        addCardToZone("Steppe Lynx", ai, ZoneType.Graveyard);
        final Card roil = addCardToZone("Roil Elemental", ai, ZoneType.Hand);
        AssertJUnit.assertFalse("its landfall is on its second line", roil.isValid("Card.unsharedKeywordOrAbilityWord", ai, stage, null));
    }

    @Test
    public void testKamiKnight() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card knight = addCard("The Kami Knight", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        final Card splitter = addCard("Bonesplitter", ai);
        splitter.attachToEntity(knight, null);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(5, knight.getNetPower());
        AssertJUnit.assertEquals("the equipment's bonus too", 4, bears.getNetPower());
        bears.addGoad(game.getNextTimestamp(), human);
        AssertJUnit.assertFalse(bears.isGoaded());
    }

    @Test
    public void testBobbyGivesSneak() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Bobby, Another Dwarf", ai);
        final Card legend = addCardToZone("Isamaru, Hound of Konda", ai, ZoneType.Hand);
        final Card equipment = addCardToZone("Bonesplitter", ai, ZoneType.Hand);
        final Card plain = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(legend.hasKeyword(Keyword.SNEAK));
        AssertJUnit.assertTrue(equipment.hasKeyword(Keyword.SNEAK));
        AssertJUnit.assertFalse(plain.hasKeyword(Keyword.SNEAK));
    }

    @Test
    public void testFioranReformist() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        game.getAction().becomeMonarch(human, null);
        AssertJUnit.assertEquals(human, game.getMonarch());
        final Card reformist = addCard("Fioran Reformist", ai);
        AbilityUtils.resolve(svar(reformist, "TrigAbolish", ai));
        AssertJUnit.assertNull(game.getMonarch());
        game.getAction().becomeMonarch(ai, null);
        AssertJUnit.assertNull("no one can become the monarch", game.getMonarch());
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", human);
        AssertJUnit.assertTrue(ai.isValid("Player.withMostCreatures", ai, reformist, null));
        AssertJUnit.assertFalse(human.isValid("Player.withMostCreatures", ai, reformist, null));
    }

    @Test
    public void testAzoriusTrafficEnforcement() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card enforcement = addCard("Azorius Traffic Enforcement", ai);
        addCard("Grizzly Bears", human);
        final Card theirs = game.getAction().moveToPlay(addCardToZone("Bola Slinger", human, ZoneType.Hand), human, null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertTrue("their backup doesn't trigger", game.getStack().isEmpty());
        addCard("Grizzly Bears", ai);
        game.getAction().moveToPlay(addCardToZone("Bola Slinger", ai, ZoneType.Hand), ai, null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        AssertJUnit.assertFalse("its controller's own backup still does", game.getStack().isEmpty());
        game.getStack().clear();

        final SpellAbility detain = enforcement.getTriggers().get(0).ensureAbility();
        detain.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(detain.canTarget(theirs));
        AssertJUnit.assertFalse(detain.canTarget(human.getCreaturesInPlay().get(0)));
    }

    @Test
    public void testTokenator() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card tokenator = addCard("The Tokenator", ai);
        final SpellAbility attack = tokenator.getTriggers().get(0).ensureAbility();
        attack.setActivatingPlayer(ai);
        AbilityUtils.resolve(attack);
        AssertJUnit.assertEquals(2, ai.getCounters(CounterType.getType("TOKEN")));
        AbilityUtils.resolve(attack);
        game.getAction().checkStateEffects(true);
        final List<Card> made = tokens(ai);
        AssertJUnit.assertEquals(2, made.size());
        final Card second = made.stream().filter(t -> t.getNetToughness() + t.getNetPower() == 4).findFirst().orElseThrow();
        AssertJUnit.assertEquals("the AI splits it evenly", 2, second.getNetPower());
        AssertJUnit.assertFalse(second.getType().getCreatureTypes().isEmpty());
        AssertJUnit.assertFalse(second.getColor().isColorless());
    }

    @Test
    public void testThirdStage() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", ai);
        addCard("Raging Goblin", ai);
        addCard("Forest", ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Graveyard);
        final Card stage = addCardToZone("Third Stage of Magic Design", ai, ZoneType.Hand);
        final SpellAbility sa = stage.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        final List<Card> made = tokens(ai);
        AssertJUnit.assertEquals("creature: the Bears and the Goblin", 2, made.size());
        AssertJUnit.assertEquals("", made.get(0).getName());
        AssertJUnit.assertTrue(made.get(0).getType().getCreatureTypes().isEmpty());
        AssertJUnit.assertTrue(made.get(0).getColor().isColorless());
        AssertJUnit.assertEquals(2, made.get(0).getNetPower());
    }

    @Test
    public void testNamingScreen() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card screen = addCard("Naming Screen", ai);
        AbilityUtils.resolve(svar(screen, "TrigToken", ai));
        game.getAction().checkStateEffects(true);
        final List<Card> heroes = tokens(ai);
        AssertJUnit.assertEquals(4, heroes.size());
        AssertJUnit.assertEquals(4, heroes.stream().map(Card::getName).distinct().count());
        AssertJUnit.assertTrue(heroes.stream().allMatch(h -> h.getName().length() <= 4 && h.getType().hasCreatureType("Hero")));
        AssertJUnit.assertTrue("no other creature shares its name", heroes.stream().allMatch(h -> h.getNetPower() == 2));
        final Card twin = addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", ai);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals(2, twin.getNetPower());
    }

    @Test
    public void testHostingSeasonOnAnyOtherDate() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Hosting Season", ai);
        final Card alarm = addCardToZone("Raise the Alarm", ai, ZoneType.Hand);
        final SpellAbility sa = alarm.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(4, tokens(ai).size());
        AssertJUnit.assertTrue(tokens(ai).stream().allMatch(t -> t.getCounters(CounterEnumType.P1P1) == 1));
    }

    @Test
    public void testMiniaturizer() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card mini = addCard("The Miniaturizer", ai);
        final Card target = addCard("Grizzly Bears", human);
        final SpellAbility soospect = svar(mini, "TrigSoospect", ai);
        soospect.getTargets().add(target);
        AbilityUtils.resolve(soospect);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertTrue(target.hasKeyword(Keyword.FIRST_STRIKE));
        AssertJUnit.assertFalse(CombatUtil.canBlock(target));

        final Card dead = addCardToZone("Craw Wurm", ai, ZoneType.Graveyard);
        final SpellAbility copy = AbilityFactory.getAbility("DB$ CopyPermanent | Defined$ Remembered | RemoveCreatureType$ True"
                + " | AddTypes$ Artifact & Equipment | AddKeywords$ Equip:ThisManaCost | AddStaticAbilities$ EquipPump | AddSVars$ X", mini);
        copy.setActivatingPlayer(ai);
        mini.addRemembered(dead);
        AbilityUtils.resolve(copy);
        final Card gear = tokens(ai).get(0);
        AssertJUnit.assertTrue(gear.isEquipment() && gear.isArtifact());
        AssertJUnit.assertFalse(gear.isCreature());
        AssertJUnit.assertTrue(gear.getType().getCreatureTypes().isEmpty());
        AssertJUnit.assertTrue(gear.getKeywords().stream().anyMatch(k -> k.getOriginal().equals("Equip:4 G G")));
        final Card bears = addCard("Grizzly Bears", ai);
        gear.attachToEntity(bears, null);
        game.getAction().checkStaticAbilities();
        AssertJUnit.assertEquals("+6/+6, the Wurm's power", 8, bears.getNetPower());
    }
}
