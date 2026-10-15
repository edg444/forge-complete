package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Upgrade (Weaponized Scrap), replacing the card's move to the battlefield: it covers another artifact you control
 * instead, or is exiled if it can't. The covered artifact carries on as the same object - tapped, counters, attacking
 * and all, with no enters triggers (its rulings) - which is mutate's merge plumbing again, with only the card on top
 * counting (see CardFactory.getUpgradedCloneStates).
 */
public class UpgradeEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player you = sa.getActivatingPlayer();
        final CardCollection artifacts = CardLists.filter(you.getCardsIn(ZoneType.Battlefield),
                c -> c.isArtifact() && !c.equals(host));
        final Card covered = artifacts.isEmpty() ? null : you.getController().chooseSingleEntityForEffect(artifacts, sa,
                "Choose an artifact for " + host.getName() + " to cover", false, null);
        if (covered == null) {
            game.getAction().exile(host, sa, AbilityKey.newMap());
            return;
        }

        host.setController(you, 0);
        host.setMergedToCard(covered);
        if (!covered.hasMergedCard()) {
            covered.addMergedCard(covered);
        }
        covered.addMergedCardToTop(host);

        covered.removeMutatedStates();
        final long ts = game.getNextTimestamp();
        covered.setMutatedTimestamp(ts);
        covered.addCloneState(CardFactory.getMutatedCloneStates(covered, sa), ts);

        game.getTriggerHandler().clearActiveTriggers(covered, null);
        game.getTriggerHandler().registerActiveTrigger(covered, false);

        game.getAction().moveTo(host.getOwner().getZone(ZoneType.Merged), host, sa);

        host.setTapped(covered.isTapped());
        covered.updateStateForView();
        covered.updateTokenView();
        if (host.isCommander()) {
            host.getOwner().updateMergedCommanderInfo(covered, host);
            covered.updateCommanderView();
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return sa.getHostCard().getName() + " covers another artifact you control, or is exiled if it can't.";
    }
}
