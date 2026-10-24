package br.com.streetplay.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.room.withTransaction
import br.com.streetplay.core.common.moved
import br.com.streetplay.core.database.AppDatabase
import br.com.streetplay.core.database.FavoriteEntity
import br.com.streetplay.core.database.LaunchEntity
import br.com.streetplay.domain.AppCategory
import br.com.streetplay.domain.InstalledApp
import br.com.streetplay.domain.ShortcutState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context, private val db: AppDatabase) {
    private val pm = context.packageManager
    private val iconCache = LruCache<String, ImageBitmap>(96)

    val favorites: Flow<List<String>> = db.favoriteDao().observe().map { l -> l.map { it.packageName } }
    fun recents(limit: Int): Flow<List<String>> = db.launchDao().recent(limit).map { l -> l.map { it.packageName } }

    /** Lista apps com ícone de launcher (sem QUERY_ALL_PACKAGES). */
    suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val seen = HashSet<String>()
        listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
            .flatMap { cat -> pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0) }
            .filter { it.activityInfo.packageName != context.packageName }
            .filter { seen.add(it.activityInfo.packageName) }
            .map {
                InstalledApp(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    category = categoryOf(it.activityInfo.applicationInfo),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun shortcuts(): List<ShortcutState> = withContext(Dispatchers.IO) {
        StreamingCatalog.all.map { ShortcutState(it, StreamingCatalog.resolve(it, ::isInstalled)) }
    }

    private fun launchIntentFor(pkg: String): Intent? =
        pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)

    fun isInstalled(pkg: String): Boolean = launchIntentFor(pkg) != null

    suspend fun icon(pkg: String): ImageBitmap? {
        iconCache.get(pkg)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { pm.getApplicationIcon(pkg).toBitmap(192, 192).asImageBitmap() }
                .getOrNull()?.also { iconCache.put(pkg, it) }
        }
    }

    suspend fun launch(pkg: String): Boolean {
        val intent = launchIntentFor(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return try {
            context.startActivity(intent)
            db.launchDao().upsert(LaunchEntity(pkg, System.currentTimeMillis()))
            true
        } catch (e: ActivityNotFoundException) { false } catch (e: SecurityException) { false }
    }

    /** Abre a ficha do app na loja oficial do aparelho (se houver Play Store). */
    fun openStoreListing(pkg: String): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ); true
    } catch (e: ActivityNotFoundException) { false }

    suspend fun toggleFavorite(pkg: String) {
        val dao = db.favoriteDao()
        db.withTransaction {
            if (dao.all().any { it.packageName == pkg }) dao.remove(pkg)
            else dao.upsert(FavoriteEntity(pkg, dao.maxPosition() + 1))
        }
    }

    suspend fun moveFavorite(pkg: String, delta: Int) {
        val dao = db.favoriteDao()
        db.withTransaction {
            val list = dao.all().map { it.packageName }
            val moved = list.moved(list.indexOf(pkg), delta)
            if (moved !== list) moved.forEachIndexed { i, p -> dao.setPosition(p, i) }
        }
    }

    private fun categoryOf(info: ApplicationInfo): AppCategory {
        if (Build.VERSION.SDK_INT < 26) return AppCategory.OTHER
        return when (info.category) {
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
            ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_MAPS -> AppCategory.TOOLS
            else -> AppCategory.OTHER
        }
    }
}
