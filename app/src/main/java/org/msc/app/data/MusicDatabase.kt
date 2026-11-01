package org.msc.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MusicDatabase(context: Context, name: String = "msc-library.db") : SQLiteOpenHelper(context, name, null, 1) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE tracks(uri TEXT PRIMARY KEY,title TEXT NOT NULL,artist TEXT NOT NULL,
            album TEXT NOT NULL,duration INTEGER NOT NULL,filename TEXT NOT NULL,folder TEXT NOT NULL,
            genre TEXT NOT NULL,added INTEGER NOT NULL,source TEXT NOT NULL,favorite INTEGER NOT NULL DEFAULT 0,
            last_played INTEGER NOT NULL DEFAULT 0,play_count INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("CREATE TABLE playlists(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)")
        db.execSQL("""CREATE TABLE playlist_tracks(playlist_id INTEGER NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
            uri TEXT NOT NULL,position INTEGER NOT NULL,PRIMARY KEY(playlist_id,uri))""")
        db.execSQL("CREATE TABLE lyrics(uri TEXT PRIMARY KEY,body TEXT NOT NULL,offset_ms INTEGER NOT NULL DEFAULT 0)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("No migration exists from $oldVersion to $newVersion")
    }
}
