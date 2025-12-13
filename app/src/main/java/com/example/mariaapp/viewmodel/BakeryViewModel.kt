package com.example.mariaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mariaapp.model.BakeryOrder
import com.example.mariaapp.model.BakeryRepository
import com.example.mariaapp.model.OrderItem
import com.example.mariaapp.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BakeryViewModel : ViewModel() {
    // 引用 Repository
    private val repository = BakeryRepository()

    // UI 狀態 (State)
    private val _orders = MutableStateFlow<List<BakeryOrder>>(emptyList())
    val orders: StateFlow<List<BakeryOrder>> = _orders.asStateFlow()

    private val _cart = MutableStateFlow<List<OrderItem>>(emptyList())
    val cart: StateFlow<List<OrderItem>> = _cart.asStateFlow()

    private val MAX_ORDERS_PER_SLOT = 3

    init {
        // 初始化時開始監聽資料庫
        fetchOrders()
    }

    private fun fetchOrders() {
        viewModelScope.launch {
            repository.getOrdersStream().collect { list ->
                _orders.value = list
            }
        }
    }

    // [顧客功能] 加入購物車
    fun addToCart(product: Product) {
        val currentList = _cart.value.toMutableList()
        val existing = currentList.find { it.name == product.name }
        if (existing != null) {
            val index = currentList.indexOf(existing)
            currentList[index] = existing.copy(qty = existing.qty + 1)
        } else {
            currentList.add(OrderItem(product.name, 1))
        }
        _cart.value = currentList
    }

    // [顧客功能] 檢查時段是否額滿 (時段鎖定邏輯)
    fun isSlotFull(timeSlot: String): Boolean {
        val count = _orders.value.count {
            it.pickupTime == timeSlot && it.status != "cancelled"
        }
        return count >= MAX_ORDERS_PER_SLOT
    }

    // [顧客功能] 送出訂單
    fun submitOrder(name: String, email: String, timeSlot: String, onSuccess: () -> Unit) {
        val newOrder = BakeryOrder(
            customerName = name,
            email = email, // ✅ 將 email 存入物件
            pickupTime = timeSlot,
            items = _cart.value,
            pickupDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            status = "pending"
        )

        viewModelScope.launch {
            try {
                repository.addOrder(newOrder)
                _cart.value = emptyList()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // [員工功能] 完成訂單
    fun markOrderAsReady(orderId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, "ready")
        }
    }

    // [員工功能] 取得統計數據
    fun getDailyStats(): Pair<Int, Int> {
        val total = _orders.value.size
        val pending = _orders.value.count { it.status == "pending" }
        return Pair(total, pending)
    }
}