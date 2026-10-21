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
            final boolean hit = aim != null && MyRandom.percentTrue(hitChance);
            if (hit) {
                touched.add(aim.getView());
                hits.merge(aim, 1, Integer::sum);
            }
            final CardView target = aim == null ? null : aim.getView();
            game.fireEvent(new GameEventFlipOntoBattlefield(host.getView(), target, touched, 1, false, style));
            game.fireEvent(new GameEventFlipOntoBattlefield(host.getView(), target, touched, 1, true, style));
            game.getGameLog().add(GameLogEntryType.INFORMATION, "Throw " + i + (aim == null ? " lands on nothing."
                    : hit ? " lands touching " + aim + "." : " misses " + aim + "."));
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
