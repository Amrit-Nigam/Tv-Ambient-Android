package com.tvport.dashboard.ui.tiles.battery

import kotlinx.serialization.Serializable

/** One device's battery as rendered by the tile. [level] null == unknown / not reporting. */
data class DeviceBattery(
    val level: Int?,
    val charging: Boolean,
    val stale: Boolean = false,
)

/** Every device the Batteries tile shows, Apple-widget style. */
data class BatteryUi(
    val mac: DeviceBattery,
    val phone: DeviceBattery,
    val airpods: DeviceBattery,
    val airpodsCase: DeviceBattery,
)

// ── Wire format from the Mac server's /battery endpoint ─────────────────────────────────────────
@Serializable
data class BatteryDto(
    val mac: DeviceDto = DeviceDto(),
    val phone: DeviceDto = DeviceDto(),
    val airpods: AirpodsDto = AirpodsDto(),
)

@Serializable
data class DeviceDto(
    val level: Int? = null,
    val charging: Boolean = false,
    val present: Boolean = false,
    val stale: Boolean = false,
)

@Serializable
data class AirpodsDto(
    val earbuds: Int? = null,
    val case: Int? = null,
    val connected: Boolean = false,
)

fun BatteryDto.toUi(): BatteryUi = BatteryUi(
    mac = DeviceBattery(level = mac.level, charging = mac.charging, stale = mac.stale),
    phone = DeviceBattery(level = phone.level, charging = phone.charging, stale = phone.stale),
    airpods = DeviceBattery(level = airpods.earbuds, charging = false),
    airpodsCase = DeviceBattery(level = airpods.case, charging = false),
)
