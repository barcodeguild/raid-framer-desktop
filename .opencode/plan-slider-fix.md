# Plan: NearbyFilterControls Slider Fix + Well Background + Layout Split

## Issues to Fix

### 1. Slider Thumb Tracking
The `DragLockedSlider` (Material2) commits every value change immediately via `onValueChange`, causing integer truncation jitter when `toFloat()`/`toInt()` round-trips. The SettingsOverlay solves this with a local float state pattern where the slider operates on a float and only commits the converted integer on release.

The BattleGraphOverlay's `CompactThresholdSlider` (Material3) uses `onValueChangeFinished` for debounced commit, but lacks `LocalDragLock` handling which is needed for desktop window drag suppression.

**Solution:** Create a new `FilterSlider` composable in `NearbyFilterControls.kt` that:
- Uses Material2 `Slider` (consistent with existing DragLockedSlider)
- Maintains a local `Float` state for smooth thumb tracking
- Commits the integer-converted value via `onValueChangeFinished` only
- Handles `LocalDragLock` via `interactionSource` collection (same pattern as `DragLockedSlider`)

This gives 1:1 cursor tracking (slider operates on float) + proper window drag suppression + clean integer commit.

### 2. Well Background
Apply the `SettingsSection` card/well pattern:
- `RFColors.CardBackground` (`Color(0xFF1A1A1A)`) fill
- `RoundedCornerShape(10.dp)` 
- `1.dp` border with `RFColors.CardBorder` (`Color(0xFF2A2A2A)`)
- `16.dp` inner padding
- Optional title at top

### 3. Layout Split
In each tab, wrap `NearbyFilterControls` in a `Row` where:
- Left column (weight 1f): The filter well
- Right column (weight 1f): Empty `Box` placeholder for future content

---

## Implementation

### Step 1: Rewrite NearbyFilterControls.kt

**File:** `composeApp/src/desktopMain/kotlin/com/reoky/raidframer/ui/component/NearbyFilterControls.kt`

Add a private `FilterSlider` composable that combines:
- `DragLockedSlider`'s `LocalDragLock` pattern
- SettingsOverlay's local float state pattern
- `onValueChangeFinished` for clean integer commit

Wrap the entire component in a well-styled container (background + border + rounded corners).

### Step 2: Update RaidOverlay tabs

**File:** `composeApp/src/desktopMain/kotlin/com/reoky/raidframer/ui/overlay/RaidOverlay.kt`

In NearbyTab, NearbyGearTab, and CompositionTab:
- Wrap `NearbyFilterControls` in a `Row` with two columns
- Left column contains the filter well
- Right column is an empty `Box` placeholder

---

## Files Modified

| File | Change |
|------|--------|
| `ui/component/NearbyFilterControls.kt` | Add `FilterSlider`, add well background, restructure layout |
| `ui/overlay/RaidOverlay.kt` | Wrap controls in split-row layout in 3 tabs |

## Verification

- Build and verify no errors
- Slider thumb should track cursor 1:1 with no jitter
- Well background should match SettingsOverlay style
- Filter controls should only take half the screen width
