package com.adkom666.shrednotes.data.google

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Authorization in Google.
 */
interface GoogleAuth {

    /**
     * Whether the user is signed in to the Google account.
     */
    val isSignedIn: Boolean

    /**
     * The name of the Google account you are signed in to display.
     */
    val accountDisplayName: String?

    /**
     * Use this intent to sign in your Google account. When you get the result, give it to the
     * [handleSignInResult] method.
     *
     * @param context [Context].
     */
    fun signInIntent(context: Context): Intent

    /**
     * Call this method from [Activity.onActivityResult], making sure that the results of the Google
     * authorization call for the previously received intent (from [signInIntent]) are returned.
     *
     * @param context [Context].
     * @param resultCode there must be [Activity.RESULT_OK] if authorization goes well.
     * @param data authorization data.
     * @param onSuccessListener invoked after successful signing in Google account.
     */
    fun handleSignInResult(
        context: Context,
        resultCode: Int,
        data: Intent?,
        onSuccessListener: () -> Unit
    )

    /**
     * Sign out from your Google account.
     *
     * @param context [Context].
     */
    fun signOut(context: Context)
}
