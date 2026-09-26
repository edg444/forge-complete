# Fork changelog

Everything this fork adds on top of upstream [Card-Forge/forge](https://github.com/Card-Forge/forge).
Upstream's own daily changes are **not** listed here — only work done in this fork.

- `origin` → `https://github.com/edg444/forge-complete.git` (this fork)
- `upstream` → `https://github.com/Card-Forge/forge.git` (merged in regularly; see README of the workflow below)

Reconstructed 2026-08-06 from git history (`git log --no-merges HEAD --not upstream/master`),
which remains the authoritative record if this file and the commits ever disagree.

**Keeping this current:** add to *Unreleased* as work lands, and date a section when it's built and
deployed. The one-line rule: if it changed behavior, it belongs here.

---

## Engine capabilities added by this fork

A reference index — the reusable machinery, as opposed to individual cards. Card scripts across the
fork depend on these, so removing one is never local.

### Half values (Unhinged)

A complete half-integer layer running parallel to the whole-number one.

- **Power/toughness** — `PT:2.5/1`, `basePowerHalves()`/`baseToughnessHalves()`, half-aware P/T
  boosts (`AddPowerHalves$`/`AddToughnessHalves$`, `addPTBoostHalves`). Net P/T is computed in halves
  and floor-divided, so `getNetToughness` must mirror `getNetPower` rather than use the whole-number
  breakdown.
- **Mana** — real half shards (`HW`, `HR`, …) in costs and in the pool, floating half mana as change,
  half mana symbols drawn in both UIs, wired into pool events and the mana-burn setting.
- **Life** — half life totals, half life payment (`PayLifeHalves<N>`), half life shown in the avatar
  and life panel.
- **Damage** — half damage marked on creatures, half-aware prevention shields.

### Honor-system mechanics

- `CostFlavorAction<description/ButtonLabel>` — a cost paid by asserting you did something physical.
- `AILogic$ Chance.N` — rolled once per turn (not at every priority), one attempt per turn, honored
  by every API rather than only ones using the base `canPlay`.
- Persistent honor states modelled as custom counters toggled by two zero-cost abilities
  (Standing Army's `STANDING`, Fat Ass's `EATING`).

### Choices the rules can't derive

- `DB$ ChooseArtist` + `Card.ArtistIsChosen` + `sharesArtistWith <restriction>` + `DB$ SetArtist`.
- `ChooseType | Type$ word | FreeInput$ True` (free text) with `MinLetters$` validation;
  `Type$ letter` with an AI branch (`MostCommonInitial`) in `chooseSomeType`.
- `DB$ ChooseEyeColor`, `DB$ GuessArtist`.
- `Card.chosenTypeKind`, so the detail panel says "chosen word"/"chosen letter", not always
  "chosen type".

### Card-text and name mechanics

- `Count$ CardPunctuationMarks`, `CardTextBoxNumbers`, `WordsInName` (readable from a static),
  `ChosenLetterInName`, `CardBingoLines`, `CurrentHour`, `MergedCount`,
  `DifferentExpansionSymbols`.
- Properties: `textHasChosenWord`, `nameStartsWithChosenLetter`, `fewerLettersInNameThanSource`,
  `HasHalfSymbol`, `Rarity<name>`, `alphabeticallyFirstNonLand`, `SilverBordered`.
- Flavor text available in-game (`CardFlavorText`, `Card.getTextBoxContents`) — several Un-set cards
  read the whole text box, flavor included.
- Flavor names count in name-based mechanics.

### Printing traits

- `res/lists/PrintingTraits.txt` (`forge.card.PrintingTraits`, lazily loaded like `CardFlavorText`) —
  per printing: watermark (and back-face watermark), open-mouth artwork (Scryfall Tagger's
  `loose-lips` tag and its children), and the printed border wherever Scryfall disagrees with the
  edition files (6,724 printings, mostly borderless). Keyed `CODE|collector`; tokens `@CODE|collector`.
  Regenerate with `_tools/printing-traits/generate.js` from Scryfall's default-cards and art-tags bulk.
- `Card.printedBorderColor()` (knows `BORDERLESS`/`YELLOW`), `getWatermark()`, `hasOpenMouthArt()`,
  `getCollectorNumberValue()` (last digit run: 12a → 12). `borderColor()` and the silver-border rules
  are deliberately unchanged.
- Properties `BlackBordered`, `CollectorNumberEven`/`Odd`, `Watermarked`, `Watermark_<name>`,
  `OpenMouthArt`; `nameWords_Odd`/`_Even`.

### Five-face split cards

Only Who // What // When // Where // Why has ever needed this.

- `CardStateName.Split3/4/5` and a shared `SPLIT_STATES` list; every hardcoded LeftSplit+RightSplit
  pair iterates the states a card actually has, so **two-face splits take an identical path**.
- `ALTERNATE` advances to the next face instead of always landing on face 1.
- Combined name, mana cost, color, color identity, type and oracle text fold in every face.
- Fuse, Aftermath and Rooms are deliberately left two-face.
- Split-state views are populated in a **separate pass** from ability-text rendering, because
  rendering one face can read a sibling.
- Network delta sync encodes a card-state key as `cardId * CSV_STATE_SLOTS + ordinal`; the fork
  widens `CSV_STATE_SLOTS` from upstream's 16 to 32 (`DeltaPacket`), since the three extra states
  push `CardStateName` to 18 values. Upstream's 16-slot guard otherwise fails `DeltaPacket`'s
  class init and breaks all network play. The client (`NetworkGuiGame`) decodes **every**
  `CardStateViewType` property as a slot reference, not just upstream's four named ones, so
  `Split3State`–`Split5State` arrive as real state views. Covered by `FiveFaceSplitNetworkTest`.

### Zones

- **Dual residency** (`Card.shadowZone`) — Yet Another Aether Vortex's top-of-library permanents are
  on the battlefield *and* in the library. Battlefield is the primary zone; the library keeps a
  shadow entry. Residency must end in `Zone.remove`, never `Zone.add`: leaving the battlefield hands
  a *copy* to the destination zone, so an add-side hook never sees the original.

### Pink, a sixth color

`MagicColor.PINK` (`1 << 5`) is a real color bit, deliberately left out of `ALL_COLORS` so nothing
that iterates the five touches it — but a pink permanent is **not** colorless. `ColorSet` gained 32
constants at indices 32–63 to preserve its ordinal-equals-mask invariant. Only Water Gun Balloon
Game's Giant Teddy Bear is pink; "choose a color" prompts still offer exactly five.

### Printed text vs. errata (R&D's Secret Lair)

- `StaticAbilityMode.IgnoreErrata` + `StaticAbilityIgnoreErrata` — is the Lair out, and does *this
  printing* predate a given errata date.
- Properties `PrintedTextActive`, `PrintedBefore <yyyyMMdd>`, `SourcePrintedBefore <yyyyMMdd>`
  (the last asks about the card doing the checking, for an Aura's Enchant restriction).
- The one broad rule is templating: a card printed before Dominaria reads "target creature or
  player" and can't hit planeswalkers or battles. Everything else is per card, on the card.

### Infinite mana

`ManaPool.addInfiniteColorless()` — a genuine flag, not a large number. Spending colorless refills
it; it clears with the pool at end of step or phase, and displays as ∞.

### Other

- `DB$ RememberNumber` — writes a number into the remembered set (which is a `Set`, so repeats
  collapse).
- `NotifyMessage$` works on **any** ability, not only dice rolls.
- `UnlessPaidSubAbility$` — fires when an Unless cost *is* paid.
- `AddTrigger$`, `RandomSet`, `Staying Power`, additional activation zones, `Rotate180` display for
  tokens printed inverted.
- Dev mode can pick which printing to add.

---

## Log

### Unreleased — self-reference sweep; AI combat and equip fixes; Unstable white 12–25; Animate Library

- **Animate Library** (ust/26): the library becomes a permanent — new `GamePieceType.LIBRARY`, a
  nameless colorless artifact creature shown as a card back, P/T from the Aura (`Count$InOwnersLibrary`).
  - `SVar:AuraSpell` overrides an Aura's generated Attach spell; `SP$ AnimateLibrary` creates the
    library permanent (or reuses it for a second Animate Library) and enters attached to it.
  - Per the user's ruling it neither enters nor leaves the battlefield: placed and removed without
    zone-change triggers. It stays a permanent while any Animate Library is on it.
  - "Exile this Aura instead" is a plain `Moved` replacement; if a library permanent would still go
    anywhere, `GameAction.changeZone` makes it just stop existing — it never becomes a card in a zone.
  - `Card.getPaperCard()` returns null for a blank name instead of throwing; property
    `AnimatedLibrary`; the library has no printed border.
  - AI casts it with 6+ cards in library and no library already animated. `AnimateLibraryTest` (4).
    Suite: 754 run, 0 failed, 6 skipped.

- **Unstable white, ust/12–25**: Knight of the Kitchen Sink (all six variants), Knight of the Widget,
  Oddly Uneven, Old Guard, Ordinary Pony, Rhino-, Sacrifice Play, Side Quest, Success!, Teacher's Pet.
  - Knight of the Kitchen Sink's protections read the new printing traits (above). Knight of the
    Widget counts `Watermark_orderofthewidget`.
  - Word counts keep "Token" in a generated token's name: CR 111.4 (effective 2026-08-07) names a
    Human Soldier token "Human Soldier Token", superseding the older Unstable ruling. Zero words is even.
  - `hasReminderText` reads a functional variant's own text, not the base face's.
  - Ordinary Pony's errata ("so you can't flicker creatures more than once each turn") marks the
    returned creature, not the Pony: a Pony that gets flickered back (a new object) still can't
    flicker the same creature again that turn, while a later flicker by anything else makes a new,
    unmarked object.
  - Sacrifice Play: `ChooseCard | AtRandom$ ThreatExtremes` — the person outside the game is
    simulated: 40% the biggest threat, 40% the smallest, 20% across the rest. Threat is the AI's
    creature evaluation, plugged into forge-game through `CardThreat` at startup.
  - Side Quest removes the creature from the game (zone `None`, not exile) until your next turn.
    `GameAction.moveTo` now passes move params for `None`, so `Duration$` returns work from there,
    and the return looks in the owner's `None` zone (which `getCardState` doesn't search).
  - Teacher's Pet: `Augment | ChangeType$` searches the library for the augment card.
  - AI: ChooseCard picks its target opponent *before* checking the choices, so `TargetedPlayerCtrl`
    choices (Blot Out, End of the Hunt, Sacrifice Play) no longer always look empty; `AILogic$
    SideQuest` sends a tapped attacker in main 2; Augment won't sacrifice Teacher's Pet into an empty
    search.
  - Tests: `PrintingTraitsTest` (10), `UnstableWhiteTest` (6). Suite: 750 run, 0 failed, 6 skipped.

- **Self-reference sweep over every card's text**, against Scryfall's Oracle bulk data of 2026-09-25.
  The earlier syncs skipped any line that differed from Oracle in more than the self-reference, and
  never handled the shortened legendary name, so Dragon Egg still read "When Dragon Egg dies" and
  Jugan's deck-editor text "When Jugan, the Rising Star dies". New
  `_tools/oracle-audit/selfref-sweep.js` aligns each line with Oracle word by word and rewrites **only**
  the card's own name where it sits opposite "this creature" / "this land" / "it" / the short name,
  leaving any other difference on the line alone:
  - 1,571 `Oracle:` lines and 821 in-game text fields (trigger, spell, static and stack descriptions)
    in 1,960 cards. 1,065 are short names: `CARDNAME` → `NICKNAME` in game ("Klauth", "Jugan",
    "Radiant"), except in `StackDescription$`, whose getter doesn't substitute `NICKNAME`.
  - A bare "it" is only used when the field names the card earlier, so a delayed trigger's own text
    ("Destroy Cinder Wall at end of combat.") isn't left with an "it" that points at nothing.
  - Token self-reference inside quoted granted text (`token-quote-sync.js`): 53 cards whose token
    text now reads "This token can't block." etc., applied only where that swap makes the `Oracle:`
    line identical to Scryfall.
  - Every changed script line was checked parameter by parameter: only display-text keys or the
    `Oracle:` line differ.
- **One line instead of two**: Keldon Marauders ("enters or leaves the battlefield") and Tergrid
  ("sacrifices a nontoken permanent or discards a permanent card") showed each trigger as its own
  line; the second is now `Secondary$ True` with the joined Oracle sentence, as the 14 other
  "enters or leaves" cards already did.
- **AI: die-roll attack pumps count.** Combat prediction skipped any attack trigger whose ability
  wasn't a Pump at the top, so Strength-Testing Hammer's roll (and Velukan Dragon's) was worth 0 and a
  Hammer-equipped 1/1 wouldn't attack into a 1/2. It now counts the lowest possible roll — certain,
  and enough to see the 1/2 always dies.
- **AI: blockers that remove what they fight.** Wall of Nets (exiles what it blocked) and Kjeldoran
  Frostbeast (destroys everything in combat with it) do it with an end-of-combat trigger no damage
  math saw, and the 0-power wall also read as a "free" attack. `canDestroyAttacker`/`canDestroyBlocker`
  now know, provided the source survives combat damage to trigger; the free-attack shortcut excludes
  attackers a single blocker removes.
  - Not changed: at low opposing life the attrition estimate counts a blocked attacker's damage in the
    first round, so with a 5/5 and a 2/2 against Wall of Nets at 7 life the AI still goes all-in (it
    does the same against any wall). Upstream heuristic; left alone.
- **AI: no second Witches' Eye on the same creature.** Added `NonStackingAttachEffect` to Witches'
  Eye and the other three Equipment that only grant a {T} ability (Siren Song Lyre, Sorcerer's Wand,
  Lobe Lobber): a second copy's ability can never be used alongside the first.
- New `CombatRemovalAndEquipAiTest` (7 tests; 6 fail on the old code, Velukan Dragon guards the formula). Suite: 734 run, 0 failed, 6 skipped.

### 2026-09-25 — Unhinged colorless; more Un-cards; pink and gold; errata reversal; Oracle retemplate

- **All remaining Unhinged colorless cards** (unh/121–135): Gleemax, Letter Bomb, Mox Lotus,
  My First Tome, Pointy Finger of Doom, Rod of Spanking, Time Machine, Togglodyte, Toy Boat,
  Urza's Hot Tub, Water Gun Balloon Game, World-Bottling Kit, City of Ass, R&D's Secret Lair.
- **Pink** as a sixth color, for the Giant Teddy Bear token.
- **Mox Lotus** produces genuinely infinite colorless mana, shown as ∞.
- **Time Machine** returns cards in a *later game*, queued on the `Match` by card name (a card
  object doesn't survive into the next game) and rebuilt as a Command-zone trigger keyed on the
  turn number.
- Card flags that survive the copy every zone change makes: `signed` (Letter Bomb),
  `switchedOn` (Togglodyte), `chosenExpansion` (World-Bottling Kit).
- **Errata reversal under R&D's Secret Lair**, printing-specific:
  - *Every pre-Dominaria printing* — "target creature or player" can't hit planeswalkers or battles.
  - *Debt of Loyalty* — control is gained unconditionally, not only on a successful regeneration.
  - *Bloodvial Purveyor* — the attack buff is permanent, with no "until end of turn".
  - *Marath, Will of the Wild* — a second ability with no `XMin1`, so X may be 0.
  - *Goblin King* (printed before Ninth Edition) — **all** Goblins get +1/+1 and mountainwalk,
    including itself; "Other" was added in 9ED.
  - *Relic Bind* (Legends printing only) — enchants **any** artifact, including your own; the
    opponent-only restriction was power-level errata applied in Fourth Edition.
  - *Lotus Vale* — a triggered ability rather than a replacement effect, so the land is already in
    play when it resolves and can be tapped for mana in response, then buried.
  - *Ashnod's Coupon* — the target player pays for the drink, not you.
  - *Celestial Dawn* — the printed card changes the **costs themselves** ("All colored mana symbols
    in all costs on all of these cards and permanents are {W}") rather than what mana may be spent
    on them. So the pips really are white — devotion, colored-pip counting and color identity all
    see {W} — and the Oracle version's "other mana only as colorless" restriction doesn't exist.
    Needed a new `SetColoredSymbolsTo$` continuous static (a *transform* of whatever the card
    happens to cost, unlike `ManaCost$`, which sets a fixed one).

- **Misprints under the Lair**, via a new `PrintingIs <SET>[:<collectorNumber>]` property. A misprint
  is printing-specific in a way errata isn't: only the physical copy carrying the mistake plays as
  written, so identity is the whole problem. All three are expressed as *boosts and cost reductions*
  gated on the printing, never as `SetPower`/`SetToughness`/`ManaCost$`, so they layer cleanly and
  the card is untouched without the Lair.
  - *Gríma Wormtongue* — the Secret Lair bonus card (SLD #734) was printed 2/4. Every SLD copy is
    misprinted, and Forge already had that printing.
  - *Orcish Oriflamme* — Alpha (LEA #166) printed the cost as {1}{R}; Beta corrected it to {3}{R}.
    Every Alpha copy is misprinted.
  - *Corpse Knight* — only **some** M20 #206 copies are 2/3, and they share a set and collector
    number with the correct ones, so this one needed a printing of its own: a new
    `206a … ${"variant": "Misprint"}` edition entry (following the existing `F203a`/`F203b`
    precedent) plus a `Variant:Misprint:` block to mark it. The toughness stays Lair-gated — the
    variant marks *which copy you own*, not what it does.

- Known cosmetic gap: the split-card *image* renderer still draws two halves for the five-face card.

- **More Un-cards**: Rules Lawyer, Grimlock (Dinobot Leader), Nerf War, By Gnome Means,
  Chivalrous Chevalier, Do-It-Yourself Seraph, Gimme Five, GO TO JAIL, and the Unstable augments
  Half-Kitten, Half- and Humming-.
  - *Rules Lawyer* — new `IgnoreStateBasedActions` static mode; every 704 check skips protected
    players and permanents, and the AI's combat/burn/lose predictions know they won't die to SBAs.
  - *Augment* — from-hand, sorcery-speed ability that merges the card onto a host through mutate's
    merge plumbing. The combined card's name, cost, colors, types, P/T and completed ETB triggers
    follow the Unstable mechanics article and FAQ. A hostless augment on the battlefield goes to the
    graveyard (Rules Lawyer exempts it).
  - *Nerf War* — new `ShootAtLibrary` effect simulating the blaster volley (6 darts, 40% hit,
    1–3 cards per hit, tunable per script).
  - *Do-It-Yourself Seraph* — new `GainsTextBoxOf` continuous param that *adds* each matching
    card's traits (unlike `GainTextOf`, which replaces); contradicting gained statics apply in exile
    order, pinned by `DoItYourselfSeraphTest`.
  - *By Gnome Means* — `PutCounter` `CounterTypeChoices$ AnyPrinted` offers every counter kind Forge
    knows.
  - AI: `GameChance.N.Key` rolls once per AI player per game for honor-system facts; `HighFives`
    number logic for Gimme Five.
- **Gold as a seventh color**, for the 4/4 gold Dragon token (Sword of Dungeons & Dragons, Urza
  Academy Headmaster), on the same terms as pink. In a game with any silver-bordered card, "choose a
  color" prompts and protection from a color of your choice also offer pink and gold.
- **Border is now per printing**: edition lines take `${"border": "Silver"}` (13 silver-bordered
  cards in black-bordered sets) and `${"stamp": "acorn"}` (all 296 acorn printings), and
  `SilverBordered` checks the printing instead of any `Type=Funny` edition.
- **13 missing creature types** added to `TypeLists` (Autobot, Cyborg, Automaton, Brainiac, Chicken,
  Custodes, Head, Naga, Reveler, Rukh, Teddy, Urzan, Walrus); type checks against them silently
  failed before.

- **Test suite back to green** (`mvn -pl forge-gui-desktop -am test`: 721 run, 0 failed). Of the 18
  failures, 8 were the fork's and 10 were environmental, reproducing identically on upstream:
  - *Network play was broken* — the five-face split states pushed `CardStateName` past the 16 that
    `DeltaPacket`'s key encoding allowed, so its static guard threw on class load. That was
    `DeltaSyncUnitTest` ×3 and `NetworkPlayIntegrationTest`'s 60s timeout. Fixed by widening the slot.
  - *Stale after the Oracle sync* — `GameSimulationTest` (Thespian's Stage ×2, Lightning
    Berserker) and `SpellAbilityPickerSimulationTest.testLandSearchForCombo` expected the old
    `CARDNAME`/card-name wording; now "This land …" / "This creature …", matching Scryfall.
  - *Profile-dependent* — the test card database loaded `%APPDATA%\Forge\custom\editions`, so
    custom sets there (a Pro Tour Collector Set with eight Hymn to Tourach prints, CEI, WC01)
    changed which printing the CardDb and DeckRecognizer art-preference tests picked. The test
    `CardDatabaseHelper` now uses no custom cards and an empty custom-editions folder.
- **Five-face split cards over the network.** The client only decoded upstream's four card-state
  slots, so Who // What // When // Where // Why's third to fifth faces arrived as raw state numbers:
  every copy logged `Error setting property Split3State` (and 4, 5), and reading its faces on the
  remote client threw `ClassCastException`. Now decoded like the others; new
  `FiveFaceSplitNetworkTest` plays a real networked game with the card and checks all five faces.
- **`Oracle:` fields retemplated to match Scryfall** — 10,927 lines in 10,826 card scripts where the
  *only* difference from current Oracle (Scryfall bulk data of 2026-09-24) was WotC's self-reference
  change: the card's own name → "this creature" / "this land" / "it" (Thespian's Stage, Lightning
  Berserker…). The earlier Oracle sync only covered the in-game ability text, so the deck editor and
  card search still showed the old wording. Only the `Oracle:` line changed; the ability, cost and
  keyword lines didn't.
  - *Verified to keep every card's function*: each script was parsed with the real `CardRules`
    reader before and after, and every public getter on the rules and on each face (abilities,
    keywords, colors, color identity, deckbuilding colors, commander eligibility, AI deck hints)
    compared — 0 differences across all 10,927; a control run that includes the Oracle getters
    flags all 10,927, so the comparison does see the edits. Counter-type detection
    (`CountersMoveEffect`), the AI and deck-generator text regexes and the mana-spent checks are
    also unchanged. Test suite: 722 run, 0 failed.
  - *Intentional side effects on features that measure the text itself*: Punctuate counts on 988
    cards and Lexivore line counts on 1,403 now follow current Oracle; Pygmy Giant loses numbers
    that only came from a card's own name on 18 (e.g. Wall of One Thousand Cuts). Adventure-mode
    reward filters (`cardText` regexes) gain or lose some matches on 2,959 cards — mostly
    accidental own-name substring hits dropping out ("Rat" in "Wrath", "Cat" in "Catapult") and
    "this Enchantment"/"this Equipment" newly matching.
  - The remaining 3,341 differing `Oracle:` fields have other wording drift and were left alone.
  - Merges: an upstream edit to one of these lines will now conflict. Take upstream's side of the
    line, then re-run `_tools/oracle-audit/sync-oracle-field.js <oracle-cards.jsonl.gz> --apply`.
- **Text-box readers now see paragraphs.** Stored Oracle text separates paragraphs with the script's
  literal two-character `\n`, not a real newline, but the text-box readers assumed real newlines:
  - *Lexivore / Frazzled Editor* (`CardFactoryUtil.getTextBoxLineCount`) split on `\r?\n`, so every
    card was measured as one long paragraph with the `\n`s counted as text, and short paragraphs lost
    their one-line minimum ("Flying\nVigilance\nTrample\nHaste" was 1 line, not 4; Shivan Dragon 4,
    not 5). "Wordy" (4+ lines) and "most lines of text" were undercounted on nearly every
    multi-paragraph card. Now splits on the literal separator and real newlines both.
  - *Pygmy Giant and Tainted Monkey* (`Card.getTextBoxContents`) — the separator glued an `n` onto
    each paragraph's first word, so a number word opening a paragraph was missed ("Two target
    creatures…" on Ruthless Disposal read as "ntwo") and a chosen word opening one never matched as a
    whole word. `getTextBoxContents` now turns the separator into a real newline. Punctuate was
    unaffected (backslash isn't one of its marks).
  - New `TextBoxTest` (5 tests). Test suite: 727 run, 0 failed, 6 skipped.

### 2026-08-06 — Unhinged green and multicolor; five-face splits

- **Cards:** B-I-N-G-O, Creature Guy, Elvish House Party, Fat Ass, Fraction Jackson, Gluetius
  Maximus, Graphic Violence, Keeper of the Sacred Word, Land Aid '04, Laughing Hyena, Monkey Monkey
  Monkey, Name Dropping, the 141-character Elemental, Remodel, Side to Side, S.N.O.T., Stone-Cold
  Basilisk, Supersize, Symbol Status, Meddling Kids, Rare-B-Gone,
  Who // What // When // Where // Why.
- Yet Another Aether Vortex's third clause (dual residency), including the battlefield→top-of-library
  round trip staying put with no triggers.
- Five-face split card support (see above).
- WOE Role tokens printed inverted (`role_sorcerer`, `role_young_hero`, `role_cursed`) render rotated,
  with a rotate control, on desktop and mobile.
- Existing Gotcha cards reworded so the text box carries the full Oracle condition.
- Expansion-Symbol registered as a creature type.
- **AI:** takes a lethal solo attack rather than benching a creature whose evasion needs it to attack
  alone; picks a letter for letter-choosing cards instead of choosing nothing.
- **Deck generation:** random decks reinforce orphaned typal payoffs with some enablers (a Zombie
  lord no longer arrives with no Zombies). Random-color piles are preserved by design.

### 2026-08-05 — Unhinged red; half mana production

- **Cards:** Assquatch, Curse of the Fire Penguin, Deal Damage, Dumb Ass, Face to Face, Frazzled
  Editor, Goblin S.W.A.T. Team, Mana Flair, Mons's Goblin Waiters, Orcish Paratroopers, Punctuate,
  Pygmy Giant, Red-Hot Hottie, Rocket-Powered Turbo Slug, Sauté, Six-y Beast, Touch and Go, Yet
  Another Aether Vortex (first two clauses), Zzzyxas's Abyss, Zombie Fanboy, Working Stiff, When
  Fluffy Bunnies Attack, Wet Willie of the Damned, Vile Bile, Tainted Monkey, Stop That!, Phyrexian
  Librarian, Persecute Artist, Necro-Impotence.
- Half mana **production**; half life fixes (cost prompt, affordability, copy, set, repaint).
- Flavor text data available in-game.
- Oracle text sync rounds 4–5 (self-reference pronouns, retemplating).
- Chosen artist shown in the detail panel.

### 2026-08-02

- AI stops attacking into walls that just absorb it.

### 2026-08-01

- **Cards:** Mother of Goons, Kill! Destroy!, Infernal Spawn of Infernal Spawn of Evil, Farewell to
  Arms, Eye to Eye, Mouth to Mouth, Loose Lips.
- `Chance.N` reworked: rolled once per turn, one attempt per turn, honored by every API.
- AI battle handling: don't let a battle swallow the whole attack step; send leftover attackers;
  only commit the whole attack to a battle it can finish.
- Four AI fixes: prowess timing, vehicle reanimation, named card legality, tap-ability hoarding.

### 2026-07-31

- Dev mode can pick which printing to add.
- Framed! chooses its mode at resolution.
- Flavor names count in name-based mechanics.

### 2026-07-28 to 07-30

- **Cards:** The Fallen Apart, Eye to Eye, Duh, Bloodletter of Nesting Vampires, Bad Ass, Aesthetic
  Consultation.
- Half damage shown on creatures; half kept when power becomes damage; `WordsInName` readable from a
  static ability.
- Head to Head prevention as a named effect; Man of Measure rules text; granted land types read from
  the card.

### 2026-07-26 to 07-27 — the half-value layer and Unhinged white/blue

- **Cards:** _____, Ambiguity, Artful Looter, Avatar of Me, Brushstroke Paintermage, Bursting
  Beebles, Carnivorous Death-Parrot, Cheatyface, Collector Protector, Double Header, Drawn Together,
  Emcee, Erase (Not the Urza's Legacy One), Fascist Art Director, First Come First Served, Flaccify,
  Framed!, Frankie Peanuts, Greater Morphling, Head to Head, Ladies' Knight, Little Girl, Look at Me
  I'm R&D, Loose Lips, Magical Hacker, Man of Measure, Moniker Mage, Mouth to Mouth, Now I Know My
  ABC's, Number Crunch, Question Elemental?, Richard Garfield Ph.D., Save Life, Smart Ass, Spell
  Counter, Standing Army, Staying Power, Wordmail.
- The half-value layer built out: half P/T on the card state, real half mana shards, half mana
  symbols in both UIs, half-aware prevention.
- Child registered as a creature type; half set-P/T params registered with the P/T layer.

### 2026-07-25

- Unhinged/Arena promo cards; half life support; half power and toughness.

### 2026-07-23

- **The full Unglued (UGL) silver-bordered set.**
- In-game ability text synced to current Scryfall Oracle wording.

### 2026-07-21 — first changes

- Runeblade Raiser (Alchemy: Tarkir), Aquatic Subtlety (Alchemy: Lorwyn Eclipsed).
- Several new/unimplemented cards plus supporting engine features.
- Android debug build made self-contained and update-safe — bundles this fork's own
  `assets.zip` so the app never silently replaces it with upstream's card database, and uses a
  timestamp-based versionCode so installs are genuine in-place updates.

---

## Build and deployment

Recorded here because the build has fork-specific patches that are easy to lose.

- **Desktop:** whole-reactor `mvn install`; launch with working directory `forge-gui/` (the jar
  doesn't bundle `res/`). Use the Desktop `.bat` launcher.
- **Android:** whole-reactor `-P android-debug` (never `-pl forge-gui-android` alone), `subst` drive
  aliases to dodge the 8191-char command line, JDK 17, sign manually with `apksigner` (the plugin's
  built-in debug signer produces a corrupt signature on JDK 9+).
- **Upstream merges:** commit local work first, `git fetch upstream`, `git merge upstream/master`.
  Conflicts, when they happen, are in the core engine files this fork patches — `Card.java`,
  `CardView.java`, `CardFactory.java`, `CardRules.java`, `CardState.java`, `CardProperty.java` — not
  in card scripts, which essentially never conflict.
