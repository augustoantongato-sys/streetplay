package br.com.streetplay.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val packageName: String, val position: Int)

@Entity(tableName = "launch_history")
data class LaunchEntity(@PrimaryKey val packageName: String, val lastLaunched: Long)
