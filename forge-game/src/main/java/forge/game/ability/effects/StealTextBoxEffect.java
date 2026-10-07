package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.keyword.KeywordInterface;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.util.Lang;

/**
 * Phoebe, Head of S.N.E.A.K.: "permanently steals target creature's text box". The creature loses its rules text,
 * flavor text and watermarks; the thief gains them, as changed traits that belong to the object, so either side
 * leaving the battlefield ends it for that side only (rulings). Abilities granted by other effects aren't in a text
 * box: they stay where they are. A text box that held stolen ones is taken whole.
 */
public class StealTextBoxEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getHostCard() + " permanently steals " + Lang.joinHomogenous(getTargetCards(sa)) + "'s text box.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        final Card host = sa.getHostCard();
        final Card thief = game.getCardState(host, null);
        final boolean thiefHere = thief != null && thief.isInPlay() && thief.equalsWithGameTimestamp(host);

        for (final Card target : getTargetCards(sa)) {
            final Card victim = game.getCardState(target, null);
            if (victim == null || !victim.isInPlay() || !victim.equalsWithGameTimestamp(target)) {
                continue;
            }
            final long ts = game.getNextTimestamp();

            final List<SpellAbility> spells = Lists.newArrayList();
            for (final SpellAbility s : victim.getSpellAbilities()) {
                // its cast spell isn't text-box text, and keyword abilities come back with the keyword
                if (s.isIntrinsic() && !s.isSpell() && s.getKeyword() == null) {
                    spells.add(s);
                }
            }
            final List<Trigger> triggers = Lists.newArrayList();
            for (final Trigger t : victim.getTriggers()) {
                if (t.isIntrinsic() && t.getKeyword() == null) {
                    triggers.add(t);
                }
            }
            final List<ReplacementEffect> replacements = Lists.newArrayList();
            for (final ReplacementEffect r : victim.getReplacementEffects()) {
                if (r.isIntrinsic() && r.getKeyword() == null) {
                    replacements.add(r);
                }
            }
            final List<StaticAbility> statics = Lists.newArrayList();
            for (final StaticAbility st : victim.getStaticAbilities()) {
                if (st.isIntrinsic() && st.getKeyword() == null) {
                    statics.add(st);
                }
            }
            final List<KeywordInterface> keywords = Lists.newArrayList();
            for (final KeywordInterface kw : victim.getKeywords()) {
                if (kw.isIntrinsic()) {
                    keywords.add(kw);
                }
            }
            final Card.StolenTextBox box = new Card.StolenTextBox(victim.getName(), victim.getTextBoxOracle(),
                    victim.getFlavorText(), victim.getWatermarks(), victim.getPrintedRulesLines(),
                    victim.getPrintedTextLines());

            // an empty pit of sadness
            victim.addChangedCardTraits(null, null, null, null, CardTraitBase::isIntrinsic, ts, 0);
            victim.addChangedCardKeywordsInternal(null, keywords, false, ts, null, true);
            victim.setTextBoxStolen(true);
            victim.updateStateForView();

            if (thiefHere) {
                final List<SpellAbility> gainedSpells = Lists.newArrayList();
                for (final SpellAbility s : spells) {
                    gainedSpells.add(s.copy(thief, false));
                }
                final List<Trigger> gainedTriggers = Lists.newArrayList();
                for (final Trigger t : triggers) {
                    gainedTriggers.add(t.copy(thief, false));
                }
                final List<ReplacementEffect> gainedReplacements = Lists.newArrayList();
                for (final ReplacementEffect r : replacements) {
                    gainedReplacements.add(r.copy(thief, false));
                }
                final List<StaticAbility> gainedStatics = Lists.newArrayList();
                for (final StaticAbility st : statics) {
                    final StaticAbility gained = st.copy(thief, false);
                    // contradicting text boxes: the most recently stolen wins (rulings)
                    gained.putParam("Timestamp", String.valueOf(ts));
                    gainedStatics.add(gained);
                }
                final List<KeywordInterface> gainedKeywords = Lists.newArrayList();
                for (final KeywordInterface kw : keywords) {
                    gainedKeywords.add(kw.copy(thief, false));
                }
                thief.addChangedCardTraits(gainedSpells, gainedTriggers, gainedReplacements, gainedStatics, null, ts, 0);
                thief.addChangedCardKeywordsInternal(gainedKeywords, null, false, ts, null, true);
                thief.addStolenTextBox(box);
                thief.updateStateForView();
            }
            game.getGameLog().add(GameLogEntryType.STACK_RESOLVE, (thiefHere ? thief.toString() : host.toString())
                    + " steals " + victim + "'s text box.");
        }
    }
}
