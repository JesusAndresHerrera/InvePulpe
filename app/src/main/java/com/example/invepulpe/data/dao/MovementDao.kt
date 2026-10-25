package com.example.invepulpe.data.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.invepulpe.data.database.DatabaseContract.Movements as M
import com.example.invepulpe.model.Movement

class MovementDao {
    fun all(db: SQLiteDatabase): List<Movement> = db.query(
        M.TABLE, null, null, null, null, null, "${M.DATE} DESC, ${M.ID} DESC"
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(read(cursor)) } }

    fun find(db: SQLiteDatabase, id: Long): Movement? = db.query(
        M.TABLE, null, "${M.ID} = ?", arrayOf(id.toString()), null, null, null
    ).use { if (it.moveToFirst()) read(it) else null }

    fun forProduct(db: SQLiteDatabase, id: Long): List<Movement> = db.query(
        M.TABLE, null, "${M.PRODUCT_ID} = ?", arrayOf(id.toString()), null, null,
        "${M.DATE}, ${M.ID}"
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(read(cursor)) } }

    fun insert(db: SQLiteDatabase, movement: Movement): Long =
        db.insertOrThrow(M.TABLE, null, values(movement))

    fun update(db: SQLiteDatabase, movement: Movement) {
        check(db.update(M.TABLE, values(movement), "${M.ID} = ?", arrayOf(movement.id.toString())) == 1)
    }

    fun delete(db: SQLiteDatabase, id: Long) {
        check(db.delete(M.TABLE, "${M.ID} = ?", arrayOf(id.toString())) == 1)
    }

    private fun values(movement: Movement) = ContentValues().apply {
        put(M.PRODUCT_ID, movement.productId)
        put(M.TYPE_ID, movement.typeId)
        put(M.QTY, movement.qty)
        put(M.DATE, movement.date)
    }

    private fun read(cursor: Cursor) = Movement(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(M.ID)),
        productId = cursor.getLong(cursor.getColumnIndexOrThrow(M.PRODUCT_ID)),
        typeId = cursor.getLong(cursor.getColumnIndexOrThrow(M.TYPE_ID)),
        qty = cursor.getInt(cursor.getColumnIndexOrThrow(M.QTY)),
        date = cursor.getString(cursor.getColumnIndexOrThrow(M.DATE))
    )
}
