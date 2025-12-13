package com.example.mariaapp.model

import com.google.firebase.Timestamp

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Int = 0,
    val iconName: String = "bread" // 用於對應圖示
)

data class OrderItem(
    val name: String = "",
    val qty: Int = 0
)

data class BakeryOrder(
    val id: String = "",
    val customerName: String = "",
    val email: String = "",
    val pickupTime: String = "", // e.g., "14:00"
    val items: List<OrderItem> = emptyList(),
    val status: String = "pending", // pending, ready, completed
    val pickupDate: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
