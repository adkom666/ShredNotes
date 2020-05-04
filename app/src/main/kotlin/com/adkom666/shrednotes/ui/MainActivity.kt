package com.adkom666.shrednotes.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.adkom666.shrednotes.R
import com.adkom666.shrednotes.databinding.ActivityMainBinding

/**
 * Main screen.
 */
class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding
        get() = _binding ?: error("View binding is not initialized!")

    private val model: MainViewModel
        get() = _model ?: error("View model is not initialized!")

    private var _binding: ActivityMainBinding? = null
    private var _model: MainViewModel? = null

    private val onQueryTextListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                TODO("Not yet implemented")
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                TODO("Not yet implemented")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        _model = ViewModelProvider(this).get(MainViewModel::class.java)

        model.isSearchVisibleAsLiveData.observe(this, Observer {
            invalidateOptionsMenu()
        })

        model.isFilterVisibleAsLiveData.observe(this, Observer {
            invalidateOptionsMenu()
        })

        model.isFilterEnabledAsLiveData.observe(this, Observer {
            invalidateOptionsMenu()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tools, menu)
        menu?.let {
            initSearch(it)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            prepareSearch(it)
            prepareFilter(it)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            return if (handleSelection(it)) {
                true
            } else {
                super.onOptionsItemSelected(it)
            }
        } ?: return false
    }

    private fun initSearch(menu: Menu) {
        val itemSearch = menu.findItem(R.id.action_search)
        val searchView = itemSearch.actionView as? SearchView
        searchView?.setOnQueryTextListener(onQueryTextListener)
    }

    private fun prepareSearch(menu: Menu) {
        val searchVisible = model.isSearchVisible
        menu.setGroupVisible(R.id.group_search, searchVisible)
    }

    private fun prepareFilter(menu: Menu) {
        val itemFilter = menu.findItem(R.id.action_filter)
        val filterVisible = model.isFilterVisibleAsLiveData.value ?: true
        if (filterVisible) {
            val filterEnabled = model.isFilterEnabledAsLiveData.value ?: false
            val iconRes = if (filterEnabled) {
                R.drawable.ic_filter_on
            } else {
                R.drawable.ic_filter_off
            }
            itemFilter?.icon = getDrawable(iconRes)
        }
        menu.setGroupVisible(R.id.group_filter, filterVisible)
    }

    private fun handleSelection(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> filter()
            R.id.action_read -> read()
            R.id.action_write -> write()
            R.id.action_sign_in -> signIn()
            R.id.action_sign_out -> signOut()
            else -> return false
        }
        return true
    }

    private fun filter() {
        // Dummy
        val filterEnabled = model.isFilterEnabled
        model.isFilterEnabled = filterEnabled.not()
    }

    private fun read() {
        TODO("Not yet implemented")
    }

    private fun write() {
        TODO("Not yet implemented")
    }

    private fun signIn() {
        TODO("Not yet implemented")
    }

    private fun signOut() {
        TODO("Not yet implemented")
    }
}
