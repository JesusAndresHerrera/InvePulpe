package com.example.invepulpe.data.dao

import android.database.sqlite.SQLiteDatabase
import com.example.invepulpe.data.database.DatabaseContract.Types as T
import com.example.invepulpe.model.MovementType

class MovementTypeDao {
    fun all(db: SQLiteDatabase): List<MovementType> = db.query(
        T.TABLE, arrayOf(T.ID, T.NAME), null, null, null, null, T.ID
    ).use { cursor -> buildList {
        while (cursor.moveToNext()) add(MovementType(cursor.getLong(0), cursor.getString(1)))
    } }
}
