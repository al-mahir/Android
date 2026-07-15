# Design System Module (`designsystem`)

A standalone Android library module that owns every visual primitive used across the app: color tokens, typography, dimensions, shapes, locale handling, and the reusable Compose components built on top of them. Feature modules consume this module to stay visually consistent and to inherit light/dark + LTR/RTL behavior for free.

- **Namespace:** `com.example.designsystem`
- **Type:** `com.android.library` + Jetpack Compose
- **min SDK:** 24 / **compile SDK:** 36
- **Public dependency exposed via `api`:** `lottie-compose` (button loaders rely on Lottie animations)

## Module Layout

```
designsystem/src/main/java/com/example/designsystem/
├── color/                  # Color tokens + light/dark schemes
│   ├── ColorScheme.kt
│   └── defaultColors.kt
├── dimensions/             # Spacing, shapes, sizes
│   └── Dimensions.kt
├── typo/                   # Typography tokens + font family
│   ├── SPTextStyle.kt
│   ├── Typography.kt
│   └── fontfamily.kt
├── theme/                  # Theme entry point + token accessor
│   ├── SPTheme.kt          # `SpTheme { ... }` composable wrapper
│   └── Theme.kt            # `Theme.colors / typography / spacing / shapes / size`
├── locale/                 # Per-tree locale override (no global state)
│   └── LocalizedContext.kt
└── components/             # Reusable UI components
    ├── button/
    │   ├── baseButton.kt           # Internal BaseButton — shared layout/state
    │   ├── PrimaryButton.kt
    │   ├── SecondaryButton.kt
    │   ├── IconButton.kt
    │   ├── LottieButtonLoader.kt   # Tinted Lottie loading indicator
    │   ├── ButtonShowcaseScreen.kt # Interactive on-device showcase
    │   └── ButtonPreview.kt        # @Preview matrix (light/dark × LTR/RTL)
    ├── textfield/
    │   ├── TextField.kt
    │   ├── TextFieldShowcaseScreen.kt
    │   └── TextFieldPreview.kt
    ├── card/
    │   ├── SectionedCard.kt              # Slotted header/body/footer card
    │   ├── SectionedCardPreview.kt
    │   ├── ExpandableSection.kt          # Collapsible header + body card
    │   ├── ExpandableSectionPreview.kt
    │   ├── ExpandableAccentCard.kt       # Collapsible card with start accent bar
    │   ├── ExpandableAccentCardPreview.kt
    │   └── SettingsActionCard.kt
    ├── tab/
    │   ├── DayTabRow.kt                  # Horizontally-scrollable selectable tabs
    │   └── DayTabRowPreview.kt
    ├── overlay/animated/
    │   ├── AnimatedStatusOverlay.kt              # `StatusOverlay(state, onDismiss)`
    │   ├── OverlayState.kt                       # `Loading` / `Success(message?)` / `Error(message?)`
    │   ├── AnimatedStatusOverlayShowcaseScreen.kt
    │   └── AnimatedStatusOverlayPreview.kt
    ├── placeholderscreens/
    │   ├── StandardEmptyState.kt                 # `EmptyState` (slotted) + `StandardEmptyState`
    │   ├── PlaceholderInternals.kt               # Tinted icon + title/description helpers
    │   ├── NetworkErrorScreen.kt
    │   ├── EmptySearchScreen.kt
    │   ├── EmptyDataScreen.kt
    │   ├── PlaceholderScreensShowcaseScreen.kt
    │   └── PlaceholderScreensPreview.kt
    ├── utils/
    │   └── LottiePlayer.kt                       # Internal Lottie wrapper
    └── preview/
        └── PreviewHelperComposable.kt
```

## Theme Entry Point

Wrap any UI tree with `SpTheme` once (typically at the app root). It installs the locale, layout direction (RTL for `ar`/`fa`/`he`/`iw`/`ur`), color scheme, typography, spacing, shapes, and font family for that subtree.

```kotlin
SpTheme(
    isDarkTheme = isSystemInDarkTheme(),
    locale      = Locale("ar"),
) {
    // app content
}
```

Inside any composable, read tokens through the singleton `Theme`:

```kotlin
Theme.colors.primary
Theme.typography.body.large
Theme.spacing.medium
Theme.shapes.medium
Theme.size.iconMedium
```

`SpTheme` rebuilds a `LocalContext` / `LocalConfiguration` scoped to the chosen `Locale` via `Context.localizedContext(...)` — so resources resolve in the requested language without mutating global app config.

## Design Tokens

### Colors — `ColorScheme`

Semantic, role-based color tokens (no raw hex in feature code). Two ready-made schemes ship: `lightColors` and `darkColors`.

| Token | Role |
|---|---|
| `primary` / `onPrimary` | Brand color and content drawn on top of it |
| `secondary` / `onSecondary` | Secondary brand color |
| `backGround` | Screen background |
| `surface` / `onSurface` | Cards, sheets, elevated surfaces |
| `surfaceVariant` | Tinted card container behind a white body band (e.g. `SectionedCard`) |
| `primaryFont` / `secondaryFont` | Default text colors |
| `hint` | Placeholder text, dividers, neutral borders |
| `warning` / `onWarning` | Caution states |
| `error` / `onError` | Validation/failure states |
| `success` / `onSuccess` | Confirmation states |
| `disable` / `onDisable` | Disabled containers and content |

### Spacing — `SPSpacing`

`default = 0`, `extraSmall = 4dp`, `small = 8dp`, `medium = 16dp`, `large = 24dp`, `extraLarge = 32dp`.

### Shapes — `SPShapes`

`small = RoundedCornerShape(4dp)`, `medium = 8dp`, `large = 16dp`, `circle = 50%`.

### Sizes — `SPSize`

Icon sizes (`iconSmall = 12dp`, `iconMedium = 24dp`, `iconLarge = 32dp`), component heights (`componentsNormalHeight = 48dp`, `componentsLargeHeight = 56dp`), and overlay sizing (`overlayContainer = 160dp`, `overlayIndicator = 80dp`).

### Typography — `SPTextStyle`

Tied to `arabicFontFamily` (URW Geometric Arabic — Regular / Medium / SemiBold).

| Token | Size / Weight |
|---|---|
| `display` | 32sp Bold, 42sp line height |
| `title` | 24sp SemiBold, 32sp line height |
| `body.large` / `medium` / `small` | 16/14/12 sp Normal |
| `hint.large` / `medium` / `small` | 18/14/12 sp Light |

`spTypographyOf(fontFamily)` rebuilds the stack against any other `FontFamily` — `SpTheme(fontFamily = …)` re-derives typography automatically.

## Components

All components consume `Theme.*` tokens, work in dark/light + LTR/RTL, and ship paired `*ShowcaseScreen` (interactive, on-device) and `*Preview` (Android Studio `@Preview` matrix) files.

### Buttons

- **`BaseButton`** *(internal)* — single 58dp-tall row that owns the disabled/loading/border/icon layout shared by every variant.
- **`PrimaryButton`** — filled with `Theme.colors.primary`; supports `iconPainter`, `isDisabled`, `isLoading` (defaults to `R.raw.button_loading` Lottie).
- **`SecondaryButton`** — outlined variant with primary border on background fill.
- **`IconButton`** — square 58dp icon-only button.
- **`LottieButtonLoader`** *(internal)* — tints any Lottie raw-res via `STROKE_COLOR` + `COLOR` dynamic properties so loaders match the button's content color.

### Text Field

`TextField` (currently `internal`) is a single building block covering: title, tip text, leading/trailing icons (with optional clicks), focus border, error border + message + icon, disabled, read-only, single-line and multi-line modes — all from one parameter list. Wired to design tokens (`Theme.size.componentsNormalHeight`, `Theme.shapes.medium`, `Theme.spacing.small`, `Theme.colors.{primary,error,hint,onDisable}`).

### Animated Status Overlay

`StatusOverlay(state, onDismiss, autoDismissTimeoutMillis = 4_000)` renders a modal Lottie-driven status indicator over the current screen. Driven by an `OverlayState?` value — pass `null` to hide.

```kotlin
sealed interface OverlayState {
    data object Loading : OverlayState
    data class Success(val message: String? = null) : OverlayState
    data class Error(val message: String? = null) : OverlayState
}
```

Behavior:
- `Loading` blocks back-press and tap-outside dismissal; back-press triggers a haptic `LongPress` so the user knows the press registered.
- `Success` / `Error` auto-dismiss when the Lottie animation finishes (via `onAnimationFinished`); a backstop timer (`autoDismissTimeoutMillis`) guarantees they don't stick if the callback never fires.
- The optional `message` on `Success` / `Error` is rendered below the animation and announced by accessibility services in place of the generic state label (live-region `Polite`).
- Sized via `Theme.size.overlayContainer` (min container) and `Theme.size.overlayIndicator` (Lottie size).
- Lottie raw assets ship in `res/raw/anim_loading.json`, `anim_success.json`, `anim_error.json`.

`LottiePlayer` is an internal wrapper around Lottie Compose. It guards against the `isAtEnd == true` race that fires while the composition is still loading (Lottie reports `endProgress = 0f` until the JSON is parsed), and uses `rememberUpdatedState` so the latest `onAnimationFinished` lambda is always invoked.

MVI integration: hold an `OverlayState?` field in your screen state; map domain results into `Loading` / `Success` / `Error` from the ViewModel; dispatch a `DismissStatus` intent from `onDismiss` rather than mutating UI state.

### Placeholder Screens

Full-screen empty/error states built on the slotted `EmptyState` (internal) and ship with monochrome vector drawables that are tinted at the call site so they follow `Theme.colors.hint` in light & dark.

| Screen | When to use | Default action |
|---|---|---|
| `NetworkErrorScreen(onRetry = { … })` | Network request failed; user can retry. | Retry button (visible when `onRetry` is non-null). |
| `EmptySearchScreen(...)` | Search returned zero results. | Optional `actionButtonText` + `onActionClick` (e.g. "Clear search"). |
| `EmptyDataScreen(...)` | Fetch succeeded but the dataset is empty. | Optional CTA (e.g. "Create new"). |

All three accept `title`/`description` overrides — the defaults pull from `R.string.placeholder_*` so they localize automatically. Builders for new placeholder variants should:

1. Call `EmptyState` (the slotted internal) directly so the icon can be `PlaceholderIcon(resId)` (tinted via `ColorFilter.tint(Theme.colors.hint)`).
2. Use `PlaceholderTitle` / `PlaceholderDescription` for consistent typography.
3. Add a `placeholder_<name>_*` string entry (en + ar) and a 24×24 monochrome vector drawable (paths in `#000000`; tinting happens at runtime).

The legacy `StandardEmptyState(title, iconRes, …)` is still available for ad-hoc placeholders that ship their own coloured illustration and don't need theme-aware tinting.

### Cards — `components/card/`

`SectionedCard` is a slotted card laid out as stacked sections: an optional `header` and
`footer` painted on `containerColor` (`Theme.colors.surfaceVariant` by default), wrapping a
`body` band painted with `bodyColor` (`Theme.colors.backGround`). Each section is a
caller-supplied `@Composable` — the component owns only the card shape, elevation, and the
section bands. Two overloads: the base (`header` + `body`) and one that adds a `footer`
section; both put `body` as the trailing content lambda. An optional `bodyWatermark: Painter`
paints a faint brand logo behind the body band, clipped to it.

`ExpandableSection` is a collapsible variant: a `surfaceVariant` header (title + a circular
rotating chevron) over a white body shown via `AnimatedVisibility` only while `expanded`.
Expansion state is hoisted (`expanded` + `onToggle`); collapsed, the card is just the header.

`ExpandableAccentCard` is a collapsible card with a coloured vertical accent bar on the
layout start edge — the meal-row / nutrition-group pattern. Caller supplies `header` and
`body` slots so the row can carry an icon + title + subtitle + meta as needed; the chevron
sits at the row's end.

`SettingsActionCard` is the older, purpose-built settings row.

### Tabs — `components/tab/`

`DayTabRow(labels, selectedIndex, onSelect)` is a horizontally-scrollable row of pill-
shaped tabs: the selected tab fills with `Theme.colors.primary`, the others sit on
`surfaceVariant` with a hairline border. RTL-safe by default. Use it for any day-of-week or
similar one-of-N text selector.

### Previews & Showcase

- `*Preview.kt` files emit the full Light/Dark × LTR/RTL matrix using Android Studio's `@Preview` system, grouped by component.
- `*ShowcaseScreen.kt` files render every state of every variant on a real screen with toggle chips for `isDisabled` / `isLoading` / `isError` / `readOnly`. Useful when iterating on the design or QA-ing on a device.
- `PreviewHelperComposable` centers content with 16dp padding so previews don't render edge-to-edge.

## Localization

`Context.localizedContext(locale)` (`com.example.core.designsystem.locale`) returns a `ContextWrapper` whose resources, assets, and `createConfigurationContext` calls all resolve in the supplied locale. `SpTheme` calls this internally — feature code shouldn't need it directly. Layout direction is derived from the locale and provided through `LocalLayoutDirection`.

## Adding a New Component

1. Create the component under `components/<name>/`.
2. Read all colors, sizes, spacing, shapes, and text styles from `Theme.*` — never hardcode `Color(...)` or raw `dp`/`sp` values that belong in tokens. If a token is missing, add it to the relevant data class in `color/`, `dimensions/`, or `typo/` first.
3. Add a `<Name>Preview.kt` covering Light/Dark × LTR/RTL.
4. Add a `<Name>ShowcaseScreen.kt` if the component has interactive states worth toggling on-device.
5. If a token needs to differ at the call site, expose it as a parameter with a `Theme.*` default — never bypass the theme.
