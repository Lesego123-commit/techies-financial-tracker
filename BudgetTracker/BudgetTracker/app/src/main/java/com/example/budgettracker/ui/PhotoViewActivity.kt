package com.example.budgettracker.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.budgettracker.databinding.ActivityPhotoViewBinding
import java.io.File

/*
 * References:
 *  - Bumptech (2023) Glide – Image loading and caching framework for Android. Available at:
 *    https://github.com/bumptech/glide (Accessed: 31 May 2026). The full-screen image loading
 *    and caching below uses the Glide API.
 */

/**
 * PhotoViewActivity – displays a full-screen view of an expense entry's photo.
 *
 * Receives the photo path and description via Intent extras.
 * Uses Glide for efficient image loading and caching.
 */
class PhotoViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoViewBinding

    companion object {
        private const val TAG = "PhotoViewActivity"
        const val EXTRA_PHOTO_PATH = "extra_photo_path"
        const val EXTRA_DESCRIPTION = "extra_description"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val photoPath = intent.getStringExtra(EXTRA_PHOTO_PATH)
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: "Expense Photo"

        supportActionBar?.title = description

        if (photoPath == null) {
            Log.e(TAG, "No photo path received, finishing")
            finish()
            return
        }

        Log.d(TAG, "Loading photo: $photoPath")

        // Load image using Glide – handles both file paths and content URIs
        val imageSource: Any = if (photoPath.startsWith("content://")) {
            Uri.parse(photoPath)
        } else {
            File(photoPath)
        }

        Glide.with(this)
            .load(imageSource)
            .fitCenter()
            .error(android.R.drawable.ic_menu_gallery)
            .into(binding.ivFullPhoto)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
