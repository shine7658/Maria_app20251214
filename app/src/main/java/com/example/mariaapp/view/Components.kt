package com.example.mariaapp.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mariaapp.model.BakeryOrder
import com.example.mariaapp.model.Product
//import com.example.mariaapp.model.BakeryOrder
//import com.example.mariaapp.model.Product

// 商品卡片
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

// 員工任務卡片 (輔具)
@Composable
fun TaskCard(order: BakeryOrder, onComplete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                Text(order.customerName, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // 訂單內容
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when {
                        item.name.contains("菠蘿") -> Icons.Default.Brightness5
                        item.name.contains("紅豆") -> Icons.Default.FiberManualRecord
                        else -> Icons.Default.BakeryDining
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(item.name, fontSize = 22.sp, modifier = Modifier.weight(1f))
                    Text("x ${item.qty}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1976D2))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 完成按鈕
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
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