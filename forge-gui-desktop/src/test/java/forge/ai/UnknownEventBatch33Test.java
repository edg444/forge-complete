package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Guess Who it Is!, Unstickerify and Higher Level Zone Monster (UNK uncommons), checked against Scryfall 2026-10-22
 * (no rulings).
 */
public class UnknownEventBatch33Test extends AITest {

    @Test
    public void testGuessWhoNarrowsItDown() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        for (final String name : new String[] {"Grizzly Bears", "Serra Angel", "Shivan Dragon", "Llanowar Elves", "Mountain", "Mountain"}) {
            addCardToZone(name, human, ZoneType.Library);
        }
        final int library = human.getCardsIn(ZoneType.Library).size();
        final Card guess = addCardToZone("Guess Who it Is!", ai, ZoneType.Hand);
        final SpellAbility cast = guess.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        cast.getTargets().add(human);
        AbilityUtils.resolve(cast);
        AssertJUnit.assertEquals("four different creatures, five questions: the AI guesses right", library, human.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertTrue(ai.getCreaturesInPlay().isEmpty());
    }

    @Test
    public void testUnstickerify() {
        final Game game = initAndCreateGame();
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        final Card wolf = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard("Throat Wolf", "CMB1", "65"), human);
        human.getZone(ZoneType.Battlefield).add(wolf);
        AssertJUnit.assertTrue(wolf.isPlaytestCard());
        final Card bears = addCard("Grizzly Bears", human);
        final Card first = addCard("Unstickerify", ai);
        first.attachToEntity(wolf, null);
        final Card second = addCard("Unstickerify", ai);
        second.attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("Wastes", wolf.getName());
        AssertJUnit.assertTrue(wolf.isLand());
        AssertJUnit.assertFalse(wolf.isCreature());
        AssertJUnit.assertTrue(bears.isCreature());
        final Card free = addCard("Llanowar Elves", human);
        bears.setSickness(false);
        free.setSickness(false);
        game.getPhaseHandler().devModeSet(forge.game.phase.PhaseType.COMBAT_DECLARE_ATTACKERS, human);
        AssertJUnit.assertTrue(forge.game.combat.CombatUtil.canAttack(free));
        AssertJUnit.assertFalse(forge.game.combat.CombatUtil.canAttack(bears));
    }

    @Test
    public void testHigherLevelZoneMonsterAddsColors() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Grizzly Bears", ai);
        final Card monster = addCardToZone("Higher Level Zone Monster", ai, ZoneType.Hand);
        monster.setChosenColors(java.util.List.of("white"));
        final SpellAbility copy = AbilityFactory.getAbility(monster.getSVar("DBCopy"), monster);
        copy.setActivatingPlayer(ai);
        AbilityUtils.resolve(copy);
        AssertJUnit.assertEquals("Grizzly Bears", monster.getName());
        AssertJUnit.assertTrue(monster.getColor().hasWhite());
        AssertJUnit.assertTrue(monster.getColor().hasGreen());
    }
}
