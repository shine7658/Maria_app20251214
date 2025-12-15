package com.example.mariaapp.view

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mariaapp.model.BakeryOrder
import com.example.mariaapp.viewmodel.BakeryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StaffScreen(viewModel: BakeryViewModel) {
    val orders by viewModel.orders.collectAsState()

    // 1. 取得今日日期字串 (yyyy-MM-dd)
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // 2. 狀態變數
    var selectedDate by remember { mutableStateOf(todayStr) } // 目前選擇的日期
    var currentBlock by remember { mutableStateOf("14:00") }
    var showDailySummary by remember { mutableStateOf(false) }

    // 3. 產生未來 7 天的日期清單
    val dateList = remember {
        val list = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        repeat(7) {
            list.add(format.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // 4. 關鍵過濾：先篩選出「所選日期」的所有訂單
    val selectedDateOrders = orders.filter { it.pickupDate == selectedDate }

    // 計算當日數據
    val totalCount = selectedDateOrders.size
    val pendingCount = selectedDateOrders.count { it.status == "pending" }

    val timeBlocks = listOf("14:00", "14:30", "15:00", "15:30", "16:00")
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8))) {
        // === 頂部藍色狀態列 ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1976D2))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("任務管理看板", color = Color.White, fontSize = 14.sp)
                // 顯示「所選日期」的進度
                Text("待完成: $pendingCount / 總單: $totalCount", color = Color.Yellow, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))

            // 生產總表按鈕
            Button(
                onClick = { showDailySummary = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("當日總表", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // === 第一層：日期選擇器 ===
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD))
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dateList) { date ->
                val isSelected = date == selectedDate
                val displayDate = date.substring(5).replace("-", "/") // 轉成 12/25 格式

                Button(
                    onClick = { selectedDate = date },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF1565C0) else Color.White,
                        contentColor = if (isSelected) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(displayDate, fontWeight = FontWeight.Bold)
                }
            }
        }

        // === 第二層：時段選擇 Tabs ===
        LazyRow(
            modifier = Modifier.padding(vertical = 16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(timeBlocks) { time ->
                val isSelected = currentBlock == time
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

        // === 任務清單邏輯 ===
        val currentTasks = orders.filter {
            it.pickupDate == selectedDate &&
                    it.pickupTime == currentBlock &&
                    it.status == "pending"
        }

        if (currentTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SentimentVerySatisfied, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                    Text("此時段無待辦訂單", fontSize = 24.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(currentTasks) { order ->
                    TaskCard(order = order) {
                        // 1. 更新資料庫
                        viewModel.markOrderAsReady(order.id)

                        // 2. Email 通知邏輯
                        val subject = "【瑪利MAMA】取貨通知：${order.customerName} 您的麵包好囉！"
                        val body = """
                            親愛的 ${order.customerName} 您好：
                            
                            您預約在 ${order.pickupDate} ${order.pickupTime} 取貨的麵包已經製作完成囉！
                            請您可以準備前來取貨了。
                            
                            訂單內容：
                            ${order.items.joinToString("\n") { "- ${it.name} x ${it.qty}" }}
                            
                            瑪利MAMA 期待您的光臨！
                        """.trimIndent()

                        if (order.email.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(order.email))
                                putExtra(Intent.EXTRA_SUBJECT, subject)
                                putExtra(Intent.EXTRA_TEXT, body)
                            }
                            try {
                                context.startActivity(intent)
                                Toast.makeText(context, "訂單完成！正在開啟信箱...", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "訂單完成！(找不到 Email App)", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "訂單已完成！", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // === 生產總表彈跳視窗 ===
        if (showDailySummary) {
            DailySummaryDialog(
                date = selectedDate,
                orders = selectedDateOrders,
                onDismiss = { showDailySummary = false }
            )
        }
    }
}

// ✅ DailySummaryDialog：加入 SalesDashboard (銷售數據分析)
@Composable
fun DailySummaryDialog(date: String, orders: List<BakeryOrder>, onDismiss: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("待製作", "已完成")

    // 使用 status 字串直接判斷
    val pendingOrders = orders.filter { it.status == "pending" }
    val completedOrders = orders.filter { it.status != "pending" }

    fun calculateSummary(list: List<BakeryOrder>): List<Pair<String, Int>> {
        return list
            .flatMap { it.items }
            .groupBy { it.name }
            .mapValues { entry -> entry.value.sumOf { it.qty } }
            .toList()
            .sortedByDescending { it.second }
    }

    val pendingSummary = calculateSummary(pendingOrders)
    val completedSummary = calculateSummary(completedOrders)

    val currentSummary = if (selectedTab == 0) pendingSummary else completedSummary
    val currentCount = if (selectedTab == 0) pendingOrders.size else completedOrders.size

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f), // 拉高一點給 Dashboard 空間
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 標題顯示日期
                Text(
                    "生產統計表 ($date)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37474F),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFEEEEEE),
                    contentColor = Color(0xFF1976D2),
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = if (selectedTab == 0) Color(0xFFD84315) else Color(0xFF4CAF50)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) { if (index == 0) Color(0xFFD84315) else Color(0xFF4CAF50) } else Color.Gray
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "此分類共有 $currentCount 筆訂單",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (currentSummary.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (selectedTab == 0) "本日目前無待製作訂單" else "本日尚無已完成訂單",
                            color = Color.LightGray
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(currentSummary) { (name, totalQty) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selectedTab == 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "$totalQty 個",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selectedTab == 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // ✅ 關鍵：這裡呼叫銷售分析儀表板 (會傳入當天訂單去算錢)
                SalesDashboard(orders)

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF546E7A))) { Text("關閉", fontSize = 18.sp) }
            }
        }
    }
}

// ✅ 修改後的：銷售數據分析儀表板 (內建價格表，確保一定算得出金額)
@Composable
fun SalesDashboard(orders: List<BakeryOrder>) {
    // 1. 定義價格表 (補救措施：防止訂單內沒存到價格)
    val priceMap = mapOf(
        "瑪麗媽媽經典" to 200, "陽光百果" to 150, "黑五寶" to 40, "裸麥南瓜" to 45,
        "法國起司堡" to 60, "天然酵母乳酪" to 35, "維也納麵包" to 30, "法國起司球" to 18,
        "蔓越莓乳酪" to 25, "黑橄欖乳酪" to 25, "巧克力葡萄乾" to 20, "核桃" to 20,
        "歐克" to 40, "布里歐莓" to 120, "小波羅(5入)" to 50, "椰香" to 35,
        "紅豆麵包" to 30, "墨西哥巧克力" to 30, "爆漿餐包(8入)" to 70, "法國魔杖" to 55,
        "德國小香腸(4入)" to 50, "法式香蒜" to 40, "不脹氣吐司" to 45, "鮮奶吐司" to 45,
        "全麥吐司" to 60, "蛋糕吐司" to 70, "葡萄乾吐司" to 75, "火腿起司吐司" to 100,
        "輕乳酪(小)" to 35, "檸檬塔" to 70, "布朗尼" to 30, "德式布丁" to 40,
        "黃金乳酪" to 35, "丹麥菊花" to 60, "丹麥巧克力" to 60, "燕麥餅乾" to 60,
        "杏仁巧克力" to 80, "核桃酥" to 80, "芝麻蘇" to 80, "英式伯爵紅茶" to 80,
        "義式咖啡" to 80, "南瓜子瓦片" to 90, "杏仁瓦片" to 90, "牛奶餅乾" to 80
    )

    // 2. 計算總營收 (使用 priceMap 重新計算，確保準確)
    val totalRevenue = orders.flatMap { it.items }.sumOf { item ->
        val price = priceMap[item.name] ?: 0 // 查表找價格
        price * item.qty
    }

    // 3. 計算熱銷商品前 3 名
    val topProducts = orders
        .flatMap { it.items }
        .groupBy { it.name }
        .mapValues { entry -> entry.value.sumOf { it.qty } }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF1976D2))
                Spacer(modifier = Modifier.width(8.dp))
                Text("今日營運數據", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 總營收
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF4CAF50))
                Text("預估總營收：", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                // 這裡顯示計算出來的金額
                Text("$$totalRevenue", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("🏆 熱銷排行榜 (前3名)", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (topProducts.isEmpty()) {
                Text("尚無銷售數據", fontSize = 12.sp, color = Color.LightGray)
            } else {
                val maxQty = topProducts.first().second.toFloat()
                topProducts.forEachIndexed { index, (name, qty) ->
                    val progress = if (maxQty > 0) qty / maxQty else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 排名與名稱
                        Text("#${index + 1} $name", fontSize = 14.sp, modifier = Modifier.width(100.dp), maxLines = 1)

                        // 進度條
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = when(index) {
                                0 -> Color(0xFFFFD700) // 金
                                1 -> Color(0xFFC0C0C0) // 銀
                                else -> Color(0xFFCD7F32) // 銅
                            },
                            trackColor = Color(0xFFEEEEEE)
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$qty", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}