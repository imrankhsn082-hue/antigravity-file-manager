package com.antigravity.filemanager

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

/**
 * Registers a global Coil [ImageLoader] that can decode video frames, so the
 * stories row and list thumbnails can show real previews for video files.
 */
class AntigravityApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
