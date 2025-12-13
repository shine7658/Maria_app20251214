package com.example.mariaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mariaapp.model.BakeryOrder
import com.example.mariaapp.model.BakeryRepository
import com.example.mariaapp.model.OrderItem
import com.example.mariaapp.model.Product
// ⚠️ 記得引用我們剛剛寫好的 GMailSender (如果它在上一層 package)
import com.example.mariaapp.GMailSender

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
            email = email,
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

    // ✅ [員工功能] 新增：背景發送 Email 通知
    fun sendEmailNotification(email: String, name: String, items: List<OrderItem>) {
        viewModelScope.launch {
            // 準備信件內容
            val subject = "【瑪利MAMA】取貨通知：$name 您的麵包好囉！"
            val body = """
                親愛的 $name 您好：
                
                您訂購的麵包已經製作完成，請您可以準備前來取貨了。
                
                訂單內容：
                ${items.joinToString("\n") { "- ${it.name} x ${it.qty}" }}
                
                瑪利MAMA 庇護工場感謝您的支持！
            """.trimIndent()

            try {
                // 呼叫 GMailSender 工具 (必須先完成 GMailSender.kt 的設定)
                GMailSender.sendEmail(email, subject, body)
                println("✅ Email 發送成功至: $email")
            } catch (e: Exception) {
                println("❌ Email 發送失敗: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // [員工功能] 取得統計數據
    fun getDailyStats(): Pair<Int, Int> {
        val total = _orders.value.size
        val pending = _orders.value.count { it.status == "pending" }
        return Pair(total, pending)
    }
}