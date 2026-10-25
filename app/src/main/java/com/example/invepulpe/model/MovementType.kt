package com.example.invepulpe.model

data class MovementType(val id: Long, val name: String) {
    override fun toString(): String = name

    companion object {
        const val ENTRY_ID = 1L
        const val EXIT_ID = 2L
    }
}
