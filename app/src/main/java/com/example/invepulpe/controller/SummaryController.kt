package com.example.invepulpe.controller

import com.example.invepulpe.service.InventoryService

class SummaryController(private val inventory: InventoryService) {
    fun load() = inventory.summary()
}
