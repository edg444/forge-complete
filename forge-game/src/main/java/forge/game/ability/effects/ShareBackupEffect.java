package forge.game.ability.effects;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.keyword.KeywordInterface;
import forge.game.spellability.SpellAbility;

/**
 * Huge Truck: "permanently gains all of the backup abilities shared this way" - the abilities the backup ability that
 * targeted (TriggeredSourceSA) would give, given to the host for good.
 */
public class ShareBackupEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getHostCard() + " permanently gains the backup abilities shared this way.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (!(sa.getRootAbility().getTriggeringObject(AbilityKey.SourceSA) instanceof SpellAbility backup)) {
            return;
        }
        final KeywordInterface keyword = backup.getKeyword();
        if (keyword == null) {
            return;
        }
        final String[] k = keyword.getOriginal().split(":");
        final Card backupCard = backup.getHostCard();
        if (k.length < 3 || !backupCard.hasSVar(k[2])) {
            return;
        }
        // read with the backup creature's SVars, which the shared abilities name
        final SpellAbility grant = AbilityFactory.getAbility(backupCard.getSVar(k[2]) + " | Defined$ Self | Duration$ Permanent",
                host, backupCard);
        grant.setActivatingPlayer(sa.getActivatingPlayer());
        AbilityUtils.resolve(grant);
    }
}
