package com.example.mariaapp.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mariaapp.GMailSender
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
    private val repository = BakeryRepository()

    // --- 1. UI 狀態 ---
    private val _orders = MutableStateFlow<List<BakeryOrder>>(emptyList())
    val orders: StateFlow<List<BakeryOrder>> = _orders.asStateFlow()

    private val _cart = MutableStateFlow<List<OrderItem>>(emptyList())
    val cart: StateFlow<List<OrderItem>> = _cart.asStateFlow()

    // ✅ 新增：目前選擇的日期 (預設今天)
    private val _selectedDate = MutableStateFlow(getTodayDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // ✅ 新增：即時庫存狀況 (Map: 商品名稱 -> 已賣出數量)
    // 用來判斷該商品在「當天」是否還可以賣
    private val _soldQtyMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val soldQtyMap: StateFlow<Map<String, Int>> = _soldQtyMap.asStateFlow()

    // ✅ 新增：SharedPreferences 用來記住帳號
    private var prefs: SharedPreferences? = null

    // 限制：每個時段最大單量 (時段鎖定用)
    private val MAX_ORDERS_PER_SLOT = 3

    init {
        fetchOrders()
    }

    // --- 2. 初始化與帳號功能 ---

    // ✅ 初始化 SharedPreferences (由 UI 層呼叫)
    fun initSharedPrefs(context: Context) {
        prefs = context.getSharedPreferences("MariaApp", Context.MODE_PRIVATE)
    }

    // ✅ 取得已儲存的用戶資料 (自動帶入帳號)
    fun getSavedUser(): Pair<String, String> {
        val name = prefs?.getString("user_name", "") ?: ""
        val email = prefs?.getString("user_email", "") ?: ""
        return Pair(name, email)
    }

    // --- 3. 核心邏輯 (日期與庫存) ---

    // 監聽訂單
    private fun fetchOrders() {
        viewModelScope.launch {
            repository.getOrdersStream().collect { list ->
                _orders.value = list
                recalculateInventory() // 🔄 資料變動時，重算庫存
            }
        }
    }

    // ✅ 更新選擇的日期 (當使用者點選日期條)
    fun updateDate(date: String) {
        _selectedDate.value = date
        recalculateInventory() // 🔄 日期變動時，重算庫存
    }

    // ✅ 計算當日已賣出的數量 (核心演算法)
    private fun recalculateInventory() {
        val targetDate = _selectedDate.value
        // 1. 篩選出「指定日期」且「非取消」的訂單
        val dayOrders = _orders.value.filter { it.pickupDate == targetDate && it.status != "cancelled" }

        // 2. 統計每個商品的總賣出量
        val map = mutableMapOf<String, Int>()
        dayOrders.forEach { order ->
            order.items.forEach { item ->
                val current = map.getOrDefault(item.name, 0)
                map[item.name] = current + item.qty
            }
        }
        _soldQtyMap.value = map
    }

    // --- 4. 購物車功能 (新增/減少/刪除) ---

    // 🔄 取代原本的 addToCart，支援增加與減少
    // delta = 1 代表增加, -1 代表減少
    fun updateCartQty(product: Product, delta: Int) {
        val currentList = _cart.value.toMutableList()
        val existing = currentList.find { it.name == product.name }

        if (existing != null) {
            val newQty = existing.qty + delta
            if (newQty <= 0) {
                currentList.remove(existing) // 數量歸零則移除 (刪除機制)
            } else {
                // 檢查是否超過每日限量 (需求 2)
                val sold = _soldQtyMap.value[product.name] ?: 0
                // Product 需要有 maxDailyQty 欄位，如果沒有請預設一個數字
                val limit = 20 // 假設上限 20，或使用 product.maxDailyQty

                if (sold + newQty <= limit) {
                    val index = currentList.indexOf(existing)
                    currentList[index] = existing.copy(qty = newQty)
                }
            }
        } else if (delta > 0) {
            // 新增商品
            val sold = _soldQtyMap.value[product.name] ?: 0
            val limit = 20 // 同上

            if (sold + 1 <= limit) {
                currentList.add(OrderItem(product.name, 1))
            }
        }
        _cart.value = currentList
    }

    // --- 5. 訂單送出 ---

    fun isSlotFull(timeSlot: String): Boolean {
        // 需同時比對「日期」與「時段」
        val count = _orders.value.count {
            it.pickupDate == _selectedDate.value &&
                    it.pickupTime == timeSlot &&
                    it.status != "cancelled"
        }
        return count >= MAX_ORDERS_PER_SLOT
    }

    fun submitOrder(name: String, email: String, timeSlot: String, onSuccess: () -> Unit) {
        // ✅ 送出前先儲存帳號 (記住帳號功能)
        prefs?.edit()
            ?.putString("user_name", name)
            ?.putString("user_email", email)
            ?.apply()

        val newOrder = BakeryOrder(
            customerName = name,
            email = email,
            pickupTime = timeSlot,
            items = _cart.value,
            pickupDate = _selectedDate.value, // ✅ 使用選擇的日期
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

    // --- 6. 員工功能 ---

    fun markOrderAsReady(orderId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, "ready")
        }
    }

    fun sendEmailNotification(email: String, name: String, items: List<OrderItem>) {
        viewModelScope.launch {
            val subject = "【瑪利MAMA】取貨通知：$name 您的麵包好囉！"
            val body = """
                親愛的 $name 您好：
                
                您預約在 ${_selectedDate.value} 取貨的麵包已經製作完成。
                
                訂單內容：
                ${items.joinToString("\n") { "- ${it.name} x ${it.qty}" }}
                
                瑪利MAMA 感謝您的支持！
            """.trimIndent()

            try {
                GMailSender.sendEmail(email, subject, body)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getDailyStats(): Pair<Int, Int> {
        // 統計「目前選擇的日期」
        val targetOrders = _orders.value.filter { it.pickupDate == _selectedDate.value }
        val total = targetOrders.size
        val pending = targetOrders.count { it.status == "pending" }
        return Pair(total, pending)
    }

    // --- 輔助 ---
    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}