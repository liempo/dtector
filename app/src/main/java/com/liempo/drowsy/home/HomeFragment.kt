package com.liempo.drowsy.home

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.liempo.drowsy.R

class HomeFragment : Fragment() {

    private lateinit var model: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.home_fragment,
        container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Setup view model
        model = ViewModelProviders.of(this)
            .get(HomeViewModel::class.java)
    }

}
