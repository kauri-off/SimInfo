package com.kauri.siminfo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SimInfoRepository(private val context: Context) {

    fun getSimCards(phoneNumbersGranted: Boolean): List<SimCardInfo> {
        return try {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            val subs = sm.activeSubscriptionInfoList ?: return emptyList()
            subs.map { info ->
                val tm = context.getSystemService(TelephonyManager::class.java)
                    .createForSubscriptionId(info.subscriptionId)
                val number = if (phoneNumbersGranted) {
                    try {
                        getPhoneNumber(sm, info)
                    } catch (_: SecurityException) {
                        null
                    }
                } else null

                SimCardInfo(
                    subscriptionId = info.subscriptionId,
                    iccIdLast4 = iccIdLast4(info.iccId),
                    slotIndex = info.simSlotIndex,
                    displayName = info.displayName?.toString() ?: "",
                    carrierName = info.carrierName?.toString() ?: "",
                    phoneNumber = number,
                    countryIso = info.countryIso ?: "",
                    mcc = getMcc(info),
                    mnc = getMnc(info),
                    dataRoaming = info.dataRoaming == SubscriptionManager.DATA_ROAMING_ENABLE,
                    networkOperatorName = tm.networkOperatorName ?: "",
                    networkCountryIso = tm.networkCountryIso ?: "",
                    networkTypeLabel = networkTypeLabel(tm.dataNetworkType),
                    phoneTypeLabel = phoneTypeLabel(tm.phoneType),
                    simStateLabel = simStateLabel(tm.simState),
                    isNetworkRoaming = tm.isNetworkRoaming,
                    callStateLabel = callStateLabel(getCallState(tm)),
                    dataStateLabel = dataStateLabel(tm.dataState),
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_NUMBERS)
    private fun getPhoneNumber(sm: SubscriptionManager, info: SubscriptionInfo): String? =
        sm.getPhoneNumber(info.subscriptionId).takeIf { it.isNotBlank() }

    private fun getMcc(info: SubscriptionInfo): String =
        info.mccString ?: ""

    private fun getMnc(info: SubscriptionInfo): String =
        info.mncString ?: ""

    private fun iccIdLast4(raw: String?): String? =
        if (!raw.isNullOrBlank() && raw.length >= 4) raw.takeLast(4) else null

    // NETWORK_TYPE_NR requires API 29; safe to reference as a constant — older devices never return it
    @SuppressLint("NewApi")
    private fun networkTypeLabel(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        TelephonyManager.NETWORK_TYPE_HSPAP,
        TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA,
        TelephonyManager.NETWORK_TYPE_UMTS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
        else -> "Unknown"
    }

    private fun phoneTypeLabel(type: Int): String = when (type) {
        TelephonyManager.PHONE_TYPE_GSM -> "GSM"
        TelephonyManager.PHONE_TYPE_SIP -> "SIP"
        else -> "None"
    }

    // NOT_READY / PERM_DISABLED / CARD_IO_ERROR / CARD_RESTRICTED require API 26;
    // safe to reference — older devices never return these values
    @SuppressLint("NewApi")
    private fun simStateLabel(state: Int): String = when (state) {
        TelephonyManager.SIM_STATE_READY -> "Ready"
        TelephonyManager.SIM_STATE_ABSENT -> "Absent"
        TelephonyManager.SIM_STATE_PIN_REQUIRED -> "PIN Required"
        TelephonyManager.SIM_STATE_PUK_REQUIRED -> "PUK Required"
        TelephonyManager.SIM_STATE_NETWORK_LOCKED -> "Network Locked"
        TelephonyManager.SIM_STATE_NOT_READY -> "Not Ready"
        TelephonyManager.SIM_STATE_PERM_DISABLED -> "Permanently Disabled"
        TelephonyManager.SIM_STATE_CARD_IO_ERROR -> "IO Error"
        TelephonyManager.SIM_STATE_CARD_RESTRICTED -> "Restricted"
        else -> "Unknown"
    }

    private fun getCallState(tm: TelephonyManager): Int {
        var state = TelephonyManager.CALL_STATE_IDLE
        val latch = CountDownLatch(1)
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(callState: Int) {
                state = callState
                latch.countDown()
            }
        }
        tm.registerTelephonyCallback({ it.run() }, callback)
        latch.await(500, TimeUnit.MILLISECONDS)
        tm.unregisterTelephonyCallback(callback)
        return state
    }

    private fun callStateLabel(state: Int): String = when (state) {
        TelephonyManager.CALL_STATE_IDLE -> "Idle"
        TelephonyManager.CALL_STATE_RINGING -> "Ringing"
        TelephonyManager.CALL_STATE_OFFHOOK -> "Off-hook"
        else -> "Unknown"
    }

    // DATA_DISCONNECTING requires API 31; safe to reference — older devices never return it
    @SuppressLint("NewApi")
    private fun dataStateLabel(state: Int): String = when (state) {
        TelephonyManager.DATA_DISCONNECTED -> "Disconnected"
        TelephonyManager.DATA_CONNECTING -> "Connecting"
        TelephonyManager.DATA_CONNECTED -> "Connected"
        TelephonyManager.DATA_SUSPENDED -> "Suspended"
        TelephonyManager.DATA_DISCONNECTING -> "Disconnecting"
        else -> "Unknown"
    }
}
