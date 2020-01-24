package com.liempo.drowsy.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.liempo.drowsy.Constants.DISPLAY_DATE_FORMAT
import com.liempo.drowsy.Constants.DISPLAY_TIME_FORMAT
import com.liempo.drowsy.Constants.FILENAME_DATE_FORMAT
import com.liempo.drowsy.Constants.FILENAME_FORMAT
import com.liempo.drowsy.R
import kotlinx.android.synthetic.main.item_gallery.view.*
import java.io.File
import java.util.*

class GalleryAdapter(private val items: List<String>,
                     private val folder: String):
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery,
                parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filename = items[position]
        val date = getDateFromFilename(filename)
        val path = "${folder}${File.separator}${filename}"

        Glide.with(holder.media)
            .load(path)
            .into(holder.media)

        holder.date.text = DISPLAY_DATE_FORMAT.format(date)
        holder.time.text = DISPLAY_TIME_FORMAT.format(date)

        holder.card.setOnClickListener {
            it.findNavController().navigate(
                GalleryFragmentDirections.
                    previewImage(path))
        }

    }

    private fun getDateFromFilename(filename: String): Date {
        var temp = FILENAME_FORMAT
            .replace("%s", "")
        temp = filename.replace(temp, "")
        temp = temp.substring(
            temp.lastIndexOf('/') + 1)
        return FILENAME_DATE_FORMAT.parse(temp)!!
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val card: CardView = view.card
        val media: ImageView = view.media_image
        val date: TextView = view.date_text
        val time: TextView = view.time_text

    }

}