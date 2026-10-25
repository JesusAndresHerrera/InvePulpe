package com.example.invepulpe.controller

import com.example.invepulpe.service.InventoryRules
import com.example.invepulpe.service.InventoryService

class MovementController(private val inventory: InventoryService) {
    fun list() = inventory.movements()
    fun products() = inventory.products()
    fun types() = inventory.movementTypes()
    fun register(productId: Long, typeId: Long, quantity: String) =
        inventory.recordMovement(productId, typeId, InventoryRules.quantity(quantity, "La cantidad"))
    fun edit(id: Long, typeId: Long, quantity: String) =
        inventory.editMovement(id, typeId, InventoryRules.quantity(quantity, "La cantidad"))
    fun delete(id: Long) = inventory.deleteMovement(id)
}
