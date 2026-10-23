package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.card.CardStateName;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.spellability.SpellAbility;

/**
 * Werewhat: the Defined exiled card becomes this creature's back face (with AddKeyword$, nightbound), making it a
 * transforming double-faced card while it's on the battlefield. A double-faced card gives whichever face its
 * controller picks (the other is ignored). The two separate again when it leaves (GameAction).
 */
public class BecomeBackFaceEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final List<Card> defined = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
        if (defined.isEmpty()) {
            return;
        }
        final Card face = defined.get(0);
        CardState chosen = face.getState(CardStateName.Original);
        final CardStateName otherFace = face.isTransformable() || face.isModal() ? CardStateName.Backside : null;
        if (otherFace != null && face.hasState(otherFace)) {
            final List<String> names = Lists.newArrayList(face.getState(CardStateName.Original).getName(),
                    face.getState(otherFace).getName());
            final String pick = sa.getActivatingPlayer().getController().isAI() ? names.get(0)
                    : sa.getActivatingPlayer().getController().chooseSomeType("face", sa, names, false);
            if (names.get(1).equals(pick)) {
                chosen = face.getState(otherFace);
            }
        }
        host.addAlternateState(CardStateName.Backside, false);
        final CardState back = host.getState(CardStateName.Backside);
        back.copyFrom(chosen, false);
        if (sa.hasParam("AddKeyword")) {
            back.addIntrinsicKeyword(sa.getParam("AddKeyword"), true);
            host.updateKeywordsCache(back);
        }
        host.setBackFaceSource(face);
        host.updateStateForView();
        host.getGame().getGameLog().add(GameLogEntryType.INFORMATION, chosen.getName() + " becomes " + host + "'s back face.");
    }
}
