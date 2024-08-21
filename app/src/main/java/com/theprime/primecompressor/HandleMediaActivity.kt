package com.theprime.primecompressor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.adsmedia.adsmodul.AdsHelper
import com.adsmedia.adsmodul.utils.AdsConfig
import com.theprime.primecompressor.utils.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.theprime.primecompressor.utils.MediaCompressor
import com.theprime.primecompressor.utils.Utils
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


class HandleMediaActivity : AppCompatActivity() {
    // by lazy means load when variable is used, lazy-loading helps performance
    // also without it there is a null error for applicationContext
    private val mediaCompressor: MediaCompressor by lazy { MediaCompressor(applicationContext) }
    private val utils: Utils by lazy { Utils(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_handle_media)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (utils.isReadPermissionGranted) {
            onMediaReceive()
        } else {
            Timber.d("Requesting read permissions")
            utils.requestReadPermissions(this)
        }

        AdsHelper.initializeAdsPrime(this, BuildConfig.APPLICATION_ID, AdsConfig.Game_App_ID)
        AdsHelper.loadInterstitialPrime(this, AdsConfig.Interstitial_ID)

    }

    override fun finish() {
        mediaCompressor.cancelAllOperations()
        scheduleCacheCleanup()
        super.finish()
    }

    override fun onStop() {
        mediaCompressor.cancelAllOperations()
        super.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // first time running app user is requested to allow app to read external storage,
        // after clicking "allow" the app will continue handling media it was shared
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            Timber.d("Read permissions granted, continuing...")
            onMediaReceive()
        }
    }

    private fun onMediaReceive() {
        // "intent" variable is the shared item
        val receivedMedia = when (intent.action) {
            Intent.ACTION_SEND -> arrayListOf(intent.getParcelableExtra(Intent.EXTRA_STREAM)!!)
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)!!
            else -> ArrayList<Uri>()
        }
        // unable to get file from intent
        if (receivedMedia.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_uri_intent), Toast.LENGTH_LONG).show()
            Timber.d("No files found in shared intent")
            finish()
        } else {
            // callback
            mediaCompressor.compressFiles(this, receivedMedia) { compressedMedia ->
                if (compressedMedia.isNotEmpty()) {
                    shareMedia(compressedMedia)
                    saveCompressedFiles(compressedMedia)
                    AdsHelper.showInterstitialPrime(this, AdsConfig.Interstitial_ID, AdsConfig.Interval)
                }
            }
        }
    }

    private fun saveCompressedFiles(mediaUris: ArrayList<Uri>) {
        mediaUris.forEach { uri ->
            val newFileName = "${uri.lastPathSegment?.substringBeforeLast(".")}_compressed.${uri.lastPathSegment?.substringAfterLast(".")}"

            // Create a ContentValues object to insert the new file into MediaStore
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, newFileName)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/CompressedPrime") // Specify your folder name
            }

            // Insert the new file into MediaStore
            val uriOutput = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            uriOutput?.let { outputUri ->
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                            inputStream.copyTo(outputStream) // Save the compressed file
                        }
                    }
                    Toast.makeText(this, "File saved!: $outputUri", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Timber.e("Error saving compressed file: ${e.message}")
                    Toast.makeText(this, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } ?: run {
                Toast.makeText(this, "Failed to create output file", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun shareMedia(mediaUris: ArrayList<Uri>) {
        val shareIntent = Intent()

        // temp permissions for other app to view file
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        // add compressed media files
        if (mediaUris.size == 1) {
            Timber.d("Creating share intent for single item")
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, mediaUris[0])
        } else {
            Timber.d("Creating share intent for multiple items")
            shareIntent.action = Intent.ACTION_SEND_MULTIPLE
            shareIntent.putExtra(Intent.EXTRA_STREAM, mediaUris)
        }

        // set mime for each file
        mediaUris.forEach { mediaUri ->
            shareIntent.setDataAndType(mediaUri, contentResolver.getType(mediaUri))
        }

        val chooser = Intent.createChooser(shareIntent, "media")
        startActivity(chooser)
    }

    private fun scheduleCacheCleanup() {
        Timber.d("Scheduling cleanup alarm")
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, CacheCleanUpReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT)

        // every 12 hours clear cache
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime(),
            AlarmManager.INTERVAL_HALF_DAY,
            pendingIntent
        )
    }
}
