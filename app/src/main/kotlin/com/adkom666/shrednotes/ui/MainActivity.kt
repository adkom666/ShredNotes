package com.adkom666.shrednotes.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.adkom666.shrednotes.BuildConfig
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityMainBinding
import com.adkom666.shrednotes.di.viewmodel.viewModel
import com.adkom666.shrednotes.ui.ask.AskFragment
import com.adkom666.shrednotes.ui.exercises.ExercisesFragment
import com.adkom666.shrednotes.ui.gdrivedialog.GoogleDriveDialogFragment
import com.adkom666.shrednotes.ui.gdrivedialog.GoogleDriveDialogMode
import com.adkom666.shrednotes.ui.notes.NotesFragment
import com.adkom666.shrednotes.ui.statistics.StatisticsFragment
import com.adkom666.shrednotes.util.dialog.ConfirmationDialogFragment
import com.adkom666.shrednotes.util.ensureNoTextInput
import com.adkom666.shrednotes.util.getCurrentlyDisplayedFragment
import com.adkom666.shrednotes.util.performIfConfirmationFoundByTag
import com.adkom666.shrednotes.util.performIfFoundByTag
import com.adkom666.shrednotes.util.temporarilyDisable
import com.adkom666.shrednotes.util.toast
import com.google.android.material.navigation.NavigationBarView
import dagger.android.AndroidInjection
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.collect
import timber.log.Timber

/**
 * Main screen.
 */
@ExperimentalTime
class MainActivity :
    AppCompatActivity(),
    NavigationBarView.OnItemSelectedListener {

    private companion object {

        private const val TAG_READ = "${BuildConfig.APPLICATION_ID}.tags.read"
        private const val TAG_WRITE = "${BuildConfig.APPLICATION_ID}.tags.write"

        private const val TAG_CONFIRM_SIGN_OUT =
            "${BuildConfig.APPLICATION_ID}.tags.confirm_sign_out"
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val binding: ActivityMainBinding
        get() = requireNotNull(_binding)

    private val model: MainViewModel
        get() = requireNotNull(_model)

    private val signInLauncher: ActivityResultLauncher<Intent>
        get() = requireNotNull(_signInLauncher)

    private val authOnReadLauncher: ActivityResultLauncher<Intent>
        get() = requireNotNull(_authOnReadLauncher)

    private val authOnWriteLauncher: ActivityResultLauncher<Intent>
        get() = requireNotNull(_authOnWriteLauncher)

    private var _binding: ActivityMainBinding? = null
    private var _model: MainViewModel? = null
    private var _signInLauncher: ActivityResultLauncher<Intent>? = null
    private var _authOnReadLauncher: ActivityResultLauncher<Intent>? = null
    private var _authOnWriteLauncher: ActivityResultLauncher<Intent>? = null

    private var menuReference: WeakReference<Menu> = WeakReference(null)

    private val onExpandSearchViewListener: MenuItem.OnActionExpandListener =
        SearchActivenessListener()

    private val onQueryTextListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = search(query)
            override fun onQueryTextChange(newText: String?): Boolean = preview(newText)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate: savedInstanceState=$savedInstanceState")
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = viewModel(viewModelFactory)

        acquireActivityLaunchers()
        invalidateSubtitle()
        observeSection(isInitialScreenPresent = savedInstanceState != null)
        initBottomNavigation()
        invalidateOptionsMenuOnFragmentChange()
        restoreFragmentListeners()
        observeLiveData()
        listenFlows()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
        skipActivityLaunchers()
        menuReference.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Timber.d("onCreateOptionsMenu: menu=$menu")
        menuInflater.inflate(R.menu.tools, menu)
        menuReference = WeakReference(menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        Timber.d("onPrepareOptionsMenu: menu=$menu")
        menu?.invalidate()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected: item=$item")
        when (item.itemId) {
            R.id.action_filter,
            R.id.action_read,
            R.id.action_write,
            R.id.action_sign_out,
            R.id.action_sign_in -> item.temporarilyDisable() // To avoid multi clicks
        }
        return if (handleToolSelection(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Timber.d("onNavigationItemSelected: item=$item")
        return handleNavSelection(item)
    }

    private fun acquireActivityLaunchers() {
        _signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Timber.d("On sign in: result=$result")
            model.handleSignInGoogleResult(this, result.resultCode, result.data)
        }
        _authOnReadLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Timber.d("On auth on read: result=$result")
            model.handleAuthOnReadResult(result.resultCode)
        }
        _authOnWriteLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Timber.d("On auth on write: result=$result")
            model.handleAuthOnWriteResult(result.resultCode)
        }
    }

    private fun skipActivityLaunchers() {
        _signInLauncher = null
        _authOnReadLauncher = null
        _authOnWriteLauncher = null
    }

    private fun observeSection(isInitialScreenPresent: Boolean) {
        var isScreenPresent = isInitialScreenPresent
        var isScreenNotFirst = false

        model.sectionAsLiveData.observe(this) { section ->
            val fragment = section.getFragment()
            fragment.setListenersIfNeed()
            if (isScreenPresent) {
                if (isScreenNotFirst) {
                    replaceFragment(fragment)
                } else {
                    // First screen is already set
                    isScreenNotFirst = true
                }
            } else {
                addFragment(fragment)
                isScreenPresent = true
                isScreenNotFirst = true
            }
        }
    }

    private fun invalidateSubtitle() {
        supportActionBar?.subtitle = model.googleAccountDisplayName
    }

    private fun initBottomNavigation() {
        binding.bottomNavigation.selectedItemId = model.section.getActionId()
        binding.bottomNavigation.setOnItemSelectedListener(this)
    }

    private fun invalidateOptionsMenuOnFragmentChange() {
        supportFragmentManager.registerFragmentLifecycleCallbacks(OptionsMenuInvalidator(), false)
    }

    private fun restoreFragmentListeners() {
        supportFragmentManager.fragments.forEach {
            it?.setListenersIfNeed()
        }
        supportFragmentManager.performIfFoundByTag<GoogleDriveDialogFragment>(TAG_READ) {
            it.setReadingListener()
        }
        supportFragmentManager.performIfFoundByTag<GoogleDriveDialogFragment>(TAG_WRITE) {
            it.setWritingListener()
        }
        supportFragmentManager.performIfConfirmationFoundByTag(TAG_CONFIRM_SIGN_OUT) {
            it.setSigningOutListener()
        }
    }

    private fun observeLiveData() {
        model.stateAsLiveData.observe(this, StateObserver())
    }

    private fun listenFlows() {
        lifecycleScope.launchWhenResumed {
            model.navigationFlow.collect(::goToScreen)
        }
        lifecycleScope.launchWhenStarted {
            model.messageFlow.collect(::show)
        }
        lifecycleScope.launchWhenStarted {
            model.signalFlow.collect(::process)
        }
    }

    private fun prepareSearch(
        menu: Menu,
        forceInvisible: Boolean,
        fragment: Fragment?
    ) {
        fun prepare(itemSearch: MenuItem, fragment: Fragment?) {

            fun MenuItem.ensureActionViewExpanded() {
                if (!isActionViewExpanded) {
                    expandActionView()
                }
            }

            fun MenuItem.ensureActionViewCollapsed() {
                if (isActionViewExpanded) {
                    collapseActionView()
                }
            }

            itemSearch.setOnActionExpandListener(null)
            val searchView = itemSearch.actionView as? SearchView
            searchView?.init()
            val isSearchVisible = if (forceInvisible.not() && fragment is Searchable) {
                val isSearchActive = fragment.isSearchActive
                val currentQuery = fragment.currentQuery
                if (currentQuery.isNullOrEmpty() && isSearchActive.not()) {
                    itemSearch.ensureActionViewCollapsed()
                } else {
                    itemSearch.ensureActionViewExpanded()
                    searchView?.setQuery(currentQuery, false)
                }
                true
            } else {
                itemSearch.ensureActionViewCollapsed()
                false
            }
            searchView?.isVisible = isSearchVisible
            menu.setGroupVisible(R.id.group_search, isSearchVisible)
            if (isSearchVisible) {
                itemSearch.setOnActionExpandListener(onExpandSearchViewListener)
                searchView?.setOnQueryTextListener(onQueryTextListener)
            }
        }

        val itemSearch = menu.findItem(R.id.action_search)
        itemSearch?.let { prepare(it, fragment) }
    }

    private fun prepareFilter(
        menu: Menu,
        forceInvisible: Boolean,
        fragment: Fragment?
    ) {
        val isFilterVisible = if (forceInvisible.not() && fragment is Filterable) {
            val iconRes = if (fragment.isFilterEnabled) {
                R.drawable.ic_filter_on
            } else {
                R.drawable.ic_filter_off
            }
            val itemFilter = menu.findItem(R.id.action_filter)
            itemFilter?.icon = ContextCompat.getDrawable(this, iconRes)
            true
        } else {
            false
        }
        menu.setGroupVisible(R.id.group_filter, isFilterVisible)
    }

    private fun prepareGoogleDrivePanel(
        menu: Menu,
        forceInvisible: Boolean,
        itemsVisibility: MainViewModel.GoogleDriveItemsVisibility
    ) {
        val itemRead = menu.findItem(R.id.action_read)
        val itemWrite = menu.findItem(R.id.action_write)
        val itemSignOut = menu.findItem(R.id.action_sign_out)
        val itemSignIn = menu.findItem(R.id.action_sign_in)
        itemRead?.isVisible = forceInvisible.not() && itemsVisibility.isItemReadVisibile
        itemWrite?.isVisible = forceInvisible.not() && itemsVisibility.isItemWriteVisibile
        itemSignOut?.isVisible = forceInvisible.not() && itemsVisibility.isItemSignOutVisibile
        itemSignIn?.isVisible = forceInvisible.not() && itemsVisibility.isItemSignInVisibile
    }

    private fun handleToolSelection(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> onFilterItemSelected()
            R.id.action_read -> onReadItemSelected()
            R.id.action_write -> onWriteItemSelected()
            R.id.action_sign_out -> onSignOutItemSelected()
            R.id.action_sign_in -> onSignInItemSelected()
            else -> return false
        }
        return true
    }

    private fun handleNavSelection(item: MenuItem): Boolean {
        getTargetSection(item)?.let { section ->
            Timber.d("Navigation button pressed: section=$section")
            model.section = section
            return true
        } ?: return false
    }

    private fun reportSearchActiveness(isActive: Boolean) {
        Timber.d("Search active: $isActive")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        if (fragment is Searchable) {
            fragment.isSearchActive = isActive
        }
    }

    private fun search(query: String?): Boolean {
        Timber.d("Search: query=$query")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        return if (fragment is Searchable) {
            ensureNoSearchInput()
            fragment.search(query)
        } else {
            false
        }
    }

    private fun ensureNoSearchInput() {
        menuReference.get()?.let { menu ->
            val itemSearch = menu.findItem(R.id.action_search)
            val searchView = itemSearch.actionView as? SearchView
            searchView?.ensureNoTextInput()
        }
    }

    private fun preview(newText: String?): Boolean {
        Timber.d("Preview search query: newText=$newText")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        return if (fragment is Searchable) {
            fragment.preview(newText)
        } else {
            false
        }
    }

    private fun onFilterItemSelected() {
        Timber.d("Item selected: filter")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        if (fragment is Filterable) {
            fragment.filter()
        }
    }

    private fun invalidateFilter() {
        Timber.d("invalidateFilter")
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        if (fragment is Filterable) {
            fragment.invalidateFilter()
        }
    }

    private fun onReadItemSelected() {
        Timber.d("Item selected: read notes")
        val dialogFragment = GoogleDriveDialogFragment.newInstance(
            mode = GoogleDriveDialogMode.READ
        )
        dialogFragment.setReadingListener()
        dialogFragment.show(supportFragmentManager, TAG_READ)
    }

    private fun onWriteItemSelected() {
        Timber.d("Item selected: write notes")
        val dialogFragment = GoogleDriveDialogFragment.newInstance(
            mode = GoogleDriveDialogMode.WRITE
        )
        dialogFragment.setWritingListener()
        dialogFragment.show(supportFragmentManager, TAG_WRITE)
    }

    private fun onSignOutItemSelected() {
        Timber.d("Item selected: sign out")
        val dialogFragment = ConfirmationDialogFragment.newInstance(
            R.string.dialog_confirm_sign_out_title,
            R.string.dialog_confirm_sign_out_message
        )
        dialogFragment.setSigningOutListener()
        dialogFragment.show(supportFragmentManager, TAG_CONFIRM_SIGN_OUT)
    }

    private fun onSignInItemSelected() {
        Timber.d("Item selected: sign in")
        model.signInGoogle(this)
    }

    private fun addFragment(fragment: Fragment) {
        Timber.d("Add fragment: $fragment")
        supportFragmentManager
            .beginTransaction()
            .replace(binding.section.id, fragment)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment) {
        Timber.d("Replace fragment: $fragment")
        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(binding.section.id, fragment)
            .commit()
    }

    private fun getTargetSection(item: MenuItem): Section? = when (item.itemId) {
        R.id.action_notes -> Section.NOTES
        R.id.action_exercises -> Section.EXERCISES
        R.id.action_statistics -> Section.STATISTICS
        R.id.action_ask -> Section.ASK
        else -> null
    }

    private fun goToScreen(direction: MainViewModel.NavDirection) = when (direction) {
        is MainViewModel.NavDirection.ToSignIn ->
            signInLauncher.launch(direction.intent)
        is MainViewModel.NavDirection.ToAuthOnRead ->
            authOnReadLauncher.launch(direction.intent)
        is MainViewModel.NavDirection.ToAuthOnWrite ->
            authOnWriteLauncher.launch(direction.intent)
    }

    private fun show(message: MainViewModel.Message) = when (message) {
        MainViewModel.Message.ShredNotesUpdate ->
            toast(R.string.message_shred_notes_has_been_updated)
        MainViewModel.Message.GoogleDriveUpdate ->
            toast(R.string.message_google_drive_has_been_updated)
        is MainViewModel.Message.Error ->
            showError(message)
    }

    private fun showError(message: MainViewModel.Message.Error) = when (message) {
        MainViewModel.Message.Error.UnauthorizedUser ->
            toast(R.string.error_unauthorized_user)
        MainViewModel.Message.Error.WrongJsonSyntax ->
            toast(R.string.error_wrong_json_syntax)
        is MainViewModel.Message.Error.UnsupportedDataVersion -> {
            val messageString = getString(
                R.string.error_unsupported_data_version,
                message.version
            )
            toast(messageString)
        }
        is MainViewModel.Message.Error.Clarified -> {
            val messageString = getString(
                R.string.error_clarified,
                message.details
            )
            toast(messageString)
        }
        MainViewModel.Message.Error.Unknown ->
            toast(R.string.error_unknown)
    }

    private fun process(signal: MainViewModel.Signal) = when (signal) {
        MainViewModel.Signal.ContentUpdated -> scrollToBegin()
    }

    private fun scrollToBegin() {
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        if (fragment is Scrollable) {
            fragment.scrollToBegin()
        }
    }

    private fun Menu.invalidate(vararg groups: MenuGroup = MenuGroup.values()) {
        val state = model.state
        val forceInvisible = state == null || state is MainViewModel.State.Waiting
        val fragment = supportFragmentManager.getCurrentlyDisplayedFragment()
        if (groups.contains(MenuGroup.SEARCH)) {
            prepareSearch(this, forceInvisible, fragment)
        }
        if (groups.contains(MenuGroup.FILTER)) {
            prepareFilter(this, forceInvisible, fragment)
        }
        if (groups.contains(MenuGroup.GOOGLE_DRIVE_PANEL)) {
            val itemsVisibility = model.googleDriveItemsVisibility
            prepareGoogleDrivePanel(this, forceInvisible, itemsVisibility)
        }
    }

    @IdRes
    private fun Section.getActionId(): Int = when (this) {
        Section.NOTES -> R.id.action_notes
        Section.EXERCISES -> R.id.action_exercises
        Section.STATISTICS -> R.id.action_statistics
        Section.ASK -> R.id.action_ask
    }

    private fun Section.getFragment(): Fragment = when (this) {
        Section.NOTES -> NotesFragment.newInstance()
        Section.EXERCISES -> ExercisesFragment.newInstance()
        Section.STATISTICS -> StatisticsFragment.newInstance()
        Section.ASK -> AskFragment.newInstance()
    }

    private fun Fragment.setListenersIfNeed() {
        setFilterListenersIfNeed()
    }

    private fun Fragment.setFilterListenersIfNeed() {
        if (this is Filterable) {
            onFilterEnablingChangedListener = {
                Timber.d("Filter enabling changed")
                menuReference.get()?.invalidate(MenuGroup.FILTER)
            }
        }
    }

    private fun SearchView.init() {
        setOnQueryTextListener(null)
        imeOptions = EditorInfo.IME_ACTION_DONE or
                EditorInfo.IME_FLAG_NO_FULLSCREEN or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI
        setOnKeyListener { view, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                view.ensureNoTextInput()
                true
            } else {
                false
            }
        }
    }

    private fun GoogleDriveDialogFragment.setReadingListener() {
        setGoogleDriveFileListener { googleDriveFile ->
            model.read(googleDriveFile)
        }
    }

    private fun GoogleDriveDialogFragment.setWritingListener() {
        setGoogleDriveFileListener { googleDriveFile ->
            model.write(googleDriveFile)
        }
    }

    private fun ConfirmationDialogFragment.setSigningOutListener() {
        setOnConfirmListener {
            model.signOutFromGoogle(this@MainActivity)
        }
    }

    private inner class OptionsMenuInvalidator : FragmentManager.FragmentLifecycleCallbacks() {

        private var isFirstFragmentActivityCreated: Boolean = false

        override fun onFragmentActivityCreated(
            fragmentManager: FragmentManager,
            fragment: Fragment,
            savedInstanceState: Bundle?
        ) {
            Timber.d("onFragmentActivityCreated")
            if (isFirstFragmentActivityCreated) {
                menuReference.get()?.invalidate()
            } else {
                isFirstFragmentActivityCreated = true
            }
        }
    }

    private inner class SearchActivenessListener : MenuItem.OnActionExpandListener {

        override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
            reportSearchActiveness(true)
            return true
        }

        override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
            reportSearchActiveness(false)
            return true
        }
    }

    private inner class StateObserver : Observer<MainViewModel.State> {

        override fun onChanged(state: MainViewModel.State?) {
            Timber.d("State is $state")
            state?.let { setState(it) }
        }

        private fun setState(state: MainViewModel.State) = when (state) {
            is MainViewModel.State.Preparation -> {
                prepare(state)
                model.ok()
            }
            MainViewModel.State.Working ->
                setWorking()
            is MainViewModel.State.Waiting -> {
                setWaiting(state.operation)
                menuReference.get()?.invalidate()
            }
        }

        private fun prepare(state: MainViewModel.State.Preparation) = when (state) {
            MainViewModel.State.Preparation.Initial ->
                Unit
            is MainViewModel.State.Preparation.Continuing -> {
                invalidateFilter()
                if (state.isForceInvalidateTools) {
                    model.invalidateTools()
                }
                menuReference.get()?.invalidate()
            }
            MainViewModel.State.Preparation.GoogleDriveStateChanged -> {
                invalidateSubtitle()
                menuReference.get()?.invalidate(MenuGroup.GOOGLE_DRIVE_PANEL)
            }
        }

        private fun setWorking() = setProgressActive(false)

        private fun setWaiting(operation: MainViewModel.State.Waiting.Operation) {
            setProgressActive(true)
            binding.operationTextView.setText(operation.stringResId())
        }

        private fun setProgressActive(isActive: Boolean) {
            binding.progress.isVisible = isActive
            binding.content.isVisible = isActive.not()
        }

        private fun MainViewModel.State.Waiting.Operation.stringResId(): Int = when (this) {
            MainViewModel.State.Waiting.Operation.READING -> R.string.progress_operation_reading
            MainViewModel.State.Waiting.Operation.WRITING -> R.string.progress_operation_writing
        }
    }

    private enum class MenuGroup {
        SEARCH,
        FILTER,
        GOOGLE_DRIVE_PANEL
    }
}
