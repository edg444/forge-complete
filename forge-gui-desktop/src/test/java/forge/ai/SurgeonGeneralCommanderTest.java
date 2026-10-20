package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/**
 * Surgeon General Commander (UND #72), checked against Scryfall and its 2 rulings 2026-10-20.
 */
public class SurgeonGeneralCommanderTest extends AITest {

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    private Game start() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        fillLibrary(ai, 5);
        addCard("Surgeon General Commander", ai);
        game.getAction().checkStateEffects(true);
        return game;
    }

    @Test
    public void fiveColorIdentity() {
        AssertJUnit.assertEquals(5, FModel.getMagicDb().getCommonCards().getCard("Surgeon General Commander")
                .getRules().getColorIdentity().countColors());
    }

    @Test
    public void augmenting() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        Card kitten = addCard("Adorable Kitten", ai);
        Card orc = addCardToZone("Half-Orc, Half-", ai, ZoneType.Hand);
        int hand = ai.getCardsIn(ZoneType.Hand).size();
        SpellAbility sa = orc.getSpellAbilities().stream().filter(s -> s.getApi() == ApiType.Augment).findFirst().orElseThrow();
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(kitten);
        AbilityUtils.resolve(sa);
        resolveAll(game);
        AssertJUnit.assertEquals("the augment card left, a card was drawn", hand, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void enchantingOnlyYourOwn() {
        Game game = start();
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        addCards("Plains", 2, ai);
        Card mine = addCard("Grizzly Bears", ai);
        Card theirs = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);

        Card strength = addCardToZone("Holy Strength", ai, ZoneType.Hand);
        int hand = ai.getCardsIn(ZoneType.Hand).size();
        SpellAbility aura = strength.getFirstSpellAbility();
        aura.setActivatingPlayer(ai);
        aura.getTargets().add(mine);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, aura, null));
        resolveAll(game);
        AssertJUnit.assertTrue(game.getCardState(strength).isAttachedToEntity(mine));
        AssertJUnit.assertEquals(hand, ai.getCardsIn(ZoneType.Hand).size());

        Card other = addCardToZone("Holy Strength", ai, ZoneType.Hand);
        hand = ai.getCardsIn(ZoneType.Hand).size();
        aura = other.getFirstSpellAbility();
        aura.setActivatingPlayer(ai);
        aura.getTargets().add(theirs);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, aura, null));
        resolveAll(game);
        AssertJUnit.assertEquals("an opponent's creature isn't yours", hand - 1, ai.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void mutating() {
        Game game = start();
        Player ai = game.getPlayers().get(1);
        addCards("Forest", 3, ai);
        Card bears = addCard("Grizzly Bears", ai);
        Card gemrazer = addCardToZone("Gemrazer", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        int hand = ai.getCardsIn(ZoneType.Hand).size();
        SpellAbility mutate = gemrazer.getSpellAbilities().stream().filter(SpellAbility::isMutate).findFirst().orElseThrow();
        mutate.setActivatingPlayer(ai);
        mutate.getTargets().add(bears);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(ai, mutate, null));
        resolveAll(game);
        AssertJUnit.assertTrue(bears.hasMergedCard());
        AssertJUnit.assertEquals("Gemrazer left, a card was drawn", hand, ai.getCardsIn(ZoneType.Hand).size());
    }
}
