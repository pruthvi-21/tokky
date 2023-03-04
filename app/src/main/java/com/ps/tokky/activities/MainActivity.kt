package com.ps.tokky.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.ps.tokky.R
import com.ps.tokky.databinding.ActivityMainBinding
import com.ps.tokky.utils.DBHelper
import com.ps.tokky.utils.DividerItemDecorator
import com.ps.tokky.utils.TokenAdapter

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val helper = DBHelper.getInstance(this)
    private val adapter: TokenAdapter by lazy {
        TokenAdapter(this, ArrayList(), binding.recyclerView, addNewActivityLauncher)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Block screenshots
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter
        val divider = DividerItemDecorator(ResourcesCompat.getDrawable(resources, R.drawable.divider, null))
        binding.recyclerView.addItemDecoration(divider)
        (binding.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        binding.fabAddNew.setOnClickListener {
            addNewActivityLauncher.launch(Intent(this, EnterKeyDetailsActivity::class.java))
        }

        binding.fabAddNew.setOnLongClickListener {
            qrActivityLauncher.launch(Intent(this, CameraScannerActivity::class.java))
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!adapter.editModeEnabled) {
                    finish()
                }
                openEditMode(false)
            }
        })

        refresh(true)
    }

    private fun refresh(reload: Boolean) {
        val list = helper.getAllEntries(reload)
        adapter.updateEntries(list)
        binding.emptyLayout.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        invalidateOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
        adapter.onResume()
    }

    override fun onPause() {
        super.onPause()
        adapter.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!adapter.editModeEnabled)
            menuInflater.inflate(R.menu.menu_main, menu)
        menu?.getItem(1)?.isEnabled = helper.getAllEntries(false).isNotEmpty()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                openEditMode(false)
                return true
            }
            R.id.menu_main_refresh -> {
                refresh(true)
                return true
            }
            R.id.menu_main_edit -> {
                openEditMode(true)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun openEditMode(open: Boolean) {
        if (open) {
            adapter.editModeEnabled = true
            invalidateOptionsMenu()
            supportActionBar?.setTitle(R.string.edit)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
            binding.fabAddNew.hide()
        } else {
            adapter.editModeEnabled = false
            invalidateOptionsMenu()
            supportActionBar?.setTitle(R.string.app_name)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setHomeAsUpIndicator(0)
            binding.fabAddNew.show()
            refresh(false)
        }
    }


    private val addNewActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val extras = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && extras != null) {
                refresh(true)
            }
        }

    private val qrActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                refresh(true)
            }
        }

}