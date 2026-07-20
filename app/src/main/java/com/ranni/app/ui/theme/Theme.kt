package com.ranni.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * All named UI themes available in the app.
 * Each entry carries a complete Material 3 ColorScheme so every screen
 * automatically inherits the correct colours via MaterialTheme.colorScheme.
 *
 * To add a new theme: add an enum entry with a darkColorScheme() block.
 * The name shown in the UI is derived from [displayName].
 */
enum class AppTheme(val colorScheme: ColorScheme, val displayName: String) {

    // Custom Ranni-branded dark theme — default
    RANNI_DARK(
        displayName = "Ranni Dark",
        colorScheme = darkColorScheme(
            // Accent — selected tabs, active icons, graph lines, sliders
            primary          = Color(0xFF9d9bd1),
            // Secondary accent — second graph line series
            tertiary         = Color(0xFF9492c6),
            // App background (behind all content)
            background       = Color(0xFF0e0f1a),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF0e0f1a),
            // Navigation bar and tab bar container
            surfaceContainer = Color(0xFF1D1F2E),
            // Primary text colour on background / surfaces
            onSurface        = Color(0xFFf2f3fc),
            // Secondary text, inactive icons, hint text
            onSurfaceVariant = Color(0xFFf2f3fc),
            // Validation errors, danger / destructive actions
            error            = Color(0xFFcfb6de),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF601410),
            // Climb dot border colour
            outline          = Color(0xFF938F99),
            // Graph grid lines, horizontal dividers
            outlineVariant   = Color(0xFF49454F),
            // Nav bar selected item pill background
            secondaryContainer    = Color(0xFF6b6791),
            // Nav bar selected icon and label colour
            onSecondaryContainer  = Color(0xFFE3E1F5),
            // Exercise card background (default Card composable colour)
            surfaceContainerLow   = Color(0xFF23213b),
        )
    ),

    // Ranni Dark's accent identity on true-black surfaces — saves battery on OLED screens
    // and gives maximum contrast. Containers get a hair of lift (near-black, not pure black)
    // so cards/nav bars stay visually separated from the pure-black background.
    RANNI_OLED(
        displayName = "Ranni OLED",
        colorScheme = darkColorScheme(
            // Accent — selected tabs, active icons, graph lines, sliders
            primary          = Color(0xFF9d9bd1),
            // Secondary accent — second graph line series
            tertiary         = Color(0xFF9492c6),
            // App background — true black for OLED power savings / max contrast
            background       = Color(0xFF000000),
            // Card / sheet / dialog surfaces — a hair off true black so tonal-elevated
            // surfaces (e.g. Select Climb gym cards) stay visible against the background
            surface          = Color(0xFF030304),
            // Navigation bar and tab bar container — near-black, just enough lift to
            // stay visible against the pure-black background
            surfaceContainer = Color(0xFF0A0A0D),
            // Primary text colour on background / surfaces
            onSurface        = Color(0xFFf2f3fc),
            // Secondary text, inactive icons, hint text
            onSurfaceVariant = Color(0xFFf2f3fc),
            // Validation errors, danger / destructive actions
            error            = Color(0xFFcfb6de),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF601410),
            // Climb dot border colour
            outline          = Color(0xFF938F99),
            // Graph grid lines, horizontal dividers
            outlineVariant   = Color(0xFF49454F),
            // Nav bar selected item pill background
            secondaryContainer    = Color(0xFF6b6791),
            // Nav bar selected icon and label colour
            onSecondaryContainer  = Color(0xFFE3E1F5),
            // Exercise card background — near-black, same lift as surfaceContainer
            surfaceContainerLow   = Color(0xFF0A0A0D),
        )
    ),

    // Standard Material 3 dark palette — matches Android system defaults
    ANDROID_DARK(
        displayName = "Android Dark",
        colorScheme = darkColorScheme(
            // Accent — selected tabs, active icons, graph lines, sliders
            primary          = Color(0xFFD0BCFF),
            // Secondary accent — second graph line series
            tertiary         = Color(0xFFEFB8C8),
            // App background (behind all content)
            background       = Color(0xFF141218),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF141218),
            // Navigation bar and tab bar container
            surfaceContainer = Color(0xFF211F26),
            // Primary text colour on background / surfaces
            onSurface        = Color(0xFFE6E1E5),
            // Secondary text, inactive icons, hint text
            onSurfaceVariant = Color(0xFFCAC4D0),
            // Validation errors, danger / destructive actions
            error            = Color(0xFFF2B8B5),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF601410),
            // Climb dot border colour
            outline          = Color(0xFF938F99),
            // Graph grid lines, horizontal dividers
            outlineVariant   = Color(0xFF49454F),
            // Nav bar selected item pill background
            secondaryContainer    = Color(0xFF4A4458),
            // Nav bar selected icon and label colour
            onSecondaryContainer  = Color(0xFFEADDFF),
            // Exercise card background (default Card composable colour)
            surfaceContainerLow   = Color(0xFF2B2930),
        )
    ),

    // Kaneko Lumi (Phase Connect) — light scheme: cream white base, gold + navy accents
    KANEKO_LUMI_LIGHT(
        displayName = "Kaneko Lumi",
        colorScheme = lightColorScheme(
            // Accent — Lumi's signature golden yellow (darkened for light bg contrast)
            primary          = Color(0xFFC49A28),
            // Secondary accent — teal/aqua from her palette swatch
            tertiary         = Color(0xFF2A9DA5),
            // App background — bright warm cream, like her white outfit
            background       = Color(0xFFFDF8EE),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFFFDF8EE),
            // Navigation bar and tab bar container — soft warm white
            surfaceContainer = Color(0xFFF0E8D4),
            // Primary text colour — deep navy from her coat
            onSurface        = Color(0xFF0D1020),
            // Secondary text, inactive icons — muted navy
            onSurfaceVariant = Color(0xFF3A3D52),
            // Validation errors, danger — amber-orange from her palette
            error            = Color(0xFFB85C2A),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFFFFFFFF),
            // Climb dot border colour — muted gold
            outline          = Color(0xFF8A7A50),
            // Graph grid lines, horizontal dividers — light warm divider
            outlineVariant   = Color(0xFFDDD0B0),
            // Nav bar selected item pill background — soft gold
            secondaryContainer    = Color(0xFFfad569),
            // Nav bar selected icon and label colour — deep navy
            onSecondaryContainer  = Color(0xFF1A1600),
            // Exercise card background — slightly deeper cream
            surfaceContainerLow   = Color(0xFFF7F0DF),
        )
    ),

    // Mari_Mari_EN — light scheme: sky blue base, vivid orange accents
    MARI_MARI(
        displayName = "MariMari",
        colorScheme = lightColorScheme(
            // Accent — vivid sky blue from her jacket
            primary          = Color(0xFF1A7EC4),
            // Secondary accent — bright orange bow and goldfish scales
            tertiary         = Color(0xFFE06010),
            // App background — pure soft white, like her silver hair
            background       = Color(0xFFF4F8FF),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFFF4F8FF),
            // Navigation bar and tab bar container — light sky blue tint
            surfaceContainer = Color(0xFFCCE4F8),
            // Primary text colour — deep navy-blue for contrast
            onSurface        = Color(0xFF0A1A2E),
            // Secondary text, inactive icons — muted blue-grey
            onSurfaceVariant = Color(0xFF304060),
            // Validation errors, danger — red gem from her brooch
            error            = Color(0xFFCC2020),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFFFFFFFF),
            // Climb dot border colour — steel blue-grey
            outline          = Color(0xFF5880A0),
            // Graph grid lines, horizontal dividers — light blue divider
            outlineVariant   = Color(0xFFA8C8E8),
            // Nav bar selected item pill background — vivid orange
            secondaryContainer    = Color(0xFFFF8C20),
            // Nav bar selected icon and label colour — deep navy
            onSecondaryContainer  = Color(0xFF1A0A00),
            // Exercise card background — very light blue-white
            surfaceContainerLow   = Color(0xFFE0EFFB),
        )
    ),

    // Ceres Fauna (Hololive EN) — light scheme: mint green base, teal + coral + gold accents
    CERES_FAUNA(
        displayName = "Ceres Fauna",
        colorScheme = lightColorScheme(
            // Accent — Fauna's sage/mint green hair
            primary          = Color(0xFF5A9E72),
            // Secondary accent — coral/salmon from her skirt gradient
            tertiary         = Color(0xFFE87A6A),
            // App background — soft mint white, like her dress base
            background       = Color(0xFFF2FAF4),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFFF2FAF4),
            // Navigation bar and tab bar container — light sage tint
            surfaceContainer = Color(0xFFD8EFE0),
            // Primary text colour — deep teal-navy from her bodice
            onSurface        = Color(0xFF0D2020),
            // Secondary text, inactive icons — muted dark teal
            onSurfaceVariant = Color(0xFF2E4A40),
            // Validation errors, danger — coral from her skirt
            error            = Color(0xFFCC5540),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFFFFFFFF),
            // Climb dot border colour — muted gold filigree
            outline          = Color(0xFF7A8A60),
            // Graph grid lines, horizontal dividers — light green divider
            outlineVariant   = Color(0xFFB8D8C0),
            // Nav bar selected item pill background — soft mint green
            secondaryContainer    = Color(0xFFA8DEB8),
            // Nav bar selected icon and label colour — deep teal
            onSecondaryContainer  = Color(0xFF0A2018),
            // Exercise card background — barely-tinted mint white
            surfaceContainerLow   = Color(0xFFE8F5EC),
        )
    ),

    // IRyS (Hololive EN) — dark scheme: deep magenta base, vivid crimson + electric cyan accents
    IRYS(
        displayName = "IRyS",
        colorScheme = darkColorScheme(
            // Accent — her vivid crimson-red hair, fully saturated
            primary          = Color(0xFFFF1A6C),
            // Secondary accent — electric teal/cyan eyes
            tertiary         = Color(0xFF00E5F0),
            // App background — deep saturated magenta-black
            background       = Color(0xFF100612),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF100612),
            // Navigation bar and tab bar container — rich purple-magenta
            surfaceContainer = Color(0xFF280A38),
            // Primary text colour — bright white like her dress
            onSurface        = Color(0xFFFFF0FF),
            // Secondary text, inactive icons — soft lavender
            onSurfaceVariant = Color(0xFFD0A8D8),
            // Validation errors, danger — screaming hot pink
            error            = Color(0xFFFF2D7A),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF3A0020),
            // Climb dot border colour — vivid purple
            outline          = Color(0xFFAA50C0),
            // Graph grid lines, horizontal dividers — deep purple divider
            outlineVariant   = Color(0xFF501860),
            // Nav bar selected item pill background — vivid magenta-purple
            secondaryContainer    = Color(0xFF8B0A60),
            // Nav bar selected icon and label colour — bright pink-white
            onSecondaryContainer  = Color(0xFFFFAAE0),
            // Exercise card background — deep saturated purple-black
            surfaceContainerLow   = Color(0xFF1C081E),
        )
    ),

    // Gawr Gura (Hololive EN) — light scheme: deep ocean blue base, vivid teal + white accents
    GAWR_GURA(
        displayName = "Gawr Gura",
        colorScheme = lightColorScheme(
            // Accent — vivid ocean blue of her shark hoodie, fully saturated
            primary          = Color(0xFF0060C0),
            // Secondary accent — electric aqua trident
            tertiary         = Color(0xFF00B8B0),
            // App background — medium ocean blue, like deep water
            background       = Color(0xFFB8D8F0),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFFB8D8F0),
            // Navigation bar and tab bar container — rich saturated blue
            surfaceContainer = Color(0xFF70A8D8),
            // Primary text colour — deep ocean navy
            onSurface        = Color(0xFF001428),
            // Secondary text, inactive icons — dark navy-blue
            onSurfaceVariant = Color(0xFF102848),
            // Validation errors, danger — vivid coral/pink (blushing cheeks)
            error            = Color(0xFFD02050),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFFFFFFFF),
            // Climb dot border colour — shark grey-white teeth
            outline          = Color(0xFF3870A0),
            // Graph grid lines, horizontal dividers — medium blue divider
            outlineVariant   = Color(0xFF6898C0),
            // Nav bar selected item pill background — electric teal aqua
            secondaryContainer    = Color(0xFF0098B0),
            // Nav bar selected icon and label colour — bright white
            onSecondaryContainer  = Color(0xFFE0FAFF),
            // Exercise card background — slightly deeper ocean blue
            surfaceContainerLow   = Color(0xFF9CCAE8),
        )
    ),

    // Nanashi Mumei (Hololive EN) — mid-dark scheme: teal-brown base, sandy brown + teal accents, brass gold highlights
    NANASHI_MUMEI(
        displayName = "Nanashi Mumei",
        colorScheme = darkColorScheme(
            // Accent — deep teal cape lining, her most dominant colour
            primary          = Color(0xFF3A9488),
            // Secondary accent — warm brass gold from belt buckles and lantern glow
            tertiary         = Color(0xFFC49A30),
            // App background — mid-dark teal-brown, blending her two dominant tones
            background       = Color(0xFF1E2420),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF1E2420),
            // Navigation bar and tab bar container — slightly lifted teal-brown
            surfaceContainer = Color(0xFF2C3630),
            // Primary text colour — warm off-white from her blouse
            onSurface        = Color(0xFFF0EDE6),
            // Secondary text, inactive icons — sandy brown from her hair
            onSurfaceVariant = Color(0xFFBBA888),
            // Validation errors, danger — crimson red from her pleated underskirt
            error            = Color(0xFFCC3030),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFFFFFFFF),
            // Climb dot border colour — dark chocolate brown from her corset and boots
            outline          = Color(0xFF6A5040),
            // Graph grid lines, horizontal dividers — muted teal-brown divider
            outlineVariant   = Color(0xFF384038),
            // Nav bar selected item pill background — muted teal
            secondaryContainer    = Color(0xFF2A6860),
            // Nav bar selected icon and label colour — warm sandy cream
            onSecondaryContainer  = Color(0xFFE8DEC8),
            // Exercise card background — dark brown-teal card surface
            surfaceContainerLow   = Color(0xFF181E1A),
        )
    ),

    // Inugami Korone (Hololive JP) — light scheme: sunny yellow base, red + cyan highlights
    INUGAMI_KORONE(
        displayName = "Inugami Korone",
        colorScheme = lightColorScheme(
            // Accent — her signature vivid yellow hoodie
            primary          = Color(0xFFD4A800),
            // Secondary accent — cyan button highlight on her shirt
            tertiary         = Color(0xFF00A8B8),
            // App background — bright sunny yellow, like her background
            background       = Color(0xFFFFF8D6),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFFFFF8D6),
            // Navigation bar and tab bar container — warm yellow tint
            surfaceContainer = Color(0xFFFFEE90),
            // Primary text colour — deep warm brown, like her hair
            onSurface        = Color(0xFF2A1A08),
            // Secondary text, inactive icons — muted warm brown
            onSurfaceVariant = Color(0xFF5A3E20),
            // Validation errors, danger — red button highlight from her shirt
            error            = Color(0xFFCC2020),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFFFFFFFF),
            // Climb dot border colour — warm amber-brown
            outline          = Color(0xFF907040),
            // Graph grid lines, horizontal dividers — soft yellow divider
            outlineVariant   = Color(0xFFE8D880),
            // Nav bar selected item pill background — vivid red accent
            secondaryContainer    = Color(0xFFE83030),
            // Nav bar selected icon and label colour — bright white
            onSecondaryContainer  = Color(0xFFFFFFFF),
            // Exercise card background — soft warm cream
            surfaceContainerLow   = Color(0xFFFFF4C0),
        )
    ),

    // Saber / Artoria Pendragon (Fate) — dark scheme: deep royal blue base, silver + gold accents
    SABER(
        displayName = "Saber",
        colorScheme = darkColorScheme(
            // Accent — her royal blue dress
            primary          = Color(0xFF4A7FD4),
            // Secondary accent — her golden ahoge and armour trim
            tertiary         = Color(0xFFD4A800),
            // App background — deep navy-black, like the dark battlefield behind her
            background       = Color(0xFF090C14),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF090C14),
            // Navigation bar and tab bar container — deep royal blue-black
            surfaceContainer = Color(0xFF141A2E),
            // Primary text colour — silver-white like her armour
            onSurface        = Color(0xFFECEEF8),
            // Secondary text, inactive icons — muted silver-grey
            onSurfaceVariant = Color(0xFFA8AABF),
            // Validation errors, danger — red (enemy faction)
            error            = Color(0xFFEF6060),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF3A0000),
            // Climb dot border colour — gold armour trim
            outline          = Color(0xFF9A8020),
            // Graph grid lines, horizontal dividers — dark blue divider
            outlineVariant   = Color(0xFF252840),
            // Nav bar selected item pill background — deep gold-tinted blue
            secondaryContainer    = Color(0xFF2A3A60),
            // Nav bar selected icon and label colour — bright gold
            onSecondaryContainer  = Color(0xFFFFE580),
            // Exercise card background — very dark blue-black
            surfaceContainerLow   = Color(0xFF0E1220),
        )
    ),

    // Diluc (Genshin Impact) — dark scheme: charcoal base, crimson red + gold accents
    DILUC(
        displayName = "Diluc",
        colorScheme = darkColorScheme(
            // Accent — his vivid crimson red hair and coat lining
            primary          = Color(0xFFD93030),
            // Secondary accent — warm ember glow / gold pendant
            tertiary         = Color(0xFFD4920A),
            // App background — deep charcoal, like his black outer coat
            background       = Color(0xFF0F0C0C),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF0F0C0C),
            // Navigation bar and tab bar container — dark maroon-grey
            surfaceContainer = Color(0xFF211418),
            // Primary text colour — soft warm white
            onSurface        = Color(0xFFF5EDE8),
            // Secondary text, inactive icons — muted warm grey
            onSurfaceVariant = Color(0xFFBBADAA),
            // Validation errors, danger — bright red
            error            = Color(0xFFFF6B6B),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF3A0000),
            // Climb dot border colour — dark gold filigree
            outline          = Color(0xFF806040),
            // Graph grid lines, horizontal dividers — dark maroon divider
            outlineVariant   = Color(0xFF3A2020),
            // Nav bar selected item pill background — deep crimson
            secondaryContainer    = Color(0xFF6B1010),
            // Nav bar selected icon and label colour — warm red-white
            onSecondaryContainer  = Color(0xFFFFD0CC),
            // Exercise card background — very dark warm charcoal
            surfaceContainerLow   = Color(0xFF1A1010),
        )
    ),

    // Frieren (Frieren: Beyond Journey's End) — light scheme: grey-tinted white base, gold + teal accents
    FRIEREN(
        displayName = "Frieren",
        colorScheme = lightColorScheme(
            // Accent — teal/emerald green eyes
            primary          = Color(0xFF2A9A80),
            // Secondary accent — ruby red earring / staff orb
            tertiary         = Color(0xFFCC1830),
            // App background — grey-tinted white, like her silver hair / white robes
            background       = Color(0xFFDEE0E4),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFFDEE0E4),
            // Navigation bar and tab bar container — slightly deeper grey-blue
            surfaceContainer = Color(0xFFC4C8D0),
            // Primary text colour — deep charcoal for contrast on light grey
            onSurface        = Color(0xFF101418),
            // Secondary text, inactive icons — muted slate
            onSurfaceVariant = Color(0xFF384048),
            // Validation errors, danger — vivid red earring / ruby staff gem
            error            = Color(0xFFCC1830),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFFFFFFFF),
            // Climb dot border colour — muted gold filigree
            outline          = Color(0xFF807060),
            // Graph grid lines, horizontal dividers — soft grey divider
            outlineVariant   = Color(0xFFB0B4BC),
            // Nav bar selected item pill background — rich gold robe trim
            secondaryContainer    = Color(0xFFB88A00),
            // Nav bar selected icon and label colour — bright off-white
            onSecondaryContainer  = Color(0xFFFFF8E0),
            // Exercise card background — slightly darker grey for dot contrast
            surfaceContainerLow   = Color(0xFFD0D4DA),
        )
    ),

    // RX-78-2 Gundam — light scheme: greyed white armour base, Federation blue + red + gold accents
    RX_78_2(
        displayName = "RX-78-2",
        colorScheme = lightColorScheme(
            // Accent — Federation blue chest and torso armour plates
            primary          = Color(0xFF1A4CA8),
            // Secondary accent — golden yellow vent grilles and waist armour
            tertiary         = Color(0xFFB88A00),
            // App background — greyed white, like the matte armour plating (not pure white)
            background       = Color(0xFFE8E8EC),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFFE8E8EC),
            // Navigation bar and tab bar container — slightly darker grey armour panel
            surfaceContainer = Color(0xFFCCCCD2),
            // Primary text colour — near-black, high contrast on pale grey background
            onSurface        = Color(0xFF0A0A0F),
            // Secondary text, inactive icons — deep charcoal, still very readable
            onSurfaceVariant = Color(0xFF282830),
            // Validation errors, danger — shield and foot armour red
            error            = Color(0xFFBE1020),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFFFFFFFF),
            // Climb dot border colour — medium grey armour joint detail
            outline          = Color(0xFF707080),
            // Graph grid lines, horizontal dividers — light grey panel seam
            outlineVariant   = Color(0xFFB0B0BC),
            // Nav bar selected item pill background — vivid Federation red (shield / head crest)
            secondaryContainer    = Color(0xFFC81020),
            // Nav bar selected icon and label colour — bright white (cockpit visor highlight)
            onSecondaryContainer  = Color(0xFFFFFFFF),
            // Exercise card background — slightly darker grey armour surface for card depth
            surfaceContainerLow   = Color(0xFFD8D8DE),
        )
    ),

    // RX-93-ν2 Hi-Nu Gundam — light scheme: blue-grey armour base (#B4B7D1), indigo-blue (#646CD1) accents + orange sensor
    RX_93_V2(
        displayName = "Hi-Nu Gundam",
        colorScheme = lightColorScheme(
            // Accent — indigo-blue fin funnels and shoulder armour, derived from #646CD1
            primary          = Color(0xFF4850C8),
            // Secondary accent — amber orange mono-eye / sensor glow
            tertiary         = Color(0xFFD06800),
            // App background — cool blue-grey armour plating, exactly #B4B7D1
            background       = Color(0xFFB4B7D1),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFFB4B7D1),
            // Navigation bar and tab bar container — slightly deeper blue-grey panel
            surfaceContainer = Color(0xFF9A9DC0),
            // Primary text colour — near-black for maximum contrast on mid-grey background
            onSurface        = Color(0xFF080810),
            // Secondary text, inactive icons — deep cool slate
            onSurfaceVariant = Color(0xFF252540),
            // Validation errors, danger — Federation red (shield emblem)
            error            = Color(0xFFBE1020),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFFFFFFFF),
            // Climb dot border colour — muted blue-grey panel line
            outline          = Color(0xFF5A5E88),
            // Graph grid lines, horizontal dividers — lighter blue-grey seam
            outlineVariant   = Color(0xFF9094B8),
            // Nav bar selected item pill background — saturated #646CD1 blue
            secondaryContainer    = Color(0xFF646CD1),
            // Nav bar selected icon and label colour — bright white
            onSecondaryContainer  = Color(0xFFFFFFFF),
            // Exercise card background — slightly deeper blue-grey for card depth
            surfaceContainerLow   = Color(0xFFA4A8C4),
        )
    ),

    // MSN-04 Sazabi — dark scheme: deep charcoal grey core, dominant crimson red + gold sensor accents
    MSN_04(
        displayName = "Sazabi",
        colorScheme = darkColorScheme(
            // Accent — dominant Zeon crimson red armour panels
            primary          = Color(0xFFCC2020),
            // Secondary accent — gold Zeon emblem and sensor ring detail
            tertiary         = Color(0xFFC08800),
            // App background — deep charcoal grey, like the inner frame and joints
            background       = Color(0xFF181818),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF181818),
            // Navigation bar and tab bar container — slightly lifted dark grey frame
            surfaceContainer = Color(0xFF282828),
            // Primary text colour — warm off-white, high contrast on near-black
            onSurface        = Color(0xFFF0ECEC),
            // Secondary text, inactive icons — muted warm grey
            onSurfaceVariant = Color(0xFFB0A8A8),
            // Validation errors, danger — brighter alert red (distinct from primary)
            error            = Color(0xFFFF5555),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF2A0000),
            // Climb dot border colour — dark steel grey panel line
            outline          = Color(0xFF606060),
            // Graph grid lines, horizontal dividers — very dark grey seam
            outlineVariant   = Color(0xFF383838),
            // Nav bar selected item pill background — deep blood red
            secondaryContainer    = Color(0xFF8A0A0A),
            // Nav bar selected icon and label colour — bright warm white
            onSecondaryContainer  = Color(0xFFFFE8E8),
            // Exercise card background — slightly deeper charcoal than bg
            surfaceContainerLow   = Color(0xFF101010),
        )
    ),

    // Robin (Fire Emblem Awakening, Female) — dark scheme: medium navy-charcoal base, dusty mauve-purple + brass gold
    ROBIN_FE(
        displayName = "Robin FE",
        colorScheme = darkColorScheme(
            // Accent — dusty mauve-purple cloak inner lining
            primary          = Color(0xFF8A78C0),
            // Secondary accent — brass gold rope toggles and accessories
            tertiary         = Color(0xFFC09838),
            // App background — medium dark navy, like the body of her cloak (not pitch black)
            background       = Color(0xFF1A1A26),
            // Card / sheet / dialog surfaces
            surface          = Color(0xFF1A1A26),
            // Navigation bar and tab bar container — purple-tinted navy, a touch more saturated
            surfaceContainer = Color(0xFF252240),
            // Primary text colour — cool silver-white like her hair
            onSurface        = Color(0xFFE8EAF0),
            // Secondary text, inactive icons — muted silver
            onSurfaceVariant = Color(0xFFB0AABF),
            // Validation errors, danger — soft red (thunder tome)
            error            = Color(0xFFD46060),
            // Text / icons drawn on top of error-coloured surfaces
            onError          = Color(0xFF3A0000),
            // Climb dot border colour — warm brown from her boots and belt
            outline          = Color(0xFF806848),
            // Graph grid lines, horizontal dividers — more saturated purple divider
            outlineVariant   = Color(0xFF443868),
            // Nav bar selected item pill background — more saturated purple
            secondaryContainer    = Color(0xFF5A3EA0),
            // Nav bar selected icon and label colour — soft lavender
            onSecondaryContainer  = Color(0xFFDDD0F0),
            // Exercise card background — purple-tinted navy card surface
            surfaceContainerLow   = Color(0xFF1E1C38),
        )
    );

}

/**
 * App-wide theme wrapper. Reads the selected [AppTheme] and applies its
 * ColorScheme to all descendant composables via MaterialTheme.
 *
 * [theme] defaults to [AppTheme.RANNI_DARK] so previews and tests work without
 * explicitly passing a theme.
 */
@Composable
fun RanniTheme(
    theme: AppTheme = AppTheme.RANNI_DARK,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = theme.colorScheme,
        typography = RanniTypography,
        content = content
    )
}
