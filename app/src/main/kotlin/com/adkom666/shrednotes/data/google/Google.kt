package com.adkom666.shrednotes.data.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import timber.log.Timber

/**
 * Interact with Google.
 */
class Google(context: Context) {

    /**
     * Use this intent to sign in your Google account. When you get the result, give it to the
     * [handleSignInResult] method.
     */
    val signInIntent: Intent
        get() = signInClient.signInIntent

    /**
     * Whether the user is signed in to the Google account.
     */
    val isSignedIn: Boolean
        get() = signInAccount != null

    /**
     * The name of the Google account you are signed in to display.
     */
    val accountDisplayName: String?
        get() = signInAccount?.displayName

    private val signInClient: GoogleSignInClient by lazy {
        buildSignInClient(context)
    }

    private var signInAccount: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    init {
        Timber.d("Init")
        Timber.d("Last Google signed in account: ${signInAccount?.displayName}")
    }

    /**
     * Call this method from [Activity.onActivityResult], making sure that the results of the Google
     * authorization call for the previously received intent (from [signInIntent]) are returned.
     *
     * @param resultCode there must be [Activity.RESULT_OK] if authorization goes well.
     * @param data authorization data.
     * @param onSignInListener callback to perform at the end of the successful authorization.
     */
    fun handleSignInResult(resultCode: Int, data: Intent?, onSignInListener: () -> Unit) {
        Timber.d("handleSignInResult: resultCode=$resultCode, data=$data")
        if (resultCode == Activity.RESULT_OK) {
            Timber.d("resultCode is Activity.RESULT_OK")
            GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener { account ->
                    Timber.d("account=$account")
                    signInAccount = account
                    onSignInListener()
                }
        }
    }

    /**
     * Sign out from your Google account.
     */
    fun signOut() {
        Timber.d("Sign out")
        signInClient.signOut()
        signInAccount = null
    }

    private fun buildSignInClient(context: Context): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, signInOptions)
    }
}
