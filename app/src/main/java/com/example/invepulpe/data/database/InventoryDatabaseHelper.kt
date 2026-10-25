package com.example.invepulpe.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.invepulpe.data.database.DatabaseContract.Products as P
import com.example.invepulpe.data.database.DatabaseContract.Movements as M
import com.example.invepulpe.data.database.DatabaseContract.Types as T

class InventoryDatabaseHelper internal constructor(context: Context, name: String?) :
    SQLiteOpenHelper(context.applicationContext, name, null, DatabaseContract.VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE ${P.TABLE} (
                ${P.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${P.NAME} TEXT NOT NULL CHECK(length(trim(${P.NAME})) BETWEEN 1 AND 100),
                ${P.BARCODE} TEXT UNIQUE CHECK(${P.BARCODE} IS NULL OR length(${P.BARCODE}) BETWEEN 1 AND 64),
                ${P.PRICE} INTEGER NOT NULL CHECK(${P.PRICE} BETWEEN 0 AND 100000000),
                ${P.STOCK} INTEGER NOT NULL DEFAULT 0 CHECK(${P.STOCK} BETWEEN 0 AND 1000000),
                ${P.MIN_STOCK} INTEGER NOT NULL DEFAULT 5 CHECK(${P.MIN_STOCK} BETWEEN 0 AND 1000000)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE ${T.TABLE} (
                ${T.ID} INTEGER PRIMARY KEY CHECK(${T.ID} IN (1, 2)),
                ${T.NAME} TEXT NOT NULL UNIQUE,
                CHECK((${T.ID} = 1 AND ${T.NAME} = 'ENTRADA') OR
                      (${T.ID} = 2 AND ${T.NAME} = 'SALIDA'))
            )
        """.trimIndent())
        db.execSQL("INSERT INTO ${T.TABLE} (${T.ID}, ${T.NAME}) VALUES (1, 'ENTRADA'), (2, 'SALIDA')")
        db.execSQL("""
            CREATE TABLE ${M.TABLE} (
                ${M.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${M.PRODUCT_ID} INTEGER NOT NULL REFERENCES ${P.TABLE}(${P.ID}) ON DELETE RESTRICT,
                ${M.TYPE_ID} INTEGER NOT NULL REFERENCES ${T.TABLE}(${T.ID}) ON DELETE RESTRICT,
                ${M.QTY} INTEGER NOT NULL CHECK(${M.QTY} BETWEEN 1 AND 1000000),
                ${M.DATE} TEXT NOT NULL CHECK(length(${M.DATE}) = 24)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_movements_product_date ON ${M.TABLE}(${M.PRODUCT_ID}, ${M.DATE})")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add explicit migrations here when increasing VERSION; never discard user inventory.
        error("Falta la migración de la versión $oldVersion a $newVersion")
    }

    companion object {
        @Volatile private var instance: InventoryDatabaseHelper? = null

        fun getInstance(context: Context): InventoryDatabaseHelper = instance ?: synchronized(this) {
            instance ?: InventoryDatabaseHelper(context, DatabaseContract.NAME).also { instance = it }
        }

        fun inMemory(context: Context): InventoryDatabaseHelper = InventoryDatabaseHelper(context, null)
    }
}
