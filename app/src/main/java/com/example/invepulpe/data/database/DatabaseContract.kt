package com.example.invepulpe.data.database

object DatabaseContract {
    const val NAME = "invepulpe.db"
    const val VERSION = 1

    object Products {
        const val TABLE = "products"
        const val ID = "id"
        const val NAME = "name"
        const val BARCODE = "barcode"
        const val PRICE = "price_cents"
        const val STOCK = "stock"
        const val MIN_STOCK = "min_stock"
    }

    object Movements {
        const val TABLE = "movements"
        const val ID = "id"
        const val PRODUCT_ID = "product_id"
        const val TYPE_ID = "type_id"
        const val QTY = "qty"
        const val DATE = "date"
    }

    object Types {
        const val TABLE = "movement_types"
        const val ID = "id"
        const val NAME = "name"
    }
}
