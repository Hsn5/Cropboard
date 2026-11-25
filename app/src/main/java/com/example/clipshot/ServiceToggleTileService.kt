package com.example.clipshot

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class ServiceToggleTileService : TileService() {

    companion object {
        private const val FULL_COMPONENT =
            "com.example.clipshot/com.example.clipshot.ScreenshotAccessibilityService"
        private const val SHORT_COMPONENT =
            "com.example.clipshot/.ScreenshotAccessibilityService"
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onClick() {
        super.onClick()
        try {
            val currentServices = getEnabledServicesList()
            val isCurrentlyEnabled = currentServices.any { isOurComponent(it) }

            val updatedServices = if (isCurrentlyEnabled) {
                currentServices.filterNot { isOurComponent(it) }
            } else {
                currentServices + FULL_COMPONENT
            }

            Settings.Secure.putString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                updatedServices.joinToString(":")
            )
            Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                if (updatedServices.isNotEmpty()) 1 else 0
            )

            applyTileState(!isCurrentlyEnabled)
        } catch (_: SecurityException) {
            Toast.makeText(
                this,
                "Grant WRITE_SECURE_SETTINGS via ADB first: adb shell pm grant com.example.clipshot android.permission.WRITE_SECURE_SETTINGS",
                Toast.LENGTH_LONG
            ).show()
            openAccessibilitySettings()
        }
    }

    private fun refreshTileState() {
        val enabled = getEnabledServicesList().any { isOurComponent(it) }
        applyTileState(enabled)
    }

    private fun applyTileState(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (enabled) "On" else "Off"
        }
        tile.updateTile()
    }

    private fun getEnabledServicesList(): List<String> {
        val raw = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return emptyList()
        return raw.split(':')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun isOurComponent(entry: String): Boolean {
        return entry == FULL_COMPONENT || entry == SHORT_COMPONENT
    }

    private fun openAccessibilitySettings() {
        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                2,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(settingsIntent)
        }
    }
}
