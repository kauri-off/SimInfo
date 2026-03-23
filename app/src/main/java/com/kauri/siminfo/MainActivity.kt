package com.kauri.siminfo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kauri.siminfo.navigation.Screen
import com.kauri.siminfo.ui.SimDetailScreen
import com.kauri.siminfo.ui.SimListScreen
import com.kauri.siminfo.ui.theme.SimInfoTheme

enum class PermissionState { GRANTED_FULL, GRANTED_PARTIAL, DENIED }

private fun resolvePermissionState(context: Context): PermissionState {
    val phoneState = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED
    val phoneNumbers = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_PHONE_NUMBERS
    ) == PackageManager.PERMISSION_GRANTED
    return when {
        phoneState && phoneNumbers -> PermissionState.GRANTED_FULL
        phoneState -> PermissionState.GRANTED_PARTIAL
        else -> PermissionState.DENIED
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimInfoTheme {
                SimInfoApp()
            }
        }
    }
}

@Composable
private fun SimInfoApp() {
    val context = LocalContext.current
    var permState by remember { mutableStateOf(resolvePermissionState(context)) }

    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { results ->
        val phoneState = results[Manifest.permission.READ_PHONE_STATE] == true
        val phoneNumbers = results[Manifest.permission.READ_PHONE_NUMBERS] == true
        permState = when {
            !phoneState -> PermissionState.DENIED
            phoneNumbers -> PermissionState.GRANTED_FULL
            else -> PermissionState.GRANTED_PARTIAL
        }
    }

    // Re-check whenever the app comes back to the foreground (e.g. user granted from Settings).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permState = resolvePermissionState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (permState) {
        PermissionState.DENIED -> Scaffold { padding ->
            Box(modifier = Modifier.padding(padding)) {
                PermissionRequestScreen(
                    onRequest = {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.READ_PHONE_NUMBERS,
                            )
                        )
                    }
                )
            }
        }

        else -> {
            val phoneNumbersGranted = permState == PermissionState.GRANTED_FULL
            val repo = remember(context) { SimInfoRepository(context) }
            var refreshKey by remember { mutableIntStateOf(0) }
            val simCards = remember(permState, refreshKey) { repo.getSimCards(phoneNumbersGranted) }

            // Refresh on real-time telephony changes (network type, call state, data state).
            DisposableEffect(Unit) {
                val tm = context.getSystemService(TelephonyManager::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val cb = object :
                        TelephonyCallback(),
                        TelephonyCallback.DataConnectionStateListener,
                        TelephonyCallback.CallStateListener,
                        TelephonyCallback.ServiceStateListener {
                        override fun onDataConnectionStateChanged(state: Int, networkType: Int) { refreshKey++ }
                        override fun onCallStateChanged(state: Int) { refreshKey++ }
                        override fun onServiceStateChanged(serviceState: ServiceState) { refreshKey++ }
                    }
                    tm.registerTelephonyCallback(context.mainExecutor, cb)
                    onDispose { tm.unregisterTelephonyCallback(cb) }
                } else {
                    @Suppress("DEPRECATION")
                    val listener = object : PhoneStateListener() {
                        override fun onDataConnectionStateChanged(state: Int, networkType: Int) { refreshKey++ }
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) { refreshKey++ }
                        override fun onServiceStateChanged(serviceState: ServiceState) { refreshKey++ }
                    }
                    @Suppress("DEPRECATION")
                    tm.listen(
                        listener,
                        PhoneStateListener.LISTEN_CALL_STATE or
                        PhoneStateListener.LISTEN_DATA_CONNECTION_STATE or
                        PhoneStateListener.LISTEN_SERVICE_STATE
                    )
                    @Suppress("DEPRECATION")
                    onDispose { tm.listen(listener, PhoneStateListener.LISTEN_NONE) }
                }
            }

            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Screen.SimList.route,
            ) {
                composable(
                    route = Screen.SimList.route,
                    exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } },
                    popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } },
                ) {
                    SimListScreen(
                        simCards = simCards,
                        onSimClick = { subId ->
                            navController.navigate(Screen.SimDetail.createRoute(subId))
                        },
                    )
                }
                composable(
                    route = Screen.SimDetail.route,
                    arguments = listOf(navArgument("subId") { type = NavType.IntType }),
                    enterTransition = { slideInHorizontally(tween(300)) { it } },
                    popExitTransition = { slideOutHorizontally(tween(300)) { it } },
                ) { backStackEntry ->
                    val subId = backStackEntry.arguments!!.getInt("subId")
                    val sim = simCards.find { it.subscriptionId == subId }
                    if (sim != null) {
                        SimDetailScreen(
                            sim = sim,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PermissionRequestScreen(onRequest: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Phone permission required",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Grant READ_PHONE_STATE permission to view SIM card information.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text("Grant Permission")
        }
    }
}

@Preview(name = "Permission Request")
@Composable
private fun PermissionRequestPreview() {
    SimInfoTheme {
        Scaffold {
            Box(modifier = Modifier.padding(it)) {
                PermissionRequestScreen()
            }
        }
    }
}
