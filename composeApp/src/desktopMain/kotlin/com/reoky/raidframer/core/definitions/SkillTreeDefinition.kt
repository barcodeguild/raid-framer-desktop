package com.reoky.raidframer.core.definitions

import org.jetbrains.compose.resources.StringResource
import raid_framer_desktop.composeapp.generated.resources.Res
import raid_framer_desktop.composeapp.generated.resources.skill_tree_archery
import raid_framer_desktop.composeapp.generated.resources.skill_tree_auramancy
import raid_framer_desktop.composeapp.generated.resources.skill_tree_battlerage
import raid_framer_desktop.composeapp.generated.resources.skill_tree_defense
import raid_framer_desktop.composeapp.generated.resources.skill_tree_gunslinger
import raid_framer_desktop.composeapp.generated.resources.skill_tree_malediction
import raid_framer_desktop.composeapp.generated.resources.skill_tree_occultism
import raid_framer_desktop.composeapp.generated.resources.skill_tree_shadowplay
import raid_framer_desktop.composeapp.generated.resources.skill_tree_songcraft
import raid_framer_desktop.composeapp.generated.resources.skill_tree_sorcery
import raid_framer_desktop.composeapp.generated.resources.skill_tree_spelldance
import raid_framer_desktop.composeapp.generated.resources.skill_tree_swiftblade
import raid_framer_desktop.composeapp.generated.resources.skill_tree_vitalism
import raid_framer_desktop.composeapp.generated.resources.skill_tree_witchcraft
import raid_framer_desktop.composeapp.generated.resources.spec_type_abolisher
import raid_framer_desktop.composeapp.generated.resources.spec_type_absolver
import raid_framer_desktop.composeapp.generated.resources.spec_type_activist
import raid_framer_desktop.composeapp.generated.resources.spec_type_adept
import raid_framer_desktop.composeapp.generated.resources.spec_type_alchemist
import raid_framer_desktop.composeapp.generated.resources.spec_type_anchorite
import raid_framer_desktop.composeapp.generated.resources.spec_type_animist
import raid_framer_desktop.composeapp.generated.resources.spec_type_annihilator
import raid_framer_desktop.composeapp.generated.resources.spec_type_anthropomancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_apostate
import raid_framer_desktop.composeapp.generated.resources.spec_type_arachnomancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_arcane_hunter
import raid_framer_desktop.composeapp.generated.resources.spec_type_arcanist
import raid_framer_desktop.composeapp.generated.resources.spec_type_archon
import raid_framer_desktop.composeapp.generated.resources.spec_type_argent
import raid_framer_desktop.composeapp.generated.resources.spec_type_assassin
import raid_framer_desktop.composeapp.generated.resources.spec_type_astral_ranger
import raid_framer_desktop.composeapp.generated.resources.spec_type_athame
import raid_framer_desktop.composeapp.generated.resources.spec_type_augmentor
import raid_framer_desktop.composeapp.generated.resources.spec_type_augurer
import raid_framer_desktop.composeapp.generated.resources.spec_type_avatar
import raid_framer_desktop.composeapp.generated.resources.spec_type_banebolt
import raid_framer_desktop.composeapp.generated.resources.spec_type_banesong
import raid_framer_desktop.composeapp.generated.resources.spec_type_banshee
import raid_framer_desktop.composeapp.generated.resources.spec_type_barrelace
import raid_framer_desktop.composeapp.generated.resources.spec_type_barricade
import raid_framer_desktop.composeapp.generated.resources.spec_type_bastion
import raid_framer_desktop.composeapp.generated.resources.spec_type_battlemage
import raid_framer_desktop.composeapp.generated.resources.spec_type_battlepriest
import raid_framer_desktop.composeapp.generated.resources.spec_type_beguiler
import raid_framer_desktop.composeapp.generated.resources.spec_type_beguiling_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_blackguard
import raid_framer_desktop.composeapp.generated.resources.spec_type_blade_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_bladechanter
import raid_framer_desktop.composeapp.generated.resources.spec_type_blademage
import raid_framer_desktop.composeapp.generated.resources.spec_type_bladespell
import raid_framer_desktop.composeapp.generated.resources.spec_type_blastfuse
import raid_framer_desktop.composeapp.generated.resources.spec_type_blightcaster
import raid_framer_desktop.composeapp.generated.resources.spec_type_blighter
import raid_framer_desktop.composeapp.generated.resources.spec_type_blinkhunter
import raid_framer_desktop.composeapp.generated.resources.spec_type_blinkshot
import raid_framer_desktop.composeapp.generated.resources.spec_type_blood_arrow
import raid_framer_desktop.composeapp.generated.resources.spec_type_bloodmage
import raid_framer_desktop.composeapp.generated.resources.spec_type_bloodreaver
import raid_framer_desktop.composeapp.generated.resources.spec_type_bloodskald
import raid_framer_desktop.composeapp.generated.resources.spec_type_bloodthrall
import raid_framer_desktop.composeapp.generated.resources.spec_type_bloody_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_bonechiller
import raid_framer_desktop.composeapp.generated.resources.spec_type_bonestalker
import raid_framer_desktop.composeapp.generated.resources.spec_type_boneweaver
import raid_framer_desktop.composeapp.generated.resources.spec_type_bounty_hunter
import raid_framer_desktop.composeapp.generated.resources.spec_type_breathstealer
import raid_framer_desktop.composeapp.generated.resources.spec_type_buccaneer
import raid_framer_desktop.composeapp.generated.resources.spec_type_bulwark
import raid_framer_desktop.composeapp.generated.resources.spec_type_cabalist
import raid_framer_desktop.composeapp.generated.resources.spec_type_cannoneer
import raid_framer_desktop.composeapp.generated.resources.spec_type_caretaker
import raid_framer_desktop.composeapp.generated.resources.spec_type_charmer
import raid_framer_desktop.composeapp.generated.resources.spec_type_clairvoyer
import raid_framer_desktop.composeapp.generated.resources.spec_type_cleric
import raid_framer_desktop.composeapp.generated.resources.spec_type_comedian
import raid_framer_desktop.composeapp.generated.resources.spec_type_confessor
import raid_framer_desktop.composeapp.generated.resources.spec_type_confutator
import raid_framer_desktop.composeapp.generated.resources.spec_type_conjuror
import raid_framer_desktop.composeapp.generated.resources.spec_type_corruptor
import raid_framer_desktop.composeapp.generated.resources.spec_type_corsair
import raid_framer_desktop.composeapp.generated.resources.spec_type_covert_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_crusader
import raid_framer_desktop.composeapp.generated.resources.spec_type_cryptwalker
import raid_framer_desktop.composeapp.generated.resources.spec_type_cultist
import raid_framer_desktop.composeapp.generated.resources.spec_type_cursebolt
import raid_framer_desktop.composeapp.generated.resources.spec_type_cursebrand
import raid_framer_desktop.composeapp.generated.resources.spec_type_cyclone
import raid_framer_desktop.composeapp.generated.resources.spec_type_daggerspell
import raid_framer_desktop.composeapp.generated.resources.spec_type_dark_aegis
import raid_framer_desktop.composeapp.generated.resources.spec_type_darkness_artist
import raid_framer_desktop.composeapp.generated.resources.spec_type_darkness_avenger
import raid_framer_desktop.composeapp.generated.resources.spec_type_darkness_controller
import raid_framer_desktop.composeapp.generated.resources.spec_type_darkness_fanatic
import raid_framer_desktop.composeapp.generated.resources.spec_type_darkness_resolver
import raid_framer_desktop.composeapp.generated.resources.spec_type_darkness_revolutionist
import raid_framer_desktop.composeapp.generated.resources.spec_type_darkness_savior
import raid_framer_desktop.composeapp.generated.resources.spec_type_darkrunner
import raid_framer_desktop.composeapp.generated.resources.spec_type_darkseer
import raid_framer_desktop.composeapp.generated.resources.spec_type_dawncaller
import raid_framer_desktop.composeapp.generated.resources.spec_type_death_prophet
import raid_framer_desktop.composeapp.generated.resources.spec_type_death_warden
import raid_framer_desktop.composeapp.generated.resources.spec_type_deathchord
import raid_framer_desktop.composeapp.generated.resources.spec_type_deathtrigger
import raid_framer_desktop.composeapp.generated.resources.spec_type_deathvicar
import raid_framer_desktop.composeapp.generated.resources.spec_type_deathwish
import raid_framer_desktop.composeapp.generated.resources.spec_type_deceiver
import raid_framer_desktop.composeapp.generated.resources.spec_type_defiant
import raid_framer_desktop.composeapp.generated.resources.spec_type_defiler
import raid_framer_desktop.composeapp.generated.resources.spec_type_demolisher
import raid_framer_desktop.composeapp.generated.resources.spec_type_demonologist
import raid_framer_desktop.composeapp.generated.resources.spec_type_demonshot
import raid_framer_desktop.composeapp.generated.resources.spec_type_dervish
import raid_framer_desktop.composeapp.generated.resources.spec_type_destroyer
import raid_framer_desktop.composeapp.generated.resources.spec_type_dirgeweaver
import raid_framer_desktop.composeapp.generated.resources.spec_type_disciple
import raid_framer_desktop.composeapp.generated.resources.spec_type_diviner
import raid_framer_desktop.composeapp.generated.resources.spec_type_doombringer
import raid_framer_desktop.composeapp.generated.resources.spec_type_doomcaller
import raid_framer_desktop.composeapp.generated.resources.spec_type_doomlord
import raid_framer_desktop.composeapp.generated.resources.spec_type_doomshot
import raid_framer_desktop.composeapp.generated.resources.spec_type_doomskald
import raid_framer_desktop.composeapp.generated.resources.spec_type_doomspeaker
import raid_framer_desktop.composeapp.generated.resources.spec_type_dourguard
import raid_framer_desktop.composeapp.generated.resources.spec_type_dreadbow
import raid_framer_desktop.composeapp.generated.resources.spec_type_dreaddart
import raid_framer_desktop.composeapp.generated.resources.spec_type_dreadhunter
import raid_framer_desktop.composeapp.generated.resources.spec_type_dreadnaught
import raid_framer_desktop.composeapp.generated.resources.spec_type_dreadrunner
import raid_framer_desktop.composeapp.generated.resources.spec_type_dreadstone
import raid_framer_desktop.composeapp.generated.resources.spec_type_dreambreaker
import raid_framer_desktop.composeapp.generated.resources.spec_type_dreamspinner
import raid_framer_desktop.composeapp.generated.resources.spec_type_druid
import raid_framer_desktop.composeapp.generated.resources.spec_type_dusk_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_duskdirge
import raid_framer_desktop.composeapp.generated.resources.spec_type_duskraider
import raid_framer_desktop.composeapp.generated.resources.spec_type_earthsinger
import raid_framer_desktop.composeapp.generated.resources.spec_type_ebonshield
import raid_framer_desktop.composeapp.generated.resources.spec_type_ebonsong
import raid_framer_desktop.composeapp.generated.resources.spec_type_edgeripper
import raid_framer_desktop.composeapp.generated.resources.spec_type_edgewalker
import raid_framer_desktop.composeapp.generated.resources.spec_type_eidolon
import raid_framer_desktop.composeapp.generated.resources.spec_type_emissary
import raid_framer_desktop.composeapp.generated.resources.spec_type_enchantrix
import raid_framer_desktop.composeapp.generated.resources.spec_type_enforcer
import raid_framer_desktop.composeapp.generated.resources.spec_type_enigmatist
import raid_framer_desktop.composeapp.generated.resources.spec_type_equalizer
import raid_framer_desktop.composeapp.generated.resources.spec_type_evangelist
import raid_framer_desktop.composeapp.generated.resources.spec_type_eviscerator
import raid_framer_desktop.composeapp.generated.resources.spec_type_evoker
import raid_framer_desktop.composeapp.generated.resources.spec_type_executioner
import raid_framer_desktop.composeapp.generated.resources.spec_type_exorcist
import raid_framer_desktop.composeapp.generated.resources.spec_type_fanatic
import raid_framer_desktop.composeapp.generated.resources.spec_type_fangborn
import raid_framer_desktop.composeapp.generated.resources.spec_type_farslayer
import raid_framer_desktop.composeapp.generated.resources.spec_type_fatemark
import raid_framer_desktop.composeapp.generated.resources.spec_type_fatespinner
import raid_framer_desktop.composeapp.generated.resources.spec_type_fear_artist
import raid_framer_desktop.composeapp.generated.resources.spec_type_fear_avenger
import raid_framer_desktop.composeapp.generated.resources.spec_type_fear_controller
import raid_framer_desktop.composeapp.generated.resources.spec_type_fear_resolver
import raid_framer_desktop.composeapp.generated.resources.spec_type_fear_revolutionist
import raid_framer_desktop.composeapp.generated.resources.spec_type_fear_savior
import raid_framer_desktop.composeapp.generated.resources.spec_type_fearful_fanatic
import raid_framer_desktop.composeapp.generated.resources.spec_type_fearstalker
import raid_framer_desktop.composeapp.generated.resources.spec_type_feversong
import raid_framer_desktop.composeapp.generated.resources.spec_type_flashblade
import raid_framer_desktop.composeapp.generated.resources.spec_type_fleshshaper
import raid_framer_desktop.composeapp.generated.resources.spec_type_forsaker
import raid_framer_desktop.composeapp.generated.resources.spec_type_freebooter
import raid_framer_desktop.composeapp.generated.resources.spec_type_fury_canon
import raid_framer_desktop.composeapp.generated.resources.spec_type_fury_mage
import raid_framer_desktop.composeapp.generated.resources.spec_type_ghostblade
import raid_framer_desktop.composeapp.generated.resources.spec_type_glamorous_avenger
import raid_framer_desktop.composeapp.generated.resources.spec_type_glamorous_revolutionist
import raid_framer_desktop.composeapp.generated.resources.spec_type_glamorous_savior
import raid_framer_desktop.composeapp.generated.resources.spec_type_gloomknight
import raid_framer_desktop.composeapp.generated.resources.spec_type_gloomspell
import raid_framer_desktop.composeapp.generated.resources.spec_type_gloomstalker
import raid_framer_desktop.composeapp.generated.resources.spec_type_grave_admiral
import raid_framer_desktop.composeapp.generated.resources.spec_type_gravebow
import raid_framer_desktop.composeapp.generated.resources.spec_type_graveshot
import raid_framer_desktop.composeapp.generated.resources.spec_type_gravesinger
import raid_framer_desktop.composeapp.generated.resources.spec_type_grimshot
import raid_framer_desktop.composeapp.generated.resources.spec_type_gypsy
import raid_framer_desktop.composeapp.generated.resources.spec_type_harbinger
import raid_framer_desktop.composeapp.generated.resources.spec_type_haruspex
import raid_framer_desktop.composeapp.generated.resources.spec_type_heartsbane
import raid_framer_desktop.composeapp.generated.resources.spec_type_hellshield
import raid_framer_desktop.composeapp.generated.resources.spec_type_hellweaver
import raid_framer_desktop.composeapp.generated.resources.spec_type_herald
import raid_framer_desktop.composeapp.generated.resources.spec_type_hereafter_artist
import raid_framer_desktop.composeapp.generated.resources.spec_type_hereafter_avenger
import raid_framer_desktop.composeapp.generated.resources.spec_type_hereafter_controller
import raid_framer_desktop.composeapp.generated.resources.spec_type_hereafter_fanatic
import raid_framer_desktop.composeapp.generated.resources.spec_type_hereafter_resolver
import raid_framer_desktop.composeapp.generated.resources.spec_type_hereafter_revolutionist
import raid_framer_desktop.composeapp.generated.resources.spec_type_hereafter_savior
import raid_framer_desktop.composeapp.generated.resources.spec_type_hex_ranger
import raid_framer_desktop.composeapp.generated.resources.spec_type_hex_warden
import raid_framer_desktop.composeapp.generated.resources.spec_type_hexblade
import raid_framer_desktop.composeapp.generated.resources.spec_type_hexbolt
import raid_framer_desktop.composeapp.generated.resources.spec_type_hexdancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_hexsniper
import raid_framer_desktop.composeapp.generated.resources.spec_type_hierophant
import raid_framer_desktop.composeapp.generated.resources.spec_type_honorguard
import raid_framer_desktop.composeapp.generated.resources.spec_type_hordebreaker
import raid_framer_desktop.composeapp.generated.resources.spec_type_howler
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_apostle
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_argent
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_blade
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_bladespell
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_chaser
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_disciple
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_evil_spirit
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_fighter
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_gunslinger
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_hunter
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_magician
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_monk
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_monk_archer
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_pilgrim
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_reaper
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_sage
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_stalker
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_swordspell
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_terminator
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_trickster
import raid_framer_desktop.composeapp.generated.resources.spec_type_illusion_warmachine
import raid_framer_desktop.composeapp.generated.resources.spec_type_immortalist
import raid_framer_desktop.composeapp.generated.resources.spec_type_incantator
import raid_framer_desktop.composeapp.generated.resources.spec_type_infiltrator
import raid_framer_desktop.composeapp.generated.resources.spec_type_inquisitor
import raid_framer_desktop.composeapp.generated.resources.spec_type_instigator
import raid_framer_desktop.composeapp.generated.resources.spec_type_invader
import raid_framer_desktop.composeapp.generated.resources.spec_type_invoker
import raid_framer_desktop.composeapp.generated.resources.spec_type_ironsoul
import raid_framer_desktop.composeapp.generated.resources.spec_type_jinxmender
import raid_framer_desktop.composeapp.generated.resources.spec_type_justicar
import raid_framer_desktop.composeapp.generated.resources.spec_type_lamentor
import raid_framer_desktop.composeapp.generated.resources.spec_type_leadbiter
import raid_framer_desktop.composeapp.generated.resources.spec_type_leaddancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_legionnaire
import raid_framer_desktop.composeapp.generated.resources.spec_type_liberator
import raid_framer_desktop.composeapp.generated.resources.spec_type_lifestealer
import raid_framer_desktop.composeapp.generated.resources.spec_type_lightsoul
import raid_framer_desktop.composeapp.generated.resources.spec_type_lorebreaker
import raid_framer_desktop.composeapp.generated.resources.spec_type_magister
import raid_framer_desktop.composeapp.generated.resources.spec_type_marauder
import raid_framer_desktop.composeapp.generated.resources.spec_type_marrowblade
import raid_framer_desktop.composeapp.generated.resources.spec_type_mentalist
import raid_framer_desktop.composeapp.generated.resources.spec_type_mercenary
import raid_framer_desktop.composeapp.generated.resources.spec_type_mindslaver
import raid_framer_desktop.composeapp.generated.resources.spec_type_mindslayer
import raid_framer_desktop.composeapp.generated.resources.spec_type_minstrel
import raid_framer_desktop.composeapp.generated.resources.spec_type_musician
import raid_framer_desktop.composeapp.generated.resources.spec_type_naturalist
import raid_framer_desktop.composeapp.generated.resources.spec_type_necromancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightbearer
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightblade
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightcloak
import raid_framer_desktop.composeapp.generated.resources.spec_type_nighthaunt
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightmare_artist
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightmare_avenger
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightmare_controller
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightmare_fanatic
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightmare_resolver
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightmare_revolutionist
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightmare_savior
import raid_framer_desktop.composeapp.generated.resources.spec_type_nightwitch
import raid_framer_desktop.composeapp.generated.resources.spec_type_nocturne
import raid_framer_desktop.composeapp.generated.resources.spec_type_nullweaver
import raid_framer_desktop.composeapp.generated.resources.spec_type_oathsworn
import raid_framer_desktop.composeapp.generated.resources.spec_type_oracle
import raid_framer_desktop.composeapp.generated.resources.spec_type_outrider
import raid_framer_desktop.composeapp.generated.resources.spec_type_paladin
import raid_framer_desktop.composeapp.generated.resources.spec_type_phantasm
import raid_framer_desktop.composeapp.generated.resources.spec_type_phantom
import raid_framer_desktop.composeapp.generated.resources.spec_type_pit_fighter
import raid_framer_desktop.composeapp.generated.resources.spec_type_pitfiend
import raid_framer_desktop.composeapp.generated.resources.spec_type_planeshifter
import raid_framer_desktop.composeapp.generated.resources.spec_type_pontifex
import raid_framer_desktop.composeapp.generated.resources.spec_type_poxbane
import raid_framer_desktop.composeapp.generated.resources.spec_type_primeval
import raid_framer_desktop.composeapp.generated.resources.spec_type_privateer
import raid_framer_desktop.composeapp.generated.resources.spec_type_purgator
import raid_framer_desktop.composeapp.generated.resources.spec_type_purifier
import raid_framer_desktop.composeapp.generated.resources.spec_type_quickdraw
import raid_framer_desktop.composeapp.generated.resources.spec_type_ragebinder
import raid_framer_desktop.composeapp.generated.resources.spec_type_ragechanter
import raid_framer_desktop.composeapp.generated.resources.spec_type_ranger
import raid_framer_desktop.composeapp.generated.resources.spec_type_ravager
import raid_framer_desktop.composeapp.generated.resources.spec_type_realmshifter
import raid_framer_desktop.composeapp.generated.resources.spec_type_reaper
import raid_framer_desktop.composeapp.generated.resources.spec_type_reincarnator
import raid_framer_desktop.composeapp.generated.resources.spec_type_requiem
import raid_framer_desktop.composeapp.generated.resources.spec_type_revelator
import raid_framer_desktop.composeapp.generated.resources.spec_type_revenant
import raid_framer_desktop.composeapp.generated.resources.spec_type_revolutionist
import raid_framer_desktop.composeapp.generated.resources.spec_type_runebreaker
import raid_framer_desktop.composeapp.generated.resources.spec_type_saboteur
import raid_framer_desktop.composeapp.generated.resources.spec_type_scion
import raid_framer_desktop.composeapp.generated.resources.spec_type_seal_artist
import raid_framer_desktop.composeapp.generated.resources.spec_type_seal_controller
import raid_framer_desktop.composeapp.generated.resources.spec_type_seal_fanatic
import raid_framer_desktop.composeapp.generated.resources.spec_type_seal_resolver
import raid_framer_desktop.composeapp.generated.resources.spec_type_seal_revolutionist
import raid_framer_desktop.composeapp.generated.resources.spec_type_secret_choreographer
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadehunter
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadestriker
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadow_prophet
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadow_reaper
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadowbane
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadowblade
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadowdancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadowguard
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadowknight
import raid_framer_desktop.composeapp.generated.resources.spec_type_shadowsong
import raid_framer_desktop.composeapp.generated.resources.spec_type_shaman
import raid_framer_desktop.composeapp.generated.resources.spec_type_shroudmaster
import raid_framer_desktop.composeapp.generated.resources.spec_type_shroudsong
import raid_framer_desktop.composeapp.generated.resources.spec_type_shroudspell
import raid_framer_desktop.composeapp.generated.resources.spec_type_skull_warden
import raid_framer_desktop.composeapp.generated.resources.spec_type_skullknight
import raid_framer_desktop.composeapp.generated.resources.spec_type_song_controller
import raid_framer_desktop.composeapp.generated.resources.spec_type_song_fanatic
import raid_framer_desktop.composeapp.generated.resources.spec_type_soothsayer
import raid_framer_desktop.composeapp.generated.resources.spec_type_sorrowsong
import raid_framer_desktop.composeapp.generated.resources.spec_type_soulbow
import raid_framer_desktop.composeapp.generated.resources.spec_type_soulleech
import raid_framer_desktop.composeapp.generated.resources.spec_type_soulsong
import raid_framer_desktop.composeapp.generated.resources.spec_type_soulthief
import raid_framer_desktop.composeapp.generated.resources.spec_type_spectre
import raid_framer_desktop.composeapp.generated.resources.spec_type_spellbinder
import raid_framer_desktop.composeapp.generated.resources.spec_type_spellbow
import raid_framer_desktop.composeapp.generated.resources.spec_type_spellsinger
import raid_framer_desktop.composeapp.generated.resources.spec_type_spellsong
import raid_framer_desktop.composeapp.generated.resources.spec_type_spellsword
import raid_framer_desktop.composeapp.generated.resources.spec_type_spellweaver
import raid_framer_desktop.composeapp.generated.resources.spec_type_spiritualist
import raid_framer_desktop.composeapp.generated.resources.spec_type_stone_arrow
import raid_framer_desktop.composeapp.generated.resources.spec_type_stormcaster
import raid_framer_desktop.composeapp.generated.resources.spec_type_stormchaser
import raid_framer_desktop.composeapp.generated.resources.spec_type_striking_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_sunset_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_swiftshot
import raid_framer_desktop.composeapp.generated.resources.spec_type_swiftstone
import raid_framer_desktop.composeapp.generated.resources.spec_type_synergist
import raid_framer_desktop.composeapp.generated.resources.spec_type_tempest
import raid_framer_desktop.composeapp.generated.resources.spec_type_templar
import raid_framer_desktop.composeapp.generated.resources.spec_type_thaumaturge
import raid_framer_desktop.composeapp.generated.resources.spec_type_tomb_warden
import raid_framer_desktop.composeapp.generated.resources.spec_type_tombcaller
import raid_framer_desktop.composeapp.generated.resources.spec_type_tombsong
import raid_framer_desktop.composeapp.generated.resources.spec_type_tough_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_tragedy_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_trickster
import raid_framer_desktop.composeapp.generated.resources.spec_type_twistedshot
import raid_framer_desktop.composeapp.generated.resources.spec_type_unknown
import raid_framer_desktop.composeapp.generated.resources.spec_type_vanguard
import raid_framer_desktop.composeapp.generated.resources.spec_type_venombite
import raid_framer_desktop.composeapp.generated.resources.spec_type_vicious_choreographer
import raid_framer_desktop.composeapp.generated.resources.spec_type_visionary
import raid_framer_desktop.composeapp.generated.resources.spec_type_voidstalker
import raid_framer_desktop.composeapp.generated.resources.spec_type_voidwalker
import raid_framer_desktop.composeapp.generated.resources.spec_type_war_dancer
import raid_framer_desktop.composeapp.generated.resources.spec_type_warcaster
import raid_framer_desktop.composeapp.generated.resources.spec_type_warpriest
import raid_framer_desktop.composeapp.generated.resources.spec_type_widowhaunt
import raid_framer_desktop.composeapp.generated.resources.spec_type_wildclaw
import raid_framer_desktop.composeapp.generated.resources.spec_type_wildmage
import raid_framer_desktop.composeapp.generated.resources.spec_type_windseeker
import raid_framer_desktop.composeapp.generated.resources.spec_type_windshaper
import raid_framer_desktop.composeapp.generated.resources.spec_type_windsoul
import raid_framer_desktop.composeapp.generated.resources.spec_type_wispwalker
import raid_framer_desktop.composeapp.generated.resources.spec_type_witch_doctor
import raid_framer_desktop.composeapp.generated.resources.spec_type_wrathslayer
import raid_framer_desktop.composeapp.generated.resources.spec_type_zealot
import raid_framer_desktop.composeapp.generated.resources.spec_type_zephyr

/*
 * Whitelist of preferred builds. This is mostly just opinionated. I'm going to put a badge next to players who are playing PvP specs.
 */
val META_CC_SPECS = setOf<SpecType>(
  SpecType.DREAMBREAKER, SpecType.DEFILER, SpecType.REVENANT, SpecType.NIGHTCLOAK, SpecType.SKULLKNIGHT
)
val META_MELEE_SPECS = setOf<SpecType>(
  SpecType.ABOLISHER, SpecType.DARKRUNNER, SpecType.DUSKDIRGE, SpecType.EXECUTIONER, SpecType.DEATHWISH, SpecType.PHANTOM,
  SpecType.DUSK_DANCER, SpecType.FEARSTALKER, SpecType.FEAR_AVENGER, SpecType.CURSEBRAND, SpecType.BLOODREAVER, SpecType.BLADE_DANCER
)
val META_HEALER_SPECS = setOf<SpecType>(
  SpecType.SOOTHSAYER, SpecType.CONFESSOR, SpecType.ASSASSIN, SpecType.DOOMBRINGER, SpecType.HIEROPHANT, SpecType.DEATH_WARDEN
)
val META_MAGE_SPECS = setOf<SpecType>(
  SpecType.FANATIC, SpecType.DREADRUNNER, SpecType.SPIRITUALIST, SpecType.EQUALIZER, SpecType.REVENANT, SpecType.THAUMATURGE, SpecType.DECEIVER, SpecType.ARCANIST
)
val META_DANCER_SPECS = setOf<SpecType>(
  SpecType.BLOODY_DANCER, SpecType.COVERT_DANCER, SpecType.DUSK_DANCER, SpecType.GLAMOROUS_AVENGER,
  SpecType.GLAMOROUS_REVOLUTIONIST, SpecType.GLAMOROUS_SAVIOR, SpecType.COMEDIAN, SpecType.FEARFUL_FANATIC, SpecType.LEADDANCER
)
val META_RANGED_SPEC = setOf<SpecType>(
  SpecType.PRIVATEER, SpecType.TEMPEST, SpecType.WRATHSLAYER, SpecType.DEATHTRIGGER, SpecType.BULWARK, SpecType.GLOOMKNIGHT, SpecType.EBONSONG, SpecType.STONE_ARROW,
  SpecType.LEADBITER, SpecType.ABSOLVER, SpecType.REVELATOR, SpecType.BOUNTY_HUNTER, SpecType.TOMBSONG, SpecType.BARRELACE, SpecType.DEFIANT
)

/*
 * The base definitions class used for identifying what classes players are playing.
 */
interface SkillTreeDefinition {
  val gameId: Int // the int id used in the game for this tree
  val tree: SkillTreeType
  val skills: List<Skill>
}

data class Skill(
  val id: Int,
  val name: String,
  val castTime: Double,
  val cooldown: Double,
  val possibleNames: List<String> = emptyList()
)

// Build a map of all skill-trees to a last-used timestamp initialized to 0.
fun buildSkillTreeLastUsedMap(): MutableMap<SkillTreeType, Long> {
  val map = mutableMapOf<SkillTreeType, Long>()
  SkillTreeType.entries.forEach { treeType -> map[treeType] = 0L }
  return map
}

/**
 * Find a Skill by a string by checking the skill's name and its possibleNames (case-insensitive).
 */
fun findSkillByName(query: String): Skill? {
  val q = query.trim()
  return SkillTreeType.entries.asSequence()
    .flatMap { it.tree.skills.asSequence() }
    .find { skill ->
      skill.name.equals(q, ignoreCase = true) ||
          skill.possibleNames.any { it.equals(q, ignoreCase = true) }
    }
}

/**
 * Return the SkillTreeDefinition that contains the provided Skill, or null if none found.
 * find anywhere one of the possible names for a spell matches the skill name
 */
fun findSkillTreeForSkill(skill: Skill): SkillTreeType? {
  return SkillTreeType.entries.find { treeType ->
    treeType.tree.skills.any { it.possibleNames.contains(skill.name) } // uses possible names for matching the log file
  }
}

/**
 * Convenience: find the SkillTreeDefinition for a skill name string.
 */
fun findSkillTreeForSpell(spell: String): SkillTreeType? {
  val skill = findSkillByName(spell) ?: return null
  return findSkillTreeForSkill(skill)
}

/*
 * The canonical display order for skill tree icons as of patch 10.x.
 * Auramancy > Archery > Shadowplay > Songcraft > Spelldance > Sorcery > Gunslinger > Vitalism > Witchcraft > Malediction > Battlerage > Occultism > Swiftblade > Defense
 */
val SKILL_TREE_DISPLAY_ORDER = listOf(
  SkillTreeType.AURAMANCY,
  SkillTreeType.ARCHERY,
  SkillTreeType.SHADOWPLAY,
  SkillTreeType.SONGCRAFT,
  SkillTreeType.SPELLDANCE,
  SkillTreeType.SORCERY,
  SkillTreeType.GUNSLINGER,
  SkillTreeType.VITALISM,
  SkillTreeType.WITCHCRAFT,
  SkillTreeType.MALEDICTION,
  SkillTreeType.BATTLERAGE,
  SkillTreeType.OCCULTISM,
  SkillTreeType.SWIFTBLADE,
  SkillTreeType.DEFENSE,
)

private val SKILL_TREE_SORT_ORDER = SKILL_TREE_DISPLAY_ORDER.withIndex().associate { it.value to it.index }

fun List<SkillTreeType>.sortedByDisplayOrder(): List<SkillTreeType> =
  sortedBy { SKILL_TREE_SORT_ORDER[it] ?: Int.MAX_VALUE }

fun Set<SkillTreeType>.sortedByDisplayOrder(): List<SkillTreeType> =
  toList().sortedBy { SKILL_TREE_SORT_ORDER[it] ?: Int.MAX_VALUE }

/*
 * Skill trees sorted by name alphabetically and then indexed by their id starting from 0.
 */
enum class SkillTreeType(val tree: SkillTreeDefinition) {
  ARCHERY(ArcheryDefinition),
  AURAMANCY(AuramancyDefinition),
  BATTLERAGE(BattlerageDefinition),
  DEFENSE(DefenseDefinition),
  GUNSLINGER(GunslingerDefinition),
  MALEDICTION(MaledictionDefinition),
  OCCULTISM(OccultismDefinition),
  SHADOWPLAY(ShadowplayDefinition),
  SONGCRAFT(SongcraftDefinition),
  SORCERY(SorceryDefinition),
  SPELLDANCE(SpelldanceDefinition),
  SWIFTBLADE(SwiftbladeDefinition),
  VITALISM(VitalismDefinition),
  WITCHCRAFT(WitchcraftDefinition);

  companion object {
    fun fromName(name: String): SkillTreeType? {
      return entries.find { it.name.equals(name, ignoreCase = true) }
    }
    fun fromGameId(id: Int): SkillTreeType? {
      return entries.find { it.tree.gameId == id }
    }
  }
}

enum class SpecType(val trees: Set<SkillTreeType>) {
  ABOLISHER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY)),
  ABSOLVER(setOf(SkillTreeType.ARCHERY, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  ACTIVIST(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  ADEPT(setOf(SkillTreeType.AURAMANCY, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  ALCHEMIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  ANCHORITE(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE)),
  ANIMIST(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  ANNIHILATOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER)),
  ANTHROPOMANCER(setOf(SkillTreeType.SORCERY, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  APOSTATE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  ARACHNOMANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE)),
  ARCANE_HUNTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY)),
  ARCANIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SORCERY)),
  ARCHON(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.ARCHERY)),
  ARGENT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.VITALISM)),
  ASSASSIN(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  ASTRAL_RANGER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY)),
  ATHAME(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  AUGMENTOR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  AUGURER(setOf(SkillTreeType.SORCERY, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  AVATAR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  BANEBOLT(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  BANESONG(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  BANSHEE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SWIFTBLADE)),
  BARRELACE(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  BARRICADE(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE)),
  BASTION(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY)),
  BATTLEMAGE(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SORCERY)),
  BATTLEPRIEST(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  BEGUILER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  BEGUILING_DANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  BLACKGUARD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.VITALISM)),
  BLADECHANTER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  BLADEMAGE(setOf(SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  BLADESPELL(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  BLADE_DANCER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  BLASTFUSE(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  BLIGHTCASTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  BLIGHTER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY)),
  BLINKHUNTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  BLINKSHOT(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  BLOODMAGE(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER)),
  BLOODREAVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM)),
  BLOODSKALD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  BLOODTHRALL(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  BLOODY_DANCER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  BLOOD_ARROW(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  BONECHILLER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  BONESTALKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY)),
  BONEWEAVER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  BOUNTY_HUNTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  BREATHSTEALER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  BUCCANEER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.GUNSLINGER)),
  BULWARK(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.GUNSLINGER)),
  CABALIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SORCERY)),
  CANNONEER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  CARETAKER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  CHARMER(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  CLAIRVOYER(setOf(SkillTreeType.DEFENSE, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  CLERIC(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  COMEDIAN(setOf(SkillTreeType.DEFENSE, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  CONFESSOR(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  CONFUTATOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  CONJUROR(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.MALEDICTION)),
  CORRUPTOR(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  CORSAIR(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  COVERT_DANCER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  CRUSADER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SORCERY)),
  CRYPTWALKER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  CULTIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  CURSEBOLT(setOf(SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  CURSEBRAND(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE)),
  CYCLONE(setOf(SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  DAGGERSPELL(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  DARKNESS_ARTIST(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  DARKNESS_AVENGER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  DARKNESS_CONTROLLER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  DARKNESS_FANATIC(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  DARKNESS_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  DARKNESS_REVOLUTIONIST(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  DARKNESS_SAVIOR(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  DARKRUNNER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY)),
  DARKSEER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  DARK_AEGIS(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT)),
  DAWNCALLER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT)),
  DEATHCHORD(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  DEATHTRIGGER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  DEATHVICAR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER)),
  DEATHWISH(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  DEATH_PROPHET(setOf(SkillTreeType.VITALISM, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  DEATH_WARDEN(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  DECEIVER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  DEFIANT(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  DEFILER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM)),
  DEMOLISHER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  DEMONOLOGIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SORCERY)),
  DEMONSHOT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  DERVISH(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM)),
  DESTROYER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  DIRGEWEAVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT)),
  DISCIPLE(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  DIVINER(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  DOOMBRINGER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  DOOMCALLER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  DOOMLORD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM)),
  DOOMSHOT(setOf(SkillTreeType.ARCHERY, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  DOOMSKALD(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  DOOMSPEAKER(setOf(SkillTreeType.SORCERY, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  DOURGUARD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION)),
  DREADBOW(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY)),
  DREADDART(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  DREADHUNTER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY)),
  DREADNAUGHT(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY)),
  DREADRUNNER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  DREADSTONE(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY)),
  DREAMBREAKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY)),
  DREAMSPINNER(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  DRUID(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  DUSKDIRGE(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  DUSKRAIDER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  DUSK_DANCER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  EARTHSINGER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  EBONSHIELD(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  EBONSONG(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  EDGERIPPER(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  EDGEWALKER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.VITALISM)),
  EIDOLON(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY)),
  EMISSARY(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION)),
  ENCHANTRIX(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT)),
  ENFORCER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SORCERY)),
  ENIGMATIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  EQUALIZER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION)),
  EVANGELIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  EVISCERATOR(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  EVOKER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  EXECUTIONER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY)),
  EXORCIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  FANATIC(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  FANGBORN(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  FARSLAYER(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  FATEMARK(setOf(SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  FATESPINNER(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  FEARFUL_FANATIC(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  FEARSTALKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  FEAR_ARTIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  FEAR_AVENGER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  FEAR_CONTROLLER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  FEAR_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SPELLDANCE)),
  FEAR_REVOLUTIONIST(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  FEAR_SAVIOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  FEVERSONG(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  FLASHBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE)),
  FLESHSHAPER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  FORSAKER(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE)),
  FREEBOOTER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  FURY_CANON(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  FURY_MAGE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  GHOSTBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION)),
  GLAMOROUS_AVENGER(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  GLAMOROUS_REVOLUTIONIST(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  GLAMOROUS_SAVIOR(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  GLOOMKNIGHT(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  GLOOMSPELL(setOf(SkillTreeType.SORCERY, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  GLOOMSTALKER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  GRAVEBOW(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  GRAVESHOT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER)),
  GRAVESINGER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  GRAVE_ADMIRAL(setOf(SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  GRIMSHOT(setOf(SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  GYPSY(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  HARBINGER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY)),
  HARUSPEX(setOf(SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  HEARTSBANE(setOf(SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  HELLSHIELD(setOf(SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  HELLWEAVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  HERALD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT)),
  HEREAFTER_ARTIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  HEREAFTER_AVENGER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  HEREAFTER_CONTROLLER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  HEREAFTER_FANATIC(setOf(SkillTreeType.AURAMANCY, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  HEREAFTER_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.SPELLDANCE)),
  HEREAFTER_REVOLUTIONIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  HEREAFTER_SAVIOR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  HEXBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE)),
  HEXBOLT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.GUNSLINGER)),
  HEXDANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SPELLDANCE)),
  HEXSNIPER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.GUNSLINGER)),
  HEX_RANGER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  HEX_WARDEN(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY)),
  HIEROPHANT(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.VITALISM)),
  HONORGUARD(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  HORDEBREAKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM)),
  HOWLER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT)),
  ILLUSION_APOSTLE(setOf(SkillTreeType.VITALISM, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  ILLUSION_ARGENT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  ILLUSION_BLADE(setOf(SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_BLADESPELL(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  ILLUSION_CHASER(setOf(SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  ILLUSION_DANCER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SPELLDANCE)),
  ILLUSION_DISCIPLE(setOf(SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_EVIL_SPIRIT(setOf(SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_FIGHTER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  ILLUSION_GUNSLINGER(setOf(SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_HUNTER(setOf(SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_MAGICIAN(setOf(SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_MONK(setOf(SkillTreeType.SORCERY, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  ILLUSION_MONK_ARCHER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  ILLUSION_PILGRIM(setOf(SkillTreeType.ARCHERY, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  ILLUSION_REAPER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_SAGE(setOf(SkillTreeType.SORCERY, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  ILLUSION_STALKER(setOf(SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_SWORDSPELL(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  ILLUSION_TERMINATOR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  ILLUSION_TRICKSTER(setOf(SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  ILLUSION_WARMACHINE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  IMMORTALIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.SWIFTBLADE)),
  INCANTATOR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  INFILTRATOR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  INQUISITOR(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  INSTIGATOR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  INVADER(setOf(SkillTreeType.DEFENSE, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  INVOKER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  IRONSOUL(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION)),
  JINXMENDER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  JUSTICAR(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.VITALISM)),
  LAMENTOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  LEADBITER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  LEADDANCER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  LEGIONNAIRE(setOf(SkillTreeType.DEFENSE, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  LIBERATOR(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.ARCHERY)),
  LIFESTEALER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  LIGHTSOUL(setOf(SkillTreeType.ARCHERY, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  LOREBREAKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT)),
  MAGISTER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  MARAUDER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  MARROWBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  MENTALIST(setOf(SkillTreeType.SORCERY, SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE)),
  MERCENARY(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SWIFTBLADE)),
  MINDSLAVER(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION)),
  MINDSLAYER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  MINSTREL(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  MUSICIAN(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  NATURALIST(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  NECROMANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.VITALISM)),
  NIGHTBEARER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  NIGHTBLADE(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY)),
  NIGHTCLOAK(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM)),
  NIGHTHAUNT(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  NIGHTMARE_ARTIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_AVENGER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_CONTROLLER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_FANATIC(setOf(SkillTreeType.OCCULTISM, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_REVOLUTIONIST(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  NIGHTMARE_SAVIOR(setOf(SkillTreeType.OCCULTISM, SkillTreeType.VITALISM, SkillTreeType.SPELLDANCE)),
  NIGHTWITCH(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  NOCTURNE(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  NULLWEAVER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  OATHSWORN(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  ORACLE(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  OUTRIDER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  PALADIN(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.VITALISM)),
  PHANTASM(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT)),
  PHANTOM(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SWIFTBLADE)),
  PITFIEND(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  PIT_FIGHTER(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION)),
  PLANESHIFTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY)),
  PONTIFEX(setOf(SkillTreeType.AURAMANCY, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  POXBANE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT)),
  PRIMEVAL(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  PRIVATEER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  PURGATOR(setOf(SkillTreeType.AURAMANCY, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  PURIFIER(setOf(SkillTreeType.VITALISM, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  QUICKDRAW(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  RAGEBINDER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  RAGECHANTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  RANGER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  RAVAGER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.SORCERY)),
  REALMSHIFTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  REAPER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  REINCARNATOR(setOf(SkillTreeType.VITALISM, SkillTreeType.MALEDICTION, SkillTreeType.SWIFTBLADE)),
  REQUIEM(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  REVELATOR(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  REVENANT(setOf(SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM, SkillTreeType.SORCERY)),
  REVOLUTIONIST(setOf(SkillTreeType.DEFENSE, SkillTreeType.MALEDICTION, SkillTreeType.SPELLDANCE)),
  RUNEBREAKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  SABOTEUR(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  SCION(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  SEAL_ARTIST(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.SPELLDANCE)),
  SEAL_CONTROLLER(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SPELLDANCE)),
  SEAL_FANATIC(setOf(SkillTreeType.DEFENSE, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  SEAL_RESOLVER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.DEFENSE, SkillTreeType.SPELLDANCE)),
  SEAL_REVOLUTIONIST(setOf(SkillTreeType.DEFENSE, SkillTreeType.SWIFTBLADE, SkillTreeType.SPELLDANCE)),
  SECRET_CHOREOGRAPHER(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  SHADEHUNTER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  SHADESTRIKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY)),
  SHADOWBANE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.VITALISM)),
  SHADOWBLADE(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY)),
  SHADOWDANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SPELLDANCE)),
  SHADOWGUARD(setOf(SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY, SkillTreeType.MALEDICTION)),
  SHADOWKNIGHT(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SHADOWPLAY)),
  SHADOWSONG(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  SHADOW_PROPHET(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  SHADOW_REAPER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  SHAMAN(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.VITALISM)),
  SHROUDMASTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SHADOWPLAY)),
  SHROUDSONG(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  SHROUDSPELL(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.GUNSLINGER)),
  SKULLKNIGHT(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.OCCULTISM)),
  SKULL_WARDEN(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.GUNSLINGER)),
  SONG_CONTROLLER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  SONG_FANATIC(setOf(SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER, SkillTreeType.SPELLDANCE)),
  SOOTHSAYER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM)),
  SORROWSONG(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  SOULBOW(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  SOULLEECH(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM, SkillTreeType.GUNSLINGER)),
  SOULSONG(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.VITALISM)),
  SOULTHIEF(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION)),
  SPECTRE(setOf(SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT, SkillTreeType.MALEDICTION)),
  SPELLBINDER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  SPELLBOW(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  SPELLSINGER(setOf(SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY, SkillTreeType.SONGCRAFT)),
  SPELLSONG(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  SPELLSWORD(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.SORCERY, SkillTreeType.SONGCRAFT)),
  SPELLWEAVER(setOf(SkillTreeType.SHADOWPLAY, SkillTreeType.VITALISM, SkillTreeType.MALEDICTION)),
  SPIRITUALIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  STONE_ARROW(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  STORMCASTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  STORMCHASER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.SORCERY)),
  STRIKING_DANCER(setOf(SkillTreeType.DEFENSE, SkillTreeType.OCCULTISM, SkillTreeType.SPELLDANCE)),
  SUNSET_DANCER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  SWIFTSHOT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  SWIFTSTONE(setOf(SkillTreeType.DEFENSE, SkillTreeType.SORCERY, SkillTreeType.SHADOWPLAY)),
  SYNERGIST(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  TEMPEST(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  TEMPLAR(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.VITALISM)),
  THAUMATURGE(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SORCERY)),
  TOMBCALLER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT)),
  TOMBSONG(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.GUNSLINGER)),
  TOMB_WARDEN(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SONGCRAFT)),
  TOUGH_DANCER(setOf(SkillTreeType.DEFENSE, SkillTreeType.AURAMANCY, SkillTreeType.SPELLDANCE)),
  TRAGEDY_DANCER(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SONGCRAFT, SkillTreeType.SPELLDANCE)),
  TRICKSTER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SHADOWPLAY)),
  TWISTEDSHOT(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  VANGUARD(setOf(SkillTreeType.DEFENSE, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),
  VENOMBITE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SWIFTBLADE, SkillTreeType.GUNSLINGER)),
  VICIOUS_CHOREOGRAPHER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SHADOWPLAY, SkillTreeType.SPELLDANCE)),
  VISIONARY(setOf(SkillTreeType.AURAMANCY, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  VOIDSTALKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  VOIDWALKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.MALEDICTION, SkillTreeType.GUNSLINGER)),
  WARCASTER(setOf(SkillTreeType.AURAMANCY, SkillTreeType.SORCERY, SkillTreeType.GUNSLINGER)),
  WARPRIEST(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.VITALISM)),
  WAR_DANCER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.DEFENSE, SkillTreeType.SPELLDANCE)),
  WIDOWHAUNT(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.GUNSLINGER)),
  WILDCLAW(setOf(SkillTreeType.OCCULTISM, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  WILDMAGE(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  WINDSEEKER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.ARCHERY, SkillTreeType.MALEDICTION)),
  WINDSHAPER(setOf(SkillTreeType.ARCHERY, SkillTreeType.SORCERY, SkillTreeType.SWIFTBLADE)),
  WINDSOUL(setOf(SkillTreeType.DEFENSE, SkillTreeType.ARCHERY, SkillTreeType.SWIFTBLADE)),
  WISPWALKER(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.AURAMANCY, SkillTreeType.SWIFTBLADE)),
  WITCH_DOCTOR(setOf(SkillTreeType.WITCHCRAFT, SkillTreeType.OCCULTISM, SkillTreeType.MALEDICTION)),
  WRATHSLAYER(setOf(SkillTreeType.BATTLERAGE, SkillTreeType.AURAMANCY, SkillTreeType.GUNSLINGER)),
  ZEALOT(setOf(SkillTreeType.OCCULTISM, SkillTreeType.SORCERY, SkillTreeType.MALEDICTION)),
  ZEPHYR(setOf(SkillTreeType.ARCHERY, SkillTreeType.SONGCRAFT, SkillTreeType.SWIFTBLADE)),

  UNKNOWN(setOf());

  companion object {
    private val byTrees = entries.associateBy { it.trees }
    fun fromTrees(trees: Set<SkillTreeType>): SpecType = byTrees[trees] ?: SpecType.UNKNOWN
    fun fromName(name: String): SpecType? = entries.find { it.name.equals(name, ignoreCase = true) }
  }
}

fun SpecType.isPvPFriendly(): Boolean {
  return when (this) {
    // tanks
    SpecType.DREAMBREAKER, SpecType.DEFILER, SpecType.REVENANT -> true
    // healers
    SpecType.CONFESSOR, SpecType.ASSASSIN -> true
    // melee dps
    SpecType.BLOODREAVER, SpecType.BLADE_DANCER, SpecType.HEX_WARDEN, SpecType.HEXBLADE -> true
    else -> false
  }
}

fun SpecType.isCCTank(): Boolean {
  if (trees.contains(SkillTreeType.WITCHCRAFT) && trees.contains(SkillTreeType.OCCULTISM)) return true
  if (META_CC_SPECS.contains(this)) return true
  return false
}

enum class SpecIndication() {
  DPS, TANK, HEALS;
}

/*
 * Initiating Spells : Spells that when cast
 */
val initiatingSpells = listOf(
  "Mana Bolts",
  "Divebomb",
  "Charge",
  "Tiger Strike",
  "Shoot Arrow",
  "Concussive Arrow",
  "Endless Arrows",
  "Absorb Lifeforce",
  "Enervated",
  "Ceaseless Fire",
  "Flamebolt",
  "Freezing Arrow",
  "Arc Lightning",
  "Electrical Arrow",
  "Rapid Strike",
  "Pin Down",
  "Blade Flurry",
  "Entangle",
  "Dancer's Touch",
  "Holy Bolt",
  "Revive",
  "Mana Barrier",
  "Fervent Healing",
  "Bull Rush",
  "Critical Discord"
)

val SkillTreeType.localizedDisplayNameRes: StringResource
  get() = when (this) {
    SkillTreeType.ARCHERY -> Res.string.skill_tree_archery
    SkillTreeType.AURAMANCY -> Res.string.skill_tree_auramancy
    SkillTreeType.BATTLERAGE -> Res.string.skill_tree_battlerage
    SkillTreeType.DEFENSE -> Res.string.skill_tree_defense
    SkillTreeType.GUNSLINGER -> Res.string.skill_tree_gunslinger
    SkillTreeType.MALEDICTION -> Res.string.skill_tree_malediction
    SkillTreeType.OCCULTISM -> Res.string.skill_tree_occultism
    SkillTreeType.SHADOWPLAY -> Res.string.skill_tree_shadowplay
    SkillTreeType.SONGCRAFT -> Res.string.skill_tree_songcraft
    SkillTreeType.SORCERY -> Res.string.skill_tree_sorcery
    SkillTreeType.SPELLDANCE -> Res.string.skill_tree_spelldance
    SkillTreeType.SWIFTBLADE -> Res.string.skill_tree_swiftblade
    SkillTreeType.VITALISM -> Res.string.skill_tree_vitalism
    SkillTreeType.WITCHCRAFT -> Res.string.skill_tree_witchcraft
  }

val SpecType.localizedDisplayNameRes: StringResource
  get() = when (this) {
    SpecType.ABOLISHER -> Res.string.spec_type_abolisher
    SpecType.ABSOLVER -> Res.string.spec_type_absolver
    SpecType.ACTIVIST -> Res.string.spec_type_activist
    SpecType.ADEPT -> Res.string.spec_type_adept
    SpecType.ALCHEMIST -> Res.string.spec_type_alchemist
    SpecType.ANCHORITE -> Res.string.spec_type_anchorite
    SpecType.ANIMIST -> Res.string.spec_type_animist
    SpecType.ANNIHILATOR -> Res.string.spec_type_annihilator
    SpecType.ANTHROPOMANCER -> Res.string.spec_type_anthropomancer
    SpecType.APOSTATE -> Res.string.spec_type_apostate
    SpecType.ARACHNOMANCER -> Res.string.spec_type_arachnomancer
    SpecType.ARCANE_HUNTER -> Res.string.spec_type_arcane_hunter
    SpecType.ARCANIST -> Res.string.spec_type_arcanist
    SpecType.ARCHON -> Res.string.spec_type_archon
    SpecType.ARGENT -> Res.string.spec_type_argent
    SpecType.ASSASSIN -> Res.string.spec_type_assassin
    SpecType.ASTRAL_RANGER -> Res.string.spec_type_astral_ranger
    SpecType.ATHAME -> Res.string.spec_type_athame
    SpecType.AUGMENTOR -> Res.string.spec_type_augmentor
    SpecType.AUGURER -> Res.string.spec_type_augurer
    SpecType.AVATAR -> Res.string.spec_type_avatar
    SpecType.BANEBOLT -> Res.string.spec_type_banebolt
    SpecType.BANESONG -> Res.string.spec_type_banesong
    SpecType.BANSHEE -> Res.string.spec_type_banshee
    SpecType.BARRELACE -> Res.string.spec_type_barrelace
    SpecType.BARRICADE -> Res.string.spec_type_barricade
    SpecType.BASTION -> Res.string.spec_type_bastion
    SpecType.BATTLEMAGE -> Res.string.spec_type_battlemage
    SpecType.BATTLEPRIEST -> Res.string.spec_type_battlepriest
    SpecType.BEGUILER -> Res.string.spec_type_beguiler
    SpecType.BEGUILING_DANCER -> Res.string.spec_type_beguiling_dancer
    SpecType.BLACKGUARD -> Res.string.spec_type_blackguard
    SpecType.BLADE_DANCER -> Res.string.spec_type_blade_dancer
    SpecType.BLADECHANTER -> Res.string.spec_type_bladechanter
    SpecType.BLADEMAGE -> Res.string.spec_type_blademage
    SpecType.BLADESPELL -> Res.string.spec_type_bladespell
    SpecType.BLASTFUSE -> Res.string.spec_type_blastfuse
    SpecType.BLIGHTCASTER -> Res.string.spec_type_blightcaster
    SpecType.BLIGHTER -> Res.string.spec_type_blighter
    SpecType.BLINKHUNTER -> Res.string.spec_type_blinkhunter
    SpecType.BLINKSHOT -> Res.string.spec_type_blinkshot
    SpecType.BLOOD_ARROW -> Res.string.spec_type_blood_arrow
    SpecType.BLOODMAGE -> Res.string.spec_type_bloodmage
    SpecType.BLOODREAVER -> Res.string.spec_type_bloodreaver
    SpecType.BLOODSKALD -> Res.string.spec_type_bloodskald
    SpecType.BLOODTHRALL -> Res.string.spec_type_bloodthrall
    SpecType.BLOODY_DANCER -> Res.string.spec_type_bloody_dancer
    SpecType.BONECHILLER -> Res.string.spec_type_bonechiller
    SpecType.BONESTALKER -> Res.string.spec_type_bonestalker
    SpecType.BONEWEAVER -> Res.string.spec_type_boneweaver
    SpecType.BOUNTY_HUNTER -> Res.string.spec_type_bounty_hunter
    SpecType.BREATHSTEALER -> Res.string.spec_type_breathstealer
    SpecType.BUCCANEER -> Res.string.spec_type_buccaneer
    SpecType.BULWARK -> Res.string.spec_type_bulwark
    SpecType.CABALIST -> Res.string.spec_type_cabalist
    SpecType.CANNONEER -> Res.string.spec_type_cannoneer
    SpecType.CARETAKER -> Res.string.spec_type_caretaker
    SpecType.CHARMER -> Res.string.spec_type_charmer
    SpecType.CLAIRVOYER -> Res.string.spec_type_clairvoyer
    SpecType.CLERIC -> Res.string.spec_type_cleric
    SpecType.COMEDIAN -> Res.string.spec_type_comedian
    SpecType.CONFESSOR -> Res.string.spec_type_confessor
    SpecType.CONFUTATOR -> Res.string.spec_type_confutator
    SpecType.CONJUROR -> Res.string.spec_type_conjuror
    SpecType.CORRUPTOR -> Res.string.spec_type_corruptor
    SpecType.CORSAIR -> Res.string.spec_type_corsair
    SpecType.COVERT_DANCER -> Res.string.spec_type_covert_dancer
    SpecType.CRUSADER -> Res.string.spec_type_crusader
    SpecType.CRYPTWALKER -> Res.string.spec_type_cryptwalker
    SpecType.CULTIST -> Res.string.spec_type_cultist
    SpecType.CURSEBOLT -> Res.string.spec_type_cursebolt
    SpecType.CURSEBRAND -> Res.string.spec_type_cursebrand
    SpecType.CYCLONE -> Res.string.spec_type_cyclone
    SpecType.DAGGERSPELL -> Res.string.spec_type_daggerspell
    SpecType.DARK_AEGIS -> Res.string.spec_type_dark_aegis
    SpecType.DARKNESS_ARTIST -> Res.string.spec_type_darkness_artist
    SpecType.DARKNESS_AVENGER -> Res.string.spec_type_darkness_avenger
    SpecType.DARKNESS_CONTROLLER -> Res.string.spec_type_darkness_controller
    SpecType.DARKNESS_FANATIC -> Res.string.spec_type_darkness_fanatic
    SpecType.DARKNESS_RESOLVER -> Res.string.spec_type_darkness_resolver
    SpecType.DARKNESS_REVOLUTIONIST -> Res.string.spec_type_darkness_revolutionist
    SpecType.DARKNESS_SAVIOR -> Res.string.spec_type_darkness_savior
    SpecType.DARKRUNNER -> Res.string.spec_type_darkrunner
    SpecType.DARKSEER -> Res.string.spec_type_darkseer
    SpecType.DAWNCALLER -> Res.string.spec_type_dawncaller
    SpecType.DEATH_PROPHET -> Res.string.spec_type_death_prophet
    SpecType.DEATH_WARDEN -> Res.string.spec_type_death_warden
    SpecType.DEATHCHORD -> Res.string.spec_type_deathchord
    SpecType.DEATHTRIGGER -> Res.string.spec_type_deathtrigger
    SpecType.DEATHVICAR -> Res.string.spec_type_deathvicar
    SpecType.DEATHWISH -> Res.string.spec_type_deathwish
    SpecType.DECEIVER -> Res.string.spec_type_deceiver
    SpecType.DEFIANT -> Res.string.spec_type_defiant
    SpecType.DEFILER -> Res.string.spec_type_defiler
    SpecType.DEMOLISHER -> Res.string.spec_type_demolisher
    SpecType.DEMONOLOGIST -> Res.string.spec_type_demonologist
    SpecType.DEMONSHOT -> Res.string.spec_type_demonshot
    SpecType.DERVISH -> Res.string.spec_type_dervish
    SpecType.DESTROYER -> Res.string.spec_type_destroyer
    SpecType.DIRGEWEAVER -> Res.string.spec_type_dirgeweaver
    SpecType.DISCIPLE -> Res.string.spec_type_disciple
    SpecType.DIVINER -> Res.string.spec_type_diviner
    SpecType.DOOMBRINGER -> Res.string.spec_type_doombringer
    SpecType.DOOMCALLER -> Res.string.spec_type_doomcaller
    SpecType.DOOMLORD -> Res.string.spec_type_doomlord
    SpecType.DOOMSHOT -> Res.string.spec_type_doomshot
    SpecType.DOOMSKALD -> Res.string.spec_type_doomskald
    SpecType.DOOMSPEAKER -> Res.string.spec_type_doomspeaker
    SpecType.DOURGUARD -> Res.string.spec_type_dourguard
    SpecType.DREADBOW -> Res.string.spec_type_dreadbow
    SpecType.DREADDART -> Res.string.spec_type_dreaddart
    SpecType.DREADHUNTER -> Res.string.spec_type_dreadhunter
    SpecType.DREADNAUGHT -> Res.string.spec_type_dreadnaught
    SpecType.DREADRUNNER -> Res.string.spec_type_dreadrunner
    SpecType.DREADSTONE -> Res.string.spec_type_dreadstone
    SpecType.DREAMBREAKER -> Res.string.spec_type_dreambreaker
    SpecType.DREAMSPINNER -> Res.string.spec_type_dreamspinner
    SpecType.DRUID -> Res.string.spec_type_druid
    SpecType.DUSK_DANCER -> Res.string.spec_type_dusk_dancer
    SpecType.DUSKDIRGE -> Res.string.spec_type_duskdirge
    SpecType.DUSKRAIDER -> Res.string.spec_type_duskraider
    SpecType.EARTHSINGER -> Res.string.spec_type_earthsinger
    SpecType.EBONSHIELD -> Res.string.spec_type_ebonshield
    SpecType.EBONSONG -> Res.string.spec_type_ebonsong
    SpecType.EDGERIPPER -> Res.string.spec_type_edgeripper
    SpecType.EDGEWALKER -> Res.string.spec_type_edgewalker
    SpecType.EIDOLON -> Res.string.spec_type_eidolon
    SpecType.EMISSARY -> Res.string.spec_type_emissary
    SpecType.ENCHANTRIX -> Res.string.spec_type_enchantrix
    SpecType.ENFORCER -> Res.string.spec_type_enforcer
    SpecType.ENIGMATIST -> Res.string.spec_type_enigmatist
    SpecType.EQUALIZER -> Res.string.spec_type_equalizer
    SpecType.EVANGELIST -> Res.string.spec_type_evangelist
    SpecType.EVISCERATOR -> Res.string.spec_type_eviscerator
    SpecType.EVOKER -> Res.string.spec_type_evoker
    SpecType.EXECUTIONER -> Res.string.spec_type_executioner
    SpecType.EXORCIST -> Res.string.spec_type_exorcist
    SpecType.FANATIC -> Res.string.spec_type_fanatic
    SpecType.FANGBORN -> Res.string.spec_type_fangborn
    SpecType.FARSLAYER -> Res.string.spec_type_farslayer
    SpecType.FATEMARK -> Res.string.spec_type_fatemark
    SpecType.FATESPINNER -> Res.string.spec_type_fatespinner
    SpecType.FEAR_ARTIST -> Res.string.spec_type_fear_artist
    SpecType.FEAR_AVENGER -> Res.string.spec_type_fear_avenger
    SpecType.FEAR_CONTROLLER -> Res.string.spec_type_fear_controller
    SpecType.FEAR_RESOLVER -> Res.string.spec_type_fear_resolver
    SpecType.FEAR_REVOLUTIONIST -> Res.string.spec_type_fear_revolutionist
    SpecType.FEAR_SAVIOR -> Res.string.spec_type_fear_savior
    SpecType.FEARFUL_FANATIC -> Res.string.spec_type_fearful_fanatic
    SpecType.FEARSTALKER -> Res.string.spec_type_fearstalker
    SpecType.FEVERSONG -> Res.string.spec_type_feversong
    SpecType.FLASHBLADE -> Res.string.spec_type_flashblade
    SpecType.FLESHSHAPER -> Res.string.spec_type_fleshshaper
    SpecType.FORSAKER -> Res.string.spec_type_forsaker
    SpecType.FREEBOOTER -> Res.string.spec_type_freebooter
    SpecType.FURY_CANON -> Res.string.spec_type_fury_canon
    SpecType.FURY_MAGE -> Res.string.spec_type_fury_mage
    SpecType.GHOSTBLADE -> Res.string.spec_type_ghostblade
    SpecType.GLAMOROUS_AVENGER -> Res.string.spec_type_glamorous_avenger
    SpecType.GLAMOROUS_REVOLUTIONIST -> Res.string.spec_type_glamorous_revolutionist
    SpecType.GLAMOROUS_SAVIOR -> Res.string.spec_type_glamorous_savior
    SpecType.GLOOMKNIGHT -> Res.string.spec_type_gloomknight
    SpecType.GLOOMSPELL -> Res.string.spec_type_gloomspell
    SpecType.GLOOMSTALKER -> Res.string.spec_type_gloomstalker
    SpecType.GRAVE_ADMIRAL -> Res.string.spec_type_grave_admiral
    SpecType.GRAVEBOW -> Res.string.spec_type_gravebow
    SpecType.GRAVESHOT -> Res.string.spec_type_graveshot
    SpecType.GRAVESINGER -> Res.string.spec_type_gravesinger
    SpecType.GRIMSHOT -> Res.string.spec_type_grimshot
    SpecType.GYPSY -> Res.string.spec_type_gypsy
    SpecType.HARBINGER -> Res.string.spec_type_harbinger
    SpecType.HARUSPEX -> Res.string.spec_type_haruspex
    SpecType.HEARTSBANE -> Res.string.spec_type_heartsbane
    SpecType.HELLSHIELD -> Res.string.spec_type_hellshield
    SpecType.HELLWEAVER -> Res.string.spec_type_hellweaver
    SpecType.HERALD -> Res.string.spec_type_herald
    SpecType.HEREAFTER_ARTIST -> Res.string.spec_type_hereafter_artist
    SpecType.HEREAFTER_AVENGER -> Res.string.spec_type_hereafter_avenger
    SpecType.HEREAFTER_CONTROLLER -> Res.string.spec_type_hereafter_controller
    SpecType.HEREAFTER_FANATIC -> Res.string.spec_type_hereafter_fanatic
    SpecType.HEREAFTER_RESOLVER -> Res.string.spec_type_hereafter_resolver
    SpecType.HEREAFTER_REVOLUTIONIST -> Res.string.spec_type_hereafter_revolutionist
    SpecType.HEREAFTER_SAVIOR -> Res.string.spec_type_hereafter_savior
    SpecType.HEX_RANGER -> Res.string.spec_type_hex_ranger
    SpecType.HEX_WARDEN -> Res.string.spec_type_hex_warden
    SpecType.HEXBLADE -> Res.string.spec_type_hexblade
    SpecType.HEXBOLT -> Res.string.spec_type_hexbolt
    SpecType.HEXDANCER -> Res.string.spec_type_hexdancer
    SpecType.HEXSNIPER -> Res.string.spec_type_hexsniper
    SpecType.HIEROPHANT -> Res.string.spec_type_hierophant
    SpecType.HONORGUARD -> Res.string.spec_type_honorguard
    SpecType.HORDEBREAKER -> Res.string.spec_type_hordebreaker
    SpecType.HOWLER -> Res.string.spec_type_howler
    SpecType.ILLUSION_APOSTLE -> Res.string.spec_type_illusion_apostle
    SpecType.ILLUSION_ARGENT -> Res.string.spec_type_illusion_argent
    SpecType.ILLUSION_BLADE -> Res.string.spec_type_illusion_blade
    SpecType.ILLUSION_BLADESPELL -> Res.string.spec_type_illusion_bladespell
    SpecType.ILLUSION_CHASER -> Res.string.spec_type_illusion_chaser
    SpecType.ILLUSION_DANCER -> Res.string.spec_type_illusion_dancer
    SpecType.ILLUSION_DISCIPLE -> Res.string.spec_type_illusion_disciple
    SpecType.ILLUSION_EVIL_SPIRIT -> Res.string.spec_type_illusion_evil_spirit
    SpecType.ILLUSION_FIGHTER -> Res.string.spec_type_illusion_fighter
    SpecType.ILLUSION_GUNSLINGER -> Res.string.spec_type_illusion_gunslinger
    SpecType.ILLUSION_HUNTER -> Res.string.spec_type_illusion_hunter
    SpecType.ILLUSION_MAGICIAN -> Res.string.spec_type_illusion_magician
    SpecType.ILLUSION_MONK -> Res.string.spec_type_illusion_monk
    SpecType.ILLUSION_MONK_ARCHER -> Res.string.spec_type_illusion_monk_archer
    SpecType.ILLUSION_PILGRIM -> Res.string.spec_type_illusion_pilgrim
    SpecType.ILLUSION_REAPER -> Res.string.spec_type_illusion_reaper
    SpecType.ILLUSION_SAGE -> Res.string.spec_type_illusion_sage
    SpecType.ILLUSION_STALKER -> Res.string.spec_type_illusion_stalker
    SpecType.ILLUSION_SWORDSPELL -> Res.string.spec_type_illusion_swordspell
    SpecType.ILLUSION_TERMINATOR -> Res.string.spec_type_illusion_terminator
    SpecType.ILLUSION_TRICKSTER -> Res.string.spec_type_illusion_trickster
    SpecType.ILLUSION_WARMACHINE -> Res.string.spec_type_illusion_warmachine
    SpecType.IMMORTALIST -> Res.string.spec_type_immortalist
    SpecType.INCANTATOR -> Res.string.spec_type_incantator
    SpecType.INFILTRATOR -> Res.string.spec_type_infiltrator
    SpecType.INQUISITOR -> Res.string.spec_type_inquisitor
    SpecType.INSTIGATOR -> Res.string.spec_type_instigator
    SpecType.INVADER -> Res.string.spec_type_invader
    SpecType.INVOKER -> Res.string.spec_type_invoker
    SpecType.IRONSOUL -> Res.string.spec_type_ironsoul
    SpecType.JINXMENDER -> Res.string.spec_type_jinxmender
    SpecType.JUSTICAR -> Res.string.spec_type_justicar
    SpecType.LAMENTOR -> Res.string.spec_type_lamentor
    SpecType.LEADBITER -> Res.string.spec_type_leadbiter
    SpecType.LEADDANCER -> Res.string.spec_type_leaddancer
    SpecType.LEGIONNAIRE -> Res.string.spec_type_legionnaire
    SpecType.LIBERATOR -> Res.string.spec_type_liberator
    SpecType.LIFESTEALER -> Res.string.spec_type_lifestealer
    SpecType.LIGHTSOUL -> Res.string.spec_type_lightsoul
    SpecType.LOREBREAKER -> Res.string.spec_type_lorebreaker
    SpecType.MAGISTER -> Res.string.spec_type_magister
    SpecType.MARAUDER -> Res.string.spec_type_marauder
    SpecType.MARROWBLADE -> Res.string.spec_type_marrowblade
    SpecType.MENTALIST -> Res.string.spec_type_mentalist
    SpecType.MERCENARY -> Res.string.spec_type_mercenary
    SpecType.MINDSLAVER -> Res.string.spec_type_mindslaver
    SpecType.MINDSLAYER -> Res.string.spec_type_mindslayer
    SpecType.MINSTREL -> Res.string.spec_type_minstrel
    SpecType.MUSICIAN -> Res.string.spec_type_musician
    SpecType.NATURALIST -> Res.string.spec_type_naturalist
    SpecType.NECROMANCER -> Res.string.spec_type_necromancer
    SpecType.NIGHTBEARER -> Res.string.spec_type_nightbearer
    SpecType.NIGHTBLADE -> Res.string.spec_type_nightblade
    SpecType.NIGHTCLOAK -> Res.string.spec_type_nightcloak
    SpecType.NIGHTHAUNT -> Res.string.spec_type_nighthaunt
    SpecType.NIGHTMARE_ARTIST -> Res.string.spec_type_nightmare_artist
    SpecType.NIGHTMARE_AVENGER -> Res.string.spec_type_nightmare_avenger
    SpecType.NIGHTMARE_CONTROLLER -> Res.string.spec_type_nightmare_controller
    SpecType.NIGHTMARE_FANATIC -> Res.string.spec_type_nightmare_fanatic
    SpecType.NIGHTMARE_RESOLVER -> Res.string.spec_type_nightmare_resolver
    SpecType.NIGHTMARE_REVOLUTIONIST -> Res.string.spec_type_nightmare_revolutionist
    SpecType.NIGHTMARE_SAVIOR -> Res.string.spec_type_nightmare_savior
    SpecType.NIGHTWITCH -> Res.string.spec_type_nightwitch
    SpecType.NOCTURNE -> Res.string.spec_type_nocturne
    SpecType.NULLWEAVER -> Res.string.spec_type_nullweaver
    SpecType.OATHSWORN -> Res.string.spec_type_oathsworn
    SpecType.ORACLE -> Res.string.spec_type_oracle
    SpecType.OUTRIDER -> Res.string.spec_type_outrider
    SpecType.PALADIN -> Res.string.spec_type_paladin
    SpecType.PHANTASM -> Res.string.spec_type_phantasm
    SpecType.PHANTOM -> Res.string.spec_type_phantom
    SpecType.PIT_FIGHTER -> Res.string.spec_type_pit_fighter
    SpecType.PITFIEND -> Res.string.spec_type_pitfiend
    SpecType.PLANESHIFTER -> Res.string.spec_type_planeshifter
    SpecType.PONTIFEX -> Res.string.spec_type_pontifex
    SpecType.POXBANE -> Res.string.spec_type_poxbane
    SpecType.PRIMEVAL -> Res.string.spec_type_primeval
    SpecType.PRIVATEER -> Res.string.spec_type_privateer
    SpecType.PURGATOR -> Res.string.spec_type_purgator
    SpecType.PURIFIER -> Res.string.spec_type_purifier
    SpecType.QUICKDRAW -> Res.string.spec_type_quickdraw
    SpecType.RAGEBINDER -> Res.string.spec_type_ragebinder
    SpecType.RAGECHANTER -> Res.string.spec_type_ragechanter
    SpecType.RANGER -> Res.string.spec_type_ranger
    SpecType.RAVAGER -> Res.string.spec_type_ravager
    SpecType.REALMSHIFTER -> Res.string.spec_type_realmshifter
    SpecType.REAPER -> Res.string.spec_type_reaper
    SpecType.REINCARNATOR -> Res.string.spec_type_reincarnator
    SpecType.REQUIEM -> Res.string.spec_type_requiem
    SpecType.REVELATOR -> Res.string.spec_type_revelator
    SpecType.REVENANT -> Res.string.spec_type_revenant
    SpecType.REVOLUTIONIST -> Res.string.spec_type_revolutionist
    SpecType.RUNEBREAKER -> Res.string.spec_type_runebreaker
    SpecType.SABOTEUR -> Res.string.spec_type_saboteur
    SpecType.SCION -> Res.string.spec_type_scion
    SpecType.SEAL_ARTIST -> Res.string.spec_type_seal_artist
    SpecType.SEAL_CONTROLLER -> Res.string.spec_type_seal_controller
    SpecType.SEAL_FANATIC -> Res.string.spec_type_seal_fanatic
    SpecType.SEAL_RESOLVER -> Res.string.spec_type_seal_resolver
    SpecType.SEAL_REVOLUTIONIST -> Res.string.spec_type_seal_revolutionist
    SpecType.SECRET_CHOREOGRAPHER -> Res.string.spec_type_secret_choreographer
    SpecType.SHADEHUNTER -> Res.string.spec_type_shadehunter
    SpecType.SHADESTRIKER -> Res.string.spec_type_shadestriker
    SpecType.SHADOW_PROPHET -> Res.string.spec_type_shadow_prophet
    SpecType.SHADOW_REAPER -> Res.string.spec_type_shadow_reaper
    SpecType.SHADOWBANE -> Res.string.spec_type_shadowbane
    SpecType.SHADOWBLADE -> Res.string.spec_type_shadowblade
    SpecType.SHADOWDANCER -> Res.string.spec_type_shadowdancer
    SpecType.SHADOWGUARD -> Res.string.spec_type_shadowguard
    SpecType.SHADOWKNIGHT -> Res.string.spec_type_shadowknight
    SpecType.SHADOWSONG -> Res.string.spec_type_shadowsong
    SpecType.SHAMAN -> Res.string.spec_type_shaman
    SpecType.SHROUDMASTER -> Res.string.spec_type_shroudmaster
    SpecType.SHROUDSONG -> Res.string.spec_type_shroudsong
    SpecType.SHROUDSPELL -> Res.string.spec_type_shroudspell
    SpecType.SKULL_WARDEN -> Res.string.spec_type_skull_warden
    SpecType.SKULLKNIGHT -> Res.string.spec_type_skullknight
    SpecType.SONG_CONTROLLER -> Res.string.spec_type_song_controller
    SpecType.SONG_FANATIC -> Res.string.spec_type_song_fanatic
    SpecType.SOOTHSAYER -> Res.string.spec_type_soothsayer
    SpecType.SORROWSONG -> Res.string.spec_type_sorrowsong
    SpecType.SOULBOW -> Res.string.spec_type_soulbow
    SpecType.SOULLEECH -> Res.string.spec_type_soulleech
    SpecType.SOULSONG -> Res.string.spec_type_soulsong
    SpecType.SOULTHIEF -> Res.string.spec_type_soulthief
    SpecType.SPECTRE -> Res.string.spec_type_spectre
    SpecType.SPELLBINDER -> Res.string.spec_type_spellbinder
    SpecType.SPELLBOW -> Res.string.spec_type_spellbow
    SpecType.SPELLSINGER -> Res.string.spec_type_spellsinger
    SpecType.SPELLSONG -> Res.string.spec_type_spellsong
    SpecType.SPELLSWORD -> Res.string.spec_type_spellsword
    SpecType.SPELLWEAVER -> Res.string.spec_type_spellweaver
    SpecType.SPIRITUALIST -> Res.string.spec_type_spiritualist
    SpecType.STONE_ARROW -> Res.string.spec_type_stone_arrow
    SpecType.STORMCASTER -> Res.string.spec_type_stormcaster
    SpecType.STORMCHASER -> Res.string.spec_type_stormchaser
    SpecType.STRIKING_DANCER -> Res.string.spec_type_striking_dancer
    SpecType.SUNSET_DANCER -> Res.string.spec_type_sunset_dancer
    SpecType.SWIFTSHOT -> Res.string.spec_type_swiftshot
    SpecType.SWIFTSTONE -> Res.string.spec_type_swiftstone
    SpecType.SYNERGIST -> Res.string.spec_type_synergist
    SpecType.TEMPEST -> Res.string.spec_type_tempest
    SpecType.TEMPLAR -> Res.string.spec_type_templar
    SpecType.THAUMATURGE -> Res.string.spec_type_thaumaturge
    SpecType.TOMB_WARDEN -> Res.string.spec_type_tomb_warden
    SpecType.TOMBCALLER -> Res.string.spec_type_tombcaller
    SpecType.TOMBSONG -> Res.string.spec_type_tombsong
    SpecType.TOUGH_DANCER -> Res.string.spec_type_tough_dancer
    SpecType.TRAGEDY_DANCER -> Res.string.spec_type_tragedy_dancer
    SpecType.TRICKSTER -> Res.string.spec_type_trickster
    SpecType.TWISTEDSHOT -> Res.string.spec_type_twistedshot
    SpecType.UNKNOWN -> Res.string.spec_type_unknown
    SpecType.VANGUARD -> Res.string.spec_type_vanguard
    SpecType.VENOMBITE -> Res.string.spec_type_venombite
    SpecType.VICIOUS_CHOREOGRAPHER -> Res.string.spec_type_vicious_choreographer
    SpecType.VISIONARY -> Res.string.spec_type_visionary
    SpecType.VOIDSTALKER -> Res.string.spec_type_voidstalker
    SpecType.VOIDWALKER -> Res.string.spec_type_voidwalker
    SpecType.WAR_DANCER -> Res.string.spec_type_war_dancer
    SpecType.WARCASTER -> Res.string.spec_type_warcaster
    SpecType.WARPRIEST -> Res.string.spec_type_warpriest
    SpecType.WIDOWHAUNT -> Res.string.spec_type_widowhaunt
    SpecType.WILDCLAW -> Res.string.spec_type_wildclaw
    SpecType.WILDMAGE -> Res.string.spec_type_wildmage
    SpecType.WINDSEEKER -> Res.string.spec_type_windseeker
    SpecType.WINDSHAPER -> Res.string.spec_type_windshaper
    SpecType.WINDSOUL -> Res.string.spec_type_windsoul
    SpecType.WISPWALKER -> Res.string.spec_type_wispwalker
    SpecType.WITCH_DOCTOR -> Res.string.spec_type_witch_doctor
    SpecType.WRATHSLAYER -> Res.string.spec_type_wrathslayer
    SpecType.ZEALOT -> Res.string.spec_type_zealot
    SpecType.ZEPHYR -> Res.string.spec_type_zephyr
  }
