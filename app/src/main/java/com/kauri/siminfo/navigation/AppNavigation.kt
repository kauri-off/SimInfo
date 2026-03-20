package com.kauri.siminfo.navigation

sealed class Screen(val route: String) {
    object SimList : Screen("sim_list")
    object SimDetail : Screen("sim_detail/{subId}") {
        fun createRoute(subId: Int) = "sim_detail/$subId"
    }
}
