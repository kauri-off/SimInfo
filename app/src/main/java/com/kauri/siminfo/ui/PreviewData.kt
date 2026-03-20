package com.kauri.siminfo.ui

import com.kauri.siminfo.SimCardInfo

internal val previewSim1 = SimCardInfo(
    subscriptionId = 1,
    iccIdLast4 = "4321",
    slotIndex = 0,
    displayName = "Personal",
    carrierName = "T-Mobile",
    phoneNumber = "+1 555-0100",
    countryIso = "us",
    mcc = "310",
    mnc = "260",
    dataRoaming = false,
    networkOperatorName = "T-Mobile",
    networkCountryIso = "us",
    networkTypeLabel = "5G",
    phoneTypeLabel = "GSM",
    simStateLabel = "Ready",
    isNetworkRoaming = false,
    callStateLabel = "Idle",
    dataStateLabel = "Connected",
)

internal val previewSim2 = SimCardInfo(
    subscriptionId = 2,
    iccIdLast4 = "8765",
    slotIndex = 1,
    displayName = "Work",
    carrierName = "Verizon",
    phoneNumber = null,
    countryIso = "us",
    mcc = "311",
    mnc = "480",
    dataRoaming = true,
    networkOperatorName = "Verizon",
    networkCountryIso = "us",
    networkTypeLabel = "LTE",
    phoneTypeLabel = "CDMA",
    simStateLabel = "Ready",
    isNetworkRoaming = true,
    callStateLabel = "Idle",
    dataStateLabel = "Disconnected",
)
