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
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mariaapp.viewmodel.BakeryViewModel

@Composable
fun StaffScreen(viewModel: BakeryViewModel) {
    val orders by viewModel.orders.collectAsState()
    var currentBlock by remember { mutableStateOf("14:00") }
    val timeBlocks = listOf("14:00", "14:30", "15:00", "15:30", "16:00")
    val stats = viewModel.getDailyStats()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8))) {
        // 頂部狀態列
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

        // 時段選擇 Tabs
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

        // 任務清單
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
                        // 1. 更新資料庫狀態 (打勾)
                        viewModel.markOrderAsReady(order.id)

                        // 2. 準備 Email 內容
                        val subject = "【瑪利MAMA】取貨通知：${order.customerName} 您的麵包好囉！"

                        // 自動生成的信件內文
                        val body = """
                            親愛的 ${order.customerName} 您好：
                            
                            您預約在 ${order.pickupTime} 取貨的麵包已經製作完成囉！
                            請您可以準備前來取貨了。
                            
                            訂單內容：
                            ${order.items.joinToString("\n") { "- ${it.name} x ${it.qty}" }}
                            
                            瑪利MAMA 期待您的光臨！
                        """.trimIndent()

                        // 3. 判斷是否有 Email 並開啟 App
                        if (order.email.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:") // 限定只開啟 Email 類型的 App
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(order.email)) // 收件人
                                putExtra(Intent.EXTRA_SUBJECT, subject) // 主旨
                                putExtra(Intent.EXTRA_TEXT, body) // 內文
                            }

                            try {
                                context.startActivity(intent)
                                Toast.makeText(context, "正開啟信箱通知 ${order.customerName}...", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "找不到 Email App，請確認手機有安裝 Gmail", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "訂單已完成！(此顧客未留 Email)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}