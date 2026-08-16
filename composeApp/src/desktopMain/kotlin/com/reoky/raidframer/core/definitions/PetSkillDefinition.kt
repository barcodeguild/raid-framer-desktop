package com.reoky.raidframer.core.definitions

/*
 * Whitelisted pet skills that are accumulated and turned into pet damage / casting / debuff events for tracking. There's no way to definitively
 * detect if a combat event is from a pet or not, so we have to whitelist known pet skills here.
 */
val petSkillWhitelist = listOf(
  Skill(
    id = 46055,
    name = "Guided Missiles",
    castTime = 0.0,
    cooldown = 60.0,
    possibleNames = listOf("Guided Missiles", "유도탄", "Ковровая бомбардировка"),
    allowedPetTypes = setOf("Siege Risopoda", "갑충 병기", "甲虫兵器", "riso"),
    relatedDamageIds = setOf(46055)
  ),
  Skill(
    id = 46058,
    name = "Guided Missiles (Rider)",
    castTime = 0.0,
    cooldown = 60.0,
    possibleNames = listOf("Guided Missiles (Rider)", "유도탄(탑승자용)", "Ковровая бомбардировка"),
    isPetInitiator = true,
    allowedPetTypes = setOf("Siege Risopoda", "갑충 병기", "甲虫兵器", "riso"),
    relatedDamageIds = setOf(46055)
  ),
  // Red Dragon's Breath (Rider) - player casts this, dragon does the damage
  Skill(
    id = 38418,
    name = "Red Dragon's Breath (Rider)",
    castTime = 0.0,
    cooldown = 30.0,
    possibleNames = listOf("Red Dragon's Breath (Rider)", "붉은 용의 숨결 (탑승자)", "Огненное дыхание"),
    isPetInitiator = true,
    allowedPetTypes = setOf("Red Dragon", "붉은 용"),
    relatedDamageIds = setOf(22608, 22609, 22618)
  ),
  // Green Dragon's Breath (Rider)
  Skill(
    id = 38699,
    name = "Green Dragon's Breath (Rider)",
    castTime = 0.0,
    cooldown = 30.0,
    possibleNames = listOf("Green Dragon's Breath (Rider)", "녹색 용의 숨결 (탑승자)", "Ядовитое дыхание"),
    isPetInitiator = true,
    allowedPetTypes = setOf("Green Dragon", "녹색 용"),
    relatedDamageIds = setOf(22608, 22609, 22618)
  ),
  // Black Dragon's Breath (Rider)
  Skill(
    id = 38701,
    name = "Black Dragon's Breath (Rider)",
    castTime = 0.0,
    cooldown = 30.0,
    possibleNames = listOf("Black Dragon's Breath (Rider)", "검은 용의 숨결 (탑승자)", "Дыхание тьмы"),
    isPetInitiator = true,
    allowedPetTypes = setOf("Black Dragon", "검은 용"),
    relatedDamageIds = setOf(22608, 22609, 22618)
  ),
  // Clinging Flame - dragon breath DoT damage
  Skill(
    id = 22608,
    name = "Clinging Flame",
    castTime = 0.0,
    cooldown = 0.0,
    possibleNames = listOf("Clinging Flame", "폭발하는 씨앗", "Раскаленная лава"),
    allowedPetTypes = setOf("Red Dragon", "붉은 용", "Green Dragon", "녹색 용", "Black Dragon", "검은 용")
  ),
  // Clinging Flame - dragon breath DoT damage
  Skill(
    id = 22609,
    name = "Clinging Flame",
    castTime = 0.0,
    cooldown = 0.0,
    possibleNames = listOf("Clinging Flame", "폭발하는 씨앗", "Раскаленная лава"),
    allowedPetTypes = setOf("Red Dragon", "붉은 용", "Green Dragon", "녹색 용", "Black Dragon", "검은 용")
  ),
  // Clinging Flame Explosion - dragon breath burst damage
  Skill(
    id = 22618,
    name = "Clinging Flame Explosion",
    castTime = 0.0,
    cooldown = 0.0,
    possibleNames = listOf("Clinging Flame Explosion", "폭발하는 씨앗", "Раскаленная лава"),
    allowedPetTypes = setOf("Red Dragon", "붉은 용", "Green Dragon", "녹색 용", "Black Dragon", "검은 용")
  ),

  // Typhoon Drake (Used for testing the app and breaths!)
  Skill(
    id = 35787,
    name = "Thunderbreath (Rider)",
    castTime = 0.0,
    cooldown = 60.0,
    possibleNames = listOf("Thunderbreath (Rider)", "천둥의 숨결 (탑승자)"),
    isPetInitiator = true,
    allowedPetTypes = setOf("Typhoon Drake"),
    relatedDamageIds = setOf(35786, 21015)
  ),
  Skill(
    id = 35786,
    name = "Thunderbreath",
    castTime = 0.0,
    cooldown = 60.0,
    possibleNames = listOf("Thunderbreath", "천둥의 숨결"),
    allowedPetTypes = setOf("Typhoon Drake")
  ),
  Skill(
    id = 21015,
    name = "Thunderbreath Aftershock",
    castTime = 0.0,
    cooldown = 0.0,
    possibleNames = listOf("Thunderbreath Aftershock", "천둥의 숨결 여파"),
    allowedPetTypes = setOf("Typhoon Drake")
  ),
)
