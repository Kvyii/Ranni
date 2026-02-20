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
    Gym(name = "Nomad", routes = emptyList(), comingSoon = true),
    Gym(name = "Blochaus", routes = emptyList(), comingSoon = true),
)

val climbColorMap: Map<String, Color> = gyms
    .flatMap { it.routes }
    .associate { it.name to it.color }

val climbGradeMap: Map<String, String> = gyms
    .flatMap { it.routes }
    .associate { it.name to it.grade }

// Maps a route color name to the gym it belongs to (e.g. "Green" -> "9 Degrees", "V3" -> "Custom")
val climbGymMap: Map<String, String> = gyms
    .flatMap { gym -> gym.routes.map { it.name to gym.name } }
    .toMap()

// Route names belonging to the Custom gym — dots render as outlines instead of filled circles
val outlineRoutes: Set<String> = gyms
    .first { it.name == "Custom" }
    .routes.map { it.name }
    .toSet()
