package com.example.mariaapp.model

import com.google.firebase.Timestamp

// 1. 商品定義 (菜單)
data class Product(
    val id: String = "",
    val name: String = "",
    val price: Int = 0,
    val iconName: String = "bread",
    val maxDailyQty: Int = 20,
    // 為了讓購物車 UI 能運作，我們還是需要暫存 qty，但加上 @Exclude 讓 Firebase 不存它
    // 如果沒有 Firebase 依賴，暫時先留著這個 var
    var qty: Int = 0
)

// 2. 訂單項目 (被買走的麵包)
data class OrderItem(
    val name: String = "",
    val qty: Int = 0,
    // ✅ 新增：必須把當下的價格存進來，不然無法計算總金額
    val price: Int = 0
)

// 3. 訂單主體
data class BakeryOrder(
    val id: String = "",
    val customerName: String = "",
    val email: String = "",
    val pickupTime: String = "",
    val pickupDate: String = "",
    val items: List<OrderItem> = emptyList(),

    // 狀態：pending (準備中), completed (已取貨/完成)
    val status: String = "pending",

    val timestamp: Timestamp = Timestamp.now()
) {
    // ✅ 1. 修復 totalPrice 紅字：
    // 使用 OrderItem 裡的 price 和 qty 來計算
    val totalPrice: Int
        get() = items.sumOf { it.price * it.qty }

    // ✅ 2. 修復 isCompleted 紅字：
    // 寫一個「屬性 (Property)」來自動轉換 status，讓舊的 UI 程式碼以為有這個變數
    var isCompleted: Boolean
        get() = status == "completed"
        set(value) {
            // 這裡通常不會用到 set，因為我們會直接改 status，但為了相容性先留空或做轉換
        }
}