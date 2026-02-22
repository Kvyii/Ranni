package com.ranni.app.data.model

import androidx.compose.ui.graphics.Color

data class RouteColor(
    val name: String,
    val grade: String,
    val color: Color,
    val score: Int
)

data class Gym(
    val name: String,
    val routes: List<RouteColor>,
    val comingSoon: Boolean = false
)

val gyms = listOf(
    Gym(
        name = "9 Degrees",
        routes = listOf(
            RouteColor("Green",  "VB",      Color(0xFF60B555),   75),
            RouteColor("Blue",   "V0",      Color(0xFF4279C7),  100),
            RouteColor("Teal",   "V1 - V2", Color(0xFF42B5C7),  150),
            RouteColor("Pink",   "V2 - V3", Color(0xFFDB72CD),  250),
            RouteColor("Orange", "V3 - V4", Color(0xFFA12F12),  450),
            RouteColor("Black",  "V5 - V6", Color(0xFF050101),  700),
            RouteColor("Purple", "V6 - V8", Color(0xFF6A3CBA), 1000),
            RouteColor("White",  "V7+",     Color(0xFFEDEDED), 1300),
        )
    ),
    Gym(
        name = "Custom",
        routes = listOf(
            RouteColor("V0",  "V0",  Color(0xFFEDEDED),  100),
            RouteColor("V1",  "V1",  Color(0xFFEDEDED),  175),
            RouteColor("V2",  "V2",  Color(0xFFEDEDED),  275),
            RouteColor("V3",  "V3",  Color(0xFFEDEDED),  400),
            RouteColor("V4",  "V4",  Color(0xFFEDEDED),  550),
            RouteColor("V5",  "V5",  Color(0xFFEDEDED),  725),
            RouteColor("V6",  "V6",  Color(0xFFEDEDED),  900),
            RouteColor("V7",  "V7",  Color(0xFFEDEDED), 1100),
            RouteColor("V8",  "V8",  Color(0xFFEDEDED), 1300),
            RouteColor("V9",  "V9",  Color(0xFFEDEDED), 1500),
            RouteColor("V10", "V10", Color(0xFFEDEDED), 1700),
            RouteColor("V11", "V11", Color(0xFFEDEDED), 1850),
            RouteColor("V12", "V12", Color(0xFFEDEDED), 2000),
        )
    ),
    Gym(
        name = "Outdoor (V-Grade)",
        routes = listOf(
            RouteColor("V0",  "V0",  Color(0xFF283673),  110),
            RouteColor("V1",  "V1",  Color(0xFF283673),  195),
            RouteColor("V2",  "V2",  Color(0xFF283673),  300),
            RouteColor("V3",  "V3",  Color(0xFF283673),  440),
            RouteColor("V4",  "V4",  Color(0xFF283673),  605),
            RouteColor("V5",  "V5",  Color(0xFF283673),  800),
            RouteColor("V6",  "V6",  Color(0xFF283673),  990),
            RouteColor("V7",  "V7",  Color(0xFF283673), 1210),
            RouteColor("V8",  "V8",  Color(0xFF283673), 1430),
            RouteColor("V9",  "V9",  Color(0xFF283673), 1650),
            RouteColor("V10", "V10", Color(0xFF283673), 1870),
            RouteColor("V11", "V11", Color(0xFF283673), 2035),
            RouteColor("V12", "V12", Color(0xFF283673), 2200),
        )
    ),
    Gym(
        name = "Outdoor (YDS Grade)",
        routes = listOf(
            RouteColor("5.9",   "5.9",   Color(0xFF67578c),  110),
            RouteColor("5.10c", "5.10c", Color(0xFF67578c),  195),
            RouteColor("5.10d", "5.10d", Color(0xFF67578c),  300),
            RouteColor("5.11a", "5.11a", Color(0xFF67578c),  440),
            RouteColor("5.11c", "5.11c", Color(0xFF67578c),  605),
            RouteColor("5.12a", "5.12a", Color(0xFF67578c),  800),
            RouteColor("5.12b", "5.12b", Color(0xFF67578c),  990),
            RouteColor("5.12c", "5.12c", Color(0xFF67578c), 1210),
            RouteColor("5.12d", "5.12d", Color(0xFF67578c), 1430),
            RouteColor("5.13a", "5.13a", Color(0xFF67578c), 1650),
            RouteColor("5.13c", "5.13c", Color(0xFF67578c), 1870),
            RouteColor("5.13d", "5.13d", Color(0xFF67578c), 2035),
            RouteColor("5.14a", "5.14a", Color(0xFF67578c), 2200),
            RouteColor("5.14b", "5.14b", Color(0xFF67578c), 2420),
        )
    ),
    Gym(name = "Nomad", routes = emptyList(), comingSoon = true),
    Gym(name = "Blochaus", routes = emptyList(), comingSoon = true),
)

// Unambiguous route lookup keyed by (gymName, routeName) pair.
// Route names are NOT globally unique (e.g. "V3" appears in Custom, Outdoor V-Grade, etc.),
// so a single-string key would cause collisions. Always use gymName + routeName together.
val routeMap: Map<Pair<String, String>, RouteColor> = gyms
    .flatMap { gym -> gym.routes.map { (gym.name to it.name) to it } }
    .toMap()

// Convenience: look up a route's display color given gym + route name
fun routeColor(gymName: String, routeName: String): Color =
    routeMap[gymName to routeName]?.color ?: Color.White

// Convenience: look up a route's grade string given gym + route name
fun routeGrade(gymName: String, routeName: String): String =
    routeMap[gymName to routeName]?.grade ?: routeName

// Gyms whose dots render as hollow/outline circles instead of filled dots.
val outlineGyms: Set<String> = setOf("Custom", "Outdoor V-Grade", "Outdoor YDS Grade")
