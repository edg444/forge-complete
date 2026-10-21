package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardDamageTable;
import forge.game.card.CardLists;
import forge.game.card.CardView;
import forge.game.event.GameEventFlipOntoBattlefield;
import forge.game.event.TossStyle;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Knife and Death: throwing Throws$ cards from outside the game onto the table, one at a time. There's no table, so each
 * throw is aimed - the thrower picks one of the Aim$ permanents - and hits it HitChance$ percent of the time (user,
 * 2026-10-21), shown with the throw animation the other dexterity cards use. Hits are only assessed once every card is
 * thrown (rulings): each permanent then takes 1 damage from the source for each card touching it.
 * <p>
 * Dart Throw and Cover the Spot throw at an Attraction for its prize instead (user, 2026-10-21: 50% and 15%): NoDamage$
 * leaves the host's Hits SVar saying how many cards touched, and CoverChance$ makes the throws one try at covering the
 * aim completely - all of them land on it or the cover fails - leaving Covered as 1 or 0.
 */
public class ThrowCardsEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " throws cards onto the table.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final int throwsLeft = AbilityUtils.calculateAmount(host, sa.getParamOrDefault("Throws", "1"), sa);
        final int hitChance = Integer.parseInt(sa.getParamOrDefault("HitChance", "50"));
        // the game decides what each card touches; the board mustn't pick its own hits
        final TossStyle style = new TossStyle(-1, 0, true, false, false);
        final Map<Card, Integer> hits = Maps.newLinkedHashMap();
        final boolean cover = sa.hasParam("CoverChance");
        final boolean covered = cover && MyRandom.percentTrue(Integer.parseInt(sa.getParam("CoverChance")));
        int touches = 0;

        for (int i = 1; i <= throwsLeft; i++) {
            final CardCollection aimable = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                    sa.getParam("Aim"), you, host, sa);
            final List<CardView> touched = new CopyOnWriteArrayList<>();
            if (aimable.isEmpty()) {
                game.fireEvent(new GameEventFlipOntoBattlefield(host.getView(), null, touched, 1, false, style));
                game.fireEvent(new GameEventFlipOntoBattlefield(host.getView(), null, touched, 1, true, style));
                continue;
            }
            final Card aim = you.getController().chooseSingleEntityForEffect(aimable, sa,
                    "Throw " + i + " of " + throwsLeft + ": choose what to aim at", false, null);
            // a failed cover still lands some cards on it, but never all of them
            final boolean hit = aim != null && (cover ? covered || (i < throwsLeft || touches < i - 1) && MyRandom.percentTrue(hitChance)
                    : MyRandom.percentTrue(hitChance));
            if (hit) {
                touches++;
                touched.add(aim.getView());
                hits.merge(aim, 1, Integer::sum);
            }
            final CardView target = aim == null ? null : aim.getView();
            game.fireEvent(new GameEventFlipOntoBattlefield(host.getView(), target, touched, 1, false, style));
            game.fireEvent(new GameEventFlipOntoBattlefield(host.getView(), target, touched, 1, true, style));
            game.getGameLog().add(GameLogEntryType.INFORMATION, "Throw " + i + (aim == null ? " lands on nothing."
                    : hit ? " lands touching " + aim + "." : " misses " + aim + "."));
        }

        if (cover) {
            host.setSVar("Covered", covered ? "Number$1" : "Number$0");
        }
        if (sa.hasParam("NoDamage") || cover) {
            host.setSVar("Hits", "Number$" + touches);
            if (cover) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, covered ? "The cards cover it completely!"
                        : "The cards don't cover it completely.");
            }
            return;
        }
        if (hits.isEmpty()) {
            return;
        }
        final Card source = game.getChangeZoneLKIInfo(host);
        final CardDamageTable damageMap = new CardDamageTable();
        final CardDamageTable preventMap = new CardDamageTable();
        final GameEntityCounterTable counterTable = new GameEntityCounterTable();
        for (final Map.Entry<Card, Integer> e : hits.entrySet()) {
            if (e.getKey().isInPlay()) {
                damageMap.put(source, e.getKey(), e.getValue());
            }
        }
        game.getAction().dealDamage(false, damageMap, preventMap, counterTable, sa);
    }
}
