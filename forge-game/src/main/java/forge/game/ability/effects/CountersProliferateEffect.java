package forge.game.ability.effects;

import java.util.List;
import java.util.Set;
import java.util.Map;

import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.PlayerPredicates;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class CountersProliferateEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Proliferate.");
        sb.append(" (Choose any number of permanents and/or players,");
        sb.append(" then give each another counter of each kind already there.)");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        int num = finiteAmount(sa, sa.hasParam("Amount") ? AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa) : 1, "proliferates");

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(p);
        repParams.put(AbilityKey.Source, sa);
        repParams.put(AbilityKey.Num, num);

        switch (game.getReplacementHandler().run(ReplacementType.Proliferate, repParams)) {
            case NotReplaced:
                break;
            case Updated:
                num = (int) repParams.get(AbilityKey.Num);
                break;
            default:
                return;
        }

        PlayerController pc = p.getController();

        for (int i = 0; i < num; i++) {
            FCollection<GameEntity> list = new FCollection<>();

            // CR 810.10d: a Two-Headed Giant head has every kind of counter their team has, so a teammate is
            // "poisoned" - and can be chosen - on their partner's poison alone
            list.addAll(game.getPlayers().filter(PlayerPredicates.hasCounters().or(pl -> pl.getPoisonCounters() > 0)));
            list.addAll(CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.hasCounters()));

            List<GameEntity> result = pc.chooseEntitiesForEffect(list, 0, list.size(), null, sa,
                    Localizer.getInstance().getMessage("lblChooseProliferateTarget"), p, null);

            // CR 701.34b: the team's poison is shared, so if more than one head of a team is chosen, only one of them
            // gets another poison counter - the proliferating player picks which
            final Set<Player> poisonedHeads = Sets.newHashSet();
            final Set<Integer> teamsDone = Sets.newHashSet();
            for (final GameEntity ge : result) {
                if (!(ge instanceof Player pl) || pl.getPoisonCounters() <= 0) {
                    continue;
                }
                if (!pl.isOnGiantTeam()) {
                    poisonedHeads.add(pl);
                } else if (teamsDone.add(pl.getTeam())) {
                    final FCollection<Player> heads = new FCollection<>();
                    for (final GameEntity other : result) {
                        if (other instanceof Player op && op.sharesTurnWith(pl)) {
                            heads.add(op);
                        }
                    }
                    poisonedHeads.add(heads.size() == 1 ? pl : pc.chooseSingleEntityForEffect(heads, sa,
                            "Choose which player on that team gets the poison counter", null));
                }
            }

            GameEntityCounterTable table = new GameEntityCounterTable();
            for (final GameEntity ge : result) {
                final boolean isPlayer = ge instanceof Player;
                for (final CounterType ct : ge.getCounters().elementSet()) {
                    if (!isPlayer || !ct.is(CounterEnumType.POISON)) {
                        ge.addCounter(ct, 1, p, table);
                    }
                }
                if (isPlayer && poisonedHeads.contains(ge)) {
                    ge.addCounter(CounterEnumType.POISON, 1, p, table);
                }
            }
            table.replaceCounterEffect(game, sa);

            game.getTriggerHandler().runTrigger(TriggerType.Proliferate, AbilityKey.mapFromPlayer(p), false);
        }
    }
}
