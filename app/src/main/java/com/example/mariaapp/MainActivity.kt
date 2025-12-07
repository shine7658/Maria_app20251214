package com.example.mariaapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- 1. MODEL 層 (Data Classes) ---

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
    val pickupTime: String = "", // e.g., "14:00"
    val items: List<OrderItem> = emptyList(),
    val status: String = "pending", // pending, ready, completed
    val pickupDate: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

// --- 2. VIEW MODEL 層 (Business Logic) ---

class BakeryViewModel : ViewModel() {
    private val db = Firebase.firestore

    // UI State
    private val _orders = MutableStateFlow<List<BakeryOrder>>(emptyList())
    val orders: StateFlow<List<BakeryOrder>> = _orders.asStateFlow()

    private val _cart = MutableStateFlow<List<OrderItem>>(emptyList())
    val cart: StateFlow<List<OrderItem>> = _cart.asStateFlow()

    // 模擬每日最大產能 (每個時段)
    private val MAX_ORDERS_PER_SLOT = 3

    init {
        listenToOrders()
    }

    // 監聽 Firebase 訂單 (Real-time)
    private fun listenToOrders() {
        // 實際專案應加入 .whereEqualTo("pickupDate", today)
        db.collection("orders")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        doc.toObject(BakeryOrder::class.java)!!.copy(id = doc.id)
                    }
                    _orders.value = list
                }
            }
    }

    // [顧客端邏輯] 加入購物車
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

    // [顧客端邏輯] 檢查時段是否額滿 (核心功能)
    fun isSlotFull(timeSlot: String): Boolean {
        val count = _orders.value.count {
            it.pickupTime == timeSlot && it.status != "cancelled"
        }
        return count >= MAX_ORDERS_PER_SLOT
    }

    // [顧客端邏輯] 送出訂單
    fun submitOrder(name: String, timeSlot: String, onSuccess: () -> Unit) {
        val newOrder = BakeryOrder(
            customerName = name,
            pickupTime = timeSlot,
            items = _cart.value,
            pickupDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            status = "pending"
        )

        db.collection("orders").add(newOrder)
            .addOnSuccessListener {
                _cart.value = emptyList() // 清空購物車
                onSuccess()
            }
    }

    // [員工端邏輯] 完成訂單 (打勾)
    fun markOrderAsReady(orderId: String) {
        db.collection("orders").document(orderId)
            .update("status", "ready")
            .addOnSuccessListener {
                // 這裡可以觸發 FCM 推播通知
                println("Order $orderId is Ready. Sending Notification...")
            }
    }

    // [員工端邏輯] 取得今日統計
    fun getDailyStats(): Pair<Int, Int> {
        val total = _orders.value.size
        val pending = _orders.value.count { it.status == "pending" }
        return Pair(total, pending)
    }
}

// --- 3. VIEW 層 (UI Components) ---

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaliMamaApp()
        }
    }
}

@Composable
fun MaliMamaApp() {
    val viewModel: BakeryViewModel = viewModel()
    var isStaffMode by remember { mutableStateOf(false) } // 切換身份

    Scaffold(
        floatingActionButton = {
            // 開發者後門：切換介面
            FloatingActionButton(
                onClick = { isStaffMode = !isStaffMode },
                containerColor = Color.Black.copy(alpha = 0.5f)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Mode", tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isStaffMode) {
                StaffScreen(viewModel)
            } else {
                CustomerScreen(viewModel)
            }
        }
    }
}

// ================= 顧客端介面 =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(viewModel: BakeryViewModel) {
    val cart by viewModel.cart.collectAsState()
    var step by remember { mutableStateOf(1) } // 1: Menu, 2: Checkout
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var customerName by remember { mutableStateOf("") }
    val context = LocalContext.current

    // 假資料
    val products = listOf(
        Product("1", "招牌菠蘿", 35, "bread"),
        Product("2", "紅豆麵包", 30, "bean"),
        Product("3", "蔥花麵包", 35, "onion")
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F0))) {
        // App Bar
        TopAppBar(
            title = { Text("瑪利MAMA 手作麵包", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFF9800), titleContentColor = Color.White)
        )

        if (step == 1) {
            // 選單模式
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(products) { product ->
                    ProductCard(product) { viewModel.addToCart(product) }
                }
            }

            // 底部結帳按鈕
            if (cart.isNotEmpty()) {
                Button(
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("前往預約 (${cart.sumOf { it.qty }} 個商品)", fontSize = 18.sp)
                }
            }
        } else {
            // 結帳與時段選擇模式
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("1. 您的訂單", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                cart.forEach {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.name, fontSize = 18.sp)
                        Text("x ${it.qty}", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("2. 選擇取貨時段", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("額滿時段將無法點選", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // 時段選擇器 Grid
                val timeSlots = listOf("14:00", "14:30", "15:00", "15:30", "16:00")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(150.dp), // 固定高度
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(timeSlots) { time ->
                        val isFull = viewModel.isSlotFull(time)
                        val isSelected = selectedTime == time

                        Button(
                            onClick = { selectedTime = time },
                            enabled = !isFull,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFFFF9800) else Color.White,
                                disabledContainerColor = Color.LightGray,
                                contentColor = if (isSelected) Color.White else Color.Black
                            ),
                            border = if(!isFull) BorderStroke(1.dp, Color.Gray) else null,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(time, fontWeight = FontWeight.Bold)
                                if (isFull) Text("額滿", fontSize = 10.sp, color = Color.Red)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("3. 訂購人資訊", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("您的稱呼") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.submitOrder(customerName, selectedTime!!) {
                            Toast.makeText(context, "預約成功！", Toast.LENGTH_LONG).show()
                            step = 1 // Reset
                        }
                    },
                    enabled = selectedTime != null && customerName.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("確認預約", fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // 這裡用 Icon 代替真實圖片
            Icon(Icons.Default.BakeryDining, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF8D6E63))
            Spacer(modifier = Modifier.height(8.dp))
            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("$${product.price}", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
            Button(onClick = onClick, modifier = Modifier.padding(top=8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE0B2), contentColor = Color(0xFFE65100))) {
                Text("+ 加入")
            }
        }
    }
}

// ================= 員工端輔具介面 (Staff) =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen(viewModel: BakeryViewModel) {
    val orders by viewModel.orders.collectAsState()
    var currentBlock by remember { mutableStateOf("14:00") }
    val timeBlocks = listOf("14:00", "14:30", "15:00", "15:30", "16:00")

    // 每日統計
    val stats = viewModel.getDailyStats()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8))) {
        // 1. 頂部狀態列 (Daily Summary)
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1976D2)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("今日任務總表", color = Color.White, fontSize = 14.sp)
                Text("待完成: ${stats.second} / 總單: ${stats.first}", color = Color.Yellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2. 時段選擇 Tabs (大按鈕切換)
        LazyRow(
            modifier = Modifier.padding(vertical = 16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(timeBlocks) { time ->
                val isSelected = currentBlock == time
                // 時段按鈕
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFF1976D2) else Color.White)
                        .clickable { currentBlock = time }
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = time,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. 任務卡片列表 (輔具核心)
        // 過濾：只顯示目前時段 且 尚未完成 (pending) 的訂單
        val currentTasks = orders.filter { it.pickupTime == currentBlock && it.status == "pending" }

        if (currentTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SentimentVerySatisfied, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                    Text("這個時段都做完囉！", fontSize = 24.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(currentTasks) { order ->
                    TaskCard(order = order) {
                        viewModel.markOrderAsReady(order.id)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(order: BakeryOrder, onComplete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // 圓角大一點，比較友善
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // 顧客資訊
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(Color(0xFFE3F2FD), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF1976D2))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(order.customerName, fontSize = 28.sp, fontWeight = FontWeight.Black) // 字體加大
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // 訂單內容圖示化
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 根據商品名稱顯示圖示 (實際開發可用資源檔)
                    val icon = when {
                        item.name.contains("菠蘿") -> Icons.Default.Brightness5 // 模擬菠蘿形狀
                        item.name.contains("紅豆") -> Icons.Default.FiberManualRecord
                        else -> Icons.Default.BakeryDining
                    }

                    Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(item.name, fontSize = 22.sp, modifier = Modifier.weight(1f))
                    // 數量顯示特大
                    Text("x ${item.qty}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1976D2))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 超大確認按鈕 (輔具核心互動)
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(80.dp), // 按鈕高度加大
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // 鮮豔綠色
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("完成了！ (打勾)", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}