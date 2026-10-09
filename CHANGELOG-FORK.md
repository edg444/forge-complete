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
  (Standing Army's `STANDING`, Fat Ass's `EATING`, Hoisted Hireling's `HOISTED`).

### Choices the rules can't derive

- `DB$ ChooseArtist` + `Card.ArtistIsChosen` + `sharesArtistWith <restriction>` + `DB$ SetArtist`. The chosen
  artist is a person (`ArtistCredit`): "A & B" credits split, nicknames ignored, merged cards' artists included.
- GenericChoice `AILogic$ MostMatchingCreatures` with an `AIMatch$ <valid>` on each choice - the AI picks what
  its creature cards in library and hand match most (Ineffable Blessing).
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
  `HasHalfSymbol`, `Rarity<name>` (a basic land is common), `alphabeticallyFirstNonLand`, `SilverBordered`.
- Die results: `ResultSubAbilities$` keys `N`, `N-M`, and open-ended `N+` ("on an N or higher").
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
- Properties `BlackBordered`, `WhiteBordered`, `CollectorNumberEven`/`Odd`, `Watermarked`, `Watermark_<name>`,
  `OpenMouthArt`; `nameWords_Odd`/`_Even`, `nameWords_EQ<n or SVar>`.
- **Printed lines of text** (`rulelines=`/`textlines=`, front and back) — counted on every printing's card
  image by `_tools/rules-lines` (OCR), not estimated from Oracle text. `Card.isWordy()` (4+ lines of rules text,
  reminder text included) and `Card.getPrintedTextLines()` (whole text box, flavor included; -1 = unread).
  After a new set: `jobs.js`, `count.py` (reads only new printings), then `generate.js`.
- **Figures in the art** (`figures=solo|multi`, art menace) — Tagger's `solo` and two-or-more tags. A solo
  artwork that also shows an animal beside another kind of being is unknown (`Card.getArtFigures()` 0).
  Properties `ArtFiguresSolo`, `ArtFiguresMultiple`, `ArtFiguresUnknown`, and `ControlledByAI` for
  honor-system facts the AI can't check.
- **Hats in the art** (`hat`, Goblin Haberdasher) — confirmed hats only; `HatInArt` also counts the controller's
  answer to `GameAction.askHatInArt`, kept on the card (`Card.getHatInArtClaim()`).
- **Trees in the art** (`tree`, Selfie Preservation) — Tagger's `tree` tag and its whole subtree (dead, fallen,
  Treefolk...) plus `bonsai-tree`; `tree-stump` isn't in it. Property `KnownTreeInArt`; untagged art is asked
  about where it matters.

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
  Masterful Ninja uses it with the hand as the second zone (`GameAction.putOntoBattlefieldAlsoInHand` /
  `endAlsoInHand` / `isAlsoInHand`, API `AlsoOnBattlefield`); `Card.isInZone(Hand)` sees that residency, and a
  control change keeps any second residency.
- **Unnoticed zone changes** — `AbilityKey.Unnoticed` in a move's params skips replacement effects, zone-change
  triggers and the "entered/left the battlefield this turn" records (Masterful Ninja).
- **Programs** (The Grand Calcutron) — a hand that is an ordered row of revealed cards: `Player.hasProgram`,
  `ReorderZone | Program$ True`, `OnlyFirstOfProgram$` on CantBeCast/CantPlayLand, static `PlaceInProgram`,
  `PlayerController.chooseProgramPosition`.

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

### Infinity

- **Infinite mana** — `ManaPool.addInfinite(color, ...)` (Mox Lotus's `addInfiniteColorless()` is now the colorless
  case): a genuine per-color flag, not a large number. Spending an unbounded color refills it, and paying one point of
  any shard with it settles the whole shard (`{∞}`, `{R}×∞` in the payment prompt). It clears with the pool and
  displays as ∞. A produced amount that's infinite becomes `Infinity-<color>` tokens (`GameActionUtil.generatedMana`);
  infinite combo mana is unbounded mana of every offered color.
- **Infinite quantities** (`forge.util.Infinity`, Infinity Elemental) — the engine counts in ints, so ∞ is the reserved
  value `Infinity.VALUE` (2^24), and anything past `Infinity.THRESHOLD` (2^20) reads as infinite: small enough that
  sums and AI evaluations can't overflow, big enough that no finite game gets there. `Infinity.add/subtract`
  saturate, `Infinity.format` shows ∞ / -∞. Wired through power and toughness (`PT:∞/5`; boosts and counters don't
  change an infinite value, a set value replaces it), life (gains and losses don't change infinite life, any amount
  can be paid from it, setting it works), damage, counters (removing some of infinitely many leaves infinitely many
  but counts as removed; removing infinitely many empties them; per-counter triggers fire once for an infinite batch)
  and display (P/T, life, damage, counters, the damage assignment dialogs, desktop and mobile).
- **X = ∞** — offered when unbounded mana that can pay X is floating, or a source of it (Mox Lotus) can still be
  activated (`ManaPool.canPayInfiniteX`).
- **Infinitely many objects or repetitions** — tokens, copies, "do this X times", dice, coin flips, Clues, explores,
  proliferates and the like ask the controller for a finite number instead (`SpellAbilityEffect.finiteAmount`,
  `PlayerController.chooseFiniteForInfinite`; the AI picks `PlayerControllerAi.FINITE_FOR_INFINITE`, 30). Drawing
  infinitely many draws the library and then fails the draw that loses.

### Two-Headed Giant

- **Giant teams** (`Game.formGiantTeam`, `Game.isGiantTeam`, `Player.isOnGiantTeam`/`sharesTurnWith`/`getGiantTeam`)
  — CR 805 shared team turns and 810, for every team in a `GameType.TwoHeadedGiant` game or the team Better Than One
  forms mid-game. One turn: `PhaseHandler.isPlayerTurn` is true for every head, `getActivePlayers()` lists them
  (primary first), and each head untaps, draws, plays a land, discards to hand size, and gets its "that player"
  phase triggers (`TriggerPhase`/`TriggerTurnBegin` fire triggers naming no player once, on the primary's run).
  Turn hand-off skips teammates (`nextTurnAfter`); a skip or added step one head gets, the team gets (805.8).
- **Shared life** — teammates hold one `Player.LifeTotal` (`joinLifeTotal`, `getLifeSharers`), so gains, losses and
  payments still happen to each player (triggers, replacements, life lost this turn) while the total is the team's.
  Can't-gain/can't-lose-life (810.9g/h) and can't-lose/can't-win the game (810.8a) are team-wide.
- **Shared poison** — counters stay on each player; `getPoisonCounters()` is the team's sum, removals come off
  teammates when needed (810.10b), `getOpponentsTotalPoisonCounters` counts a team once, and a team of n loses at
  `getPoisonCountersToLose()` = 5n + 5.
- **Team combat** — each head declares into one combined attack (805.10b); a defender may block creatures attacking
  any player on their team (805.10d, `CombatUtil.canBeBlocked`). AI blocks and danger checks count attackers aimed
  at teammates (`ComputerUtilCombat.getAttackersOfTeam`).
- **Starting team** — whoever starts, their team's primary player takes the first turn (103.1a, 805.2); mulligans
  are declared by everyone, starting team first, then taken together (`MulliganService.runTeamMulligans`, 805.3a).
- **Proliferate** — one poison counter per team however many heads are chosen (701.34b); heads count their team's
  poison as theirs (810.10d).
- **Team wins and losses** — a head losing takes the team with them, a head winning doesn't make teammates lose
  (`GameAction.checkGameOverCondition`). Teammates see each other's hands (810.5, `PlayerView.isGiantTeammateOf`).
- **Lobby variant** (desktop and mobile): equal lobby teams of 2+, or seats 1+2 vs 3+4 when every slot is on its
  own team; teammates are seated together; 30 life (+15 per head beyond two); the starting team skips its first draw.
- **Players joining mid-game** (`Game.addOutsidePlayer`, `OutsidePlayers` factory plugged in by FModel) — an AI player
  seated beside a team, appended to the registered players (which match registrations by position) with their own
  `RegisteredPlayer`; `GameEventPlayerAdded` gives them a field on desktop and a panel on mobile.

### Other

- `DB$ RememberNumber` — writes a number into the remembered set (which is a `Set`, so repeats
  collapse).
- `NotifyMessage$` works on **any** ability, not only dice rolls.
- `UnlessPaidSubAbility$` — fires when an Unless cost *is* paid.
- `AddTrigger$`, `RandomSet`, `Staying Power`, additional activation zones, `Rotate180` display for
  tokens printed inverted.
- Dev mode can pick which printing to add.
- **Several dice rolled for one** (`ReplaceEffect | VarName$ CombinedDice`, The Big Idea): a `RollDice`
  replacement that turns the first die into extra dice whose total is its result; roll triggers still see each
  physical die.
- **Last strike** (`K:Last Strike`, Unstable) — a further combat damage step after the regular one, run as a
  repeat of `COMBAT_DAMAGE` rather than a new `PhaseType`.
- **K: lines keep upstream's text.** The engine parses a K: line's head into a `Keyword` and looks sentence
  keywords up by their exact text, so Oracle wording for them is display-only, from
  `CardFactoryUtil.keywordAsPrinted`. `KeywordScriptTextTest` enforces it.

---

## Log

### Unreleased

- **Two-Headed Giant** (CR 805, 810; rules checked against the 2026-09-25 Comprehensive Rules). See the engine
  capabilities section for what it covers. Fixed on the way: a human who is a team's second head used the first
  head's phase stops and would have auto-passed through their own main phase; their own stops now apply.
  Follow-ups: every head's field (desktop) / panel (mobile) lights up in the phase display; proliferate gives a
  team one poison counter however many heads are chosen, the proliferating player picking which (701.34b), and a
  head whose partner is poisoned can be chosen (810.10d); the starting player is normalized to their team's primary,
  so the starting team goes first for mulligans and opening-hand actions, "if you're not the starting player" means
  the starting team (103.1a), and team games declare every mulligan before any is taken (805.3a).
  `TwoHeadedGiantTest` (10).
- **Better Than One** (ust/128; previously deferred, not scripted upstream). `SP$ GainTeammate`: the caster picks
  hand cards, a number of cards from the top of their library and permanents; an AI-played person from outside the
  game (user ruling) joins beside the caster's team, owns those cards (set directly, not as an ante ownership change -
  the FAQ says they're still yours after the game) and the caster's side becomes a Two-Headed Giant team with the
  caster's life total (FAQ: the teammate brings none), or a Three-Headed one if it already had two heads. AI
  (`GainTeammateAi`) casts it with 20+ cards in library, giving half its library and half the lands in its hand.
  `BetterThanOneTest` (3).
- **Mary O'Kill** (ust/138; not scripted upstream).
  - **Switching a card in hand with a permanent** (`AB$ Switch | HandValid$ | BattlefieldValid$`): the permanent object
    stays put and the two objects trade printed cards (`CardCopyService.swapPrintedCards`; `Card.paperCard` is no
    longer final for it), so tapped/attacking/blocking status, attachments, counters, damage, effects on it and
    anything targeting it carry over and nothing enters or leaves (rulings, FAQ: "pulls off a mask").
  - "One on the battlefield" is any player's (user): that permanent keeps its controller; the card that leaves it
    goes to your hand still owned by its owner, held by you as Five-Finger Discount holds a card.
  - Activatable from hand too (`AdditionalActivationZone$ Hand`, rulings). New creature type **Killbot** in
    `TypeLists.txt` (it was missing, so Killbot checks matched nothing; changelings now count).
  - AI (`SwitchAi`): the best value swing - upgrading its own Killbot or taking an opponent's Mary for its weakest
    Killbot - after blockers on its turn or at an opponent's end step.
  - `MaryOKillTest` (3).
- Already scripted upstream, checked against Scryfall (fields, Oracle, rulings): Angelic Rocket (ust/139), **Border
  Guardian** (ust/140), Buzzing Whack-a-Doodle (141), Clock of DOOOOOOOOOOOOM! (142), Cogmentor (143), Contraption
  Cannon (144), the four Killbots (145a-d).
  Fixed: `BorderColor<color>` (Border Guardian) and `BlackBordered` (Knight of the Kitchen Sink) read the physical
  border, so an acorn spell counted as black-bordered; they now go through `Card.bordersAs()`, which treats acorn
  cards as silver like `SilverBordered` already did (user: acorn = silver-bordered, everywhere). `BorderGuardianTest` (1).
- **Entirely Normal Armchair** (ust/146; not scripted upstream).
  - **Hidden permanents** (`ST$ Hide`, a special action from hand during your turn): it's on the battlefield behind one
    of your permanents or in plain sight (you choose; pointing at yourself = plain sight), and its controller's
    opponents don't see it - it isn't drawn for them (desktop `PlayArea`, mobile `VField`), `CardView.canBeShownTo`
    says no, they can't target it and can't activate its abilities (`Card.isHiddenFrom`, checked in
    `Card.canBeTargetedBy` and `SpellAbilityRestriction.canPlay`). If what it's behind leaves, it's in plain sight.
  - **Finding it** (user: a hidden spot to guess): each opponent gets a "Something Hidden" command-zone effect with
    `ST$ SeekHidden`, once each turn - point at one of the hider's permanents or at them. Right: it's found (no longer
    hidden, the effects go) and they may return it at once with its {0} ability; wrong: nothing. The effects exile
    themselves when it leaves the battlefield.
  - Its controller sees a reminder marker on it - "Hidden: behind <permanent>" or "Hidden: in plain sight" - kept
    current by state-based checks (`Card.refreshHiddenMarker`); only their side ever draws it.
  - AI: hides it in its second main phase at a random spot (`HideAi`); seeks once a turn at a uniformly random spot,
    never using what it knows of the game (`SeekHiddenAi`), and returns it when found.
  - `EntirelyNormalArmchairTest` (4).
- **Handy Dandy Clone Machine** (ust/149; not scripted upstream).
  - **Tokens held by real hands** (Token `RepresentedByHand$ True`, `forge.game.player.Hands`; user: track hands): every
    player has two; a new token takes its controller's free hand, else other players in turn order are asked to lend
    one (AI opponents decline, AI teammates agree), else its controller is asked whether someone outside the game will
    (honor; the AI says no); with none it ceases to exist (FAQ). A hand frees up when its token is gone; a player
    leaving the game takes their hands with them (`Game.onPlayerLost`). Each token shows a "Hand: <whose>" marker.
  - AI won't make one when no hand it can count on is free (`TokenAi`). Not modeled: a hand choosing to stop
    representing its token.
  - `HandyDandyCloneMachineTest` (2).
- **Kindslaver** (ust/150; not scripted upstream). `ControlPlayer | OutsidePerson$ True`: when the target's next turn
  begins, a person from outside the game takes over their decisions for that turn - an AI that's neither the caster
  nor the target, one of three kinds at random (user): aggressive in the target's interest (the AI on its Reckless
  profile), a novice who just says "go" (no land, no spells, no attacks, forced choices at random; rulings), or
  aggressive against them (no land or spells, every creature attacks every combat, the best cards discarded and
  sacrificed when forced). `PlayerControllerOutsidePerson`, built by an `OutsidePlayers` factory FModel registers; the
  log names the person and their kind. `KindslaverTest` (3).
- **Krark's Other Thumb** (ust/151; not scripted upstream). Static `Mode$ RollTwoKeepOne | ValidPlayer$ You`: every
  die its controller rolls - each one of several, every reroll (Wall of Fortune), the planar die - is rolled as two
  and they choose which counts (rulings); the ignored roll never happened (only the kept one counts as rolled, so
  "whenever you roll" triggers and roll counts don't see it). A second Thumb (Mirror Gallery) applies to each of the
  first's dice. `RollDiceEffect.rollCountedDie`, `PlanarDice.rollFace`. The AI now ignores the lowest roll and a
  blank planar face instead of a random one (`PlayerControllerAi.chooseRollToIgnore`/`choosePDRollToIgnore`).
  `KrarksOtherThumbTest` (3).
- Labro Bot (ust/152), Lobe Lobber (153), Mad Science Fair Project (154) already scripted upstream; checked against
  Scryfall.
- **Modular Monstrosity** (ust/155; not scripted upstream). `DB$ ChooseKeywordInTime | Seconds$ 5`:
  - **The keyword list** (`PrintedCreatureKeywords`): every keyword line on a creature face in the card database, by
    title, so only printed forms - flying, annihilator 2, protection from Demons - never annihilator 500 (rulings).
  - **"Today"** (`KeywordDayLog`): the calendar day, per person, across every Monstrosity and every game; a keyword
    counts as used whatever its modifier (protection is protection). FModel keeps it in
    `<user dir>/modular-monstrosity-keywords.txt`.
  - **Five real seconds**: `PlayerController.chooseKeywordInTime` -> `IGuiGame.chooseStringInTime` (desktop
    `ListChooser.showTimed` counts down in the title; mobile `ListChooser.timeOut` on a gdx Timer; untimed elsewhere).
    Chosen: it gains the keyword for good. Not in time: it loses all keyword abilities, ones gained other ways too.
  - AI: never short of time; takes the best keyword it hasn't used today (flying, double strike, indestructible, ...).
  - `ModularMonstrosityTest` (3).
- **Proper Laboratory Attire** (ust/156; not scripted upstream). Protection from die rolls = `Protection:Card.RollsDice:die
  rolls`; card property `RollsDice` (`Card.rollsDice`): anything that allows or requires someone to roll a die, rerolls
  included - a RollDice/RollPlanarDice ability anywhere (sub-abilities, triggers, replacements, script it grants, like
  Lobe Lobber's), Wall of Fortune's reroll permission, Krark's Other Thumb - but not cards that only care about results
  (Snickering Squirrel, Squirrel-Powered Scheme; rulings). `ProperLaboratoryAttireTest` (1).

### 2026-10-09 (deployed: desktop, Android, GitHub) — upstream merge (94 commits, incl. dice and flip animations); The Grand Calcutron; Hot Fix; Ol' Buzzbark; Phoebe, Head of S.N.E.A.K.; X; Urza, Academy Headmaster audit

- **Upstream merge** (94 commits: dice and coin-flip animations, FlipOntoBattlefield corner hits and a mobile flip
  animation, AI paying for mana abilities that cost mana (Signets, filter lands), simultaneous-entry fix, netplay
  host fixes, TRK/TRC cards, edition and net-deck updates). Conflicts:
  - `AiController.saSideEffects`: took upstream's reordering; the Maro-style `HandSizeAi` check stays right after
    the spells-and-lands gate.
  - `CardView` power/toughness and `Player.loseLife`: kept both sides (half P/T flags and `Infinity.add` alongside
    upstream's lethal-damage refresh and trigger-repeat clearing).
  - `RollDiceEffect`: the fork's dice loop (The Big Idea, Wall of Fortune rerolls, installed results) stays;
    `rollPhysicalDie` now fires upstream's `GameEventRollDie(sides, roll)` and collects each die's final face for
    `GameEventRollDice`. An installed result isn't a face that came up, so the animation shows the die.
  - `FlipOntoBattlefieldEffect`: rebuilt on upstream's version with the fork's empty-battlefield guard, `Thrown$`
    and protected `getNeighboringCard` (Goblin Sleigh Ride's `SlideOntoBattlefieldEffect`). A throw reports
    `TimesFlipped` 0 but animates as one tumble: the mobile board picks real hits only for a card that flew
    (`wanted = flew ? hit.size() : 0`), so a 0 there would have made Slaying Mantis miss everything on Android.
  - Ashling's Prerogative, Demonic Pact: upstream's param fixes with the fork's self-reference wording.
  - Oracle sync afterwards: Accident-Prone Apprentice, Agent of Raffine (Scryfall retemplates).
  - Suite after the merge: 1055 run, 0 failed, 6 skipped.
- **The Grand Calcutron** (ust/131; previously deferred, not scripted upstream).
  - **Programs**: `DB$ ReorderZone | Zone$ Hand | Program$ True` has each player order their hand and records the
    host (id + timestamp) on the player (`Player.addProgramSource` / `hasProgram`); the program lasts while that
    object stays on the battlefield (phased out still counts), so it ends on its own when the Calcutron leaves
    (rulings, FAQ). Its order is the hand zone's order, first = leftmost. While a hand is a program: it's revealed
    to everyone (`CardView.canBeShownTo`), the hand-sorting preference doesn't apply (game layer and desktop
    `CHand`), dragging cards around in the desktop hand is refused, and the card detail panel shows
    "(program: N of M, playable)". `PlayerView.hasProgram` is refreshed in state-based checks.
  - `CantBeCast`/`CantPlayLand` param `OnlyFirstOfProgram$ True`: a player with a program can play only its first
    card, **from anywhere** (user ruling: graveyard, exile, library and command zone are closed too). Suspend
    isn't playing, so it isn't stopped. A spell already moved to the stack is judged by `Card.wasFirstOfProgram`
    (set where `castFrom` is).
  - Static `Mode$ PlaceInProgram`: a card moved into a program player's hand (`GameAction.programPosition`, every
    `zoneTo.add` of a hand move, merged cards included) is placed where that player chooses
    (`PlayerController.chooseProgramPosition`; human: numbered "First, before X / Between X and Y / Last, after Y"
    list), and the game log says where. Applied after other replacements.
  - Player property `hasProgram`; end-step refill is a plain Phase trigger with `ValidPlayer$ Player.hasProgram`,
    an intervening-if on hand size and a resolution check that the program still exists.
  - AI (`AiProgram`): orders turn by turn - a land if it can still play one, then the spells that fit that turn's
    mana, biggest first; a new card goes before the first card the same plan would put after it.
  - `GrandCalcutronTest` (4). Suite: 1023 run, 0 failed, 6 skipped.
- **Hot Fix** (ust/133; not scripted upstream).
  - **Real-time limits**: `ReorderZone | Seconds$ N` gives the player N seconds to rearrange the zone
    (`PlayerController.rearrangeInTime`, `IGuiGame.rearrangeInTime` returning `TimedArrangement`); if they're still
    touching a card when time runs out, the zone is shuffled and the log says so. Contents never change (ruling).
  - Desktop: the drag-to-rearrange window (`ListCardArea.showTimed`, top first, drag anywhere, click = to top)
    counts down in its title and closes itself at zero; "touching" = a mouse button held down on a card, dragging
    or not (`CardPanelContainer.isTouchingCard`). Done before the buzzer = hands off.
  - Mobile: the order dialog counts down in its header and closes itself (`DualListBox.finishNow`); cards moved
    across go on top in that order, the rest keep their order below; "touching" = a finger on the screen at zero
    (`Gdx.input.isTouched`). Network clients get the untimed default.
  - AI (`AiTimedRearrange`): pulls up to five cards it wants next to the top (lands until it has enough for its
    hand, then the best spell it can cast) and always lets go in time.
  - `HotFixTest` (3). Suite: 1026 run, 0 failed, 6 skipped.
- **Ol' Buzzbark** (ust/134; not scripted upstream).
  - **Dice rolled onto the battlefield**: `RollDice | OntoBattlefield$ <height>` - the roller aims the volley at a
    creature before rolling (all dice leave the hand at once - ruling), then each counted die comes to rest
    (`DiceLanding`, simulated like upstream's FlipOntoBattlefield): it lands around the aimed creature in its
    controller's row of creatures, scattering more and staying on a card less often the higher the drop; it can
    stop on the edge between two neighbors and touch both. `DieLandedSubAbility$` runs once per die with
    `DieResult` set and the touched creatures Remembered; each die is announced and logged. Roll modifiers
    (Krark's Other Thumb, Snickering Squirrel, ...) apply since it's a normal roll. Not modeled: one die touching
    creatures of two different players (ruling allows it; the rows don't meet).
  - AI aims at the best opposing creature the dice could plausibly kill, otherwise its own best creature.
  - `OlBuzzbarkTest` (4). Suite: 1030 run, 0 failed, 6 skipped.
- **Phoebe, Head of S.N.E.A.K.** (ust/135; not scripted upstream).
  - New `AB$ StealTextBox` (`StealTextBoxEffect`, `StealTextBoxAi`): the target loses its intrinsic (text-box)
    abilities and keywords - not ones granted by other effects - and the thief gains copies, as changed traits on
    each object, so a zone change ends it for that side only (rulings). Copied statics carry the theft's timestamp,
    so contradicting boxes resolve most-recent-first (rulings). Stealing from a Phoebe takes her stolen boxes too.
  - **Text boxes as data**: `Card.isTextBoxStolen` / `getStolenTextBoxes` (`StolenTextBox`: name, Oracle text,
    flavor text, watermarks, printed line counts), carried into LKI copies and AI game copies. `getTextBoxOracle`,
    `getFlavorText`, `getWatermarks`, `getTextBoxContents`, `getPrintedRulesLines` (wordy) and
    `getPrintedTextLines` (Lexivore) now read all of a card's text boxes as one and see nothing on a robbed one;
    `hasFlavorText`, `Watermarked`, `Watermark_`, `rulesTextHasWord_` and capital-letter counts use them.
  - AI steals from the best opposing creature that has a text box.
  - `PhoebeTest` (6). Suite: 1036 run, 0 failed, 6 skipped.
- Urza, Academy Headmaster (ust/136) already scripted upstream; card fields and Oracle checked against Scryfall.
- **X** (ust/137; not scripted upstream). Script on the HandOf$ idiom (the holder controls a card in their hand
  that isn't theirs, so "that opponent" is `You`); statics from the hand let its owner cast it, stop the holder
  from casting it, and reveal the holder's hand while it's in an opponent's hand.
  - Card property `inOwnersOpponentsHand`.
  - ChangeZone (known origin): when an ability with `Defined$` targets only players, the targets say where the
    Defined cards go rather than replacing them ("Put X into target opponent's hand").
  - The free play uses `DB$ Play` from the hand X is in, activatable only by its owner from there; lands only on
    your turn with a land play left (ruling, PlayEffect's own check). X itself is in that hand and fair game.
  - AI (`AILogic$ SneakIntoOpponentsHand`): at an opponent's end step, into the fullest hand, once it has five
    mana sources for the follow-up.
  - `XTest` (3). Suite: 1039 run, 0 failed, 6 skipped.
- **Urza, Academy Headmaster** (ust/136, upstream) audited against the official tables (Wizards Un-resources
  page and Urza-bilities_DARupdate.pdf, the PDF taking precedence). Fixed:
  - −6 #20 (gain 7 life, draw seven, put up to seven permanents) was scripted but missing from the random table,
    so −6 picked from 19 outcomes instead of 20.
  - −6 #19 gave one extra turn however many of the five coins came up heads; it now gives one per heads.
  - −6 #11 now forgets stale remembered objects before destroying, as Sorin, Lord of Innistrad's identical −6 does.
  - Description wording brought in line with the PDF on −1 #9 and −6 #7, #13, #17, #19.

### 2026-10-03 (deployed: desktop, Android, GitHub) — upstream merge (2 commits, data only); Unstable green complete and multicolor through Grusilda; audit of upstream Unstable scripts; open-ended die results; artists as people; tree-in-art; just a second; whole-card combination

- **Upstream merge** (2 commits: an SLD commander precon deck and FRA/FRC achievements, data only; no conflicts).
  Suite before the merge: 1019 run, 0 failed, 6 skipped.

- **Clever Combo** (ust/105, first green Unstable card; not scripted upstream). Script only: a tutor whose
  `ChangeType$ Host,Card.withAugment` is Labro Bot's host-or-augment filter. Scryfall lists no rulings.
- **Druid of the Sacred Beaker** (ust/106; not scripted upstream). Script plus a new creature type, **Deer**, in
  `TypeLists.txt` (Scryfall's creature-type catalog leaves out Un-only types, as with Hatificer). The mana count is
  Knight of the Widget's `Permanent.YouCtrl+Watermark_crossbreedlabs`; the Unstable FAQ's one ruling for the card
  (Contraptions' faction symbols count) is already covered, since the printing traits carry Contraption
  watermarks. A combined permanent counts once: no host, augment, or mutate printing has a watermark (Scryfall
  search, 2026-10-03), so a second card under one can't add another.
- **Half-Squirrel, Half-** (ust/111; not scripted upstream). Script only: the standard augment shape with the
  first negative adjustment, `PT:-1/-0` - `augmentAdjustment` and `CardFace.parsePT` both read it via
  `Integer.parseInt`, and the P/T box shows the printed `[-1 / -0]`. Its seven Scryfall rulings are the generic
  augment ones.
- **Audit of the upstream-scripted Unstable cards** (ust/1-111, the 47 cards skipped as already scripted, every
  printing and variant). Cost, type line, P/T, color indicator and Oracle text all match Scryfall; each script was
  read against its Oracle text and Scryfall rulings. One real bug: **"on an N or higher" die results** were
  scripted as closed ranges (Inhumaniac `5-6`), so a result pushed past the die's maximum (Snickering Squirrel,
  Squirrel-Powered Scheme, The Big Idea's combined dice) did nothing. `ResultSubAbilities$` now takes an
  open-ended `N+` key (`RollDiceEffect.resolveSub`); Inhumaniac and Lobe Lobber use `5+`, Dungeon Master `12+`
  (a d20 with a modifier can pass 20).
  - `InhumaniacTest` (1).
- **Hydradoodle** (ust/112, upstream) checked against Scryfall and the FAQ: matches, including X = 0 (no dice, dies
  as a 0/0).
- **Ineffable Blessing** (ust/113a-f, all six; not scripted upstream). Each variant chooses as it enters
  (`K:ETBReplacement`) and draws for creatures you control that match. A **Flavorful**/**Bland** (`hasFlavorText`,
  per printing, so Lexivore counts as the ruling says), B **an artist**, C **white-** or **silver-bordered**, D
  **a rarity**, E **odd** or **even** collector number, F **a number** of words in the name (0-25: the longest
  name in the pool is 25 words, and a face-down creature's is none). Choices with a trigger each are gated on the
  chosen mode with only the first printed (`Secondary$`). A face-down creature shows only a card back, so it has no
  flavor text, artist, border, rarity or collector number.
  - **Artists are people, not credit lines** (`ArtistCredit`): "A & B" credits both, and nicknames are dropped
    (ruling: "ignore all the various nicknames"), so `Rebecca "Don't Mess with Me" Guay` is Rebecca Guay. Applies
    to every choose-an-artist card: `ChooseArtist` offers individual names, `ArtistIsChosen` and `artIsBy` match
    any credited artist (Very Cryptic Command's `artIsBy Wayne England` now also finds `Wayne "King of" England`),
    and a combined (augmented) creature has every merged card's artist.
  - New card property `WhiteBordered` (printed border; borderless is neither). `Rarity*` treats a basic land as
    common (ruling: black expansion symbol). `nameWords_EQ` accepts an SVar.
  - AI: GenericChoice `AILogic$ MostMatchingCreatures` picks the choice whose `AIMatch$` the most creature cards
    in library and hand satisfy; ChooseArtist `MostCreaturesToCome`; ChooseNumber `MostCommonNameWordCount`.
  - `IneffableBlessingTest` (9).
- **Joyride Rigger** (ust/114, upstream) checked against Scryfall: matches, no rulings or FAQ entry.
- **Monkey-** (ust/115; not scripted upstream). Script only, the standard augment shape; its rulings are the generic
  augment ones.
- **Mother Kangaroo** (ust/116, upstream) checked against Scryfall: matches; its one ruling is the generic host one.
- **Multi-Headed** (ust/117; not scripted upstream). Script only: Half-Orc's intervening-if shape with
  `Count$YouRollThisTurn`, which counts every die you rolled, the planar die included.
- **Really Epic Punch** (ust/118; not scripted upstream). Script only: Savage Swipe's pump-then-fight with Success!'s
  `Card.Host,Card.withAugment` condition and `Duration$ Permanent`, following the three rulings (the +2/+2 never
  wears off; a combined creature still has augment; any creature you control can fight without the bonus).
- **Selfie Preservation** (ust/119; not scripted upstream). A new printing trait, `tree`: Scryfall Tagger's tree tag
  and everything under it, which follows the rulings and FAQ (dead and fallen trees count, Treefolk count, stumps
  don't and aren't under it), plus bonsai. 4,679 tagged arts; 291 of 375 basic Forest arts. A tagged tree goes
  straight onto the battlefield tapped; Tagger is incomplete, so for an untagged land the caster, who can see the
  revealed art, is asked (Goblin Haberdasher's rule: confirmed data first, a question only for the rest). The AI
  can't look, so it answers no. Search shape is Cultivate's.
  - `PrintingTraits.txt` regenerated (same Scryfall bulk files): only `tree`/`backtree` added, plus two rows for
    the Unfinity sticker-sheet edition upstream added after the last run.
  - `SelfiePreservationTest` (2).
- **Serpentine** (ust/120; not scripted upstream). Script only, the standard augment shape with a landfall
  condition.
- **Shellephant** (ust/121; not scripted upstream). `{0}` Charm activatable in any zone (_____'s
  `AdditionalActivationZone$` list); each mode is `Animate | Duration$ Perpetual`, so the definition follows the card
  between zones - the FAQ has you define it before it's on the battlefield, where an undefined 0/0 (ruling) dies
  first. "Turtle and/or Elephant" is a stack-free special action (`A:ST$`), changeable anywhere, anytime (FAQ); it's
  both until changed. AI: defines it once as a 3/3 (new CharmAi `AIPreferred$` mode marker, gated by
  `AICheckSVar$` on base toughness 0), never changes its types.
  - `ShellephantTest` (4).
- **Slaying Mantis** (ust/122; not scripted upstream).
  - **Just a second** (`Keyword.JUST_A_SECOND`): the ruling makes it split second (no spells, no abilities) plus not
    moving permanents, so `MagicStack.isSplitSecondOnStack` counts it; prints with its reminder text.
  - **The throw** reuses upstream's Chaos Orb/Falling Star simulation (`FlipOntoBattlefield`: aim at a card, land on
    it and/or a neighbor by chance) as an ETB replacement, with a new `Thrown$ True` that skips the flip-over roll
    (it's thrown; it always lands). With nothing on the battlefield it touches nothing (no longer an exception).
  - **The fight**: RepeatEach over the touched opposing creatures with `DamageMap$ True`, so each fight is
    one-on-one but all damage lands at once (the ruling's 3/3 + 4/4 + 5/5 example: all four die).
  - **Flip/throw AI** now aims at an opponent's permanent (it took the first battlefield card, often its own):
    the best creature, or with `AILogic$ ThrowFight` the best one the Mantis kills. Applies to Chaos Orb and
    Falling Star too.
  - New creature type **Wrestler**. `SlayingMantisTest` (4).
- **Squirrel Dealer** (ust/123; not scripted upstream). Script only: honor-system GenericChoice, as It That Gets Left
  Hanging - only a yes makes the Squirrel (FAQ: no reply isn't one); the AI has no one to ask, so no Squirrel.
- **Steamflogger Service Rep, Wild Crocodile, Willing Test Subject** (ust/124-126, upstream) checked against Scryfall
  and rulings: match. **Green Unstable complete.**
- **Baron Von Count** (ust/127, first multicolor; not scripted upstream). One real doom counter (counter removal takes
  it); the numeral it sits on is the chosen number, set by `ChooseNumber` with Min = Max (no prompt) and a new
  `NumberLabel$` so the panel reads "doom counter on: 4". New property `hasNumeral_<digit or SVar>`
  (`CardFactoryUtil.hasNumeral`): mana cost, text box with flavor text, printed power/toughness - not name, art,
  collector number or legal text (FAQ); 10 counts as 1 and 0 (ruling). Moving from 1 is a reflexive trigger
  (checked before the move, so 2 -> 1 doesn't fire it): the targeted player loses the game (rulings), back on 5.
  - **GameLossAi**: a targeted lose-the-game trigger outside combat aimed at the AI itself (the Phage path); it
    now aims at the strongest opponent.
  - `BaronVonCountTest` (4).
- **Better Than One** (ust/128) deferred by the user: needs a player joining mid-game and Two-Headed Giant rules,
  neither of which Forge has (same blocker as Splendid Genesis).
- **Cramped Bunker** (ust/129; not scripted upstream). Script only. The Bunker remembers what touches it (forgotten
  when it leaves the battlefield; each one shows "This permanent is touching Cramped Bunker."). Room for 8 (user:
  one per side and one per corner). "Can't" = full or nothing of theirs left outside, checked before anything
  moves; then everything they control outside is destroyed and the Bunker is sacrificed. The AI moves in its best
  permanent (what's inside survives the wipe).
  - `CrampedBunkerTest` (4).
- **Dr. Julius Jumblemorph** (ust/130; not scripted upstream). Every creature type in every zone (Mistform Ultimus's
  static). The host trigger is a searched `Augment` with a new `SearchZones$ Library,Graveyard`: the searcher picks
  "library and graveyard", "library only" or "graveyard only", so searching just the graveyard keeps the library
  unshuffled; `Shuffle$ True` shuffles only when the library was searched (found or not). AugmentAi takes the
  optional trigger when there's a card with augment to find. No rulings or FAQ entry.
  - `DrJuliusJumblemorphTest` (4).
- **The Grand Calcutron** (ust/131) deferred by the user (too complex; not scripted upstream either).
- **Grusilda, Monster Masher** (ust/132; not scripted upstream).
  - New `DB$ Combine` (`CombineEffect`, `CombineAi`): two target creature cards from graveyards are merged while
    still in the graveyard, then enter as one creature under your control, so both cards' enter abilities fire
    (two hosts both trigger, and the result is still a host an augment can go on - rulings). A host and a card
    with augment combine the augment way; a card with augment and a non-host keep augment with no host, and the
    existing state-based action sends both to the graveyard (ruling).
  - Whole-card combination (`Card.isCombinedWhole`, `CardFactory.getCombinedCloneStates`): summed power and
    toughness, both names (shown "A // B"; `Card.getNames` feeds `sharesNameWith` and the legend rule, which now
    indexes each name and won't put the same permanent away twice), combined mana cost, colors, types, abilities.
    Not done: two characteristic-defining P/T abilities aren't added together (FAQ) - the later one wins.
  - `Combined` card property (hers, host with augment, melded, merged) for her menace static.
  - **Merged permanents leaving the battlefield** now send each card to its own owner's zone (they all went to the
    top card's owner's): mattered for Grusilda's two graveyards and for augmenting another player's host.
  - AI: picks the two best creature cards, a host with an augment when it can, never an augment with a non-host.
  - `GrusildaTest` (4).

### 2026-10-02 (deployed: desktop, Android, GitHub) — upstream merge (12 commits, incl. Unfinity stickers); Unstable red complete (The Big Idea through Three-Headed Goblin); printed-line OCR; infinity; π damage; Party Crasher; triple strike; Jester's Sombrero AI

- **Upstream merge** (12 commits, among them Unfinity stickers and sticker sheets, Goblin Blastronauts, and the
  draw-from-the-bottom keyword becoming a `DrawFromBottom` static). Six conflicts, each kept both sides:
  `StaticAbility` (the fork's `GainsTextBoxOf` beside the sticker params), `PlayerControllerAi`
  (`chooseStringForEffect` beside the sticker choices), `GamePieceType` (`LIBRARY` and `STICKER_SHEET`),
  `CardView` (chosen expansion and player-counter info beside `updateStickers`), `RollDiceEffect` (The Big Idea's
  per-die `RolledDie` triggers now also pass upstream's `SourceSA`), and `Player` (drawing another player's library
  still ignores your own `drawsFromBottom()`). No fork-only script params were stripped. Suite after the merge:
  983 run, 0 failed, 6 skipped.
- **The Big Idea** (ust/76, first red Unstable card; not scripted upstream either). Its shield is an Effect
  (`Duration$ Permanent`, since the card has no "this turn") with a `RollDice` replacement that raises a new
  replacement value, `CombinedDice`. `RollDiceEffect.rollAction` rolls that many extra dice for the first die
  and sums them into one result (`DieRollResult.getCombinedDice()`), so the total feeds everything that reads
  the result, and the log shows "Rolled 2 dice for one: 3 + 3 = 6". Two shields both apply to the same next
  roll (614.5), making it three dice for one. Following the three Scryfall rulings (2018-01-19): `RolledDie`
  triggers fire once per physical die with that die's own value (Chittering Doom doesn't see the total), and a
  roll-two-dice-and-use-the-difference ability gets only one die replaced. `Number$` (Resolute Veggiesaur)
  and "dice rolled this turn" count the physical dice. The AI shields at the opponent's end step
  (`AILogic$ AtOppEOT`), when tapping its Brainiacs costs nothing.
  - `TheBigIdeaTest` (7). Suite: 904 run, 0 failed, 6 skipped.
- **Garbage Elemental A, E and F** (ust/82a, 82e, 82f; B, C and D were upstream and match Scryfall). The edition's
  82a and 82e-f printings no longer load as an unsupported variant.
  - **A** — Frenzy 2 and "can't be blocked by wordy creatures". Frenzy had a `Keyword` entry but no ability and
    no card using it; it now triggers on `AttackerUnblocked` and prints with its reminder text.
  - **E** — Unleash, and creatures you control with any kind of counter have **art menace**: new keyword
    `Art menace`, a `CantBlockBy` static. Art Tagger counts as one figure can't block; untagged art is left to
    the players (as with Sly Spy's facing), except that the AI doesn't block with it.
  - **F** — last strike and a Battalion trigger (target creature can't block this turn).
- **"Wordy" and "most lines of text" now come from the printed card.** Garbage Elemental's wordy is "four or
  more lines of rules text"; the Unstable FAQ's Silver Rule says to look at the actual card, and how text wraps
  isn't in any dataset. `_tools/rules-lines` reads every face of every English paper printing Forge has (92,407)
  off Scryfall's card images with RapidOCR on the GPU, and `PrintingTraits.txt` now carries the counts:
  - Rows below the type line, up to the artist/copyright footer, minus the rows matching that printing's flavor
    text. 90,842 faces read (98.3%); Sagas, Rooms, Classes, Cases, flip and level-up cards, planes, schemes and
    other layouts whose text isn't one block are left unread, and unread never restricts anything.
  - Checked by eye on 54 sampled faces across frames and card types: rules lines exact on all 54 after the one
    miss was fixed ("+1/+1." on its own line had been dropped as a P/T box). Profiling the failures caught
    "Wizards" in rules text taken for the copyright line, and instants and sorceries counted from the wrong row
    (15,851 corrected); comparing flavor rows with Scryfall's flavor text caught trailing artist names counted
    as flavor (471). About 0.2% of flavor-bearing faces still miss a flavor line the OCR didn't detect.
  - **Replaces the fork's Oracle-text estimate** (`CardFactoryUtil.getTextBoxLineCount`, 40 characters a line,
    removed): Frazzled Editor's protection from `wordy` and Lexivore's `mostLinesOfText` read the printed
    counts too. Lexivore may target an unread permanent; the AI goes by known counts only.
  - `GarbageElementalTest` (7), `PrintedLinesTest` (3); `TextBoxTest` loses its three estimator tests. Suite: 911 run, 0 failed, 6 skipped.
- **Goblin Haberdasher** (ust/83; not scripted upstream). New creature type **Hatificer**. "Wearing hats in their
  art" (`Creature.HatInArt`) is confirmed by the printing data or, failing that, asked:
  - **Confirmed** (`hat` in PrintingTraits, 1,116 printings): worn headwear in Tagger's hat tree - helmets and
    crowns included, per the FAQ's "garment worn on the head that's not part of another garment" - on art tagged
    as one figure, since a background character's hat doesn't count (rulings) and Tagger doesn't say who wears
    it. Not counted: a helmet being held, sticker sheets, "not-a-hat", and the plain `helmet` tag alone (8ED
    Seasoned Marshal carries hers and is tagged `helmet`). Checked by eye on 12 random printings: all wearing one.
  - **Asked** otherwise, honor system and only when it matters: right after attackers are declared, the
    controller of each attacker a hat would give something to is asked once (`GameAction.askHatInArt`). The answer
    is about the physical card, so like a signature it stays with the card through every zone. The AI can't look,
    so it never claims a hat.
  - `GoblinHaberdasherTest` (5). Suite: 916 run, 0 failed, 6 skipped.
- **Half-Orc, Half-** (ust/84; not scripted upstream). The first augment whose unfinished condition has an
  intervening "if" (`CheckSVar$` with `PlayerCountRegisteredOpponents$HasPropertywasDealtDamageThisTurn`); the
  combined trigger keeps the condition and its SVar, so no engine change was needed. `HalfOrcTest` (3, with
  Adorable Kitten as the host). Suite: 919 run, 0 failed, 6 skipped.
- **Hammerfest Boomtacular** (ust/87; 85 Hammer Helper and 86 Hammer Jammer were already upstream). Script only:
  a `SpellCast` trigger on `Card.Watermark_goblinexplosioneers`, the printing's watermark from PrintingTraits.
  `HammerfestBoomtacularTest` (2, casting Goblin Haberdasher's UST printing and Grizzly Bears).
- **Infinity Elemental** (ust/88; not scripted upstream) and **infinity** throughout the engine - see the *Infinity*
  index entry. Built to the five Scryfall rulings (2020-02-29) and the Unstable FAQ: its power ties with another
  Infinity Elemental's, lifelink gains infinite life, infinite life survives even infinite damage, setting life works,
  and drawing from an empty library still loses. User decisions (2026-10-02): an infinite number of objects or
  repetitions becomes a finite number the controller picks; X = ∞ is allowed with infinite mana, and Mox Lotus works
  the same way (checked: it pays X = ∞, and ∞ is offered before it's tapped). Mox Lotus's infinite colorless now goes
  through the same per-color pool flag. The desktop text-only card renderer now uses the P/T display strings, so it
  shows half and infinite values too.
  - `InfinityTest` (12, including an AI turn with it on the battlefield). Suite: 933 run, 0 failed, 6 skipped.
- **It That Gets Left Hanging** (ust/89; not scripted upstream). Script only, honor system like Skull Saucer: the
  haste is an `UnlessCost$ FlavorAction` the player pays by confirming the high five - or that there was no one to
  ask, since then it doesn't gain haste (rulings). The AI has no one to ask, so it never gains haste.
  `ItThatGetsLeftHangingTest` (2).
- **Just Desserts** (ust/90; not scripted upstream) and **π damage**. `NumDmg$ 3 | Pi$ True`: the whole 3 goes
  through damage as usual (prevention, redirection, triggers), and `GameAction.dealDamage` marks the π - 3 left over
  on wherever the 3 actually landed, which is how the rulings' redirected-to-a-player case gets its 3.14.
  - Creatures count it exactly (`Card.addPiDamage`, `getExactDamage`; the lethal check compares the real number), so
    the rulings and FAQ hold: one destroys toughness 3, two 6 (not 6 1/2), four 12 1/2 (not 13), eight 25.
  - Players take .14 more per π ("use 3.14"): 20 life becomes 16.86. Life's fraction is now kept in hundredths
    (`Player.lifeHundredths`, replacing the 0-or-1 half): a half is 50, and the half-life API reads the same as
    before. `changeLifeByHundredths` carries like `changeLifeByHalves`; 704.5a counts any fraction as more than 0.
  - Doubled or tripled damage doubles or triples the π; added or partly prevented damage keeps one; fully prevented,
    none. Damage both doubled and then partly prevented is read as one π. π damage to a planeswalker or battle removes
    the whole 3 counters only, since counters are whole.
  - Display: card damage "3.14" / "6.28", life "16.86", "16.36" from 19 1/2, "-0.14" just below zero; the mobile
    life-change label counts in hundredths ("-3.14"), and the mobile life label shrinks its font to fit totals
    longer than two digits. `PlayerView.getLifeHundredths`, trackable `LifeHundredths` and `PiDamage`.
  - Merges: `Pi$` is a fork-only script param the engine depends on - check upstream's linter didn't strip it.
  - `JustDessertsTest` (7). Suite: 942 run, 0 failed, 6 skipped.
- **Jester's Sombrero AI.** It cast the Sombrero and activated it at players with no sideboard. Now:
  - New deck flag `AI:RemoveDeck:NoSideboard` (`CardAiHints.getRemNoSideboardDecks`): out of generated decks for games
    without sideboards - random constructed, Commander and themed decks, through `IS_KEPT_IN_RANDOM_DECKS` and
    `CardThemedDeckBuilder` - but kept by Limited, whose decks have the rest of the pool as a sideboard.
  - ChooseCard `AILogic$ TargetHasChoices`: target only a player with something to choose, and the one with the most
    (a targeted "choose up to N" with MinAmount 0 otherwise counted zero as enough).
  - `SVar:NeedsToPlayVar` on opponents' sideboard cards, so it isn't cast with nothing to raid.
  - `JestersSombreroTest` (3). Suite: 945 run, 0 failed, 6 skipped.
- **Party Crasher** (ust/92; not scripted upstream) - a second player attacking in someone else's combat. New static
  mode `AttackDuringOpponentsTurn` (`StaticAbilityAttackDuringOpponentsTurn`).
  - In the declare attackers step, after the active player declares, each of their opponents in turn order may
    declare such creatures (`PhaseHandler.declareOpponentsTurnAttackers`; the choice is
    `PlayerController.chooseAttackDuringOpponentsTurn`, an optional pick of what it attacks). It attacks any of its
    controller's opponents (user, 2026-10-02), so `Combat.addDefender` adds the active player, their planeswalkers or
    battles when needed. Attack costs and tapping work as for any attacker. Since it's offered once per declare
    attackers step, it's once each combat.
  - Blocking (rulings): the player it attacks blocks after the others (declare blockers already ends with the active
    player). An untapped attacking creature can block it, and the Crasher, if untapped, can block back. Two creatures
    blocking each other deal combat damage to each other once, as attackers. The human block input picks out only
    other players' attackers, so your own untapped attackers can be chosen as blockers, and removing one of them
    removes only the block. Removing a creature that's both attacking and blocking takes it out of both.
  - Combat damage from an attacker is now assigned by its controller, not by the active player.
  - "Whenever you attack" (AttackersDeclared) now fires once for each player who attacked, with their own
    attackers. A player's "attacked this turn" lists now clear at every cleanup, not only on their own turn, so an
    attack on an opponent's turn doesn't count toward their next turn. "Attacked this combat" clears for everyone.
  - AI: it stays home when it's needed to block - its life is in danger, or it can block an attacker, kill it and
    survive - unless it has vigilance. Otherwise it attacks the opponent with the least life among those with nothing
    able to block and kill it. `lifeInDanger` no longer assumes the active player can't be attacked, and the AI's
    blocker reset only undoes the block of a creature that's also attacking.
  - `PartyCrasherTest` (5). Suite: 950 run, 0 failed, 6 skipped.
- **Super-Duper Death Ray** (ust/97; not scripted upstream; 93-96, the Steamfloggers, are upstream and match Oracle).
  Trample on an instant: `K:Trample:Spell`, whose title is plain "Trample" with the reminder "This spell can deal
  excess damage to its target's controller.", and which instant and sorcery text now prints. The damage is the existing
  `ExcessDamage$ TargetedController` (as on Flame Spill), conditioned on the spell having trample. Per the rulings,
  marked damage counts toward lethal, and only the creature is targeted, so a hexproof controller still takes the
  excess.
  - `SuperDuperDeathRayTest` (4). Suite: 954 run, 0 failed, 6 skipped.
- **Three-Headed Goblin** (ust/99; not scripted upstream; 98a-d Target Minotaur, 100 and 101 are upstream) and
  **triple strike**: new `Keyword.TRIPLE_STRIKE` ("Triple strike", printed with its reminder text as on the card).
  The Unstable FAQ says double strike + last strike = triple strike, so `Card.hasDoubleStrike()` and
  `hasLastStrike()` both include it, and the last-strike step deals its third hit. It has to survive each step to
  strike again (ruling): a 3/3 first striker trades with it in the first-strike step, and unblocked it deals 9.
  - AI: `Card.getCombatDamageStrikes()` (3, 2 or 1, also counting first + last strike as 2) replaces the "double
    strike = x2" in damage and poison predictions, attack planning and blocker damage. Creature evaluation values
    triple strike above double strike, and first-strike-safe blockers include it.
  - Every Unstable printing up to 101 now has a script, which completes red.
  - `ThreeHeadedGoblinTest` (4). Suite: 958 run, 0 failed, 6 skipped.

### 2026-10-01, fourth build (deployed: desktop, Android, GitHub) — Deck Editor augment P/T; Kefnet's Monument deck gen and AI; MustBeBlockedByAll text

- **Augment P/T in the Deck Editor.** Display-only cards (negative id) get no keywords, so the augment check
  missed them and Zombified still read 0/0 there; `CardView` now also asks the printed face
  (`CardFactory.isAugmentFace`).
- **Random color decks honor a colorless card's `DeckNeeds:Color$`.** Kefnet's Monument (blue creatures)
  landed in a red/black deck: colorless cards were let in unchecked. `DeckGeneratorBase` now requires one of
  the needed colors (new `DeckHints.getColors()`); colorless cards with no color need are unaffected.
- **"Doesn't untap during its controller's next untap step" AI.** Kefnet's Monument, Fogwalker and Skyline
  Cascade lacked `IsCurse$ True`, so the AI treated the effect as a buff and forced it onto the opponent's
  worst creature, tapped or not. And as a curse, `PumpAiBase` only valued it from main 2 on the AI's turn, so
  a main-1 creature cast fell back to the best creature; now any tapped creature that can untap is the
  useful target, whenever it lands (it stays tapped until that untap step either way).
- **Marble Priest / Talruum Piper text.** `MustBeBlockedByAll:<valid>:<description>` printed the whole
  keyword as card text (upstream too); only the description prints now.

### 2026-10-01, third build (deployed: desktop, Android, GitHub) — augment P/T fix

- **Fixed the second build's augment P/T box breaking every card's display.** An unset tracked string reads
  back as `""`, not null, so every card off the battlefield got an empty `[]` P/T box, and the card image
  renderers threw splitting it - only the first hand/library card drew and the stack flickered endlessly.
  `getAugmentPT()` now treats empty as "not an augment"; `AugmentTest` checks a non-augment in hand and a
  drawn augment.

### 2026-10-01, second build (deployed: desktop, Android, GitHub) — augment P/T box

- **Augment P/T box shows the adjustment.** Off the battlefield an augment card (Half-Kitten, Half-; Ninja; ...)
  is a 0/0, so its P/T box read "0/0". It now shows the printed adjustment bracketed like a Vehicle's,
  `[+1/+2]`, in the battlefield/hand card overlay, the card detail panel, and mobile's card and list renderers
  (desktop's drawn card image shows `+1/+2` unbracketed, as it does Vehicles). On the battlefield, combined or
  alone, the real P/T shows as before. New tracked `CardStateView.getAugmentPT()` (null on the battlefield).

### 2026-10-01 (deployed: desktop, Android, GitHub) — upstream merge (15 commits, 2.0.16-SNAPSHOT); Unstable black complete (Dirty Rat through Zombified); functional keyword text restored; doubled cost words; AI fixes

- **Functional keyword text restored** (found 2026-10-01: Amber Prison's untap choice was gone). The 2026-09-30
  in-game text retemplating rewrote K: lines to Oracle wording, but a K: line is engine input: its head is
  parsed into a `Keyword`, and sentence keywords are found by their exact text from Java
  (`hasKeyword("You may choose not to untap CARDNAME during your untap step.")`). 154 lines on 152 cards had
  silently stopped working:
  - 107 sentence keywords on 105 cards: optional untap (42), must be blocked if able (11), lure (10), ante
    removal (9, so ante cards stayed in decks for games not played for ante), can't attack or block alone and
    its variants (13), damage prevention (9), can't attack or block (2), and one-offs on Okk, Orcish
    Conscripts, Scarred Puma, Panglacial Wurm, Ogre Enforcer, Nacatl War Pride, Butcher Orgg, Cunning Giant and
    Xenosquirrels (its die modification).
  - 46 `Start your engines!` lines no longer parsed as the keyword, so none of those cards gave speed.
  - Collective Brutality's escalate cost had become `Discard.<1/Card>`, which parsed as Escalate {0}.

  All are back to upstream's exact text, which also keeps those lines out of merge conflicts. The Oracle
  wording now comes from the display: `CardFactoryUtil.keywordAsPrinted` puts in "this artifact", "this card"
  or a legendary card's short name, but only when the card's own Oracle text prints that sentence. A full
  re-render: 0 faces newly drifting, 5 more matching Oracle (the ante line now shows on ante sorceries, plus
  Collective Brutality's cost and Gorm's and B.O.B.'s short names).
  - Guard: `KeywordScriptTextTest` fails on any K: line whose keyword head has print punctuation added, whose
    cost type has punctuation glued on, or whose self-reference was reworded away from a string the engine's
    Java looks up. Run against the broken scripts, it flagged the same 153 sentence and keyword lines as the git
    audit (`_tools/oracle-audit/k-line-audit.js`). `FunctionalKeywordTextTest` (9) covers Amber Prison,
    Hivis, a lure, must-be-blocked, ante removal, Xenosquirrels, speed and Collective Brutality. Suite: 882 run,
    0 failed, 6 skipped.
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
  - Flaky test fixed: `testHandSizeCreatureKeepsTheHand` failed about half the time on "Lightning Bolt is
    played". `HandSizeAi` passed the Bolt every time. The failures came from upstream's
    `ComputerUtilCard.useRemovalNow`, which deliberately rolls dice on holding instant removal: on the AI's
    own main 2, Bolt on a Hill Giant has a value of 0.5, so it was cast 94 times in 200 calls. That's a
    reasonable hold, so the AI is unchanged. The test now checks `HandSizeAi` directly in main 2, and
    checks the full decision at the opponent's end step, where the tempo value doubles to 1.0 and the roll
    always passes (200 of 200).

- **Zombified** (ust/75), the last black Unstable card. Two pieces of augment the fork hadn't built:
  - Combining from the graveyard: `AB$ Augment | ActivationZone$ Graveyard | FromZone$ Graveyard` ("{4}{B}:
    Combine this card from your graveyard with target host."). `AugmentEffect` takes `FromZone$` (default Hand).
  - Activated conditions: an augment's "{2}{B}, Exile a creature card from your graveyard:" is a placeholder
    ability marked `AugmentCondition$ True` (never usable alone); `CardFactory.getAugmentedCloneStates` replaces it
    with an ability of that cost whose effect is the host's enters effect, and the combined Oracle line gets the
    effect capitalized after the colon. Steam-Powered (red) can use the same. Zombified + Adorable Kitten is
    "Zombified Kitten", 3/3, "{2}{B}, Exile a creature card from your graveyard: Roll a six-sided die. You gain life
    equal to the result."
  - A cost with an empty `SpellDescription$` no longer leaves a trailing space (`SpellAbility.rebuiltDescription`);
    `CardState.removeSpellAbility` added. `ZombifiedTest` (2). Suite: 892 run, 0 failed, 6 skipped.

- **Summon the Pack** (ust/74). Upstream's `MakeCard | Booster$ True` (Booster Tutor's random Forge booster) gains
  `PutAll$ True`: every card in the pack that passes `Filter$`, duplicates included, with nothing to choose. Here
  every creature card goes onto the battlefield (their enters abilities trigger, so the FAQ's Phage the
  Untouchable really does lose you the game), and an `Animate` makes them Zombies for as long as they stay. A pack
  with no creature cards does nothing (FAQ). `Reveal$ True` on a booster now shows the whole opened pack to
  everyone, as "reveal the cards" says, for Booster Tutor too (it used to show only the card taken).
  `SummonThePackTest` (2). Suite: 890 run, 0 failed, 6 skipped.

- **Squirrel-Powered Scheme** (ust/70; 71 Steady-Handed Mook and 72 Stinging Scorpion were already upstream and
  match Scryfall). New static mode `IncreaseRollResult` (`ValidPlayer$`, `Amount$`): each die result that player
  rolls goes up, after rerolls and swaps and before Snickering Squirrel is offered; the natural roll is unchanged.
  Several add up. `RollDiceEffect.rollResultIncrease`. `SquirrelPoweredSchemeTest` (2). Suite: 888 run, 0 failed,
  6 skipped.

- **Subcontract** (ust/73). The person outside the game is simulated as for Defective Detective: a random nonland
  card from the target opponent's hand, likelier the more impactful it is (`ChooseCard | AtRandom$ ThreatWeighted`),
  which that player discards. Script only. `SubcontractTest` (2: over 200 casts a land is never picked and Shivan
  Dragon is picked more often than Grizzly Bears).

- **Spike, Tournament Grinder** (ust/69). Outside the game is the sideboard, as for the Wishes. "Banned or restricted
  in a Constructed format" is `res/lists/BannedOrRestricted.txt` (card property `everBannedOrRestricted`,
  `forge.card.BannedOrRestricted`), built by `_tools/spike-list/generate.js`: the Unstable FAQ's list as of December
  2017 (193 names, all matched), every card Scryfall marks banned or restricted on 2026-09-30 in a format Wizards of
  the Coast runs (Standard, Pioneer, Modern, Legacy, Vintage, Pauper, Historic, Timeless, Alchemy, Brawl, Standard
  Brawl, Competitive Brawl, Commander: 377 cards), legendary cards first printed in Legends, Ice Age or Homelands
  (79), and conspiracies (29, per the FAQ). 556 names in all. Ante cards are on it but, as the FAQ says, can't be in a
  game that isn't for ante. Not covered: bans since 2018 that were lifted or whose cards rotated out (Scryfall only
  has current legality), e.g. Oko's 2019 Standard ban (it counts anyway through Pioneer and Modern).
  - `SpikeTournamentGrinderTest` (2). Suite: 884 run, 0 failed, 6 skipped.

- **Snickering Squirrel** (ust/68). New static mode `TapToIncreaseRoll` (not a keyword-text match, so a wording
  change can't break it): once a die's result is final, `RollDiceEffect.tapToIncrease` offers each untapped
  creature with it to its controller, in turn order and again after every increase, so several can go on one
  die and a 6 can become a 7 (rulings). It isn't a {T} ability, so a Squirrel that just arrived can be tapped. The
  AI raises only its own rolls. Note that upstream scripts which encode "N or higher" as a range ending at the
  die's top face (Inhumaniac's 5-6) won't count a raised 7.
  - `SnickeringSquirrelTest` (3, one a real Sly Spy roll in an AI combat). Suite: 870 run, 0 failed, 6 skipped.

- **Sly Spy** (ust/67a-f), all six printings (upstream had only F, the die roll; its text now says "this creature"
  like Oracle). Each is "Whenever this creature deals combat damage to a player, ...":
  - **A** reveals the hand and discards a card with the longest name, counting characters without spaces
    (ruling). New card property `longestNameInHand`.
  - **B/D** destroy a creature facing left/right in its art. Facing comes from Scryfall Tagger's `left-facing`
    and `right-facing` tags, now in `PrintingTraits.txt` (`facingleft`/`facingright`, plus back faces) and
    exposed as `Card.getArtFacing()` and the properties `FacingLeft`, `FacingRight`, `FacingUnknown`. Only 4,090
    of 26,580 creature artworks are tagged (45 both ways, which counts as unknown), so an untagged creature stays
    a legal target for the player to judge from the art (honor system); the AI targets only known facings
    (`AITgts$`). Right means the viewer's right (FAQ; Tagger agrees).
  - **C** honor system: a "Lost Finger" effect for that player, lasting until Sly Spy leaves the battlefield.
  - **E** puts the top card of that player's library into your hand (still theirs: `HandOf$ You`) and you lose
    life equal to its mana value.
  - `_tools/printing-traits/generate.js` writes the facing traits; regenerated from the same bulk data, every
    other trait is unchanged (7,981 new rows, facing only). `SlySpyTest` (7, each variant played through an AI
    combat). Suite: 867 run, 0 failed, 6 skipped.

- **Skull Saucer** (ust/66), honor system. Its enters trigger destroys the target, then asks you to put your head
  on the table (`UnlessCost$ FlavorAction`): decline and it's sacrificed, as the ruling says (the creature is
  still destroyed). Later, "I lifted my head" sacrifices it; that line reads exactly like Oracle via
  `CostDesc$`/`SpellDescription$`. The AI keeps its head down for good. Rulings and FAQ: any part of your head,
  any playing surface, and nobody may physically lift yours.
  - `AILogic$ Never` is now honored for every API (`SpellAbilityAi.canPlayWithSubs`, next to `Chance.N`):
    APIs that override `canPlay` (Sacrifice among them) never reached `checkAiLogic`, so the AI would have lifted
    its head. Upstream scripts that use `Never` on an activated ability or spell now really never do it.
  - `SkullSaucerTest` (2). Suite: 860 run, 0 failed, 6 skipped.

- **"Rumors of My Death . . ."** (ust/65). Script on the existing per-printing `Watermark_` property: the cost
  exiles `Permanent.YouCtrl+Watermark_leagueofdastardlydoom` (itself included: it has that watermark), the
  effect returns a `Permanent.YouOwn` card with it from the graveyard, chosen on resolution. Contraptions count
  (the Unstable FAQ treats their faction symbol as a watermark, and Scryfall records it). No Scryfall rulings or
  FAQ entry. Watermarks follow the printing, so Hoisted Hireling's Unsanctioned reprint (no watermark) doesn't
  qualify.
  - AI (`ChangeZoneAi` logic `ExileAndRetFromGrave`): only when the best returnable card is worth more than the
    cheapest permanent it would exile (`ComputerUtilCard.evaluateCardImpact`), in main 2 or at an opponent's end
    step. The cost is paid with that same card (`ComputerUtil.leastValuableToExile`), keeping Rumors unless it's
    the only choice. The default AI had it backwards: it exiled by lowest power (Rumors itself first) and would
    trade down. `RumorsOfMyDeathTest` (4). Suite: 858 run, 0 failed, 6 skipped.

- **Over My Dead Bodies** (ust/63). Graveyard creatures fight on Masterful Ninja's dual residency, with the
  graveyard as the second zone:
  - Declaring: as attackers (the attacking player's graveyard) or blockers (a defending player's, only when a
    graveyard creature attacks them) are declared, creature cards there are put onto the battlefield unnoticed
    and stay listed in the graveyard (`GameAction.enlistGraveyardCombatants`); the ones not declared go straight
    back. One that state-based actions would remove at once (0 toughness, a legend-rule clash) can't be declared
    (ruling). A card in the graveyard since before this turn isn't summoning sick; "creature cards in your
    graveyard have haste" covers the rest.
  - In combat they're treated as on the battlefield (rulings): lords apply, Giant Growth can target them, attack
    triggers fire, and nothing notices them enter or leave. `CantBlockBy` statics and the new card property
    `GraveyardCombatant` keep graveyard and battlefield creatures from blocking each other.
  - Destroyed (lethal damage, Murder) a graveyard creature is just removed from combat; sent anywhere else
    (Unsummon, exile) it goes from the graveyard. Undeathtouch is a `DamageDone` replacement that exiles the
    creature card instead. At end of combat, or once Over My Dead Bodies is gone, they're just dead again (a
    state-based sweep plus `PhaseHandler.endCombat`).
  - New static mode `GraveyardCombat`; `Card.isInZone(Graveyard)` sees the graveyard listing. The residency
    helpers Masterful Ninja used are now zone-generic.
  - The AI attacks and blocks with graveyard creatures through its normal combat code, but when it attacks it
    doesn't foresee the defender's graveyard blockers (they only show up in the block step).
  - `OverMyDeadBodiesTest` (4, one a full AI turn). Suite: 854 run, 0 failed, 6 skipped.

- **Old-Fashioned Vampire** (ust/62), honor system. Whether it's dark outdoors is one fact for the whole game:
  any player may say "It's dark out" (a `Dark Outdoors` effect in the command zone) or "It's light out"
  (removes it), and every Old-Fashioned Vampire reads it (`IsPresent$ Effect.namedDark_Outdoors`). Only the
  claim that changes things is offered. The AI never says, since it can't look outside. No Scryfall rulings
  or FAQ entry. New creature type **Vampyre** (as printed; plural "Vampyres") in `TypeLists`.
  `OldFashionedVampireTest` (2). Suite: 850 run, 0 failed, 6 skipped.

- **Ninja** (ust/61), an augment. "You may activate this card's augment ability any time you could cast an
  instant": a `CastWithFlash` static from hand (`EffectZone$ Hand`) with `ValidSA$ Activated.Augment` (new SA
  property `Augment`). Combined with a host it's "Ninja <host's last word>" (Ninja Kitten), and the host's
  effect finishes "Whenever this creature deals combat damage to a player,". Augmenting an attacking host
  leaves it attacking.
  - `K:Augment:<cost>:<sentence>`: an optional third field replaces the reminder's "Augment only as a
    sorcery." (Ninja: "Augment only as—oh, never mind.").
  - Every augment card printed its raw keyword (`Augment:3 W`) above its augment line in game; it no longer
    does.
  - AI (`AugmentAi`): an augment it may activate at instant speed goes on an unblocked attacking host after
    blockers are declared; it waits while it has a host that would attack, and otherwise augments in main 2
    or at an opponent's end step. `NinjaAugmentTest` (4). Suite: 848 run, 0 failed, 6 skipped.

- **Masterful Ninja** (ust/60). "Reveal this card from your hand: Masterful Ninja is on the battlefield and in your
  hand until end of turn." Built on the dual residency Yet Another Aether Vortex uses, with the hand as the
  second zone: the card really is on the battlefield (a permanent with haste, its pump works, it attacks and
  blocks) and is still listed in its owner's hand (hand size counts it, it can be chosen to discard).
  - Rulings and FAQ: nothing notices it enter or, at end of turn, leave (new `AbilityKey.Unnoticed` move);
    moving it between battlefield and hand does nothing, so Unsummon leaves it in both; destroyed or discarded,
    it's in the graveyard only. Discarding it to hand size at cleanup happens before it goes back (CR 514.1
    before 514.2), as in paper. A control change no longer ends a second residency (this also fixes a stolen
    Vortex permanent dropping out of its owner's library).
  - Not offered: casting it from your hand while it's in both zones (rules-legal; it would leave the
    battlefield for the stack), or revealing it again then (does nothing).
  - AI (`AlsoOnBattlefieldAi`): on its own turn before combat when the Ninja would attack; on an opponent's turn
    once attackers are declared, to block one it beats or when the attack is dangerous. Never otherwise.
  - `MasterfulNinjaTest` (5).

- **Hoisted Hireling** (ust/58), honor system on the Standing Army idiom: its controller lifts it (a `HOISTED`
  counter) and sets it back down with two free abilities, and it has flying while the counter is on it. No
  Scryfall rulings and no entry in the Unstable FAQ. Script only.

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
