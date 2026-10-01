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
- Persistent honor states modeled as custom counters toggled by two zero-cost abilities
  (Standing Army's `STANDING`, Fat Ass's `EATING`).

### Choices the rules can't derive

- `DB$ ChooseArtist` + `Card.ArtistIsChosen` + `sharesArtistWith <restriction>` + `DB$ SetArtist`.
- `ChooseType | Type$ word | FreeInput$ True` (free text) with `MinLetters$` validation;
  `Type$ letter` with an AI branch (`MostCommonInitial`) in `chooseSomeType`.
- `DB$ ChooseEyeColor`, `DB$ GuessArtist`.
- `Card.chosenTypeKind`, so the detail panel says "chosen word"/"chosen letter", not always
  "chosen type".

### Card-text and name mechanics

- `Count$ CardPunctuationMarks`, `CardCapitalLetters`, `CardTextBoxNumbers`, `WordsInName` (readable from a static),
  `ChosenLetterInName`, `CardBingoLines`, `CurrentHour`, `MergedCount`,
  `DifferentExpansionSymbols`.
- Properties: `textHasChosenWord`, `nameStartsWithChosenLetter`, `fewerLettersInNameThanSource`,
  `HasHalfSymbol`, `Rarity<name>`, `alphabeticallyFirstNonLand`, `SilverBordered`.
- Flavor text available in-game (`CardFlavorText`, `Card.getTextBoxContents`) — several Un-set cards
  read the whole text box, flavor included.
- Flavor names count in name-based mechanics.
- **A secret word the players guess at** (Hangman): ChooseType `Guessable$ True` (with `MinLetters$`,
  `MaxLetters$`, `WordList$ True` for the AI's vocabulary from `res/lists/Words.txt`), `DB$ GuessWord` with
  `GuessWrong$`/`GuessCorrect$`, trigger `Mode$ WordGuessed`. The word never reaches the view - only the
  hangman sheet does.

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
- **Last strike** (`K:Last Strike`, Unstable) — a further combat damage step after the regular one, run as a
  repeat of `COMBAT_DAMAGE` rather than a new `PhaseType`.

---

## Log

### Unreleased

- **Doubled words in in-game cost text** (reported on Spire Mechcycle: "Tap another untapped another Mount or
  Vehicle"). A script's cost description that already said what the engine adds printed twice: "untapped" and
  "another" (`CostTapType`), "a"/"an" (`CostSacrifice`), "card" (`CostDiscard`). The engine now leaves out
  what the description already says. Also a double space after "Exhaust —", "Boast —", "Power-up —" and other
  ability words (from my 2026-09-30 keyword-dash change), and two scripts (Threefold Thunderhulk "another
  artifact", Saruman of Many Colors's ward cost with Oracle's serial comma). A full re-render fixed 15 faces
  and broke none; no doubled words are left. Component Pouch's d20 rows were already restored by the VERT fix
  in the 2026-09-30 deploy.
- **AI fixes from play:**
  - Vibranium Strike Gauntlets (and any "attach it to target creature" Equipment or Aura): an attach trigger
    with no creature to target no longer reads as worth it, so the AI doesn't cast it with no creature out.
    Flash Equipment that attaches as it enters now gets the flash-Aura timing (`AttachAi.doAdvancedFlashAuraLogic`):
    a combat trick on a creature that survives, or a main-phase play, never a buff for a creature that's about
    to die. The attach target skips our creatures that are dying in the current combat when there's another
    choice. That survival check also counts damage already marked and blockers now (it compared power with
    toughness using `>`, so exactly lethal damage read as survivable).
  - Terror of Mount Velus: an enters trigger that pumps our creatures until end of turn (`PumpAll` of
    `...YouCtrl` with keywords or +power) is now a reason to cast precombat when we have a creature that can
    attack (`ComputerUtil.castPermanentInMain1`).
  - Signal Pest: once attackers are chosen, a creature that deals no damage and whose only attack value is
    boosting other attackers (battle cry, "other attacking creatures get") goes home if nothing else attacks
    (`AiAttackController.pruneSupportOnlyAttackers`).
  - Hand-size creatures (Maro, Masumaro, Sturmgeist, Psychosis Crawler, Body of Knowledge, Kagemaro...): with one
    out, a card from hand is played only if it's worth the shrink (`HandSizeAi`, new decision
    `KeepInHandForSize`): removal and other interaction, cards that draw two or more, creatures worth more
    than the lost points, planeswalkers, a land when short of mana for something in hand, a combat trick
    during combat. Anything goes when the hand is over its maximum size or the AI's life is in danger.
  - `AiPlayReportsTest` (6). Suite: 839 run, 0 failed, 6 skipped.

- **Hazmat Suit (Used)** (ust/57), honor system: the touch is an ability any player may activate to own up
  (`FlavorAction`, as on Vile Bile) and lose 2 life. The prompt notes that the sleeve counts (ruling); touching
  the Aura itself isn't offered (ruling). The ability reads exactly as Oracle does, its wording carried by
  `CostDesc$`/`SpellDescription$`.
  - New AI param `AIActivator$ <player>`: an ability any player may activate that the AI only considers when
    it matches. Here `Player.controlsCreature.EnchantedBy`, so only the AI that handles the enchanted creature
    ever "touches" it (10% of its turns, in its own main phase, per `Chance.10`).
  - Vile Bile now reads like its Oracle sentence in game too (its AI is unchanged). `HazmatSuitTest` (2).
    Suite: 833 run, 0 failed, 6 skipped.

- **Hangman** (ust/56). The game keeps the hangman sheet, so nothing here is honor system except that the
  noted word is a real word.
  - Noting: a person types any word of six to eight letters (A-Z only; re-asked if it isn't, and offered the
    given one from the list, told only to them, if they give none); the AI takes a random everyday word from
    `res/lists/Words.txt`. Everyone is told the length (ruling). The word is held in `chosenType` but kept out of `CardView`:
    the detail panel shows `(chosen word: _ A _ _ E _ - wrong: Q, X)`.
  - Guessing: `{1}`, any player may activate, target is `Player.!controlsCard.Self`. The guesser picks an
    unguessed letter from a list or "Guess the word" and types it. A right letter fills in every instance; a
    letter is never offered twice (rulings). A wrong guess resolves `GuessWrong$` (+1/+1 counter). The guess
    that finishes the word runs `WordGuessed`, a real trigger of Hangman's that sacrifices it.
  - AI (`GuessWordAi`) plays from the sheet, never from the word: it matches the pattern, wrong letters and
    wrong words against the list and guesses the letter most matches share (an everyday word counting four
    times a rare one), or the word when one match is left; with no match it goes by English letter frequency. It reads the word itself only if it noted it
    (ruling: the noter keeps answering for the word, and knowing it, after a control change). With its own
    Hangman it makes an opponent guess once a turn (second main phase or the opponent's end step) while three
    or more letters are hidden; against another player's it pays only when its guess will finish the word.
  - New: `forge.util.WordList`, `Card.setGuessableWord` and friends (copied by `CardCopyService` and
    `GameCopier`), `GuessWordEffect`, `TriggerWordGuessed`, `ComputerUtil.chooseSomeType` "word" +
    `AILogic$ Random`. `HangmanTest` (10; against words from the list the AI averaged 2.1 wrong guesses on everyday words and 3.4 on rare ones). Suite: 831 run, 0 failed, 6 skipped.
  - **Word list:** `res/lists/Words.txt` is 43,478 six-to-eight-letter words in American spelling, built by
    `_tools/wordlist/build-words.js` from SCOWL 2020.12.07 (wordlist.aspell.net): its `english-words` and
    `american-words` files only, never the British, Canadian, Australian or variant ones. `[common]` (18,423,
    SCOWL sizes 10-35) is what the AI notes from; `[more]` (25,055, sizes 40-70) is only there so it can
    recognize a rarer word a person notes. 115 crude or grim words were moved from common to more so the AI
    never puts one on screen. SCOWL's license needs its notice kept with copies: it ships verbatim as
    `res/lists/Words-SCOWL-Copyright.txt`. (The first version of this list was 563 words written by hand.)
- **American spelling** in the fork's own comments and this changelog (58 lines: colour, labelled, recognised,
  afterwards and the like). `_tools/american-spelling.js` rewrites only lines the fork added, so upstream's
  lines and merges are untouched.
- **Finders, Keepers** (ust/55) was already in from upstream; script checked against Scryfall, no change.

- **Extremely Slow Zombie** (ust/54a–d, four printings that differ only in flavor text; the edition file already
  listed them) and the **last strike** keyword it needs. `Combat` holds a creature with last strike out of the
  regular damage; when that step ends, `PhaseHandler` repeats `COMBAT_DAMAGE` once as the last-strike step if
  a creature still in combat has last strike (or was held back for it). Players get priority in between, as
  with first strike, and the log names the step "Last strike:". Per the Unstable FAQ: it deals damage only if
  it survives first-strike and regular damage; first strike + last strike deals damage twice (first-strike and
  last-strike steps, not the regular one); double strike + last strike three times.
  - Judgment calls the FAQ doesn't cover, by analogy with CR 510.4: no last-strike step when no creature in
    combat has last strike; a creature that lost last strike after sitting out the regular damage still deals
    its damage in the last-strike step.
  - AI: `ComputerUtilCombat.dealsDamageBefore` replaces the "first strike vs. none" checks in
    `canDestroyAttacker`/`canDestroyBlocker`, the wither predictions and the gang-block filter, so it knows a
    creature that kills the Zombie takes no damage from it. `CreatureEvaluator` values last strike as a
    drawback (-5 per power). `LastStrikeTest` (7). Suite: 821 run, 0 failed, 6 skipped.
- **Dirty Rat** (ust/53) was already in from upstream; script checked against Scryfall, no change.

### 2026-09-30 (deployed: desktop, Android, GitHub) — upstream merge (2.0.16-SNAPSHOT); capital offense; Oracle: line sync; in-game ability text sync

- **Upstream merge 2026-09-30** (30 commits, through the 2.0.15 release; the fork is now 2.0.16-SNAPSHOT): one
  conflict, the `Oracle:` line of The Disciple of Vess (upstream removed a stray second line) - took upstream's
  side and re-ran `oracle-drift.js --apply`. No fork params were stripped from card scripts. Suite: 814 run,
  0 failed, 6 skipped.

- **capital offense** (ust/52, printed all lowercase, and the name is too): `Count$ CardCapitalLetters`
  (`CardFactoryUtil.getCapitalLetterCount`) counts capitals in the target's `Oracle:` text, leaving out
  reminder text and mana/tap symbols (printed as icons, not letters). Ability words and mid-sentence capitals
  count (Unstable rulings). AI: `PumpAi` now works out a target-dependent -X/-X (`SVar:X:Targeted...`) for
  each candidate and targets the best creature it kills. Before, X read as 0 or a guess before any target
  was chosen. Also fixes Flunk's AI. `CapitalOffenseTest` (3). Suite: 801 run, 0 failed, 6 skipped.
  - The count skips Forge's own `STATION 8+` lines, which Oracle writes as `8+ |` (they'd add 7 capitals
    per threshold). Now that `Oracle:` lines are synced (below), every card with an unambiguous Scryfall
    text counts the same as Scryfall except Phila, Unsealed (kept on purpose).
- **Big Boa Constrictor** (ust/51): already scripted upstream, checked against Scryfall, no change.
- **`Oracle:` fields synced to current Scryfall Oracle** (bulk data of 2026-09-29): 2,035 lines in 1,999
  scripts that still differed after the self-reference retemplate. 628 reminder text only, 55 keyword
  line splits, 42 punctuation or line breaks, and 1,310 wording (242 of them WotC's "of their choice",
  plus "enters" and "greatest" template changes, boons becoming "one-time boons" and so on).
  - *Forge's house formats are kept*: loyalty abilities stay `[+1]:` / `[-2]:` (Scryfall `+1:` / `−2:`),
    and a Station threshold stays its own `STATION 8+` line (Scryfall `8+ | `; d20 tables keep `15+ |`).
    `_tools/oracle-audit/forge-format.js` does the conversion.
  - *Left alone*: 11 names with more than one Scryfall text (B.F.M.'s halves, Everythingamajig and the other
    Un variants, Cunning's Planechase theme card), Phila, Unsealed (the script implements one outcome of the
    Unknown event; Scryfall prints both), and Khod, Etlan Shiis Envoy (playtest card printed before WotC
    folded Cephalid into Octopus; the script rightly pumps Octopuses).
  - *Verified*: every affected script was parsed with the real `CardRules` reader before and after, and
    every public getter on the rules and each face compared, plus what each game-code reader of the text
    derives (`CountersMoveEffect`'s counter kind, `SpellAbility`'s mana-spent checks, `hasReminderText`,
    the "(Transforms" header, the renderer's "Level up" test, `DeckGeneratorBase`'s creature-type and
    dual/fetch-land regexes). No card's rules changed except two color-identity fixes: **Fetching Garden**
    (G/W) and **The Belligerent and Useless Island** (U) were colorless because their stored text lacked
    the `({T}: Add ...)` line that Tundra-style lands have; Scryfall agrees with the new identities. A control
    run including the Oracle getters flags every edit. Harness: `_tools/oracle-audit/OracleSyncHarness.java`.
  - *Meant to follow the new text*: Punctuate (646 faces), capital offense (357), Lexivore line counts (111),
    Duh's `hasReminderText` (387), the "refers to a kind of counter" check on 18 cards, deck-generator
    type hints on 8, and four back faces now get the "(Transforms from ...)" header in play.
  - *Script bugs the drift exposed*, each confirmed against Scryfall: **Dragonborn Immolator** pumped +2/+0
    (Oracle +1/+0). **Obscura Polymorphist** could exile nothing (Oracle: "exile target creature").
    **Charred Graverobber** escaped for {3}{B} (Oracle {3}{B}{B}). **Disciple of Perdition** could exile
    your own graveyard and never made that player lose 1 life (the life loss wasn't chained). **Psychic
    Whorl** could target you (Oracle: opponent). **Kodama of the West Tree** needed damage to an opponent
    (Oracle: a player). **Mizzix, Replica Rider**'s copies were sacrificed at the next end step (Oracle:
    your end step). **Verdant Dread** triggered on any player's Verdant Dread (Oracle: a permanent you
    control). **The Forgotten Place** didn't enter tapped. **Gryffwing Cavalry** couldn't target itself
    (Oracle drops "another"). **Supernatural Rescue** could enchant any creature (Oracle: creature you
    control). Ability names corrected: Primaris Eliminator's Hyperfrag Round, Lychguard's Guardian
    Protocols, Necron Overlord's Relentless March, Shard of the Void Dragon's Spear of the Void Dragon.
    `OracleDriftFixesTest` (7); `TextBoxTest` now pins Shivan Dragon's current text. Suite: 808 run,
    0 failed, 6 skipped.
  - Merges: resolve an `Oracle:` conflict by taking upstream's side, then
    `node _tools/oracle-audit/oracle-drift.js <oracle-cards.jsonl.gz> --apply` (it replaces
    `sync-oracle-field.js`, which only knew self-references).
- **In-game ability text brought to current Oracle** (2026-09-30). Measured directly this time: every
  face of every card is rendered through the engine (`Card.getAbilityText`, 34,870 faces) and compared with
  its `Oracle:` text sentence by sentence. Line breaks, ability order and which keywords share a line are
  print layout and don't count. **29,592 faces matched at the start; 33,931 (97.3%) match now.**
  - *Engine* (one change, many cards): a card referring to itself in a cost or an "enters with" line says
    "this creature" / "this artifact" / "this Equipment" / "this card" as Oracle does
    (`CardFactoryUtil.getSelfReferenceNoun`, which matches 2,547 of the 2,555 such references in Scryfall's
    Oracle). Where a script can't tell two wordings apart - "Remove X +1/+1 counters" vs "Remove any number
    of storage counters", or a digital-only card that still prints its name - the card's own `Oracle:`
    text picks (`SpellAbility.withSelfReferences`). `Lang.getPlural` uses each type's own plural and leaves
    already-plural phrases alone (no more "creatureses", "Elfs", "Merfolks"). Alchemy cards say "Orcish
    Bowmasters", not "A-Orcish Bowmasters", in their own text. Two protections or hexproofs join
    ("protection from white and from blue"). Periods on "Adapt 1.", "Monstrosity 4.", "Buyback—Sacrifice a
    land."; "Mill a card"; "0:" loyalty; "Living weapon", "Power-up", "Web-slinging", "Start your engines!";
    Aftermath printed once; NICKNAME filled in on keyword lines.
  - *Scripts*: about 2,100 display fields reworded (`SpellDescription$` and the like, and the description
    part of `K:` lines). Template-only edits were applied automatically; everything else was read first, and
    checked against the script where a rules-bearing word changed. Every rewritten line was verified to
    change no parameter but display ones.
  - **d20 tables were broken by this fork and are restored.** An earlier round (`fix-vert.js`, 2026-08) took
    the scripts' `VERT` placeholder for corruption and replaced it with a literal `|`, which is the script's
    param separator, so 43 cards showed their result ranges with no result text. `VERT` is back (114 rows;
    counts match upstream file by file). `,,,` is a placeholder too (a line break in the card panel).
  - *Script bugs the review exposed*, each confirmed on Scryfall: Corpse Traders, Pilfering Imp, Dread Rider
    and Demonic Pact's discard could target any player (Oracle: opponent). Blood Speaker, Roots of Life and
    Timeless Witness were optional (Oracle has no "you may"). Prayer of Binding and Struggle for Skemfar's
    fight had to target (Oracle: up to one). Resounding Wave's cycle trigger and Twigwalker allowed fewer
    than two targets. Mercenary Informer could target tokens. Vivien Reid's +1 didn't randomize the bottom.
    Blast from the Past couldn't hit planeswalkers.
  - *Mistakes made and repaired along the way*: the text fixer had three flaws - a one-character mis-cut (12
    lines lost a comma or dash), re-inserting text when run on a stale render (20 doubled words), and
    matching a generic tail on the wrong ability (7 cards). All were found by an audit that checks every
    new sentence against the card's Oracle text (`audit-edits.js`), repaired, and guarded against.
  - *Not done* (939 faces): text Forge never renders (Conspiracy draft text, ante and deck-building lines),
    sub-abilities with no description, sentences Oracle and the script split differently, and one-off
    keyword wordings. `render-review.json` lists the open sentences.
  - Tooling in `_tools/oracle-audit/`: `RenderAbilityTextDump.java` + `rerender.sh` (render and diff),
    `render-diff.js`, `render-attribute.js`, `render-fix.js`, `render-triage.js`, `regress.js`,
    `audit-edits.js`, `restore-vert.js`. Always re-render before applying a batch.
  - Tests: `GameSimulationTest` and `CountersPutAiTest` find abilities by their new text. Suite: 808 run,
    0 failed, 6 skipped.

### 2026-09-27 (deployed: desktop, Android, GitHub) — self-reference sweep; AI combat and equip fixes; Unstable white 12–25; Animate Library; Blurry Beeble; Clocknapper; Crafty Octopus; Defective Detective; Five-Finger Discount; Graveyard Busybody; Half-Shark, Half-; Kindly Cognician; Magic Word; More or Less; S.N.E.A.K. Dispatcher; Socketed Sprocketer; Spy Eye; Very Cryptic Command; Wall of Fortune

- **Upstream merge 2026-09-27** (12 commits): upstream's card-script linter (#12048) strips params it doesn't
  know, including the fork's `Host$ True` on every host's enter trigger, which augment relied on. A host
  trigger is now also recognized by shape (`CardFactory.isHostTrigger`: a Host card's trigger for itself
  entering), so future linter passes can't break augment. Gnome-Made Engine keeps `TokenOwner$ You` over
  upstream's new `TriggeredCardController`, which has no triggered card once augmented.

- **Wall of Fortune** (ust/50): static `Mode$ RerollWithWall`. After any die roll - including the planar die -
  anyone with the permission may tap an untapped Wall they control to have the roller reroll it, seen first
  and repeatable with more Walls (`RollDiceEffect.tapWallToReroll`, also called from `PlanarDice.roll`). Not an
  activated ability, so a Wall that just arrived can be tapped. AI rerolls its own below-average rolls and
  opponents' above-average ones (not the planar die). `WallOfFortuneTest` (2). Suite: 798 run, 0 failed, 6 skipped.

- **Very Cryptic Command** (ust/49a-f): none of the six printings existed; one script with six variants, each
  its own "Choose two -" Charm (base face `<Unsupported Variant>`, as Everythingamajig).
  - 49a: card property `artIsBy <artist>` (one of the printing's credited artists) - "if that card's art is by
    Wayne England, you may reveal it and draw another card".
  - 49d: SetState `Mode$ TurnOver` - a face-down creature turns face up, a double-faced one transforms, anything
    else turns face down as a 2/2 (Unstable ruling). "Its controller's hand" uses `HandOf$ TargetedController`.
  - Built on earlier fork pieces: `nameWords_EQ1` (49b), Draw `FromLibraryOf$` (49c), `BlackBordered` and
    `Watermarked` (49e), `NudgeNumber` (49f).
  - Not covered: turning over a melded permanent (the ruling splits it into two creatures; it turns face down
    instead). `VeryCrypticCommandTest` (5). Suite: 796 run, 0 failed, 6 skipped.
- Time Out (ust/48, upstream) verified against Scryfall, unchanged.

- **Spy Eye** (ust/46): Draw `FromLibraryOf$ <player>` (`Player.drawCards(..., libraryOf)`) — still your draw
  (counts, Drawn triggers, draw replacements), the card enters your hand still its owner's (controller set, as
  `HandOf$`). Drawing from an empty library still loses (CR 704.5b says "a library"), so the AI declines then.
  `SpyEyeTest` (2, one through real combat). Suite: 791 run, 0 failed, 6 skipped.
- Spell Suck (ust/45, upstream): `Oracle:` gains its reminder text to match Scryfall.

- **Socketed Sprocketer** (ust/44): "installing" a die result. `Card.installedResults` holds the dice sitting on
  a permanent (not counters, so proliferate and counter effects never see them), shown in the detail panel
  ("installed: 5", `TrackableProperty.InstalledResults`); a new object starts without any.
  - `InstallResult` API: `Uninstall$ All`, `Install$ <amount>`.
  - `Uninstall<N>` cost (`CostUninstall`), a real cost so a 6 can't also be spent on a roll in response.
  - Static `Mode$ UseInstalledResult`: after each die you roll (you see it first, per the ruling), an installed
    result on a permanent you control may take its place (`RollDiceEffect.useInstalledResult`); logged.
  - AI: swaps in a higher result, spending a 6 only on a roll of 1-2; re-rolls when nothing 4+ is installed, at
    an opponent's end step or its own second main. `SocketedSprocketerTest` (4). Suite: 789 run, 0 failed, 6 skipped.

- **S.N.E.A.K. Dispatcher** (ust/43): Dig from any player's library; an Agents of S.N.E.A.K. card
  (`Watermark_agentsofsneak`) may go into your hand, still its owner's.
  - Dig `HandOf$` (shared `SpellAbilityEffect.moveToHandOf`, now also behind ChangeZone's `HandOf$`) and
    `RestTopOrBottom$` (the chooser's `willPutCardOnTop` decision per card).
  - AI `willPutCardOnTop` on an opponent's card: keeps it on top only when its owner wouldn't want it.
    `SneakDispatcherTest` (2). Suite: 785 run, 0 failed, 6 skipped.

- **More or Less** (ust/40): new `NudgeNumber` API — one number printed on a spell or permanent reads 1
  higher or lower until end of turn (or until it changes zones, CR 400.7).
  - `NumberInstances.of(card)` lists what can be picked: power, toughness, the numeral in the mana cost,
    numbers in keywords (Bushido 2), activation-cost numerals ({1} in Mind Stone's cost), and ability
    parameters whose number is really printed in that ability's text. "Draw a card" prints "a", which isn't a
    number word, so it isn't offered; X never is (Unstable rulings). Repeated numbers are told apart by
    occurrence ("the second 2"), power bonus before toughness bonus.
  - `Card.numberNudges` (`NumberNudge`) is applied where each is read: base P/T, `getCMC`, keyword text
    (`updateChangedText`), trait parameters (`CardTraitBase.changeText`, which every stack/trigger copy re-runs),
    descriptions (`applyDescriptionTextChangeEffects`, word form kept: "two" → "three"), and activation costs
    (`SpellAbility` rebuilds its `Cost`).
  - `PlayerController.chooseStringForEffect(options, sa, prompt)`: a labeled pick with a real prompt; the AI
    asks the API's `SpellAbilityAi.chooseString`.
  - AI: casts it to kill an opposing creature one less toughness kills; lowers numbers on opponents' objects
    and raises its own. `MoreOrLessTest` (6). Suite: 782 run, 0 failed, 6 skipped.
  - Fix after live testing: a spell on the stack is targeted as its spell ability, so the effect (and its AI)
    read targets with `getCardsfromTargets`, as Mind Bend does - it silently did nothing on a spell before.
    Noncreatures no longer offer a "Power 0"/"Toughness 0" (`CardState.hasPrintedPT`).
  - Not covered: numbers inside token scripts ("create a 1/1") and in SVar-computed amounts.

- **Magic Word** (ust/38): the word is chosen as it enters (Keeper of the Sacred Word's free-input
  `ChooseType`). FlavorAction descriptions accept `CHOSENWORD`: "the chosen word" in the ability text,
  the word itself in the payment prompt (`CostFlavorAction.getPrompt`), so you're asked
  `Whisper "xyzzy"?`. The AI puts it on an opponent's creature (`Curse`) and taps it down on their turn.
  `MagicWordTest` (1).

- **Kindly Cognician** (ust/37): card property `rulesTextHasWord_<word>` — the word or its plural, whole
  words only, in the Oracle text with reminder text and the card's own name removed (the Unstable ruling
  excludes reminder text, name, type line and flavor text; "nonartifact" doesn't count).
  `KindlyCognicianTest` (2).

- **Half-Shark, Half-** (ust/35): augment (+3/+3, {5}{U}) whose condition is "At the beginning of your upkeep,".

- **Graveyard Busybody** (ust/34): "All graveyards are also your graveyards." New static
  `Mode$ AllGraveyardsYours`; `Game.getGraveyardHolder()` is the controller of the one that entered most
  recently, recomputed before the layers on every static-ability pass. Everything is gated on it, so games
  without a Busybody are untouched. Per the Unstable rulings, while it's out:
  - `Player.getCardsIn(Graveyard)` gives the holder every graveyard (own last) and everyone else none —
    counts, delve/escape/exile costs, "from your graveyard" fetches, threshold/delirium all follow.
  - Cards in graveyards are controlled by the holder (`Card.getController`), so only they can use those
    cards' activated, triggered and static abilities; flashback and the like from any graveyard
    (`SpellAbilityRestriction.zoneHolder`, `PlayerZone` activatable list).
  - `YouOwn`/`YouDontOwn`/`OppOwn`/`TargetedPlayerOwn`/`OwnedBy` read a graveyard card's zone holder
    (scripts write "in your graveyard" as YouOwn); ownership itself never changes, so cards still go to,
    and are shuffled back from, their owners' graveyards.
  - "Top/bottom/above" graveyard properties now read the physical graveyard.
  - P/T: `Count$ValidGraveyard Card.YouOwn+hasFlavorText` (the per-printing flavor text table). A second
    Busybody's controller has no graveyards, so theirs is 0/0.
  - `GraveyardBusybodyTest` (5, incl. AI turns played with one out). Suite: 774 run, 0 failed, 6 skipped.

- **Five-Finger Discount** (ust/33): a card in another player's hand. ChangeZone `HandOf$ <player>` puts
  it into that player's hand without changing its owner (unlike `GainOwnership`, which Last-Minute
  Chopping and Gifts Given use — the Unstable rulings say ownership stays put here). While it's there
  the holder controls it, so they see it and the owner doesn't; any later zone change clears that, so it
  dies into, is bounced to, or is shuffled into its owner's zones.
  - Casting from hand and playing lands now check whose hand the card is in rather than its owner
    (`SpellAbilityRestriction.zoneHolder`, `Player.canPlayLand`) — identical for every other card.
  - "Any color the next time you cast that card": a `ManaConvert` effect with `ForgetOnMoved$ Hand`,
    which lasts through paying for the cast and ends when the cast is done.
  - `FiveFingerDiscountTest` (3), including the AI casting a stolen Shivan Dragon off six Islands.
    Suite: 769 run, 0 failed, 6 skipped.

- **Defective Detective** (ust/32): the person outside the game is simulated. New ChooseCard
  `AtRandom$ ThreatWeighted` picks at random with weight = impact², so bigger cards are clearly likelier
  and near-equal ones stay near-equal (Forest 30 / Grizzly Bears 161 / Shivan Dragon 316 came up
  0.5% / 20% / 80% over 3000 trials). The chosen card is revealed; its controller never sees the hand.
  - `ComputerUtilCard.evaluateCardImpact`: one scale across card types — creatures by
    `evaluateCreature`, other spells 50 + 30 × mana value (the AI's own cross-type scale), lands 30.
    It's now `CardThreat`'s evaluator; creatures score as before, so Sacrifice Play is unchanged.
    `ThreatWeightedTest` (2). Suite: 766 run, 0 failed, 6 skipped.

- **Crafty Octopus** (ust/30): host whose ETB has the creature itself assemble a Contraption
  (`AssembleContraption` defaults the assembler to a creature host, so Steamflogger Boss doubles it).

- **Clocknapper** (ust/29): phase stealing. `DB$ StealPhase | Phase$ Beginning|Main1|Combat|Main2|Ending`
  (`PhaseHandler.stealPhase`) marks that phase of the victim's next turn; as it begins the thief becomes
  the active player (`getPlayerTurn()`), and the victim gets it back when it ends. Everything keyed off
  the active player follows from that, matching the Unstable rulings: the thief untaps, gets the upkeep
  triggers and draws; gets sorcery timing; attacks its opponents while the victim can only block; and
  discards to hand size in a stolen cleanup. `getActualTurnPlayer()` says whose turn it really is — the
  next turn, extra turns and turn counts use it, so turn order never shifts.
  - Only the first instance of the phase is stolen (an additional combat after it isn't). A later steal
    of the same phase from the same turn replaces an earlier one; stealing from yourself does nothing; a
    skipped phase fizzles the steal; it expires once that turn ends. "End the turn" during a stolen
    phase hands the cleanup back (or starts a stolen ending phase there). Logged as "X steals Y's …".
  - Carried through `GameSnapshot` and the AI `GameCopier`.
  - Card: a GenericChoice of the five phases (`Defined$ You` so the controller chooses, the target on it).
    AI (`AILogic$ StealPhase`): targets the lowest-life opponent; steals combat when its untapped,
    unsick creatures that won't attack this turn deal lethal, otherwise the beginning phase.
    `StealPhaseTest` (6). Suite: 764 run, 0 failed, 6 skipped.

- **Blurry Beeble** (ust/27): Blurry is honor system. As it's cast (static `SpellCast` trigger), every
  player — the caster too, since a control change can make them the defending player — answers
  whether they were wearing glasses; those who were are remembered, and `CantBlockBy` with
  `Creature.!RememberedPlayerCtrl` lets only their creatures block. A `!wasCast` Beeble (put onto the
  battlefield, or flickered — the copy keeps its remembered players) can't be blocked at all, per the
  Unstable ruling.
  - GenericChoice: an option with `RememberChooser$ True` remembers the player who picked it on the
    host (after any `TempRemember` swap, so it sticks).
  - AI: GenericChoice accepts `AILogic$ GameChance.N.Key` — the second choice is the "yes" answer,
    taken when that player's per-game roll for Key succeeds (`Glasses`, 50%). `BlurryTest` (3).

- **Silver border follows the printing**: `isSilverBorderedOrAcorn` and Border Guardian's `BorderColor*`
  properties read Scryfall's printed border (PrintingTraits) over the edition default. Steamflogger Boss
  (ust/93) is black-bordered, Unstable's Contraptions (ust/167–211) and full-art basics (ust/212–216)
  borderless, so none count as silver-bordered any more. Test in `PrintingTraitsTest`.

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
  - The remaining 3,341 differing `Oracle:` fields have other wording drift and were left alone
    (synced 2026-09-30, see that entry).
  - Merges: an upstream edit to one of these lines will now conflict. Take upstream's side of the
    line, then re-run `_tools/oracle-audit/oracle-drift.js <oracle-cards.jsonl.gz> --apply`.
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
