package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.AnimateLibraryEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Animate Library (ust/26): the library is a permanent while enchanted, and never leaves the battlefield. */
public class AnimateLibraryTest extends AITest {

    private Card animate(Player p) {
        Card aura = addCardToZone("Animate Library", p, ZoneType.Hand);
        SpellAbility sa = null;
        for (SpellAbility s : aura.getSpellAbilities()) {
            if (s.getApi() == ApiType.AnimateLibrary) {
                sa = s;
            }
        }
        AssertJUnit.assertNotNull(sa);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        // resolving off the real stack refreshes the last-known state that SBA replacements are read from
        p.getGame().copyLastState();
        p.getGame().getAction().checkStateEffects(true);
        return p.getGame().getCardState(aura);
    }

    @Test
    public void testLibraryBecomesACreatureSizedByItsCards() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        fillLibrary(me, 20);
        addCard("Soul Warden", me);
        int life = me.getLife();

        Card aura = animate(me);
        Card lib = AnimateLibraryEffect.findLibraryPermanent(me);
        AssertJUnit.assertNotNull(lib);
        AssertJUnit.assertEquals(GamePieceType.LIBRARY, lib.getGamePieceType());
        AssertJUnit.assertTrue(aura.isAttachedToEntity(lib));
        AssertJUnit.assertTrue(lib.isCreature() && lib.isArtifact());
        AssertJUnit.assertEquals(20, lib.getNetPower());
        AssertJUnit.assertEquals(20, lib.getNetToughness());
        AssertJUnit.assertTrue(lib.isSick());
        // it doesn't enter the battlefield, so Soul Warden doesn't see a creature arrive
        AssertJUnit.assertEquals(life, me.getLife());
        AssertJUnit.assertEquals(20, me.getCardsIn(ZoneType.Library).size());

        me.drawCards(3);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals(17, lib.getNetPower());
    }

    @Test
    public void testDestroyingTheLibraryExilesTheAuraInstead() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(me, 10);
        Card aura = animate(me);
        Card lib = AnimateLibraryEffect.findLibraryPermanent(me);

        SpellAbility murder = addCardToZone("Murder", opp, ZoneType.Hand).getFirstSpellAbility();
        murder.setActivatingPlayer(opp);
        murder.getTargets().add(lib);
        AbilityUtils.resolve(murder);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(game.getCardState(aura).isInZone(ZoneType.Exile));
        AssertJUnit.assertNull(AnimateLibraryEffect.findLibraryPermanent(me));
        AssertJUnit.assertEquals(10, me.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertTrue(me.getCardsIn(ZoneType.Graveyard).stream().noneMatch(c -> c.getGamePieceType() == GamePieceType.LIBRARY));
    }

    @Test
    public void testEmptyLibraryIsZeroToughness() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        fillLibrary(me, 2);
        Card aura = animate(me);
        me.drawCards(2);
        game.copyLastState();
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(game.getCardState(aura).isInZone(ZoneType.Exile));
        AssertJUnit.assertNull(AnimateLibraryEffect.findLibraryPermanent(me));
    }

    @Test
    public void testLosingTheAuraMakesItALibraryAgain() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(me, 10);
        Card first = animate(me);
        Card second = animate(me);
        Card lib = AnimateLibraryEffect.findLibraryPermanent(me);
        AssertJUnit.assertTrue(first.isAttachedToEntity(lib) && second.isAttachedToEntity(lib));
        AssertJUnit.assertEquals(1, me.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getGamePieceType() == GamePieceType.LIBRARY).count());

        // one gone, the other still keeps it a permanent
        game.getAction().destroy(first, null, true, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNotNull(AnimateLibraryEffect.findLibraryPermanent(me));

        game.getAction().destroy(game.getCardState(second), null, true, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNull(AnimateLibraryEffect.findLibraryPermanent(me));
        AssertJUnit.assertEquals(10, me.getCardsIn(ZoneType.Library).size());
    }
}
