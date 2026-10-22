package forge.game.ability.effects;

import java.util.List;

import com.google.common.collect.Lists;

import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Your Favorite Character: name a character from the Magic storyline (Scryfall Tagger's characters, as Vorthos
 * names them), choose up to three colors, two creature types and two of vigilance, flying, haste, trample and menace.
 * The card has them in every zone. The AI takes the character on most of its spells, its deck's main colors and
 * creature types, and flying and trample.
 */
public class FavoriteCharacterEffect extends SpellAbilityEffect {

    private static final List<String> KEYWORDS = List.of("Vigilance", "Flying", "Haste", "Trample", "Menace");

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Name a character, and choose its colors, creature types and keywords.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard().getGame().getCardState(sa.getHostCard());
        final Player p = sa.getActivatingPlayer();
        final String label = host.getTranslatedName() + ": ";
        final SpellAbility ask = new SpellAbility.EmptySa(ApiType.ChooseType, host, p);
        final String name = p.getController().chooseSomeType("Character", ask, forge.card.PrintingTraits.allCharacterNames());
        final SpellAbility colorAsk = new SpellAbility.EmptySa(ApiType.ChooseColor, host, p);
        colorAsk.putParam("AILogic", "MostProminentInComputerDeck");
        final ColorSet colors = p.getController().chooseColors(label + "choose up to three colors", colorAsk, 0, 3, ColorSet.WUBRG);
        final List<String> types = Lists.newArrayList();
        final List<String> typeChoices = Lists.newArrayList(CardType.getAllCreatureTypes());
        for (int i = 0; i < 2; i++) {
            final String type = p.getController().chooseSomeType("Creature", ask, typeChoices);
            if (type == null) {
                break;
            }
            types.add(type);
            typeChoices.remove(type);
        }
        final List<String> keywords = Lists.newArrayList();
        if (p.getController().isAI()) {
            keywords.add("Flying");
            keywords.add("Trample");
        } else {
            final List<String> keywordChoices = Lists.newArrayList(KEYWORDS);
            for (int i = 0; i < 2; i++) {
                final String kw = p.getController().chooseSomeType("FavoriteKeyword", ask, keywordChoices);
                if (kw == null) {
                    break;
                }
                keywords.add(kw);
                keywordChoices.remove(kw);
            }
        }
        for (final CardStateName stateName : host.getStates()) {
            final CardState state = host.getState(stateName);
            if (name != null && !name.isEmpty()) {
                state.setName(name);
            }
            state.setColor(colors);
            // "has the chosen ... types": they're its creature types now, Shapeshifter included only if chosen
            state.setCreatureTypes(types);
            state.addIntrinsicKeywords(keywords);
        }
        host.updateStateForView();
    }
}
