package com.example.mariaapp.view

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
import androidx.compose.material.icons.filled.History // 新增圖示
import androidx.compose.material.icons.filled.Close // 新增圖示
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog // 新增 Dialog
import com.example.mariaapp.model.BakeryOrder // 需確認有引用此 Model
import com.example.mariaapp.model.Product
import com.example.mariaapp.viewmodel.BakeryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(viewModel: BakeryViewModel) {
    val context = LocalContext.current

    // 1. 初始化設定
    LaunchedEffect(Unit) {
        viewModel.initSharedPrefs(context)
    }

    // 2. 訂閱 ViewModel 的資料流
    val cart by viewModel.cart.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val soldMap by viewModel.soldQtyMap.collectAsState()
    // 假設 ViewModel 有公開所有訂單資料 (為了查詢功能)
    val allOrders by viewModel.orders.collectAsState()

    // 3. 狀態變數
    var step by remember { mutableStateOf(1) }
    var selectedTime by remember { mutableStateOf<String?>(null) }

    // === 步驟五新增：控制訂單查詢視窗的開關 ===
    var showOrderHistory by remember { mutableStateOf(false) }

    // 自動帶入儲存的帳號資料
    val savedUser = viewModel.getSavedUser()
    var customerName by remember(savedUser) { mutableStateOf(savedUser.first) }
    var customerEmail by remember(savedUser) { mutableStateOf(savedUser.second) }

    // 商品資料 (略，保持原樣)
    val products = listOf(
        Product("1", "瑪麗媽媽經典", 200,),
        Product("2", "陽光百果", 150, ),
        Product("3", "黑五寶", 40, ),
        Product("4", "裸麥南瓜", 45, ),
        Product("5", "法國起司堡", 60,),
        Product("6", "天然酵母乳酪", 35, ),
        Product("7", "維也納麵包", 30, ),
        Product("8", "法國起司球", 18, ),
        Product("9", "蔓越莓乳酪", 25,),
        Product("10", "黑橄欖乳酪", 25, ),
        Product("11", "巧克力葡萄乾", 20, ),
        Product("12", "核桃", 20, ),
        Product("13", "歐克", 40,),
        Product("14", "布里歐莓", 120, ),
        Product("15", "小波羅(5入)", 50, ),
        Product("16", "椰香", 35, ) ,
        Product("17", "紅豆麵包", 30,),
        Product("18", "墨西哥巧克力", 30, ),
        Product("19", "爆漿餐包(8入)", 70, ),
        Product("20", "法國魔杖", 55, ),
        Product("21", "德國小香腸(4入)", 50,),
        Product("22", "法式香蒜", 40, ),
        Product("23", "不脹氣吐司", 45, ),
        Product("24", "鮮奶吐司", 45, ),
        Product("25", "全麥吐司", 60,),
        Product("26", "蛋糕吐司", 70, ),
        Product("27", "葡萄乾吐司", 75, ),
        Product("28", "火腿起司吐司", 100, ),
        Product("29", "輕乳酪(小)", 35,),
        Product("30", "檸檬塔", 70, ),
        Product("31", "布朗尼", 30, ),
        Product("32", "德式布丁", 40, ),
        Product("33", "黃金乳酪", 35, ),
        Product("34", "丹麥菊花", 60, ),
        Product("35", "丹麥巧克力", 60, ),
        Product("36", "燕麥餅乾", 60,),
        Product("37", "杏仁巧克力", 80, ),
        Product("38", "核桃酥", 80, ),
        Product("39", "芝麻蘇", 80, ),
        Product("40", "英式伯爵紅茶", 80,),
        Product("41", "義式咖啡", 80, ),
        Product("42", "南瓜子瓦片", 90, ),
        Product("43", "杏仁瓦片", 90, ),
        Product("44", "牛奶餅乾", 80, )
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F0))) {
        TopAppBar(
            title = { Text("瑪利MAMA 手作麵包", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFF9800), titleContentColor = Color.White),
            // === 步驟五新增：右上角的查詢按鈕 ===
            actions = {
                IconButton(onClick = { showOrderHistory = true }) {
                    Icon(Icons.Default.History, contentDescription = "查詢訂單", tint = Color.White)
                }
            }
        )

        // === 步驟五新增：訂單查詢彈跳視窗 ===
        if (showOrderHistory) {
            OrderQueryDialog(
                allOrders = allOrders,
                currentEmail = customerEmail,
                onDismiss = { showOrderHistory = false }
            )
        }

        if (step == 1) {
            // === 步驟一：商品選購頁面 ===
            DateSelector(selectedDate) { newDate ->
                viewModel.updateDate(newDate)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(products) { product ->
                    val inCartQty = cart.find { it.name == product.name }?.qty ?: 0
                    val soldQty = soldMap[product.name] ?: 0

                    ProductCard(
                        product = product,
                        cartQty = inCartQty,
                        soldQty = soldQty,
                        onUpdateQty = { delta ->
                            viewModel.updateCartQty(product, delta)
                        }
                    )
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
            // === 步驟二：訂單確認與結帳 ===
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

// === 步驟五新增：訂單查詢視窗元件 ===
@Composable
fun OrderQueryDialog(
    allOrders: List<BakeryOrder>,
    currentEmail: String,
    onDismiss: () -> Unit
) {
    // 根據目前輸入的 Email 篩選訂單
    val myOrders = allOrders.filter { it.email == currentEmail }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 標題列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("我的訂單紀錄", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "關閉")
                    }
                }

                Text("查詢 Email: $currentEmail", fontSize = 14.sp, color = Color.Gray)
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (myOrders.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("目前沒有以此 Email 預約的紀錄", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(myOrders) { order ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(order.pickupDate, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                        Text(order.pickupTime, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 顯示商品摘要
                                    order.items.forEach { item ->
                                        Text("• ${item.name} x${item.qty}", fontSize = 14.sp)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 狀態顯示
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        val statusColor = if (order.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                        val statusText = if (order.isCompleted) "已取貨" else "準備中"
                                        Text(
                                            text = statusText,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 日期選擇器元件 (保持原樣)
@Composable
fun DateSelector(selectedDate: String, onDateSelected: (String) -> Unit) {
    val dates = remember {
        val list = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        repeat(7) {
            list.add(format.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E0))) {
        Text(
            "請選擇預約日期：",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            color = Color(0xFFE65100)
        )

        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dates) { date ->
                val isSelected = date == selectedDate
                val displayDate = date.substring(5).replace("-", "/")

                Button(
                    onClick = { onDateSelected(date) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFFFF9800) else Color.White,
                        contentColor = if (isSelected) Color.White else Color.Black
                    ),
                    border = if (!isSelected) BorderStroke(1.dp, Color.Gray) else null,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(displayDate)
                }
            }
        }
    }
}