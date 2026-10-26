package br.com.streetplay

import android.app.Application
import android.content.Context
import androidx.room.Room
import br.com.streetplay.core.database.AppDatabase
import br.com.streetplay.core.storage.PrefsStore
import br.com.streetplay.core.storage.XtreamAccountStore
import br.com.streetplay.data.remote.XtreamApi
import br.com.streetplay.data.AppRepository
import br.com.streetplay.data.CatalogRepository
import br.com.streetplay.data.remote.RemoteImageLoader

class AppContainer(context: Context) {
    private val db = Room.databaseBuilder(context, AppDatabase::class.java, "street_play.db").build()
    val repository = AppRepository(context, db)
    val prefs = PrefsStore(context)
    val xtreamAccountStore = XtreamAccountStore(context)
    val xtreamApi = XtreamApi()
    val catalog = CatalogRepository(context, BuildConfig.TMDB_TOKEN)
    val imageLoader = RemoteImageLoader(context)
}

class StreetPlayApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
