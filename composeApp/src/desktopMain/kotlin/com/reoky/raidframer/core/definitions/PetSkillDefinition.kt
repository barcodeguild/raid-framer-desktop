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
    possibleNames = listOf("Guided Missiles", "유도탄", "Ковровая бомбардировка")
  ),
  Skill(
    id = 46058,
    name = "Guided Missiles (Rider)",
    castTime = 0.0,
    cooldown = 60.0,
    possibleNames = listOf("Guided Missiles (Rider)", "유도탄(탑승자용)", "Ковровая бомбардировка")
  ),

  // Mara's scratch ~
  Skill(
    id = 8001707,
    name = "Scratch",
    castTime = 0.0,
    cooldown = 18.0,
    possibleNames = listOf("Scratch")
  ),
  Skill(
    id = 8001708,
    name = "Scratch (Rider)",
    castTime = 0.0,
    cooldown = 18.0,
    possibleNames = listOf("Scratch (Rider)")
  ),

  // Red Dragon's Breath (Rider) - player casts this, dragon does the damage
  Skill(
    id = 38418,
    name = "Red Dragon's Breath (Rider)",
    castTime = 0.0,
    cooldown = 30.0,
    possibleNames = listOf("Red Dragon's Breath (Rider)", "붉은 용의 숨결 (탑승자)", "Огненное дыхание")
  ),
  // Green Dragon's Breath (Rider)
  Skill(
    id = 38699,
    name = "Green Dragon's Breath (Rider)",
    castTime = 0.0,
    cooldown = 30.0,
    possibleNames = listOf("Green Dragon's Breath (Rider)", "녹색 용의 숨결 (탑승자)", "Ядовитое дыхание")
  ),
  // Black Dragon's Breath (Rider)
  Skill(
    id = 38701,
    name = "Black Dragon's Breath (Rider)",
    castTime = 0.0,
    cooldown = 30.0,
    possibleNames = listOf("Black Dragon's Breath (Rider)", "검은 용의 숨결 (탑승자)", "Дыхание тьмы")
  ),
  // Clinging Flame - dragon breath DoT damage
  Skill(
    id = 22608,
    name = "Clinging Flame",
    castTime = 0.0,
    cooldown = 0.0,
    possibleNames = listOf("Clinging Flame", "폭발하는 씨앗", "Раскаленная лава")
  ),
  // Clinging Flame - dragon breath DoT damage
  Skill(
    id = 22609,
    name = "Clinging Flame",
    castTime = 0.0,
    cooldown = 0.0,
    possibleNames = listOf("Clinging Flame", "폭발하는 씨앗", "Раскаленная лава")
  ),
  // Clinging Flame Explosion - dragon breath burst damage
  Skill(
    id = 22618,
    name = "Clinging Flame Explosion",
    castTime = 0.0,
    cooldown = 0.0,
    possibleNames = listOf("Clinging Flame Explosion", "폭발하는 씨앗", "Раскаленная лава")
  ),

  // Typhoon Drake (Used for testing the app and breaths!)
  Skill(
    id = 35787,
    name = "Thunderbreath (Rider)",
    castTime = 0.0,
    cooldown = 60.0,
    possibleNames = listOf("Thunderbreath (Rider)", "천둥의 숨결 (탑승자)")
  ),
  Skill(
    id = 35786,
    name = "Thunderbreath",
    castTime = 0.0,
    cooldown = 60.0,
    possibleNames = listOf("Thunderbreath", "천둥의 숨결")
  ),
  Skill(
    id = 21015,
    name = "Thunderbreath Aftershock",
    castTime = 0.0,
    cooldown = 0.0,
    possibleNames = listOf("Thunderbreath Aftershock", "천둥의 숨결 여파")
  ),
)