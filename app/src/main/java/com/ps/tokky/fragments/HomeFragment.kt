package com.ps.tokky.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.ps.tokky.R
import com.ps.tokky.activities.SettingsActivity
import com.ps.tokky.databinding.FragmentHomeBinding
import com.ps.tokky.utils.TokenAdapter
import com.ps.tokky.utils.Utils
import com.ps.tokky.utils.changeOverflowIconColor
import com.ps.tokky.viewmodels.TokensViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentHomeBinding
    private val adapter: TokenAdapter by lazy {
        TokenAdapter(
            requireContext(),
            tokensViewModel,
            navController
        )
    }

    private val tokensViewModel: TokensViewModel by activityViewModels()

    private val navController: NavController by lazy { findNavController() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        val activity = requireActivity()

        tokensViewModel.fetchTokens()

        binding.toolbar.changeOverflowIconColor(
            Utils.getColorFromAttr(
                context,
                com.google.android.material.R.attr.colorPrimary,
                Color.WHITE
            )
        )

        tokensViewModel.tokensState.observe(activity) { uiState ->
            when (uiState) {
                is TokensViewModel.UIState.Success -> {
                    adapter.updateEntries(uiState.data)
                    binding.emptyLayout.visibility = if (uiState.data.isEmpty()) View.VISIBLE else View.GONE
                }

                is TokensViewModel.UIState.Error -> {

                }

                is TokensViewModel.UIState.Loading -> {

                }
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HomeFragment.adapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        binding.expandableFab.fabManual.setOnClickListener(this)
        binding.expandableFab.fabQr.setOnClickListener(this)

        activity.onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.expandableFab.isFabExpanded) {
                        binding.expandableFab.isFabExpanded = false
                        return
                    }
                    activity.finish()
                }
            })

        binding.toolbar.addMenuProvider(toolbarMenuProvider)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.expandableFab.fabManual.id -> {
                binding.expandableFab.isFabExpanded = false
                navController.navigate(R.id.action_home_fragment_to_token_details_fragment)
            }

            binding.expandableFab.fabQr.id -> {
                binding.expandableFab.isFabExpanded = false
//                addNewQRActivityLauncher.launch(
//                    Intent(context, BarcodeScanningActivity::class.java)
//                )
            }
        }
    }

    private val toolbarMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_main_settings -> {
                    startActivity(Intent(context, SettingsActivity::class.java))
                }
            }
            return true
        }

    }
}