package com.kauri.siminfo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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

enum class PermissionState { CHECKING, GRANTED_FULL, GRANTED_PARTIAL, DENIED }

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
    var permState by remember { mutableStateOf(PermissionState.CHECKING) }

    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { results ->
        val phoneState = results[Manifest.permission.READ_PHONE_STATE] == true
        val phoneNumbers = results[Manifest.permission.READ_PHONE_NUMBERS] == true
        permState = when {
            !phoneState -> PermissionState.DENIED
            phoneNumbers -> PermissionState.GRANTED_FULL
            else -> PermissionState.GRANTED_PARTIAL
        }
    }

    LaunchedEffect(Unit) {
        val phoneState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        val phoneNumbers = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED

        permState = when {
            phoneState && phoneNumbers -> PermissionState.GRANTED_FULL
            phoneState -> PermissionState.GRANTED_PARTIAL
            else -> {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_PHONE_NUMBERS,
                    )
                )
                PermissionState.CHECKING
            }
        }
    }

    when (permState) {
        PermissionState.CHECKING -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        PermissionState.DENIED -> PermissionDeniedScreenWithContext()

        else -> {
            val phoneNumbersGranted = permState == PermissionState.GRANTED_FULL
            val repo = remember(context) { SimInfoRepository(context) }
            var refreshKey by remember { mutableIntStateOf(0) }
            val simCards = remember(permState, refreshKey) { repo.getSimCards(phoneNumbersGranted) }

            // Refresh whenever the app comes back to the foreground.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) refreshKey++
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Refresh on real-time telephony changes (network type, call state, data state).
            DisposableEffect(Unit) {
                val tm = context.getSystemService(TelephonyManager::class.java)
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
internal fun PermissionDeniedScreen(onOpenSettings: () -> Unit = {}) {
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
        Button(onClick = onOpenSettings) {
            Text("Open Settings")
        }
    }
}

@Composable
private fun PermissionDeniedScreenWithContext() {
    val context = LocalContext.current
    PermissionDeniedScreen(onOpenSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    })
}

@Preview(name = "Permission Denied")
@Composable
private fun PermissionDeniedPreview() {
    SimInfoTheme {
        Scaffold {
            Box(modifier = Modifier.padding(it)) {
                PermissionDeniedScreen()
            }
        }
    }
}