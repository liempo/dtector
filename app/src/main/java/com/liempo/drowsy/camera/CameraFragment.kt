package com.liempo.drowsy.camera

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.ImageReaderMode.*
import androidx.camera.core.ImageCapture.OnImageSavedListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.liempo.drowsy.Constants.FILENAME_DATE_FORMAT
import com.liempo.drowsy.Constants.FILENAME_FORMAT
import com.liempo.drowsy.Constants.FOLDER_NAME
import com.liempo.drowsy.R
import kotlinx.android.synthetic.main.fragment_camera.*
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.math.roundToInt

// TODO FIX THIS FUCKING HELL
class CameraFragment : Fragment() {

    /** Internal variable used to keep track
     * of the use-case's output rotation */
    private var bufferRotation: Int = 0

    /** Internal variable used to
     * keep track of the view's rotation */
    private var viewFinderRotation: Int? = null

    /** Internal variable used to keep
     * track of the use-case's output dimension */
    private var bufferDimens: Size = Size(0, 0)

    /** Internal variable used to
     * keep track of the view's dimension */
    private var viewFinderDimens: Size = Size(0, 0)

    /** Internal variable used to keep
     * track of the view's display */
    private var viewFinderDisplay: Int = -1

    /** Internal variable used to keep track
     * of the calculated dimension of the preview image */
    private var cachedTargetDimens = Size(0, 0)

    /** Internal reference of the [DisplayManager] */
    private lateinit var displayManager: DisplayManager

    private var isTimerRunning = false
    /** For counting purposes lol */
    private lateinit var timer: CountDownTimer

    /** Sound objects for alarm_1 stuff*/
    private lateinit var alarm: MediaPlayer
    private lateinit var tick: MediaPlayer

    /** CameraX components */
    private lateinit var preview: Preview
    private lateinit var analysis: ImageAnalysis
    private lateinit var capture: ImageCapture

    /** CameraX listeners */
    private lateinit var onImageSavedListener: OnImageSavedListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(
        R.layout.fragment_camera,
        container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back_button.setOnClickListener { activity?.onBackPressed() }

        // Check Camera permissions
        if (activity?.checkSelfPermission(CAMERA)
            == PackageManager.PERMISSION_GRANTED)
            camera_preview.post { startCameraX() }
        else {
            Timber.i("Requesting permission for camera")
            requestPermissions(arrayOf(CAMERA), RC_CAMERA_PERMISSION)
        }

        val sharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        val alarmId = when(sharedPreferences.getString(
            "pref_alarm", "alarm_1")) {
            "alarm_1" -> R.raw.alarm_1
            "alarm_2" -> R.raw.alarm_2
            "alarm_3" -> R.raw.alarm_3
            else -> R.raw.alarm_1
        }

        tick = MediaPlayer.create(context, R.raw.tick) .apply { isLooping = true }
        alarm = MediaPlayer.create(context, alarmId).apply { isLooping = true }

        cancel_alarm_button.setOnClickListener {
            if (alarm.isPlaying) {
                alarm.pause()
                alarm.seekTo(0)
            }

            if (tick.isPlaying) {
                tick.pause()
                tick.seekTo(0)
            }

            cancel_alarm_button.hide()
        }

        val seconds = (PreferenceManager.getDefaultSharedPreferences(context)
            .getInt("pref_seconds", 3) + 1).toLong() * 1000L

        timer = object: CountDownTimer(seconds , 1000) {

            override fun onTick(millisUntilFinished: Long) {
                time_text.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                startAlarm()
            }
        }

        // Initially hide fab
        cancel_alarm_button.hide()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        val isGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        Timber.i("IsPermissionGranted: $isGranted")

        if (requestCode == RC_CAMERA_PERMISSION && isGranted)
            camera_preview.post { startCameraX() }
        else {
            Snackbar.make(
                camera_preview,
                "Permissions not granted by the user.",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun startCameraX() {
        val rotation = camera_preview.display.rotation
        val ratio = AspectRatio.RATIO_16_9

        // Initialize the display and rotation
        // from texture view information
        viewFinderDisplay = camera_preview.display.displayId
        viewFinderRotation = getDisplaySurfaceRotation(
            camera_preview.display) ?: 0

        // Initialize public use-cases with the given config
        preview = Preview(
            PreviewConfig.Builder().apply {
                setLensFacing(CameraX.LensFacing.FRONT)
                setTargetAspectRatio(ratio)
                setTargetRotation(rotation)
            }.build())

        analysis = ImageAnalysis(ImageAnalysisConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.FRONT)
            setImageReaderMode(ACQUIRE_LATEST_IMAGE)
            setTargetRotation(rotation)
            setTargetAspectRatio(ratio)
        }.build()).apply {
            setAnalyzer(
                ContextCompat.getMainExecutor(
                    context
                ), FaceAnalyzer().apply {

                    noFaceListener = {
                        stopCount()
                    }

                    eyesClosedListener = {
                        if (alarm.isPlaying.not()) {
                            if (it)
                                startCount()
                            else stopCount()
                        }
                    }

                })
        }

        capture = ImageCapture(ImageCaptureConfig.Builder()
            .setLensFacing(CameraX.LensFacing.FRONT)
            .setTargetAspectRatio(ratio)
            .setTargetRotation(rotation)
            .build())


        onImageSavedListener = object: OnImageSavedListener {
            override fun onImageSaved(file: File) {
                Toast.makeText(context, "Saved",
                    Toast.LENGTH_LONG).show()
            }

            override fun onError(
                imageCaptureError: ImageCapture.ImageCaptureError,
                message: String,
                cause: Throwable?
            ) {
                Toast.makeText(context, "Error: $message",
                    Toast.LENGTH_LONG).show()
                Timber.e(cause)
            }

        }

        // Every time the view finder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {
            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = camera_preview.parent as ViewGroup
            parent.removeView(camera_preview)
            parent.addView(camera_preview, 0)

            camera_preview.surfaceTexture = it.surfaceTexture
            bufferRotation = it.rotationDegrees
            val rot = getDisplaySurfaceRotation(
                camera_preview.display)
            updateTransform(camera_preview, rot,
                it.textureSize, viewFinderDimens)
        }

        // Every time the provided texture view changes, recompute layout
        camera_preview.addOnLayoutChangeListener {
                view, left, top, right, bottom, _, _, _, _ ->
            val vf = view as TextureView
            val newViewFinderDimens = Size(
                right - left, bottom - top)
            val rot = getDisplaySurfaceRotation(vf.display)
            updateTransform(vf, rot, bufferDimens, newViewFinderDimens)
        }

        val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == viewFinderDisplay) {
                    val display = displayManager.getDisplay(displayId)
                    val rot = getDisplaySurfaceRotation(display)
                    updateTransform(camera_preview, rot,
                        bufferDimens, viewFinderDimens)
                }
            }
        }

        // Every time the orientation of device changes, recompute layout
        displayManager = requireContext()
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        // Remove the display listeners when the view is detached to avoid
        // holding a reference to the View outside of a Fragment.
        // NOTE: Even though using a weak reference should take care of this,
        // we still try to avoid unnecessary calls to the listener this way.
        camera_preview.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View?) {
                    displayManager.registerDisplayListener(displayListener, null)
                }

                override fun onViewDetachedFromWindow(view: View?) {
                    displayManager.registerDisplayListener(displayListener, null)
                }
            })

        // Bind preview and analyzer
        CameraX.bindToLifecycle(this, preview, analysis, capture)
    }

    private fun startAlarm() {
        time_text.text = getString(R.string.msg_wake_up)

        if (tick.isPlaying) {
            tick.pause()
            tick.seekTo(0)
            alarm.start()
            cancel_alarm_button.show()
        }

        // Generate a filename
        val filename = String.format(FILENAME_FORMAT,
            FILENAME_DATE_FORMAT.format(Calendar.getInstance().time))
        val folder = File(context?.applicationInfo?.dataDir, FOLDER_NAME)
        // Check dirs
        if ((folder.exists() && folder.isDirectory).not())
            folder.mkdir()
        val captured = File(folder, filename)

        capture.takePicture(captured, ContextCompat
            .getMainExecutor(context), onImageSavedListener)
    }

    private fun startCount() {
        // Handles error in threading
        if (time_text == null) return

        time_text.visibility = View.VISIBLE

        if (!isTimerRunning) {
            isTimerRunning = true
            timer.start()
        }

        if (tick.isPlaying.not()) {
            tick.start()
        }
    }

    private fun stopCount() {
        // Handles error in threading
        if (time_text == null) return

        time_text.visibility = View.INVISIBLE

        if (isTimerRunning) {
            isTimerRunning = false
            timer.cancel()

            if (tick.isPlaying) {
                tick.pause()
                tick.seekTo(0)
            }

            if (alarm.isPlaying) {
                alarm.pause()
                alarm.seekTo(0)
            }
        }
    }

    /** Helper function that fits a camera preview into the given [TextureView] */
    private fun updateTransform(
        textureView: TextureView?, rotation: Int?, newBufferDimens: Size,
        newViewFinderDimens: Size
    ) {
        // This should not happen anyway, but now the linter knows
        val tv = textureView ?: return

        if (rotation == viewFinderRotation &&
            Objects.equals(newBufferDimens, bufferDimens) &&
            Objects.equals(newViewFinderDimens, viewFinderDimens)
        ) {
            // Nothing has changed, no need
            // to transform output again
            return
        }

        if (rotation == null) {
            // Invalid rotation - wait for
            // valid inputs before setting matrix
            return
        } else {
            // Update internal field with new inputs
            viewFinderRotation = rotation
        }

        if (newBufferDimens.width == 0 ||
            newBufferDimens.height == 0) {
            // Invalid buffer dimens - wait for
            // valid inputs before setting matrix
            return
        } else {
            // Update internal field with new inputs
            bufferDimens = newBufferDimens
        }

        if (newViewFinderDimens.width == 0 ||
            newViewFinderDimens.height == 0) {
            // Invalid view finder dimens - wait for
            // valid inputs before setting matrix
            return
        } else {
            // Update internal field with new inputs
            viewFinderDimens = newViewFinderDimens
        }

        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinderDimens.width / 2f
        val centerY = viewFinderDimens.height / 2f

        // Correct preview output to account for display rotation
        matrix.postRotate(-viewFinderRotation!!
            .toFloat(), centerX, centerY)

        // Buffers are rotated relative to the device's
        // 'natural' orientation: swap width and height
        val bufferRatio = bufferDimens.height /
                bufferDimens.width.toFloat()

        val scaledWidth: Int
        val scaledHeight: Int

        // Match longest sides together -- i.e. apply center-crop transformation
        if (viewFinderDimens.width > viewFinderDimens.height) {
            scaledHeight = viewFinderDimens.width
            scaledWidth = ((viewFinderDimens.width
                    * bufferRatio).roundToInt())
        } else {
            scaledHeight = viewFinderDimens.height
            scaledWidth = ((viewFinderDimens.height
                    * bufferRatio).roundToInt())
        }

        // save the scaled dimens for use with the overlay
        cachedTargetDimens = Size(scaledWidth, scaledHeight)

        // Compute the relative scale value
        val xScale = scaledWidth / viewFinderDimens.width.toFloat()
        val yScale = scaledHeight / viewFinderDimens.height.toFloat()

        // Scale input buffers to fill the view finder
        matrix.preScale(xScale, yScale, centerX, centerY)

        // Finally, apply transformations to our TextureView
        tv.setTransform(matrix)
    }


    companion object {
        private const val RC_CAMERA_PERMISSION = 4512

        /** Helper function that gets the
         * rotation of a [Display] in degrees */
        fun getDisplaySurfaceRotation(display: Display?)
                = when (display?.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> null
        }
    }

}
