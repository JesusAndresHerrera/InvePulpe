package com.example.invepulpe.data.dao

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.invepulpe.data.database.DatabaseContract.Products as P
import com.example.invepulpe.model.Product

class ProductDao {
    fun all(db: SQLiteDatabase): List<Product> = db.query(
        P.TABLE, null, null, null, null, null, "${P.NAME} COLLATE NOCASE, ${P.ID}"
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(read(cursor)) } }

    fun find(db: SQLiteDatabase, id: Long): Product? = db.query(
        P.TABLE, null, "${P.ID} = ?", arrayOf(id.toString()), null, null, null
    ).use { if (it.moveToFirst()) read(it) else null }

    fun barcodeExists(db: SQLiteDatabase, barcode: String, exceptId: Long): Boolean = db.query(
        P.TABLE, arrayOf(P.ID), "${P.BARCODE} = ? AND ${P.ID} <> ?",
        arrayOf(barcode, exceptId.toString()), null, null, null, "1"
    ).use { it.moveToFirst() }

    fun insert(db: SQLiteDatabase, product: Product): Long =
        db.insertOrThrow(P.TABLE, null, values(product))

    fun update(db: SQLiteDatabase, product: Product) {
        check(db.update(P.TABLE, values(product), "${P.ID} = ?", arrayOf(product.id.toString())) == 1)
    }

    fun setStock(db: SQLiteDatabase, id: Long, stock: Int) {
        val values = ContentValues().apply { put(P.STOCK, stock) }
        check(db.update(P.TABLE, values, "${P.ID} = ?", arrayOf(id.toString())) == 1)
    }

    fun delete(db: SQLiteDatabase, id: Long) {
        check(db.delete(P.TABLE, "${P.ID} = ?", arrayOf(id.toString())) == 1)
    }

    private fun values(product: Product) = ContentValues().apply {
        put(P.NAME, product.name)
        put(P.BARCODE, product.barcode)
        put(P.PRICE, product.priceCents)
        put(P.STOCK, product.stock)
        put(P.MIN_STOCK, product.minStock)
    }

    private fun read(cursor: Cursor) = Product(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(P.ID)),
        name = cursor.getString(cursor.getColumnIndexOrThrow(P.NAME)),
        barcode = cursor.getString(cursor.getColumnIndexOrThrow(P.BARCODE)),
        priceCents = cursor.getLong(cursor.getColumnIndexOrThrow(P.PRICE)),
        stock = cursor.getInt(cursor.getColumnIndexOrThrow(P.STOCK)),
        minStock = cursor.getInt(cursor.getColumnIndexOrThrow(P.MIN_STOCK))
    )
}
