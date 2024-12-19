package com.ps.tokky.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.ps.tokky.ui.viewmodels.TokensViewModel

abstract class BaseFragment : Fragment() {
    protected val navController: NavController by lazy { findNavController() }

    protected val tokensViewModel: TokensViewModel by activityViewModels()
}