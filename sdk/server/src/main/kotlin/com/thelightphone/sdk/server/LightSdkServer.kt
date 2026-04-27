package com.thelightphone.sdk.server

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.util.Log
import com.thelightphone.sdk.shared.LightConstants
import com.thelightphone.sdk.shared.LightCrypto


data class InstalledClient(
    val packageInfo: PackageInfo,
    val sdkVersion: String,
)

object LightSdkServer {
    private const val TAG = "LightSdkServer"

    val Context.runningAsSystemApp: Boolean
        get() {
            val isSystemUid = (Process.myUid() == Process.SYSTEM_UID)
            val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            return isSystemApp && isSystemUid
        }

    fun Context.messageClient(clientPackageName: String, clientPublicKeyBase64: String, data: String) {
        try {
            val encoded: String = LightCrypto.encrypt(data, clientPublicKeyBase64)

            val senderIdentity =
                PendingIntent.getBroadcast(this, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)

            val intent = Intent(LightConstants.ACTION_SDK_MARKER)
            intent.setPackage(clientPackageName)
            intent.putExtra("data", encoded)
            intent.putExtra("sender_identity", senderIdentity)
            if (runningAsSystemApp) {
                val current = UserHandle::class.java.getField("CURRENT").get(null) as UserHandle
                sendBroadcastAsUser(intent, current)
            } else {
                sendBroadcast(intent)
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to send encrypted message to client: $clientPackageName",
                e
            )
        }
    }

    fun Context.queryInstalledClients(): List<InstalledClient> {
        val marker = Intent(LightConstants.ACTION_SDK_MARKER)

        val results = packageManager.queryBroadcastReceivers(
            marker,
            PackageManager.GET_META_DATA or PackageManager.MATCH_DISABLED_COMPONENTS
        )

        return results.map {
            val packageName = it.activityInfo.packageName
            val packageInfo: PackageInfo
            try {
                packageInfo =
                    packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Could not find SDK package", e)
                return@map null
            }
            val meta = it.activityInfo.metaData ?: run {
                Log.e(TAG, "SDK client didn't provide metadata: $packageName")
                return@map null
            }
            val sdkVersion = meta.getString(LightConstants.SDK_VERSION_KEY) ?: run {
                Log.e(TAG, "SDK client didn't provide sdkVersion: $packageName")
                return@map null
            }

            InstalledClient(packageInfo, sdkVersion)
        }.filterNotNull()
    }
}