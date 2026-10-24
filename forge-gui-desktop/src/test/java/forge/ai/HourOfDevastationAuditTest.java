package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.model.FModel;

/** Hour of Devastation and Archenemy: Nicol Bolas review (2026-10-23). */
public class HourOfDevastationAuditTest extends AITest {

    /** Sets a scheme in motion for p, resolving its "set in motion" trigger directly. */
    private void setInMotion(final String scheme, final Player p) {
        Card c = Card.fromPaperCard(FModel.getMagicDb().getVariantCards().getCard(scheme), p);
        p.getZone(ZoneType.Command).add(c);
        SpellAbility sa = c.getTriggers().get(0).ensureAbility();
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
    }

    private int payTributeLeft(final int lands) {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < lands; i++) {
            addCard("Forest", p);
        }
        addCards("Grizzly Bears", 2, opp);
        game.getAction().checkStateEffects(true);
        setInMotion("Pay Tribute to Me", p);
        return opp.getCreaturesInPlay().size();
    }

    @Test
    public void testPayTributeToMeSecondCreatureNeedsSixLands() {
        AssertJUnit.assertEquals(1, payTributeLeft(1));
        AssertJUnit.assertEquals(0, payTributeLeft(6));
    }

    @Test
    public void testEveryDreamANightmareDrawsForDiscardedLands() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
        addCardToZone("Forest", opp, ZoneType.Hand);
        int hand = p.getCardsIn(ZoneType.Hand).size();
        setInMotion("Every Dream a Nightmare", p);
        AssertJUnit.assertEquals(hand + 1, p.getCardsIn(ZoneType.Hand).size());
    }

    @Test
    public void testHourOfGloryExilesTheGodsCopiesInHand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card god = addCard("Rhonas the Indomitable", opp);
        addCardToZone("Rhonas the Indomitable", opp, ZoneType.Hand);
        Card glory = addCardToZone("Hour of Glory", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = glory.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(god);
        AbilityUtils.resolve(sa);
        AssertJUnit.assertEquals(0, opp.getCardsIn(ZoneType.Hand).size());
    }
}
