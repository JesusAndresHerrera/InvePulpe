package com.example.invepulpe.model

data class InventorySummary(
    val totalValueCents: Long,
    val topProducts: List<ProductValuation>,
    val lowStockProducts: List<Product>,
    val productCount: Int,
    val totalUnits: Long
)
