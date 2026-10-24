package br.com.streetplay.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY position")
    fun observe(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY position")
    suspend fun all(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE packageName = :pkg")
    suspend fun remove(pkg: String)

    @Query("SELECT COALESCE(MAX(position), -1) FROM favorites")
    suspend fun maxPosition(): Int

    @Query("UPDATE favorites SET position = :position WHERE packageName = :pkg")
    suspend fun setPosition(pkg: String, position: Int)
}

@Dao
interface LaunchDao {
    @Query("SELECT * FROM launch_history ORDER BY lastLaunched DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<LaunchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LaunchEntity)
}
