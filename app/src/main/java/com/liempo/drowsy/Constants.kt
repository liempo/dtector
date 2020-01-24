package com.liempo.drowsy

import java.text.SimpleDateFormat
import java.util.*

object Constants {

    internal const val FILENAME_FORMAT = "DROWSY_%s"
    internal const val FOLDER_NAME = "gallery"
    internal val FILENAME_DATE_FORMAT = SimpleDateFormat(
        "MM_dd_yyyy_HHmmss", Locale.US)
    internal val DISPLAY_DATE_FORMAT = SimpleDateFormat(
        "dd MMM yyyy", Locale.US)
    internal val DISPLAY_TIME_FORMAT = SimpleDateFormat(
        "hh:mm a", Locale.US)
}