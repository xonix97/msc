package org.msc.app

import android.app.Application
import org.msc.app.data.LibraryRepository
import org.msc.app.data.ArtworkCache

class MscApplication : Application() {
    val library by lazy { LibraryRepository(this) }
    val artwork by lazy { ArtworkCache(this) }
}
