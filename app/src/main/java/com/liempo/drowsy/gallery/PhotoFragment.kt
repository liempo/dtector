package com.liempo.drowsy.gallery


import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.ORIENTATION_USE_EXIF
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.ImageLoader
import com.github.piasy.biv.loader.glide.GlideImageLoader

import com.liempo.drowsy.R
import kotlinx.android.synthetic.main.fragment_photo.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.Exception

class PhotoFragment : Fragment() {

    private val args: PhotoFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize photo viewer
        BigImageViewer.initialize(
            GlideImageLoader.with(context))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_photo,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back_button.setOnClickListener { activity?.onBackPressed() }

        delete_button.setOnClickListener {
            try {
                if (File(args.path).delete()) {
                    Toast.makeText(context,
                        R.string.msg_success_delete,
                        Toast.LENGTH_SHORT).show()
                    activity?.onBackPressed()
                }
            } catch (e: IOException) { Timber.e(e) }
        }

        media_image.showImage(
            Uri.fromFile(File(args.path)))

        // Fix auto rotate bug
        // https://github.com/Piasy/BigImageViewer/issues/154
        media_image.setImageLoaderCallback(object : ImageLoader.Callback {

            override fun onCacheHit(imageType: Int, image: File?) {
                media_image.ssiv.orientation = ORIENTATION_USE_EXIF
            }

            override fun onFinish() {}
            override fun onSuccess(image: File?) {}
            override fun onFail(error: Exception?) {}
            override fun onCacheMiss(imageType: Int, image: File?) {}
            override fun onProgress(progress: Int) {}
            override fun onStart() {}
        })
    }

}
