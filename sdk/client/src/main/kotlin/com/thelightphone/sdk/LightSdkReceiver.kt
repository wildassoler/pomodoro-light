package com.thelightphone.sdk

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.thelightphone.sdk.shared.LightCrypto

class LightSdkReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "LightSdkReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (!isFromLightServer(intent)) return

        val encryptedData = intent?.extras?.getString("data") ?: return

        val decrypted = runCatching { LightClientCrypto.decrypt(encryptedData) }
            .onFailure { Log.e(TAG, "broadcast decryption error") }
            .getOrNull() ?: return

        Log.d(TAG, "successfully decrypted message from server app")
        // TODO figure out what to do with these messages!
    }

    private fun isFromLightServer(intent: Intent?): Boolean {
        val senderIdentity = intent?.getParcelableExtra("sender_identity", PendingIntent::class.java)
            ?: return false
        return senderIdentity.creatorPackage == BuildConfig.LIGHT_SERVER_PACKAGE
    }
}
