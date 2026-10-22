package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.perpetual.PerpetualClone;
import forge.game.card.perpetual.PerpetualInterface;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Mysterious Hero's Super Secret Identity, like disguise (user ruling 2026-10-22): before the game begins, a sleeved
 * player may put another creature card from their deck on top of it in the sleeve. It's then that creature in every
 * zone, for everyone, until its controller pays the cost to remove the card above it (this effect, a special action
 * from the battlefield). The covering card is out of the deck. The AI covers it with its best other creature.
 */
public class SecretIdentityEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Remove the card above " + sa.getHostCard() + " in its sleeve.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        reveal(sa.getHostCard().getGame().getCardState(sa.getHostCard()));
    }

    public static void reveal(final Card hero) {
        for (final PerpetualInterface p : java.util.List.copyOf(hero.getPerpetual())) {
            if (p instanceof PerpetualClone clone && clone.sa() != null && clone.sa().hasParam("SecretIdentity")) {
                hero.removePerpetual(clone.timestamp());
                hero.removeCloneState(clone.timestamp());
            }
        }
        hero.updateStateForView();
        hero.getGame().fireEvent(new GameEventCardStatsChanged(hero));
    }

    public static boolean isCovered(final Card hero) {
        return hero.getPerpetual().stream().anyMatch(p -> p instanceof PerpetualClone clone && clone.sa() != null
                && clone.sa().hasParam("SecretIdentity"));
    }

    /** As the game begins: offer the owner of each such card in their library a cover, if their deck is sleeved. */
    public static void offerCovers(final Player p) {
        if (!p.isSleeved()) {
            return;
        }
        for (final Card hero : CardLists.filter(p.getCardsIn(ZoneType.Library), c -> c.hasSVar("AsksSecretIdentity"))) {
            final CardCollection options = CardLists.filter(p.getCardsIn(ZoneType.Library),
                    CardPredicates.CREATURES, c -> c != hero && !isCovered(c) && !c.hasSVar("AsksSecretIdentity"));
            if (options.isEmpty()) {
                continue;
            }
            final Card cover;
            if (p.getController().isAI()) {
                // its best other creature: the one with the highest mana value, then power
                options.sort(java.util.Comparator.comparingInt((Card c) -> c.getCMC()).thenComparingInt(Card::getNetPower).reversed());
                cover = options.getFirst();
            } else {
                final SpellAbility ask = new SpellAbility.EmptySa(forge.game.ability.ApiType.SecretIdentity, hero, p);
                cover = p.getController().chooseSingleEntityForEffect(options, ask,
                        hero.getTranslatedName() + ": put a creature card from your deck on top of it in the sleeve? (Don't tell anyone!)",
                        true, null);
            }
            if (cover != null) {
                cover(hero, cover, p);
            }
        }
    }

    /** Puts the cover on top of the hero in its sleeve: the cover is out of the deck, and the hero is it from now on. */
    public static void cover(final Card hero, final Card cover, final Player p) {
        final Game game = p.getGame();
        final SpellAbility cause = AbilityFactory.getAbility("DB$ Clone | AddAbilities$ " + hero.getSVar("AsksSecretIdentity"), hero);
        cause.putParam("SecretIdentity", "True");
        cause.setActivatingPlayer(p);
        if (cover.getZone() != null) {
            cover.getZone().remove(cover);
        }
        final long ts = game.getNextTimestamp();
        hero.addCloneState(CardFactory.getCloneStates(cover, hero, cause), ts);
        hero.addPerpetual(new PerpetualClone(ts, cover, cause));
        hero.updateStateForView();
    }
}
