package com.reoky.raidframer.core.definitions

import com.reoky.raidframer.core.database.PlayerCacheEntity
import com.reoky.raidframer.core.database.incrementPackedItemUsage
import com.reoky.raidframer.core.model.PlayerCard
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.bd_2h_sword
import raid_framer_desktop.composeapp.generated.resources.bd_bow
import raid_framer_desktop.composeapp.generated.resources.bd_club
import raid_framer_desktop.composeapp.generated.resources.bd_rifle
import raid_framer_desktop.composeapp.generated.resources.bd_shield
import raid_framer_desktop.composeapp.generated.resources.bd_staff
import raid_framer_desktop.composeapp.generated.resources.bd_sword
import raid_framer_desktop.composeapp.generated.resources.garden_anth_set
import raid_framer_desktop.composeapp.generated.resources.halcy_neck
import raid_framer_desktop.composeapp.generated.resources.honor_nodachi
import raid_framer_desktop.composeapp.generated.resources.jola_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_sungold_anth_set_pull
import raid_framer_desktop.composeapp.generated.resources.item_name_bd_2h_sword
import raid_framer_desktop.composeapp.generated.resources.item_name_bd_bow
import raid_framer_desktop.composeapp.generated.resources.item_name_bd_club
import raid_framer_desktop.composeapp.generated.resources.item_name_bd_gun
import raid_framer_desktop.composeapp.generated.resources.item_name_bd_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_bd_staff
import raid_framer_desktop.composeapp.generated.resources.item_name_bd_sword
import raid_framer_desktop.composeapp.generated.resources.item_name_mist_nodachi
import raid_framer_desktop.composeapp.generated.resources.item_name_garden_anth_set_pull
import raid_framer_desktop.composeapp.generated.resources.item_name_halcy_neck
import raid_framer_desktop.composeapp.generated.resources.item_name_honor_nodachi
import raid_framer_desktop.composeapp.generated.resources.item_name_jola_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_kraken_scepter
import raid_framer_desktop.composeapp.generated.resources.item_name_kraken_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_kraken_spear
import raid_framer_desktop.composeapp.generated.resources.item_name_lib_bow
import raid_framer_desktop.composeapp.generated.resources.item_name_lib_dagger
import raid_framer_desktop.composeapp.generated.resources.item_name_lib_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_lib_shortspear
import raid_framer_desktop.composeapp.generated.resources.item_name_lib_staff
import raid_framer_desktop.composeapp.generated.resources.item_name_library_greatclub
import raid_framer_desktop.composeapp.generated.resources.item_name_mist_dagger
import raid_framer_desktop.composeapp.generated.resources.item_name_mist_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_serp_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_serp_staff
import raid_framer_desktop.composeapp.generated.resources.item_name_snake_greataxe
import raid_framer_desktop.composeapp.generated.resources.item_name_snake_greatsword
import raid_framer_desktop.composeapp.generated.resources.item_name_snake_gun
import raid_framer_desktop.composeapp.generated.resources.item_name_snake_scepter
import raid_framer_desktop.composeapp.generated.resources.item_name_snake_shield
import raid_framer_desktop.composeapp.generated.resources.item_name_snake_sword
import raid_framer_desktop.composeapp.generated.resources.item_name_soul_neck
import raid_framer_desktop.composeapp.generated.resources.kraken_scepter
import raid_framer_desktop.composeapp.generated.resources.kraken_spear
import raid_framer_desktop.composeapp.generated.resources.kraken_shield
import raid_framer_desktop.composeapp.generated.resources.lib_bow
import raid_framer_desktop.composeapp.generated.resources.lib_dagger
import raid_framer_desktop.composeapp.generated.resources.lib_shield
import raid_framer_desktop.composeapp.generated.resources.lib_shortspear
import raid_framer_desktop.composeapp.generated.resources.lib_staff
import raid_framer_desktop.composeapp.generated.resources.library_greatclub
import raid_framer_desktop.composeapp.generated.resources.mistsong_dagger
import raid_framer_desktop.composeapp.generated.resources.mistsong_nodachi
import raid_framer_desktop.composeapp.generated.resources.mistsong_shield
import raid_framer_desktop.composeapp.generated.resources.regular_anth_set
import raid_framer_desktop.composeapp.generated.resources.serp_shield
import raid_framer_desktop.composeapp.generated.resources.serp_staff
import raid_framer_desktop.composeapp.generated.resources.snake_axe
import raid_framer_desktop.composeapp.generated.resources.snake_greatsword
import raid_framer_desktop.composeapp.generated.resources.snake_gun
import raid_framer_desktop.composeapp.generated.resources.snake_scepter
import raid_framer_desktop.composeapp.generated.resources.snake_shield
import raid_framer_desktop.composeapp.generated.resources.snake_sword
import raid_framer_desktop.composeapp.generated.resources.soul_neck
import raid_framer_desktop.composeapp.generated.resources.unknown

/**
 * Special overloaded enum to identify when a utility skill/item is used by players. We're talking about items like kraken scepter, lib shield, etc that can
 * have a lot of diversity in how we need to handle their recognition and cache updating.
 * skillId: List of possible skill ids for the item spell
 * itemSpecificBuffId: Item specific buffs for the item spell (if any)
 * castTime: Cast time in seconds
 * cooldown: Cooldown in seconds
 * friendlyNameRes: String resource for the item name (for i18n)
 * iconRes: Drawable resource for the item icon (falls back to unknown if missing)
 * packedUsageField: Lambda to read the packed usage counter + timestamp from the cache
 * possibleSpellNames: List of possible spell names for the item spell in the game logs
 * updateCache: Lambda function code that gets executed on the player cache to update any relevant info (like usage counts)
 */
enum class ItemSpell(
  override val itemSpecificSkillIds: List<Int>,
  override val itemSpecificBuffIds: List<Int>,
  override val castTime: Double,
  override val cooldown: Double,
  override val friendlyNameRes: StringResource,
  override val iconRes: DrawableResource,
  override val packedUsageField: (PlayerCacheEntity) -> Long,
  override val possibleSpellNames: List<String>,
  override val updateCard: (PlayerCard) -> PlayerCard = { it }
) : UtilityItem {

  //
  // Kraken Weapons and Shield
  //
  KRAKEN_SCEPTER(
    itemSpecificSkillIds = listOf(36733),
    itemSpecificBuffIds = listOf(), // gives slow and snare but no unique buff id
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_kraken_scepter,
    iconRes = Res.drawable.kraken_scepter,
    packedUsageField = { it.lastKrakenScepter },
    possibleSpellNames = listOf("Desolate Sea Sovereign"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastKrakenScepter = incrementPackedItemUsage(card.cache.lastKrakenScepter))
    )} // lambda to increment the usage count on the cache because most items won't have a cache update
  ),
  KRAKEN_SPEAR(
    itemSpecificSkillIds = listOf(36734), // Desolate Sea Pillager
    itemSpecificBuffIds = listOf(), // gives slow and snare but no unique buff id
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_kraken_spear,
    iconRes = Res.drawable.kraken_spear,
    packedUsageField = { it.lastKrakenSpear },
    possibleSpellNames = listOf("Desolate Sea Pillager"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastKrakenSpear = incrementPackedItemUsage(card.cache.lastKrakenSpear))
    )}
  ),
  KRAKEN_SHIELD(
    itemSpecificSkillIds = listOf(36735), // Desolate Sea Guardianq
    itemSpecificBuffIds = listOf(4846, 4848), // gives slow and snare but no unique buff id, 4848 is the buff id, 4846 is the debuff curse id
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_kraken_shield,
    iconRes = Res.drawable.kraken_shield,
    packedUsageField = { it.lastKrakenShield },
    possibleSpellNames = listOf("Desolate Sea Guardian"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastKrakenShield = incrementPackedItemUsage(card.cache.lastKrakenShield))
    )}
  ),
  HONOR_NODACHI(
    itemSpecificSkillIds = listOf(14975, 14976), // Rank 1 and 2
    itemSpecificBuffIds = listOf(2114, 2113), // Berserk buff
    castTime = 0.0,
    cooldown = 120.0,
    friendlyNameRes = Res.string.item_name_honor_nodachi,
    iconRes = Res.drawable.honor_nodachi,
    packedUsageField = { it.lastHonorNodachi },
    possibleSpellNames = listOf("Honor's Mighty Frenzied Nodachi", "Honor's Frenzied Nodachi"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastHonorNodachi = incrementPackedItemUsage(card.cache.lastHonorNodachi))
    )}
  ),


  //
  // JMG Items
  //
  JOLAS_GRUDGE(
    itemSpecificSkillIds = listOf(23954),
    itemSpecificBuffIds = listOf(7018),
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_jola_shield,
    iconRes = Res.drawable.jola_shield,
    packedUsageField = { it.lastJolaShield },
    possibleSpellNames = listOf("Jola's Grudge"),
    updateCard = { card -> card.copy(
      sessionCCTotal = card.sessionCCTotal + 1, // I guess this counts as cc
      cache = card.cache?.copy(lastJolaShield = incrementPackedItemUsage(card.cache.lastJolaShield))
    )}
  ),

  //
  // Our nation's many necklace ~
  //
  HALCY_NECKLACE(
    itemSpecificSkillIds = listOf(38036), // the skill that applies the buff
    itemSpecificBuffIds = listOf(6765, 6766, 6767, 15077, 15078, 17361, 22111), // rank 2 - 8 of deliverance shield
    castTime = 0.0,
    cooldown = 120.0,
    friendlyNameRes = Res.string.item_name_halcy_neck,
    iconRes = Res.drawable.halcy_neck,
    packedUsageField = { it.lastHalcyNecklace },
    possibleSpellNames = listOf("Halcyon's Spiritual Necklace"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastHalcyNecklace = incrementPackedItemUsage(card.cache.lastHalcyNecklace))
    )}
  ),
  SOUL_NECKLACE(
    itemSpecificSkillIds = listOf(43400, 43402, 43403, 43404, 43405, 43406, 43407, 43408),
    itemSpecificBuffIds = listOf(25825, 25826, 25827, 25828, 25829, 25830, 25831, 25832), // ranks 1 - 8 of soul necklace toughen debuff
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_soul_neck,
    iconRes = Res.drawable.soul_neck,
    packedUsageField = { it.lastSoulNecklace },
    possibleSpellNames = listOf("Soulbinder's Necklace"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSoulNecklace = incrementPackedItemUsage(card.cache.lastSoulNecklace))
    )}
  ),

  //
  // Snake Weapons and Shield
  //
  SNAKE_GREATSOWRD(
    itemSpecificSkillIds = listOf(45765, 45886), // Soulslake Edge, Eminent Soulslake Edge
    itemSpecificBuffIds = listOf(27821, 27822, 27949),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_snake_greatsword,
    iconRes = Res.drawable.snake_greatsword,
    packedUsageField = { it.lastSnakeGreatsword },
    possibleSpellNames = listOf("Soulslake Edge", "Eminent Soulslake Edge"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSnakeGreatsword = incrementPackedItemUsage(card.cache.lastSnakeGreatsword))
    )}
  ),
  SNAKE_SHIELD(
    itemSpecificSkillIds = listOf(45798, 45925), // Soulslake Bulwark, Eminent Soulslake Bulwark
    itemSpecificBuffIds = listOf(27892, 27893, 27894, 27976, 27977),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_snake_shield,
    iconRes = Res.drawable.snake_shield,
    packedUsageField = { it.lastSnakeShield },
    possibleSpellNames = listOf("Soulslake Bulwark", "Eminent Soulslake Bulwark"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSnakeShield = incrementPackedItemUsage(card.cache.lastSnakeShield))
    )}
  ),
  SNAKE_SWORD(
    itemSpecificSkillIds = listOf(45889, 45892), // Soulslake Razor, Eminent Soulslake Razor
    itemSpecificBuffIds = listOf(27898, 27900, 27952),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_snake_sword,
    iconRes = Res.drawable.snake_sword,
    packedUsageField = { it.lastSnakeSword },
    possibleSpellNames = listOf("Soulslake Razor", "Eminent Soulslake Razor"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSnakeSword = incrementPackedItemUsage(card.cache.lastSnakeSword))
    )}
  ),
  SNAKE_AXE(
    itemSpecificSkillIds = listOf(45919, 45920), // Soulslake Cleaver, Eminent Soulslake Cleaver
    itemSpecificBuffIds = listOf(27972, 27973),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_snake_greataxe,
    iconRes = Res.drawable.snake_axe,
    packedUsageField = { it.lastSnakeAxe },
    possibleSpellNames = listOf("Cleaver Target"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSnakeAxe = incrementPackedItemUsage(card.cache.lastSnakeAxe))
    )}
  ),
  SNAKE_SCEPTER(
    itemSpecificSkillIds = listOf(45938, 45939), // Soulslake Leech, Eminent Soulslake Leech
    itemSpecificBuffIds = listOf(27906, 27908, 27981),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_snake_scepter,
    iconRes = Res.drawable.snake_scepter,
    packedUsageField = { it.lastSnakeScepter },
    possibleSpellNames = listOf("Soulslake Leech", "Eminent Soulslake Leech"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSnakeScepter = incrementPackedItemUsage(card.cache.lastSnakeScepter))
    )}
  ),
  SNAKE_GUN(
    itemSpecificSkillIds = listOf(47007, 47061), // Soulslake Bullet, Eminent Soulslake Bullet
    itemSpecificBuffIds = listOf(28878, 28906),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_snake_gun,
    iconRes = Res.drawable.snake_gun,
    packedUsageField = { it.lastSnakeGun },
    possibleSpellNames = listOf("Soulslake Bullet", "Eminent Soulslake Bullet"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSnakeGun = incrementPackedItemUsage(card.cache.lastSnakeGun))
    )}
  ),

  //
  // Black Dragon related items
  //
  BD_SHIELD(
    itemSpecificSkillIds = listOf(40538, 44634), // Black Dragon Fireguard , Ferocious Black Dragon Fireguard
    itemSpecificBuffIds = listOf(23931, 26723), // Black Dragon's Self-Recovery , Ferocious Black Dragon's Self-Recovery
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_bd_shield,
    iconRes = Res.drawable.bd_shield,
    packedUsageField = { it.lastBdShield },
    possibleSpellNames = listOf("Black Dragon's Self-Recovery", "Ferocious Black Dragon's Self-Recovery"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastBdShield = incrementPackedItemUsage(card.cache.lastBdShield))
    )}
  ),
  BD_CLUB(
    itemSpecificSkillIds = listOf(40541, 44638), // Black Dragon's Serenity, Ferocious Black Dragon's Serenity
    itemSpecificBuffIds = listOf(23709, 26726), // Black Dragon's Serenity, Ferocious Black Dragon's Serenity
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_bd_club,
    iconRes = Res.drawable.bd_club,
    packedUsageField = { it.lastBdClub },
    possibleSpellNames = listOf("Black Dragon's Serenity", "Ferocious Black Dragon's Serenity"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastBdClub = incrementPackedItemUsage(card.cache.lastBdClub))
    )}
  ),
  BD_BOW(
    itemSpecificSkillIds = listOf(40539, 44635), // Black Dragon's Windsong, Ferocious Black Dragon's Wingbeat
    itemSpecificBuffIds = listOf(),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_bd_bow,
    iconRes = Res.drawable.bd_bow,
    packedUsageField = { it.lastBdBow },
    possibleSpellNames = listOf("Black Dragon's Windsong", "Ferocious Black Dragon's Wingbeat"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastBdBow = incrementPackedItemUsage(card.cache.lastBdBow))
    )}
  ),
  BD_RIFLE(
    itemSpecificSkillIds = listOf(47028, 47029), // Black Dragon's Breath, Ferocious Black Dragon's Bite
    itemSpecificBuffIds = listOf(28873, 28890), // Black Dragon's Breath, Ferocious Black Dragon's Bite
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_bd_gun,
    iconRes = Res.drawable.bd_rifle,
    packedUsageField = { it.lastBdRifle },
    possibleSpellNames = listOf("Black Dragon's Breath", "Ferocious Black Dragon's Bite"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastBdRifle = incrementPackedItemUsage(card.cache.lastBdRifle))
    )}
  ),
  BD_STAFF(
    itemSpecificSkillIds = listOf(40543), // Black Dragon's Meteor Strike
    itemSpecificBuffIds = listOf(),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_bd_staff,
    iconRes = Res.drawable.bd_staff,
    packedUsageField = { it.lastBdStaff },
    possibleSpellNames = listOf("Black Dragon's Meteor Strike"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastBdStaff = incrementPackedItemUsage(card.cache.lastBdStaff))
    )}
  ),
  BD_SWORD(
    itemSpecificSkillIds = listOf(40537, 44633), // Black Dragon's Breath, Ferocious Black Dragon's Breath
    itemSpecificBuffIds = listOf(),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_bd_sword,
    iconRes = Res.drawable.bd_sword,
    packedUsageField = { it.lastBdSword },
    possibleSpellNames = listOf(),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastBdSword = incrementPackedItemUsage(card.cache.lastBdSword))
    )}
  ),
  BD_2H_SWORD(
    itemSpecificSkillIds = listOf(40540, 44636), // Black Dragon's Fury, Ferocious Black Dragon's Fury
    itemSpecificBuffIds = listOf(),
    castTime = 0.0,
    cooldown = 60.0,
    friendlyNameRes = Res.string.item_name_bd_2h_sword,
    iconRes = Res.drawable.bd_2h_sword,
    packedUsageField = { it.lastBd2hSword },
    possibleSpellNames = listOf("Black Dragon's Fury"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastBd2hSword = incrementPackedItemUsage(card.cache.lastBd2hSword))
    )}
  ),

  //
  // Anthalon Arche Items
  //
  ANTH_SET_PULL(
    itemSpecificSkillIds = listOf(19063), // Necromantic Flame
    itemSpecificBuffIds = listOf(4272), // Necromantic Flame - Pull #2
    castTime = 2.0,
    cooldown = 180.0,
    friendlyNameRes = Res.string.item_name_sungold_anth_set_pull,
    iconRes = Res.drawable.regular_anth_set,
    packedUsageField = { it.lastAnthSetPull },
    possibleSpellNames = listOf("Necromantic Flame"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastAnthSetPull = incrementPackedItemUsage(card.cache.lastAnthSetPull))
    )}
  ),
  GARDEN_ANTH_SET_PULL(
    itemSpecificSkillIds = listOf(44652), // Necromantic Flame
    itemSpecificBuffIds = listOf(25075), // Necromantic Flame - Pull #3
    castTime = 2.0,
    cooldown = 180.0,
    friendlyNameRes = Res.string.item_name_garden_anth_set_pull,
    iconRes = Res.drawable.garden_anth_set,
    packedUsageField = { it.lastGardenAnthSetPull },
    possibleSpellNames = listOf("Necromantic Flame"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastGardenAnthSetPull = incrementPackedItemUsage(card.cache.lastGardenAnthSetPull))
    )}
  ),

  //
  // Library Dungeon Items
  //
  LIB_BOW(
    itemSpecificSkillIds = listOf(39450, 41232, 45106), // Disciple's, Radiant Disciple's and Immortal Warden's Bow
    itemSpecificBuffIds = listOf(23212, 27127), // Bleeding from bow
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_lib_bow,
    iconRes = Res.drawable.lib_bow,
    packedUsageField = { it.lastLibBow },
    possibleSpellNames = listOf("Disciple's Bow", "Radiant Disciple's Bow", "Immortal Warden's Bow"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastLibBow = incrementPackedItemUsage(card.cache.lastLibBow))
    )}
  ),
  LIB_DAGGER(
    itemSpecificSkillIds = listOf(39446, 41228, 45097), // Immortal Warden's Dagger
    itemSpecificBuffIds = listOf(23193, 27121), // DEADLY POISON BUFF
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_lib_dagger,
    iconRes = Res.drawable.lib_dagger,
    packedUsageField = { it.lastLibDagger },
    possibleSpellNames = listOf("Immortal Warden's Dagger"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastLibDagger = incrementPackedItemUsage(card.cache.lastLibDagger))
    )}
  ),
  LIB_SHORTSPEAR(
    itemSpecificSkillIds = listOf(339448, 41230, 45104), // Disciple's, Radiant Disciple's and Immortal Warden's Shortspear
    itemSpecificBuffIds = listOf(23192), // Provoked
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_lib_shortspear,
    iconRes = Res.drawable.lib_shortspear,
    packedUsageField = { it.lastLibShortspear },
    possibleSpellNames = listOf("Immortal Warden's Shortspear"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastLibShortspear = incrementPackedItemUsage(card.cache.lastLibShortspear))
    )}
  ),
  LIB_STAFF(
    itemSpecificSkillIds = listOf(39462, 41231, 45105), // Disciple's, Radiant Disciple's and Immortal Warden's Staff
    itemSpecificBuffIds = listOf(27124),
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_lib_staff,
    iconRes = Res.drawable.lib_staff,
    packedUsageField = { it.lastLibStaff },
    possibleSpellNames = listOf("Immortal Warden's Staff"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastLibStaff = incrementPackedItemUsage(card.cache.lastLibStaff))
    )}
  ),
  LIB_GREATCLUB(
    itemSpecificSkillIds = listOf(45099, 39452, 41234),
    itemSpecificBuffIds = listOf(23216, 24395),
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_library_greatclub,
    iconRes = Res.drawable.library_greatclub,
    packedUsageField = { it.lastGreatclub },
    possibleSpellNames = listOf("Disciple's Greatclub", "Immortals's Greatclub"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastGreatclub = incrementPackedItemUsage(card.cache.lastGreatclub))
    )}
  ),
  LIB_SHIELD(
    itemSpecificSkillIds = listOf(39451),
    itemSpecificBuffIds = listOf(),
    castTime = 4.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_lib_shield,
    iconRes = Res.drawable.lib_shield,
    packedUsageField = { it.lastLibShieldPull },
    possibleSpellNames = listOf("Disciple's Shield", "Immortal Warden's Shield"),
    updateCard = { card -> card.copy(
      sessionCCTotal = card.sessionCCTotal + 1, // allows us to count lib shield as cc points even though there's no visible debuff
      cache = card.cache?.copy(lastLibShieldPull = incrementPackedItemUsage(card.cache.lastLibShieldPull))
    )} // lambda to increment the usage count on the cache because most items won't have a cache update
  ),

  //
  // Serpentis Dungeon Items
  //
  SERP_STAFF(
    itemSpecificSkillIds = listOf(22438),
    itemSpecificBuffIds = listOf(6136), // Corrupted Wit
    castTime = 0.0,
    cooldown = 180.0,
    friendlyNameRes = Res.string.item_name_serp_staff,
    iconRes = Res.drawable.serp_staff,
    packedUsageField = { it.lastSerpStaff },
    possibleSpellNames = listOf("Corrupted Wit"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSerpStaff = incrementPackedItemUsage(card.cache.lastSerpStaff))
    )}
  ),
  SERP_SHIELD(
    itemSpecificSkillIds = listOf(22445),
    itemSpecificBuffIds = listOf(6148), // Blooddrinker Blossom
    castTime = 0.0,
    cooldown = 180.0,
    friendlyNameRes = Res.string.item_name_serp_shield,
    iconRes = Res.drawable.serp_shield,
    packedUsageField = { it.lastSerpShield },
    possibleSpellNames = listOf("Corrupted Wit"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastSerpShield = incrementPackedItemUsage(card.cache.lastSerpShield))
    )}
  ),

  //
  // Mistsong Dungeon Stuff
  //
  MIST_DOOMSHADOW_NODACHI(
    itemSpecificSkillIds = listOf(32321, 32428, 33602), // Superior Doomshadow Nodachi, Prime Doomshadow Nodachi, Supreme Doomshadow Nodachi (Amplified Black Magic skill name)
    itemSpecificBuffIds = listOf(16767, 16842, 18169), // Pervasive Black Magic Rank 1, Pervasive Black Magic Rank 2, Pervasive Black Magic Rank 3
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_mist_nodachi,
    iconRes = Res.drawable.mistsong_nodachi,
    packedUsageField = { it.lastMistNodachi },
    possibleSpellNames = listOf("Pervasive Black Magic", "Amplified Black Magic"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastMistNodachi = incrementPackedItemUsage(card.cache.lastMistNodachi))
    )}
  ),
  MIST_DAGGER(
    itemSpecificSkillIds = listOf(32322, 32426, 33600), // Spell Cast for Dagger Wicked Whisper Dagger, Prime Wicked Whisper Dagger, Supreme Wicked Whisper Dagger
    itemSpecificBuffIds = listOf(16765), // Wicked Whisper Dagger debuff
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_mist_dagger,
    iconRes = Res.drawable.mistsong_dagger,
    packedUsageField = { it.lastMistDagger },
    possibleSpellNames = listOf("Wicked Whisper Dagger", "Prime Wicked Whisper Dagger", "Supreme Wicked Whisper Dagger "),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastMistDagger = incrementPackedItemUsage(card.cache.lastMistDagger))
    )}
  ),
  MIST_SHIELD(
    itemSpecificSkillIds = listOf(32415, 32416, 33603), // Bewitching Talisman
    itemSpecificBuffIds = listOf(16825, 16826, 18171), // Healing Talisman
    castTime = 0.0,
    cooldown = 45.0,
    friendlyNameRes = Res.string.item_name_mist_shield,
    iconRes = Res.drawable.mistsong_shield,
    packedUsageField = { it.lastMistShield },
    possibleSpellNames = listOf("Healing Talisman", "Mistsong Bewitching Talisman"),
    updateCard = { card -> card.copy(
      cache = card.cache?.copy(lastMistShield = incrementPackedItemUsage(card.cache.lastMistShield))
    )}
  );
}
