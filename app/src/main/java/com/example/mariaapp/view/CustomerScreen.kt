package com.example.mariaapp.view

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place // ✅ 新增：地圖圖示
import androidx.compose.material.icons.filled.Phone // ✅ 新增：電話圖示
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mariaapp.model.BakeryOrder
import com.example.mariaapp.model.Product
import com.example.mariaapp.viewmodel.BakeryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(viewModel: BakeryViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initSharedPrefs(context)
    }

    val cart by viewModel.cart.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val soldMap by viewModel.soldQtyMap.collectAsState()
    val allOrders by viewModel.orders.collectAsState()

    var step by remember { mutableStateOf(1) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    // 控制視窗開關
    var showOrderHistory by remember { mutableStateOf(false) }
    var showRanking by remember { mutableStateOf(false) }
    var showStoreInfo by remember { mutableStateOf(false) } // ✅ 新增：門市資訊視窗狀態

    // 分類功能
    var currentCategory by remember { mutableStateOf("全部") }
    val categories = listOf("全部", "麵包", "吐司", "甜點", "餅乾", "飲料")

    val savedUser = viewModel.getSavedUser()
    var customerName by remember(savedUser) { mutableStateOf(savedUser.first) }
    var customerEmail by remember(savedUser) { mutableStateOf(savedUser.second) }

    // 商品資料 (含分類)
    val products = listOf(
        // === 麵包類 ===
        Product("1", "瑪麗媽媽經典", 200, "麵包"),
        Product("2", "陽光百果", 150, "麵包"),
        Product("3", "黑五寶", 40, "麵包"),
        Product("4", "裸麥南瓜", 45, "麵包"),
        Product("5", "法國起司堡", 60, "麵包"),
        Product("6", "天然酵母乳酪", 35, "麵包"),
        Product("7", "維也納麵包", 30, "麵包"),
        Product("8", "法國起司球", 18, "麵包"),
        Product("9", "蔓越莓乳酪", 25, "麵包"),
        Product("10", "黑橄欖乳酪", 25, "麵包"),
        Product("11", "巧克力葡萄乾", 20, "麵包"),
        Product("12", "核桃麵包", 20, "麵包"),
        Product("13", "歐克麵包", 40, "麵包"),
        Product("14", "布里歐莓", 120, "麵包"),
        Product("15", "小波羅(5入)", 50, "麵包"),
        Product("16", "椰香麵包", 35, "麵包"),
        Product("17", "紅豆麵包", 30, "麵包"),
        Product("18", "墨西哥巧克力", 30, "麵包"),
        Product("19", "爆漿餐包(8入)", 70, "麵包"),
        Product("20", "法國魔杖", 55, "麵包"),
        Product("21", "德國小香腸(4入)", 50, "麵包"),
        Product("22", "法式香蒜", 40, "麵包"),
        // === 吐司類 ===
        Product("23", "不脹氣吐司", 45, "吐司"),
        Product("24", "鮮奶吐司", 45, "吐司"),
        Product("25", "全麥吐司", 60, "吐司"),
        Product("26", "蛋糕吐司", 70, "吐司"),
        Product("27", "葡萄乾吐司", 75, "吐司"),
        Product("28", "火腿起司吐司", 100, "吐司"),
        // === 甜點類 ===
        Product("29", "輕乳酪(小)", 35, "甜點"),
        Product("30", "檸檬塔", 70, "甜點"),
        Product("31", "布朗尼", 30, "甜點"),
        Product("32", "德式布丁", 40, "甜點"),
        Product("33", "黃金乳酪", 35, "甜點"),
        Product("34", "丹麥菊花", 60, "甜點"),
        Product("35", "丹麥巧克力", 60, "甜點"),
        // === 餅乾類 ===
        Product("36", "燕麥餅乾", 60, "餅乾"),
        Product("37", "杏仁巧克力", 80, "餅乾"),
        Product("38", "核桃酥", 80, "餅乾"),
        Product("39", "芝麻蘇", 80, "餅乾"),
        // === 飲料類 ===
        Product("40", "英式伯爵紅茶", 80, "飲料"),
        Product("41", "義式咖啡", 80, "飲料"),
        // === 更多餅乾 ===
        Product("42", "南瓜子瓦片", 90, "餅乾"),
        Product("43", "杏仁瓦片", 90, "餅乾"),
        Product("44", "牛奶餅乾", 80, "餅乾")
    )

    val displayedProducts = if (currentCategory == "全部") {
        products
    } else {
        products.filter { it.category == currentCategory }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F0))) {
        TopAppBar(
            title = { Text("瑪利MAMA 手作麵包", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFF9800), titleContentColor = Color.White),
            actions = {
                // ✅ 新增：門市資訊按鈕 (地圖圖示)
                IconButton(onClick = { showStoreInfo = true }) {
                    Icon(Icons.Default.Place, contentDescription = "門市資訊", tint = Color.White)
                }

                IconButton(onClick = { showRanking = true }) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "熱銷排行", tint = Color.Yellow)
                }
                IconButton(onClick = { showOrderHistory = true }) {
                    Icon(Icons.Default.History, contentDescription = "查詢訂單", tint = Color.White)
                }
            }
        )

        // ✅ 新增：門市資訊彈窗
        if (showStoreInfo) {
            StoreInfoDialog(onDismiss = { showStoreInfo = false })
        }

        if (showRanking) {
            HotSalesDialog(products = products, soldMap = soldMap, onDismiss = { showRanking = false })
        }

        if (showOrderHistory) {
            OrderQueryDialog(allOrders = allOrders, currentEmail = customerEmail, onDismiss = { showOrderHistory = false })
        }

        if (step == 1) {
            // === 步驟一 ===
            DateSelector(selectedDate) { viewModel.updateDate(it) }

            CategoryTabs(
                categories = categories,
                selectedCategory = currentCategory,
                onCategorySelected = { currentCategory = it }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (displayedProducts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("此分類暫無商品", color = Color.Gray)
                        }
                    }
                } else {
                    items(displayedProducts) { product ->
                        val inCartQty = cart.find { it.name == product.name }?.qty ?: 0
                        val soldQty = soldMap[product.name] ?: 0

                        ProductCard(
                            product = product,
                            cartQty = inCartQty,
                            soldQty = soldQty,
                            onUpdateQty = { delta -> viewModel.updateCartQty(product, delta) }
                        )
                    }
                }
            }

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
            // === 步驟二 (保持不變) ===
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("1. 您的訂單", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("預約日期: $selectedDate", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                cart.forEach {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.name, fontSize = 18.sp)
                        Text("x ${it.qty}", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("2. 選擇取貨時段", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val timeSlots = listOf("14:00", "14:30", "15:00", "15:30", "16:00")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(150.dp),
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
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customerEmail,
                    onValueChange = { customerEmail = it },
                    label = { Text("Email (接收取貨通知)") },
                    placeholder = { Text("example@gmail.com") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("返回修改", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.submitOrder(customerName, customerEmail, selectedTime!!) {
                            Toast.makeText(context, "預約成功！確認信已寄至 $customerEmail", Toast.LENGTH_LONG).show()
                            step = 1
                            selectedTime = null
                        }
                    },
                    enabled = selectedTime != null && customerName.isNotEmpty() && customerEmail.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("確認預約", fontSize = 20.sp)
                }
            }
        }
    }
}

// === ✅ 新增：門市資訊與導航元件 ===
@Composable
fun StoreInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    // 瑪利MAMA 麵包店的真實資訊 (位於台中市政府)
    val address = "台中市西屯區臺灣大道三段99號"
    val locationName = "瑪利MAMA手作麵包"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏡 門市資訊", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                Spacer(modifier = Modifier.height(16.dp))

                // 店名
                Text("瑪利MAMA 手作麵包", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // 地址與電話
                Text("📍 $address", fontSize = 16.sp)
                Text("(台中市政府 惠中樓 1樓)", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("04-2251-7909", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("🕒 營業時間: 週一至週五 08:00 - 18:00", fontSize = 14.sp, color = Color(0xFF388E3C))

                Spacer(modifier = Modifier.height(24.dp))

                // 導航按鈕
                Button(
                    onClick = {
                        // 開啟 Google Maps 導航 Intent
                        val gmmIntentUri = Uri.parse("geo:0,0?q=$address($locationName)")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")

                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // 如果沒安裝 Google Maps，改用網頁版或其他地圖
                            Toast.makeText(context, "找不到地圖應用程式", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("開啟導航")
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("關閉", color = Color.Gray)
                }
            }
        }
    }
}

// === 分類按鈕列元件 (保持不變) ===
@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF8D6E63),
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = Color.Black
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if(isSelected) Color(0xFF8D6E63) else Color.Gray,
                    borderWidth = 1.dp
                )
            )
        }
    }
}

// HotSalesDialog, OrderQueryDialog, DateSelector 等元件請保持原樣 (不需要更動)
@Composable
fun HotSalesDialog(products: List<Product>, soldMap: Map<String, Int>, onDismiss: () -> Unit) {
    val topProducts = products.map { product -> product to (soldMap[product.name] ?: 0) }
        .sortedByDescending { it.second }.take(5)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("本月熱銷 TOP 5", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 350.dp)) {
                    items(topProducts) { (product, count) ->
                        val rank = topProducts.indexOfFirst { it.first == product } + 1
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = if (rank <= 3) Color(0xFFFFD700) else Color.LightGray, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(24.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text("$rank", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(product.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                            Text("已售出 $count", fontSize = 14.sp, color = Color.Gray)
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(top = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), modifier = Modifier.fillMaxWidth()) {
                    Text("關閉")
                }
            }
        }
    }
}

@Composable
fun OrderQueryDialog(allOrders: List<BakeryOrder>, currentEmail: String, onDismiss: () -> Unit) {
    val myOrders = allOrders.filter { it.email == currentEmail }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("我的訂單紀錄", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "關閉") }
                }
                Text("查詢 Email: $currentEmail", fontSize = 14.sp, color = Color.Gray)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                if (myOrders.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("目前沒有以此 Email 預約的紀錄", color = Color.Gray) }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(myOrders) { order ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${order.pickupDate} ${order.pickupTime}", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                        val isReady = order.status != "pending"
                                        Text(if (isReady) "可取貨 / 已完成" else "準備中", color = if (isReady) Color(0xFF4CAF50) else Color(0xFFFF9800), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    order.items.forEach { item -> Text("• ${item.name} x${item.qty}", fontSize = 14.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateSelector(selectedDate: String, onDateSelected: (String) -> Unit) {
    val dates = remember {
        val list = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        repeat(7) { list.add(format.format(calendar.time)); calendar.add(Calendar.DAY_OF_YEAR, 1) }
        list
    }
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E0))) {
        Text("請選擇預約日期：", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 8.dp), color = Color(0xFFE65100))
        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dates) { date ->
                val isSelected = date == selectedDate
                Button(
                    onClick = { onDateSelected(date) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color(0xFFFF9800) else Color.White, contentColor = if (isSelected) Color.White else Color.Black),
                    border = if (!isSelected) BorderStroke(1.dp, Color.Gray) else null,
                    shape = RoundedCornerShape(50)
                ) { Text(date.substring(5).replace("-", "/")) }
            }
        }
    }
}