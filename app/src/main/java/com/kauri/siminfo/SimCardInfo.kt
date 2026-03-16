package com.kauri.siminfo

data class SimCardInfo(
    // From SubscriptionInfo
    val subscriptionId: Int,
    val iccIdLast4: String?,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String?,
    val countryIso: String,
    val mcc: String,
    val mnc: String,
    val dataRoaming: Boolean,
    // From TelephonyManager (subscription-scoped)
    val networkOperatorName: String,
    val networkCountryIso: String,
    val networkTypeLabel: String,
    val phoneTypeLabel: String,
    val simStateLabel: String,
    val isNetworkRoaming: Boolean,
    val callStateLabel: String,
    val dataStateLabel: String,
)
