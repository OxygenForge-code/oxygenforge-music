package com.music.bitchord

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * How an attempt to switch the launcher icon ended.
 *
 * The switch is the one settings action that reaches outside this app: it edits
 * the enabled state of the `<activity-alias>` entries the home screen draws its
 * icon from, and the platform can refuse that outright (a managed profile, a
 * manufacturer policy, a component the installed build no longer has). Nothing
 * here may throw into the UI thread — a failed icon change is a message, not a
 * crash — so every outcome the caller can act on is a value.
 */
sealed interface IconChangeResult {
    /** The chosen icon is now the only enabled launcher entry. */
    data object Changed : IconChangeResult

    /** Why [Failed] happened. [messageRes] names the user-facing explanation. */
    enum class Failure(val messageRes: Int) {
        /** The alias for this choice is not part of the installed build. */
        Unavailable(R.string.app_icon_notice_unavailable),

        /** The platform rejected the component state change. */
        Refused(R.string.app_icon_notice_refused),

        /** The change was accepted but the system did not end up with one entry. */
        Unverified(R.string.app_icon_notice_unverified),

        /** Anything else the platform threw. */
        Unknown(R.string.app_icon_notice_unknown),
    }

    /**
     * [detail] is the platform's own words, shown small under the explanation so
     * a refusal has something concrete behind it; null when there is nothing to
     * add.
     */
    data class Failed(val reason: Failure, val detail: String? = null) : IconChangeResult
}

/**
 * The home-screen icon choices. Each is an `<activity-alias>` in the manifest
 * carrying the launcher intent filter; exactly one is enabled at a time.
 *
 * Every variant shares one foreground (the OxygenForge mark) and differs by its
 * tile: a teal, violet or ember glass squircle, or a Pixel-style circle, in a
 * light, dark or clear finish. [previewRes] is a flattened render of the whole
 * finished icon — tile included — so the settings picker shows it as designed
 * rather than through the device's launcher mask. All rasters come from
 * `tools/generate-icons.py`. The alias names are kept from earlier releases so a
 * choice made before an update stays enabled.
 */
enum class AppIcon(val alias: String, val iconRes: Int, val labelRes: Int, val previewRes: Int) {
    Default("LauncherDefault", R.mipmap.ic_launcher, R.string.app_icon_default, R.mipmap.ic_launcher_preview),
    LiquidGlass("LauncherGlassLight", R.mipmap.ic_liquid_glass, R.string.app_icon_liquid_glass, R.mipmap.ic_liquid_glass_foreground),
    LiquidGlassDark("LauncherGlassDark", R.mipmap.ic_liquid_glass_dark, R.string.app_icon_liquid_glass_dark, R.mipmap.ic_liquid_glass_dark_foreground),
    LiquidGlassClear("LauncherGlassClear", R.mipmap.ic_liquid_glass_clear, R.string.app_icon_liquid_glass_clear, R.mipmap.ic_liquid_glass_clear_foreground),
    LiquidGlassClearDark("LauncherGlassClearDark", R.mipmap.ic_liquid_glass_clear_dark, R.string.app_icon_liquid_glass_clear_dark, R.mipmap.ic_liquid_glass_clear_dark_foreground),
    SamsungGlass("LauncherSamsungGlass", R.mipmap.ic_samsung_glass, R.string.app_icon_samsung_glass, R.mipmap.ic_samsung_glass_foreground),
    SamsungGlassDark("LauncherSamsungGlassDark", R.mipmap.ic_samsung_glass_dark, R.string.app_icon_samsung_glass_dark, R.mipmap.ic_samsung_glass_dark_foreground),
    SamsungGlassClear("LauncherSamsungGlassClear", R.mipmap.ic_samsung_glass_clear, R.string.app_icon_samsung_glass_clear, R.mipmap.ic_samsung_glass_clear_foreground),
    SamsungGlassClearDark("LauncherSamsungGlassClearDark", R.mipmap.ic_samsung_glass_clear_dark, R.string.app_icon_samsung_glass_clear_dark, R.mipmap.ic_samsung_glass_clear_dark_foreground),
    PixelGlass("LauncherPixelGlass", R.mipmap.ic_pixel_glass, R.string.app_icon_pixel_glass, R.mipmap.ic_pixel_glass_foreground),
    PixelGlassDark("LauncherPixelGlassDark", R.mipmap.ic_pixel_glass_dark, R.string.app_icon_pixel_glass_dark, R.mipmap.ic_pixel_glass_dark_foreground),
    PixelGlassClear("LauncherPixelGlassClear", R.mipmap.ic_pixel_glass_clear, R.string.app_icon_pixel_glass_clear, R.mipmap.ic_pixel_glass_clear_foreground),
    PixelGlassClearDark("LauncherPixelGlassClearDark", R.mipmap.ic_pixel_glass_clear_dark, R.string.app_icon_pixel_glass_clear_dark, R.mipmap.ic_pixel_glass_clear_dark_foreground);

    private fun component(context: Context) =
        ComponentName(context.packageName, "${context.packageName}.$alias")

    /** False when this build's manifest has no such alias — never assume it has. */
    private fun isInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getActivityInfo(component(context), 0) }.isSuccess

    private fun isEnabled(context: Context): Boolean =
        runCatching {
            context.packageManager.getComponentEnabledSetting(component(context)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }.getOrDefault(false)

    companion object {
        private const val ENABLED = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        private const val DISABLED = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        private const val KEEP_PROCESS = PackageManager.DONT_KILL_APP

        /**
         * The icon the launcher is showing now. Read from the enabled state
         * rather than a preference: the platform's copy is the one the home
         * screen draws, and a preference can drift from it across reinstalls.
         */
        fun current(context: Context): AppIcon =
            entries.firstOrNull { it.isEnabled(context) } ?: Default

        /**
         * Enables [icon] before disabling the rest so there is never a moment
         * with no launcher entry, then confirms exactly one survived — an icon
         * change that leaves two (or none) is rolled back rather than reported
         * as done. Never throws; see [IconChangeResult].
         */
        fun apply(context: Context, icon: AppIcon): IconChangeResult {
            val pm = context.packageManager
            if (!icon.isInstalled(context)) {
                return IconChangeResult.Failed(IconChangeResult.Failure.Unavailable, icon.alias)
            }
            val previous = current(context)

            try {
                pm.setComponentEnabledSetting(icon.component(context), ENABLED, KEEP_PROCESS)
            } catch (e: Exception) {
                // Typically SecurityException on a managed profile, or
                // IllegalArgumentException if the package changed under us.
                return IconChangeResult.Failed(classify(e), e.readableMessage())
            }

            var disableFailure: Exception? = null
            entries.forEach { other ->
                if (other == icon || !other.isInstalled(context)) return@forEach
                try {
                    pm.setComponentEnabledSetting(other.component(context), DISABLED, KEEP_PROCESS)
                } catch (e: Exception) {
                    disableFailure = e
                }
            }

            val enabledCount = entries.count { it.isEnabled(context) }
            if (enabledCount != 1 || !icon.isEnabled(context)) {
                restore(context, previous = previous)
                return IconChangeResult.Failed(
                    IconChangeResult.Failure.Unverified,
                    disableFailure?.readableMessage() ?: "enabled=$enabledCount",
                )
            }
            return IconChangeResult.Changed
        }

        /**
         * Best effort: put the icon that was in place before the attempt back,
         * so a half-applied change cannot leave the home screen with a second
         * OxygenForge entry or with none at all.
         */
        private fun restore(context: Context, previous: AppIcon) {
            runCatching {
                val pm = context.packageManager
                if (previous.isInstalled(context)) {
                    pm.setComponentEnabledSetting(previous.component(context), ENABLED, KEEP_PROCESS)
                }
                // Every other alias goes back off, including the one just tried:
                // the point of a rollback is exactly one entry, not one extra.
                entries.filter { it != previous && it.isInstalled(context) }.forEach {
                    runCatching {
                        pm.setComponentEnabledSetting(it.component(context), DISABLED, KEEP_PROCESS)
                    }
                }
            }
        }

        private fun classify(e: Exception): IconChangeResult.Failure = when (e) {
            is SecurityException -> IconChangeResult.Failure.Refused
            is IllegalArgumentException -> IconChangeResult.Failure.Unavailable
            else -> IconChangeResult.Failure.Unknown
        }

        /** The platform's message, trimmed to something a one-line notice can hold. */
        private fun Exception.readableMessage(): String? =
            (message ?: javaClass.simpleName).takeIf { it.isNotBlank() }?.take(160)
    }
}
