# Example: Adding Mana Potions

This is a practical example showing how to extend the potion system to track mana potions.

## Step 1: Add String Resources

First, add to your string resources (assuming you have a similar structure):

```xml
<!-- values/strings.xml -->
<string name="potion_name_minor_mana_rank_one">Minor Mana Potion (Rank 1)</string>
<string name="potion_name_minor_mana_rank_two">Minor Mana Potion (Rank 2)</string>
<string name="potion_name_mana">Mana Potion</string>
```

## Step 2: Update PotionDefinition.kt

Add the imports and enum entries:

```kotlin
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_mana_rank_one
import raid_framer_desktop.composeapp.generated.resources.potion_name_minor_mana_rank_two
import raid_framer_desktop.composeapp.generated.resources.potion_name_mana

enum class PotionDefinition(...) : PotionItem {
  // ... existing health potions ...
  
  // Mana Potions
  MinorManaRankOne(35235, 21.0, Res.string.potion_name_minor_mana_rank_one, listOf("Minor Mana Potion")),
  MinorManaRankTwo(46260, 21.0, Res.string.potion_name_minor_mana_rank_two, listOf("Minor Mana Potion")),
  ManaPotion(35237, 21.0, Res.string.potion_name_mana, listOf("Mana Potion")),
  ;
}
```

## Step 3: That's It!

The system automatically:
- ✅ Counts mana potion uses in `sessionPotionTotal`
- ✅ Detects by spell ID or name
- ✅ Works with cooldown tracking if you use `withPotionTracking()`

## Advanced: Track Potion Types Separately

If you want to track health and mana potions separately:

### Option A: Add to PlayerCard.kt

```kotlin
data class PlayerCard(
  // ... existing fields ...
  val sessionPotionTotal: Int = 0,
  val sessionHealthPotionTotal: Int = 0,  // ← New
  val sessionManaPotionTotal: Int = 0,    // ← New
)
```

### Option B: Add Category to PotionItem

```kotlin
enum class PotionCategory {
  HEALTH, MANA, BUFF, DEBUFF
}

interface PotionItem {
  val skillId: Int
  val cooldown: Double
  val friendlyNameRes: StringResource
  val possibleSpellNames: List<String>
  val category: PotionCategory  // ← New
}

enum class PotionDefinition(...) : PotionItem {
  MinorHealthRankOne(35234, 21.0, Res.string.potion_name_minor_healing_rank_one, 
    listOf("Minor Healing Potion"), PotionCategory.HEALTH),
  MinorManaRankOne(35235, 21.0, Res.string.potion_name_minor_mana_rank_one, 
    listOf("Minor Mana Potion"), PotionCategory.MANA),
}
```

### Option C: Update PlayerCardExtensions.kt

```kotlin
fun PlayerCard.postSuccessfulCastEvent(event: SuccessfulCastEvent): PlayerCard {
  val matchedPotion = findPotionByEvent(event)
  val shouldIncrementPotionUses = matchedPotion != null
  
  // Category-specific tracking
  val isHealthPotion = matchedPotion?.category == PotionCategory.HEALTH
  val isManaPotion = matchedPotion?.category == PotionCategory.MANA
  
  val isMarasNineTails = event.spell == "Charm (Rider Skill)" && 
    (PlayerCacheInteractor.isRealPlayer(event.target) || RFConfig.state.value.allowPVEDamage)
  
  val card = this.copiedWithUtilityItemDetectionMiddleWare(event)
  return card.copy(
    lastEvent = event.timestamp,
    cache = cache?.copy(
      lastSeen = event.timestamp,
      lifetimeTotalCharms = if (isMarasNineTails) cache.lifetimeTotalCharms + 1 else cache.lifetimeTotalCharms
    ),
    sessionPotionTotal = if (shouldIncrementPotionUses) this.sessionPotionTotal + 1 else this.sessionPotionTotal,
    sessionHealthPotionTotal = if (isHealthPotion) this.sessionHealthPotionTotal + 1 else this.sessionHealthPotionTotal,
    sessionManaPotionTotal = if (isManaPotion) this.sessionManaPotionTotal + 1 else this.sessionManaPotionTotal,
    sessionCharmTotal = if (isMarasNineTails) this.sessionCharmTotal + 1 else this.sessionCharmTotal,
    recentCastSuccessfulCastEvents = (this.recentCastSuccessfulCastEvents + event)
  )
}
```

## Real-World Spell IDs

You'll need to find the actual spell IDs from your combat logs. Look for entries like:

```
SPELL_CAST_SUCCESS,Player-123,"PlayerName",0x511,0x0,0x0,35235,"Minor Mana Potion"
```

The spell ID is the number before the spell name (35235 in this example).

## Testing

To verify your potions are being detected:

```kotlin
// In your test or debug code
fun testPotionDetection() {
  val event = SuccessfulCastEvent(
    timestamp = System.currentTimeMillis(),
    spell = "Minor Mana Potion",
    spellId = 35235,
    target = "PlayerName"
  )
  
  val potion = findPotionByEvent(event)
  println("Detected: ${potion?.friendlyNameRes}") // Should print mana potion
}
```

