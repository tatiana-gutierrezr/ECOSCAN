package com.example.ecoscan

data class User(
    var fullName: String? = "",
    var username: String? = "",
    var email: String? = "",
    var imageUrl: String? = null // Añadir este campo si lo necesitas
)