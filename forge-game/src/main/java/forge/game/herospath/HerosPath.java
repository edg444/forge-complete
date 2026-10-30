package forge.game.herospath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import forge.StaticData;
import forge.card.CardType;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameType;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.ICardTraitChanges;
import forge.game.card.perpetual.PerpetualAbilities;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.RegisteredPlayer;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.PaperCard;

/**
 * The Theros block's Hero's Path challenge decks: Face the Hydra, Battle the Horde and Defeat a God. One to four heroes
 * (one team) play against a deck that plays itself under its playmat's rules.
 * <p>
 * Sources: the Battle the Horde playmat (media.wizards.com/images/magic/daily/arcana/EN_9lakm7zfyxoe.pdf); the Hero's
 * Path FAQ (Dave Guskin, 2013-09-16) for Hero card limits and Defeat a God's multiplayer rules; "Battle the Horde With
 * Your Friends" (Trick Jarrett, 2014-02-28) for the Horde's; the Face the Hydra and Defeat a God playmat text as
 * transcribed on mtg.wiki, and the Hydra's starting Heads from Wizards' photo of its playmat; decklists from mtg.wiki.
 * <p>
 * The challenge deck's seat is played by {@code PlayerControllerChallenge}: it only does what its rules make it do, and
 * the heroes make its choices ("You make any choices for the Hydra").
 */
public final class HerosPath {
    private HerosPath() { }

    /** Internal name of the Horde's Unquenchable Fury, which shares its printed name with a Commander card. */
    public static final String HORDE_UNQUENCHABLE_FURY = "Unquenchable Fury (Battle the Horde)";
    public static final String XENAGOS_ASCENDED = "Xenagos Ascended";
    public static final String ENHANCEMENT_STICKERS = "Enhancement Stickers";

    public static final int HERO_TEAM = 0;
    public static final int CHALLENGE_TEAM = 1;

    public enum Difficulty { EASY, NORMAL, HARD }

    public enum Challenge {
        HYDRA(GameType.FaceTheHydra, "The Hydra", "Hydra Head", 2, true),
        HORDE(GameType.BattleTheHorde, "The Horde", "Minotaur Goreseeker", 3, false),
        GOD(GameType.DefeatAGod, "Xenagos", XENAGOS_ASCENDED, 3, false);

        public final GameType gameType;
        public final String opponentName;
        private final String imageCard;
        public final int maxHeroCards;
        public final boolean duplicateHeroCards;

        Challenge(GameType gameType, String opponentName, String imageCard, int maxHeroCards, boolean duplicateHeroCards) {
            this.gameType = gameType;
            this.opponentName = opponentName;
            this.imageCard = imageCard;
            this.maxHeroCards = maxHeroCards;
            this.duplicateHeroCards = duplicateHeroCards;
        }

        public static Challenge of(final GameType type) {
            for (final Challenge c : values()) {
                if (c.gameType == type) {
                    return c;
                }
            }
            return null;
        }

        public static Challenge of(final Iterable<GameType> variants) {
            if (variants != null) {
                for (final GameType type : variants) {
                    final Challenge c = of(type);
                    if (c != null) {
                        return c;
                    }
                }
            }
            return null;
        }

        /** The 60-card challenge deck. */
        public Map<String, Integer> deckList() {
            final Map<String, Integer> deck = new LinkedHashMap<>();
            switch (this) {
            case HYDRA:
                deck.put("Hydra Head", 11);
                deck.put("Ravenous Brute Head", 4);
                deck.put("Savage Vigor Head", 1);
                deck.put("Snapping Fang Head", 1);
                deck.put("Shrieking Titan Head", 1);
                deck.put("Disorienting Glower", 5);
                deck.put("Distract the Hydra", 5);
                deck.put("Grown from the Stump", 4);
                deck.put("Hydra's Impenetrable Hide", 4);
                deck.put("Neck Tangle", 3);
                deck.put("Noxious Hydra Breath", 4);
                deck.put("Strike the Weak Spot", 2);
                deck.put("Swallow the Hero Whole", 5);
                deck.put("Torn Between Heads", 4);
                deck.put("Unified Lunge", 6);
                break;
            case HORDE:
                deck.put("Minotaur Goreseeker", 10);
                deck.put("Minotaur Younghorn", 15);
                deck.put("Mogis's Chosen", 4);
                deck.put("Phoberos Reaver", 10);
                deck.put("Reckless Minotaur", 4);
                deck.put("Consuming Rage", 2);
                deck.put("Descend on the Prey", 2);
                deck.put("Intervention of Keranos", 2);
                deck.put("Touch of the Horned God", 2);
                deck.put(HORDE_UNQUENCHABLE_FURY, 2);
                deck.put("Altar of Mogis", 1);
                deck.put("Massacre Totem", 1);
                deck.put("Plundered Statue", 2);
                deck.put("Refreshing Elixir", 2);
                deck.put("Vitality Salve", 1);
                break;
            case GOD:
                deck.put(XENAGOS_ASCENDED, 1);
                deck.put("Maddened Oread", 2);
                deck.put("Ecstatic Piper", 16);
                deck.put("Pheres-Band Revelers", 4);
                deck.put("Rollicking Throng", 6);
                deck.put("Serpent Dancers", 2);
                deck.put("Wild Maenads", 2);
                deck.put("Impulsive Charge", 7);
                deck.put("Impulsive Destruction", 2);
                deck.put("Impulsive Return", 4);
                deck.put("Rip to Pieces", 2);
                deck.put("Xenagos's Scorn", 3);
                deck.put("Xenagos's Strike", 5);
                deck.put("Dance of Flame", 2);
                deck.put("Dance of Panic", 2);
                break;
            }
            return deck;
        }

        /**
         * What begins the game on the battlefield. The Hydra's playmat: Easy 2, Normal 3, Hard 4 cards named Hydra Head.
         * Xenagos: Xenagos Ascended and two Rollicking Throng (three harder, one easier), plus one Rollicking Throng
         * for each additional player (Hero's Path FAQ).
         */
        public List<String> startingCards(final Difficulty difficulty, final int heroes) {
            final List<String> cards = new ArrayList<>();
            final int level = difficulty.ordinal(); // 0, 1, 2
            if (this == HYDRA) {
                cards.addAll(Collections.nCopies(2 + level, "Hydra Head"));
            } else if (this == GOD) {
                cards.add(XENAGOS_ASCENDED);
                cards.addAll(Collections.nCopies(1 + level + Math.max(0, heroes - 1), "Rollicking Throng"));
            }
            return cards;
        }

        /** Battle the Horde: you take three turns before the Horde's first (two harder, four easier). */
        public int heroTurnsFirst(final Difficulty difficulty) {
            return this == HORDE ? 4 - difficulty.ordinal() : 1;
        }

        /** Battle the Horde: each additional player makes it reveal and cast one more card each turn. */
        public int cardsRevealedEachTurn(final int heroes) {
            return this == HYDRA ? 1 : 2 + (this == HORDE ? Math.max(0, heroes - 1) : 0);
        }

        /** Battle the Horde and Defeat a God: the players share a life total of 20. */
        public boolean sharesLife() {
            return this != HYDRA;
        }

        public String getName() {
            return gameType.toString();
        }
    }

    /** A Hero card a player brings, with the Enhancement Stickers stuck on it (1-based, Scryfall's order). */
    public record HeroChoice(PaperCard card, List<Integer> enhancements) {
        /** Lobby form: the card name, then "|" and the enhancement numbers, comma-separated. */
        public static HeroChoice parse(final String s) {
            final String[] parts = s.split("\\|", 2);
            final PaperCard pc = StaticData.instance().getCommonCards().getCard(parts[0]);
            if (pc == null) {
                return null;
            }
            final List<Integer> enh = new ArrayList<>();
            if (parts.length > 1) {
                for (final String n : parts[1].split(",")) {
                    if (StringUtils.isNumeric(n.trim()) && !n.trim().isEmpty()) {
                        enh.add(Integer.parseInt(n.trim()));
                    }
                }
            }
            return new HeroChoice(pc, enh);
        }

        public static String encode(final String name, final List<Integer> enhancements) {
            return enhancements == null || enhancements.isEmpty() ? name : name + "|" + StringUtils.join(enhancements, ",");
        }
    }

    public static List<HeroChoice> parseHeroChoices(final Iterable<String> encoded) {
        final List<HeroChoice> result = new ArrayList<>();
        if (encoded != null) {
            for (final String s : encoded) {
                final HeroChoice hc = HeroChoice.parse(s);
                if (hc != null) {
                    result.add(hc);
                }
            }
        }
        return result;
    }

    /** Every Hero card, in collector number order. */
    public static List<PaperCard> allHeroCards() {
        final List<PaperCard> result = new ArrayList<>();
        for (final PaperCard pc : StaticData.instance().getCommonCards().getUniqueCards()) {
            if (pc.getRules().getType().hasType(CardType.CoreType.HeroCard)) {
                result.add(pc);
            }
        }
        result.sort((a, b) -> a.getName().compareTo(b.getName()));
        return result;
    }

    /** Whether a Hero card takes Enhancement Stickers: the Journey into Nyx weapons are Hero Equipment. */
    public static boolean takesEnhancements(final PaperCard pc) {
        return pc.getRules().getType().hasType(CardType.CoreType.HeroCard) && pc.getRules().getType().isEquipment();
    }

    /** The seven enhancements' rules text, from the Enhancement Stickers card. */
    public static List<String> enhancementDescriptions() {
        final List<String> result = new ArrayList<>();
        final PaperCard sheet = stickerSheet();
        if (sheet == null) {
            return result;
        }
        for (int i = 1; ; i++) {
            final String ability = svarOf(sheet, "Enhancement" + i);
            if (ability == null) {
                break;
            }
            final String desc = StringUtils.substringAfter(ability, "Description$");
            result.add(StringUtils.substringBefore(desc, "|").trim());
        }
        return result;
    }

    /** Sticker sheets are variant cards, kept apart from the regular card database. */
    private static PaperCard stickerSheet() {
        final PaperCard sheet = StaticData.instance().getVariantCards().getCard(ENHANCEMENT_STICKERS);
        return sheet != null ? sheet : StaticData.instance().getCommonCards().getCard(ENHANCEMENT_STICKERS);
    }

    /** A problem with a player's Hero cards for this challenge (Hero's Path FAQ), or null. */
    public static String heroCardProblem(final Challenge c, final List<String> names) {
        if (names.size() > c.maxHeroCards) {
            return "up to " + c.maxHeroCards + " Hero cards";
        }
        if (!c.duplicateHeroCards && names.stream().distinct().count() < names.size()) {
            return "each Hero card only once";
        }
        return null;
    }

    private static String svarOf(final PaperCard pc, final String name) {
        for (final Entry<String, String> e : pc.getRules().getMainPart().getVariables()) {
            if (e.getKey().equals(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static int intSVarOf(final PaperCard pc, final String name) {
        final String v = svarOf(pc, name);
        return v == null ? 0 : Integer.parseInt(v.trim());
    }

    private static PaperCard card(final String name) {
        final PaperCard pc = StaticData.instance().getCommonCards().getCard(name);
        if (pc == null) {
            throw new IllegalStateException("Hero's Path card missing: " + name);
        }
        return pc;
    }

    // ---- setting up the match ----

    /**
     * The challenge deck's seat: its library, what it starts with on the battlefield, no hand, its own team. Seat it
     * after the heroes, so that seat order is turn order.
     */
    public static RegisteredPlayer registerChallenge(final Challenge c, final Difficulty difficulty, final int heroes) {
        final Map<String, Integer> library = c.deckList();
        final List<IPaperCard> onBattlefield = new ArrayList<>();
        for (final String name : c.startingCards(difficulty, heroes)) {
            library.merge(name, -1, Integer::sum);
            onBattlefield.add(card(name));
        }
        final Deck deck = new Deck(c.getName());
        for (final Entry<String, Integer> e : library.entrySet()) {
            if (e.getValue() > 0) {
                deck.getMain().add(card(e.getKey()), e.getValue());
            }
        }
        final RegisteredPlayer rp = new RegisteredPlayer(deck);
        rp.addExtraCardsOnBattlefield(onBattlefield);
        rp.setStartingHand(0);
        rp.setTeamNumber(CHALLENGE_TEAM);
        rp.setChallenge(c, difficulty, heroes);
        return rp;
    }

    /** The heroes: one team, with their Hero cards' starting life and hand bonuses. */
    public static void prepareHero(final RegisteredPlayer hero, final List<String> heroCards) {
        hero.setTeamNumber(HERO_TEAM);
        final List<HeroChoice> choices = parseHeroChoices(heroCards);
        hero.setHeroChoices(choices);
        for (final HeroChoice hc : choices) {
            hero.setStartingLife(hero.getStartingLife() + intSVarOf(hc.card(), "HeroStartingLife"));
        }
    }

    public static Challenge getChallenge(final Game game) {
        if (game == null) {
            return null;
        }
        for (final Challenge c : Challenge.values()) {
            if (game.getRules().hasAppliedVariant(c.gameType)) {
                return c;
            }
        }
        return null;
    }

    public static boolean isChallengeDeck(final Player p) {
        if (p == null || p.getGame() == null || getChallenge(p.getGame()) == null) {
            return false;
        }
        final RegisteredPlayer rp = p.getRegisteredPlayer();
        return rp != null && rp.getChallenge() != null;
    }

    public static Player getChallengePlayer(final Game game) {
        if (getChallenge(game) == null) {
            return null;
        }
        for (final Player p : game.getRegisteredPlayers()) {
            if (isChallengeDeck(p)) {
                return p;
            }
        }
        return null;
    }

    /** The heroes still in the game, in turn order. */
    public static PlayerCollection getHeroes(final Game game) {
        final PlayerCollection heroes = new PlayerCollection();
        for (final Player p : game.getPlayers()) {
            if (!isChallengeDeck(p)) {
                heroes.add(p);
            }
        }
        return heroes;
    }

    /**
     * The hero who makes the challenge deck's choices: the first human hero, else the first hero. ("You make any
     * choices for the Hydra." With several heroes, user ruling 2026-10-30: the heroes choose.)
     */
    public static Player getChooser(final Game game) {
        final PlayerCollection heroes = getHeroes(game);
        for (final Player p : heroes) {
            if (!p.getController().isAI()) {
                return p;
            }
        }
        return heroes.isEmpty() ? null : heroes.getFirst();
    }

    /** Called as the game is created: the heroes' shared life total (Battle the Horde, Defeat a God). */
    public static void onGameCreated(final Game game) {
        final Challenge c = getChallenge(game);
        if (c == null || !c.sharesLife()) {
            return;
        }
        final PlayerCollection heroes = getHeroes(game);
        if (heroes.size() < 2) {
            return;
        }
        // "All players facing the Minotaurs will have a shared life total of 20 life"; each The Slayer still adds 7
        int life = 20;
        for (final Player hero : heroes) {
            life += hero.getStartingLife() - 20;
        }
        for (final Player hero : heroes) {
            hero.setStartingLife(life);
            hero.joinLifeTotal(heroes.getFirst(), life);
        }
    }

    /** Called once each player's cards are in place: Hero cards onto the battlefield with their stickers. */
    public static void putHeroCardsOntoBattlefield(final Player player, final RegisteredPlayer rp) {
        if (rp.getHeroChoices() == null) {
            return;
        }
        for (final HeroChoice hc : rp.getHeroChoices()) {
            final Card c = Card.fromPaperCard(hc.card(), player);
            c.setStartsGameInPlay(true);
            player.getZone(ZoneType.Battlefield).add(c);
            final int bonusHand = intSVarOf(hc.card(), "HeroStartingHand");
            if (bonusHand != 0) {
                player.addStartingHandOnlyBonus(bonusHand);
            }
            if (!hc.enhancements().isEmpty()) {
                applyEnhancements(c, hc.enhancements());
            }
        }
    }

    /** Stickers stay stuck on the card wherever it goes, so they're perpetual. */
    private static void applyEnhancements(final Card c, final List<Integer> enhancements) {
        final PaperCard sheet = stickerSheet();
        if (sheet == null) {
            return;
        }
        for (final Entry<String, String> e : sheet.getRules().getMainPart().getVariables()) {
            if (e.getKey().startsWith("Enhancement") && !e.getKey().matches("Enhancement\\d+")) {
                c.setSVar(e.getKey(), e.getValue());
            }
        }
        final List<Trigger> triggers = new ArrayList<>();
        final List<StaticAbility> statics = new ArrayList<>();
        for (final int n : enhancements) {
            final String ability = svarOf(sheet, "Enhancement" + n);
            if (ability == null) {
                continue;
            }
            if (ability.startsWith("T:")) {
                triggers.add(TriggerHandler.parseTrigger(ability.substring(2), c, false));
            } else if (ability.startsWith("S:")) {
                statics.add(StaticAbility.create(ability.substring(2), c, c.getCurrentState(), false));
            }
        }
        final long ts = c.getGame().getNextTimestamp();
        final ICardTraitChanges changes = c.addChangedCardTraits(null, triggers, null, statics, null, ts, 0);
        c.addPerpetual(new PerpetualAbilities(ts, changes));
    }

    /** Called as the game starts: the challenge's rules, and the Horde's turns it waits out. */
    public static void onGameStarted(final Game game) {
        final Player opp = getChallengePlayer(game);
        if (opp == null) {
            return;
        }
        final RegisteredPlayer rp = opp.getRegisteredPlayer();
        final Challenge c = rp.getChallenge();
        final Card host = Card.fromPaperCard(card(c.imageCard), opp);
        final Card eff = SpellAbilityEffect.createEffect(null, host, opp, c.getName(), host.getImageKey(), game.getNextTimestamp());
        final List<String> rules = rulesFor(c, rp.getChallengeHeroes());
        // the SVars first: abilities are looked up as their triggers and replacements are parsed
        for (final String rule : rules) {
            if (rule.startsWith("V:")) {
                addRule(eff, rule);
            }
        }
        for (final String rule : rules) {
            if (!rule.startsWith("V:")) {
                addRule(eff, rule);
            }
        }
        game.getAction().moveTo(ZoneType.Command, eff, null, AbilityKey.newMap());

        final int skip = c.heroTurnsFirst(rp.getChallengeDifficulty()) - 1;
        if (skip > 0) {
            final Card skipper = SpellAbilityEffect.createEffect(null, host, opp, c.getName() + " waits",
                    host.getImageKey(), game.getNextTimestamp());
            skipper.setSVar("TurnsLeft", "Number$" + skip);
            skipper.setSVar("Waited", "DB$ StoreSVar | SVar$ TurnsLeft | Type$ CountSVar | Expression$ TurnsLeft/Minus.1"
                    + " | SubAbility$ DoneWaiting");
            skipper.setSVar("DoneWaiting", "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile"
                    + " | ConditionCheckSVar$ TurnsLeft | ConditionSVarCompare$ EQ0");
            final ReplacementEffect re = ReplacementHandler.parseReplacement("Event$ BeginTurn | ActiveZones$ Command"
                    + " | ValidPlayer$ You | ReplaceWith$ Waited | Description$ You take " + (skip + 1)
                    + " turns at the start of the game, before the Horde takes its first turn.", skipper, true);
            re.setLayer(ReplacementLayer.Control);
            skipper.addReplacementEffect(re);
            game.getAction().moveTo(ZoneType.Command, skipper, null, AbilityKey.newMap());
        }
    }

    private static void addRule(final Card eff, final String rule) {
        final String body = rule.substring(2);
        switch (rule.charAt(0)) {
        case 'S':
            eff.addStaticAbility(body);
            break;
        case 'T':
            final Trigger t = TriggerHandler.parseTrigger(body, eff, true);
            t.setActiveZone(EnumSet.of(ZoneType.Command));
            eff.addTrigger(t);
            break;
        case 'R':
            eff.addReplacementEffect(ReplacementHandler.parseReplacement(body, eff, true));
            break;
        default: // V:Name:value
            final String[] kv = body.split(":", 2);
            eff.setSVar(kv[0], kv[1]);
        }
    }

    /** The playmat's rules, as an effect in the challenge deck's command zone. */
    static List<String> rulesFor(final Challenge c, final int heroes) {
        final List<String> rules = new ArrayList<>();
        final String who = c == Challenge.GOD ? "Xenagos" : c.opponentName.replace("The ", "the ");
        final String whos = c == Challenge.GOD ? "Xenagos's" : who + "'s";
        final int reveal = c.cardsRevealedEachTurn(heroes);
        switch (c) {
        case HYDRA:
            rules.add("S:Mode$ CanBeAttacked | EffectZone$ Command | ValidCard$ Creature.Head+YouCtrl | Description$ You can attack Heads"
                    + " directly with your creatures. Any number of creatures can attack a single Head.");
            rules.add("T:Mode$ ChangesZone | Origin$ Battlefield | Destination$ Any | ValidCard$ Card.Head"
                    + " | TriggerZones$ Command | Execute$ HydraGrow | TriggerDescription$ Growing New Heads — Whenever a"
                    + " Head leaves the battlefield, reveal the top two cards of the Hydra's library. Put any Heads onto"
                    + " the battlefield and any sorcery cards into the Hydra's graveyard.");
            rules.add("V:HydraGrow:DB$ Dig | Defined$ You | DigNum$ 2 | Reveal$ True | ChangeNum$ All"
                    + " | ChangeValid$ Card.Head | DestinationZone$ Battlefield | DestinationZone2$ Graveyard");
            rules.add("T:Mode$ Phase | Phase$ Main1 | ValidPlayer$ You | TriggerZones$ Command | Execute$ ChallengeReveal"
                    + " | TriggerDescription$ The Hydra's Turn — Reveal the top card of the Hydra's library. The Hydra"
                    + " casts that card.");
            rules.add("T:Mode$ Phase | Phase$ BeginCombat | ValidPlayer$ You | TriggerZones$ Command | Execute$ HydraBite"
                    + " | TriggerDescription$ The Hydra deals 1 damage to you for each untapped card named Hydra Head it"
                    + " controls and 2 damage to you for each untapped elite Head it controls.");
            rules.add("V:HydraBite:DB$ RepeatEach | RepeatCards$ Creature.namedHydra Head+untapped+YouCtrl"
                    + " | Zone$ Battlefield | RepeatSubAbility$ HydraBiteOne | SubAbility$ HydraBiteElite");
            rules.add("V:HydraBiteOne:DB$ DamageAll | ValidPlayers$ Opponent | NumDmg$ 1 | DamageSource$ Remembered");
            rules.add("V:HydraBiteElite:DB$ RepeatEach | RepeatCards$ Creature.Head+Elite+untapped+YouCtrl"
                    + " | Zone$ Battlefield | RepeatSubAbility$ HydraBiteTwo");
            rules.add("V:HydraBiteTwo:DB$ DamageAll | ValidPlayers$ Opponent | NumDmg$ 2 | DamageSource$ Remembered");
            rules.add("R:Event$ DamageDone | ActiveZones$ Command | ValidTarget$ You | IsPresent$ Creature.Head+YouCtrl"
                    + " | ReplaceWith$ HydraHurtHead | Description$ If the Hydra would be dealt damage or lose life,"
                    + " instead deal that much damage to a Head of your choice.");
            rules.add("V:HydraHurtHead:DB$ ChooseCard | Defined$ You | Choices$ Creature.Head+YouCtrl | Mandatory$ True"
                    + " | AILogic$ ChallengeWeakest | ChoiceTitle$ Choose a Head to be dealt the damage"
                    + " | SubAbility$ HydraHurtRedirect");
            rules.add("V:HydraHurtRedirect:DB$ ReplaceEffect | VarName$ Affected | VarValue$ ChosenCard | VarType$ Card");
            rules.add("R:Event$ DamageDone | ActiveZones$ Command | ValidTarget$ You | IsPresent$ Creature.Head+YouCtrl"
                    + " | PresentCompare$ EQ0 | Prevent$ True | Secondary$ True | Description$ With no Head to deal it"
                    + " to, damage to the Hydra is ignored.");
            rules.add("R:Event$ LifeReduced | ActiveZones$ Command | ValidPlayer$ You | IsDamage$ False"
                    + " | ReplaceWith$ HydraLossHead | Secondary$ True | Description$ If the Hydra would lose life,"
                    + " instead deal that much damage to a Head of your choice.");
            rules.add("V:HydraLossHead:DB$ ChooseCard | Defined$ You | Choices$ Creature.Head+YouCtrl | Mandatory$ True"
                    + " | AILogic$ ChallengeWeakest | ChoiceTitle$ Choose a Head to be dealt the damage"
                    + " | SubAbility$ HydraLossDamage");
            rules.add("V:HydraLossDamage:DB$ DealDamage | Defined$ ChosenCard | NumDmg$ HydraLoss");
            rules.add("V:HydraLoss:ReplaceCount$Amount");
            rules.add("R:Event$ Moved | ActiveZones$ Command | Origin$ Battlefield | ExcludeDestination$ Graveyard"
                    + " | ValidLKI$ Card.Head+YouOwn | ReplaceWith$ ChallengeToGraveyard | Description$ If a Head would"
                    + " move to any zone other than the graveyard, instead put it into the Hydra's graveyard.");
            rules.add("S:Mode$ Continuous | EffectZone$ Command | Affected$ You | Description$ At the end of any turn, if there are no Heads on"
                    + " the battlefield, you win! Ignore effects that would cause the Hydra to draw or discard cards,"
                    + " or any impossible actions. You make any choices for the Hydra.");
            break;
        case HORDE:
            rules.add("T:Mode$ Phase | Phase$ Main1 | ValidPlayer$ You | TriggerZones$ Command | Execute$ ChallengeReveal"
                    + " | TriggerDescription$ At the beginning of the Horde's precombat main phase each turn, reveal the"
                    + " top " + (reveal == 2 ? "two cards" : reveal + " cards") + " of the Horde's library. Then the"
                    + " Horde casts those cards." + (reveal > 2 ? " (One more card for each additional player.)" : ""));
            rules.add("R:Event$ LifeReduced | ActiveZones$ Command | ValidPlayer$ You | ReplaceWith$ HordeMill"
                    + " | Description$ The Horde doesn't have a life total, but its library serves a similar purpose. If"
                    + " the Horde would lose life, put that many cards from the top of its library into its graveyard.");
            rules.add("V:HordeMill:DB$ Mill | Defined$ You | NumCards$ HordeLoss");
            rules.add("V:HordeLoss:ReplaceCount$Amount");
            rules.add("R:Event$ Moved | ActiveZones$ Command | Origin$ Battlefield"
                    + " | ExcludeDestination$ Graveyard,Library | ValidLKI$ Card.YouOwn | ReplaceWith$ ChallengeToGraveyard"
                    + " | Description$ If one of the Horde's permanents would move to any zone other than its library or"
                    + " graveyard, that card is put into the Horde's graveyard.");
            rules.add("S:Mode$ Continuous | EffectZone$ Command | Affected$ You | Description$ You win the game if the Horde has no cards left in"
                    + " its library and the Horde controls no creatures on the battlefield. Ignore effects that would"
                    + " cause the Horde to draw or discard cards, or perform any impossible actions. You make the choice"
                    + " if the Horde needs to make a decision.");
            break;
        case GOD:
            rules.add("S:Mode$ CanBeAttacked | EffectZone$ Command | ValidCard$ Creature.Reveler+YouCtrl,Creature.namedXenagos Ascended+YouCtrl"
                    + " | Description$ You attack Xenagos Ascended and his Revelers directly with your creatures.");
            rules.add("T:Mode$ Phase | Phase$ Main1 | ValidPlayer$ You | TriggerZones$ Command | Execute$ ChallengeReveal"
                    + " | TriggerDescription$ At the beginning of Xenagos's first main phase each turn, reveal the top"
                    + " two cards of Xenagos's library. Then Xenagos casts those cards.");
            rules.add("R:Event$ DamageDone | ActiveZones$ Command | ValidTarget$ You"
                    + " | IsPresent$ Card.namedXenagos Ascended+YouCtrl | ReplaceWith$ ToXenagosAscended"
                    + " | Description$ You may target Xenagos as though he were a player. Damage that would be dealt to"
                    + " him is dealt to Xenagos Ascended instead, and he doesn't lose life.");
            rules.add("V:ToXenagosAscended:DB$ ReplaceEffect | VarName$ Affected"
                    + " | VarValue$ Valid Card.namedXenagos Ascended+YouCtrl | VarType$ Card");
            rules.add("R:Event$ DamageDone | ActiveZones$ Command | ValidTarget$ You"
                    + " | IsPresent$ Card.namedXenagos Ascended+YouCtrl | PresentCompare$ EQ0 | Prevent$ True"
                    + " | Secondary$ True | Description$ Damage to Xenagos is ignored.");
            rules.add("R:Event$ LifeReduced | ActiveZones$ Command | ValidPlayer$ You | IsDamage$ False"
                    + " | Layer$ CantHappen | Secondary$ True | Description$ Xenagos doesn't lose life.");
            rules.add("R:Event$ Moved | ActiveZones$ Command | Origin$ Battlefield"
                    + " | ExcludeDestination$ Graveyard,Library | ValidLKI$ Card.YouOwn | ReplaceWith$ ChallengeToGraveyard"
                    + " | Description$ If one of Xenagos's permanents would move to any zone other than his library or"
                    + " graveyard, that card is put into Xenagos's graveyard.");
            rules.add("S:Mode$ Continuous | EffectZone$ Command | Affected$ You | Description$ Xenagos Ascended and Reveler creatures don't"
                    + " attack unless a card specifically says they do. You make the choice if Xenagos needs to make a"
                    + " decision. Ignore effects that would cause Xenagos to draw or discard cards, or perform any other"
                    + " impossible actions.");
            break;
        }
        rules.add("V:ChallengeReveal:DB$ PeekAndReveal | Defined$ You | PeekAmount$ " + reveal
                + " | RememberRevealed$ True | SubAbility$ ChallengeCast");
        rules.add("V:ChallengeCast:DB$ Play | Defined$ Remembered | Controller$ You | WithoutManaCost$ True | Amount$ All"
                + " | SubAbility$ ChallengeForget");
        rules.add("V:ChallengeForget:DB$ Cleanup | ClearRemembered$ True");
        rules.add("V:ChallengeToGraveyard:DB$ ReplaceEffect | VarName$ Destination | VarValue$ Graveyard");
        // it can't lose the game any other way than its playmat's
        rules.add("R:Event$ GameLoss | ActiveZones$ Command | ValidPlayer$ You | Layer$ CantHappen | Secondary$ True"
                + " | Description$ " + StringUtils.capitalize(who) + " can't lose the game except as " + whos
                + " challenge says.");
        return rules;
    }

    // ---- during the game ----

    /** The challenge deck has no hand and never draws. */
    public static boolean ignoresDraw(final Player p) {
        return isChallengeDeck(p);
    }

    /**
     * Whether this player skips this turn's draw step: the challenge deck always; in Face the Hydra each hero on their
     * first turn ("You go first. (Don't draw a card on your first turn.)", each player taking "you").
     */
    public static boolean skipsDrawStep(final Player p) {
        if (isChallengeDeck(p)) {
            return true;
        }
        return getChallenge(p.getGame()) == Challenge.HYDRA && p.getTurn() <= 1;
    }

    /** The heroes go first in Face the Hydra and Battle the Horde; Defeat a God decides as usual. */
    public static Player firstPlayer(final Game game) {
        final Challenge c = getChallenge(game);
        if (c == null || c == Challenge.GOD) {
            return null;
        }
        final PlayerCollection heroes = getHeroes(game);
        return heroes.isEmpty() ? null : heroes.getFirst();
    }

    /** Battle the Horde, checked with state-based actions: no library and no creatures, and the heroes win. */
    public static void checkDefeated(final Game game) {
        if (getChallenge(game) != Challenge.HORDE || game.getAge().ordinal() < forge.game.GameStage.Play.ordinal()) {
            return;
        }
        final Player horde = getChallengePlayer(game);
        if (horde != null && !horde.hasLost() && horde.getCardsIn(ZoneType.Library).isEmpty()
                && horde.getCreaturesInPlay().isEmpty()) {
            // already inside checkGameOverCondition, which sees the win next
            markHeroesWon(game, "Battle the Horde");
        }
    }

    /** Face the Hydra: "At the end of any turn, if there are no Heads on the battlefield, you win!" */
    public static boolean checkEndOfTurn(final Game game) {
        if (getChallenge(game) != Challenge.HYDRA) {
            return false;
        }
        final CardCollection heads = CardLists.getType(game.getCardsIn(ZoneType.Battlefield), "Head");
        if (!heads.isEmpty()) {
            return false;
        }
        heroesWin(game, "Face the Hydra");
        return true;
    }

    public static void heroesWin(final Game game, final String challenge) {
        markHeroesWon(game, challenge);
        game.getAction().checkGameOverCondition();
    }

    private static void markHeroesWon(final Game game, final String challenge) {
        for (final Player hero : getHeroes(game)) {
            hero.altWinBySpellEffect(challenge);
        }
    }
}
