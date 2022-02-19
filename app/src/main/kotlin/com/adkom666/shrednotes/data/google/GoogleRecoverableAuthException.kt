package com.adkom666.shrednotes.data.google

import android.content.Intent
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import java.lang.Exception

/**
 * User does not have enough rights to perform current operation. This exception contains [Intent]
 * to allow user interaction to recover his rights. This exception also contains the map
 * [GoogleRecoverableAuthException.additionalData] to keep additional data, which can be used after
 * the rights are recovered.
 *
 * @param e wrapped [UserRecoverableAuthIOException].
 */
class GoogleRecoverableAuthException(
    e: UserRecoverableAuthIOException
) : Exception(e.message, e.cause) {

    /**
     * Supply this [Intent] to [android.app.Activity.startActivityForResult] to allow user
     * intervention.
     */
    val intent: Intent? = e.cause?.intent

    /**
     * Keep here the data, which can be used after the rights are recovered.
     */
    val additionalData: MutableMap<String, Any> = mutableMapOf()
}
