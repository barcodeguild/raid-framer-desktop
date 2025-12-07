package com.reoky.raidframer.core.model

data class RaidMember(val name: String, val health: Int, val role: String = "Healer")

typealias Party = Array<RaidMember>
