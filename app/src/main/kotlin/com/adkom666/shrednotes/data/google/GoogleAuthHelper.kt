package com.adkom666.shrednotes.data.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import timber.log.Timber

/**
 * Operations for Google authorization.
 */
class GoogleAuthHelper(
    context: Context,
    private val scope: Scope,
    vararg scopes: Scope
) {
    /**
     * Google account you are signed in.
     */
    val signInAccount: GoogleSignInAccount?
        get() = _signInAccount

    private val scopeArray: Array<Scope> = scopes.toList().toTypedArray()
    private var _signInClient: GoogleSignInClient? = null
    private var _signInAccount: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    /**
     * Use this intent to sign in your Google account. When you get the result, give it to the
     * [handleSignInResult] method.
     *
     * @param context [Context].
     */
    fun signInIntent(context: Context): Intent {
        Timber.d("Sign in intent requested")
        return signInClient(context).signInIntent
    }

    /**
     * Call this method from [Activity.onActivityResult], making sure that the results of the Google
     * authorization call for the previously received intent (from [signInIntent]) are returned.
     *
     * @param resultCode there must be [Activity.RESULT_OK] if authorization goes well.
     * @param data authorization data.
     * @param onSuccessListener invoked after successful signing in Google account.
     */
    fun handleSignInResult(
        resultCode: Int,
        data: Intent?,
        onSuccessListener: () -> Unit
    ) {
        Timber.d("handleSignInResult: resultCode=$resultCode, data=$data")
        if (resultCode == Activity.RESULT_OK) {
            Timber.d("resultCode is Activity.RESULT_OK")
            GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener { account ->
                    Timber.d("account=$account")
                    _signInAccount = account
                    onSuccessListener()
                }
        }
    }

    /**
     * Sign out from your Google account.
     *
     * @param context [Context].
     */
    fun signOut(context: Context) {
        Timber.d("Sign out")
        signInClient(context).signOut()
        _signInAccount = null
    }

    private fun signInClient(context: Context): GoogleSignInClient = _signInClient ?: run {
        val client = getSignInClient(context)
        _signInClient = client
        client
    }

    private fun getSignInClient(context: Context): GoogleSignInClient {
        @Suppress("SpreadOperator")
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(scope, *scopeArray)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context.applicationContext, signInOptions)
    }
}
