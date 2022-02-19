package com.adkom666.shrednotes.di.viewmodel

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Used to create a [ViewModel] in an activity.
 *
 * @param factory factory of the models.
 */
inline fun <reified T : ViewModel> FragmentActivity.viewModel(
    factory: ViewModelProvider.Factory
): T {
    return ViewModelProvider(this, factory)[T::class.java]
}

/**
 * Used to create a [ViewModel] in a fragment.
 *
 * @param factory factory of the models.
 */
inline fun <reified T : ViewModel> Fragment.viewModel(
    factory: ViewModelProvider.Factory
): T {
    return ViewModelProvider(this, factory)[T::class.java]
}
