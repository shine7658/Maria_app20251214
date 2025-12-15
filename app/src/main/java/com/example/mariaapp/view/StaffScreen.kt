package com.example.mariaapp.view

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday // 新增日期圖示
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
    var selectedDate by remember { mutableStateOf(todayStr) } // ✅ 新增：目前選擇的日期
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

    // 4. ✅ 關鍵過濾：先篩選出「所選日期」的所有訂單
    val selectedDateOrders = orders.filter { it.pickupDate == selectedDate }

    // 計算當日數據 (取代原本 ViewModel 的 getDailyStats，這樣數據才會隨日期變動)
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

        // === ✅ 第一層：日期選擇器 (Date Selector) ===
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD)) // 淺藍色背景區分
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

        // === 任務清單邏輯 (加入日期篩選) ===
        // 篩選條件：日期對 && 時段對 && 狀態是 pending
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
            // ✅ 注意：這裡傳入 selectedDateOrders (已篩選過日期的清單)
            // 這樣總表才會顯示「那一天」的統計，而不是全部歷史紀錄
            DailySummaryDialog(
                date = selectedDate,
                orders = selectedDateOrders,
                onDismiss = { showDailySummary = false }
            )
        }
    }
}

// ✅ DailySummaryDialog：加入日期標示，確保員工知道在看哪一天的表
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
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 標題顯示日期
                Text(
                    "生產統計表 ($date)", // ✅ 顯示日期
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
                    "此日期共有 $currentCount 筆訂單",
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

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF546E7A))) { Text("關閉", fontSize = 18.sp) }
            }
        }
    }
}