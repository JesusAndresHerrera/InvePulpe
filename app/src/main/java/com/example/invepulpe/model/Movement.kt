package com.example.invepulpe.model

data class Movement(
    val id: Long = 0,
    val productId: Long,
    val typeId: Long,
    val qty: Int,
    val date: String
)
