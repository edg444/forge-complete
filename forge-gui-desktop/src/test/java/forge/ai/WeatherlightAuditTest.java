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

/** Weatherlight script review and R&D's Secret Lair printed text (2026-10-23). */
public class WeatherlightAuditTest extends AITest {

    private static Card printing(final String name, final String set, final Player owner, final ZoneType zone) {
        final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(name, set), owner);
        c.setGameTimestamp(owner.getGame().getNextTimestamp());
        owner.getZone(zone).add(c);
        return c;
    }

    private static void resolveAll(final Game game) {
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        while (!game.getStack().isEmpty()) {
            game.getStack().resolveStack();
            game.getAction().checkStateEffects(true);
            game.getStack().addAllTriggeredAbilitiesToStack();
        }
    }

    @Test
    public void testFireWhipIgnoresSummoningSicknessAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card bear = addCard("Grizzly Bears", p);
        bear.setSickness(true);
        Card whip = printing("Fire Whip", "WTH", p, ZoneType.Battlefield);
        whip.attachToEntity(bear, null);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = null;
        for (SpellAbility s : whip.getSpellAbilities()) {
            if (s.getDescription().contains("(As printed)")) {
                sa = s;
            }
        }
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(sa.getRestrictions().canPlay(whip, sa));
        AssertJUnit.assertTrue(sa.getPayCosts().canPay(sa, p, false));
    }

    @Test
    public void testScorchedRuinsEntersBeforeItsSacrificeAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("R&D's Secret Lair", p);
        Card ruins = printing("Scorched Ruins", "WTH", p, ZoneType.Hand);
        ruins = game.getAction().moveToPlay(ruins, p, null, null);
        AssertJUnit.assertTrue("in play while the trigger waits", ruins.isInPlay());
        resolveAll(game);
        AssertJUnit.assertTrue("no lands to sacrifice, so it's buried", p.getCardsIn(ZoneType.Graveyard).contains(ruins));
    }

    @Test
    public void testAbeyanceStopsManaAbilitiesAsPrinted() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCard("R&D's Secret Lair", p);
        addCardToZone("Island", p, ZoneType.Library);
        Card forest = addCard("Forest", opp);
        Card abeyance = printing("Abeyance", "WTH", p, ZoneType.Hand);
        SpellAbility sa = abeyance.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.setHostCard(game.getAction().moveToStack(abeyance, sa));
        sa.getTargets().add(opp);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
        SpellAbility mana = forest.getManaAbilities().getFirst();
        mana.setActivatingPlayer(opp);
        AssertJUnit.assertTrue(forge.game.staticability.StaticAbilityCantBeCast.cantBeActivatedAbility(mana, forest, opp));
    }
}
