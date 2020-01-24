package com.liempo.drowsy.gallery

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.liempo.drowsy.Constants

import com.liempo.drowsy.R
import kotlinx.android.synthetic.main.fragment_gallery.*
import java.io.File

class GalleryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_gallery,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back_button.setOnClickListener { activity?.onBackPressed() }

        with (recycler_view) {
            layoutManager = GridLayoutManager(context, 2)

            val folder = File(context?.applicationInfo?.
                dataDir, Constants.FOLDER_NAME)

            folder.list()?.run {
                adapter = GalleryAdapter(toList(), folder.absolutePath)
            }
        }
    }

}
