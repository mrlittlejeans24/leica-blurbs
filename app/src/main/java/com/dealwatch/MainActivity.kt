package com.dealwatch

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dealwatch.ui.AddProductScreen
import com.dealwatch.ui.ProductDetailScreen
import com.dealwatch.ui.ProductListScreen
import com.dealwatch.ui.SettingsScreen
import com.dealwatch.ui.theme.DealWatchTheme
import com.dealwatch.viewmodel.ProductViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DealWatchTheme {
                val vm: ProductViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}

@Composable
private fun AppRoot(vm: ProductViewModel) {
    val navController = rememberNavController()

    // Ask for notification permission once on first composition (Android 13+).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored; DealNotifier re-checks before posting */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            val products by vm.products.collectAsStateWithLifecycle()
            ProductListScreen(
                products = products,
                onAdd = { navController.navigate("add") },
                onOpen = { id -> navController.navigate("detail/$id") },
                onCheckNow = vm::checkNow,
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(
                initialKey = vm.getApiKey(),
                onSave = { key -> vm.saveApiKey(key) },
                onBack = { navController.popBackStack() },
            )
        }
        composable("add") {
            AddProductScreen(
                onSave = { query, target ->
                    vm.addProduct(query, target)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            ProductDetailScreen(
                vm = vm,
                productId = id,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}
