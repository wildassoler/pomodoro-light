package com.thelightphone.sdk.server

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.thelightphone.sdk.server.LightSdkServer.messageClient
import com.thelightphone.sdk.shared.LightCrypto

private const val TAG = "LightOSSdkHandshake"
private const val RESULT_OK = 0
private const val RESULT_ERROR = -1

class LightSdkHandshakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val senderIdentity = intent.getParcelableExtra("sender_identity", PendingIntent::class.java)
            ?: run {
                Log.w(TAG, "Handshake missing sender_identity")
                setResultCode(RESULT_ERROR)
                return
            }

        val creatorPackage = senderIdentity.creatorPackage!!
        val publicKey = intent.getStringExtra("public_key")
            ?: run {
                Log.w(TAG, "Handshake missing public_key from $creatorPackage")
                setResultCode(RESULT_ERROR)
                return
            }

        try {
            val responseData = "helloooo from LightOS"
            val encrypted: String = LightCrypto.encrypt(responseData, publicKey)
            setResultCode(RESULT_OK)
            setResultData(encrypted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt handshake response for $creatorPackage", e)
            setResultCode(RESULT_ERROR)
        }
    }
}