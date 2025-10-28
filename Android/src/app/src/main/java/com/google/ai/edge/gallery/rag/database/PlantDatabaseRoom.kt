/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.rag.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [PlantChunkRoom::class],
    version = 1,
    exportSchema = false
)
abstract class PlantDatabaseRoom : RoomDatabase() {
    abstract fun plantChunkDao(): PlantChunkDao

    companion object {
        @Volatile
        private var INSTANCE: PlantDatabaseRoom? = null

        fun getDatabase(context: Context, databasePath: String): PlantDatabaseRoom {
            return INSTANCE ?: synchronized(this) {
                // Extract just the filename from the full path
                val databaseName = java.io.File(databasePath).name

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlantDatabaseRoom::class.java,
                    databaseName
                )
                    .fallbackToDestructiveMigration() // TODO: Add proper migrations for production
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

