package com.example.mariaapp.view

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
                        viewModel.markOrderAsReady(order.id)

                        // ✅ 這裡實作「模擬發送通知」 (符合 PDF P.6)
                        Toast.makeText(
                            context,
                            "系統已發送取貨通知信給：${order.email}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}