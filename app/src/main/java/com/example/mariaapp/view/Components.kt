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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mariaapp.model.BakeryOrder
import com.example.mariaapp.model.Product

// --- 輔助函式：根據商品名稱決定圖示 ---
// 這樣 ProductCard 和 TaskCard 都可以共用這套邏輯，不用寫兩遍
private fun getBakeryIcon(name: String): ImageVector {
    return when {
        // 飲料類
        name.contains("咖啡") || name.contains("紅茶") -> Icons.Default.LocalCafe
        // 吐司類 (用選單圖示代表層層堆疊的吐司)
        name.contains("吐司") -> Icons.Default.Menu
        // 餅乾/瓦片/酥類 (用圓形代表)
        name.contains("餅乾") || name.contains("瓦片") || name.contains("酥") -> Icons.Default.Adjust
        // 甜點/蛋糕/塔/布丁 (用星星代表甜點)
        name.contains("蛋糕") || name.contains("塔") || name.contains("布丁") || name.contains("布朗尼") -> Icons.Default.Star
        // 鹹食/肉類/香腸/火腿 (用餐廳圖示)
        name.contains("香腸") || name.contains("火腿") || name.contains("肉鬆") || name.contains("起司") -> Icons.Default.Restaurant
        // 特殊口味
        name.contains("紅豆") || name.contains("芋頭") -> Icons.Default.FavoriteBorder // 愛心
        name.contains("菠蘿") -> Icons.Default.Brightness5 // 太陽(像菠蘿)
        name.contains("蔥花") || name.contains("蒜") -> Icons.Default.Spa // 葉子(像蔥蒜)
        // 預設
        else -> Icons.Default.BakeryDining
    }
}

// 1. 商品卡片 (顧客端選單)
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
            // ✅ 使用動態圖示：讓顧客選單也看得到區別
            Icon(
                imageVector = getBakeryIcon(product.name),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF8D6E63)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 商品名稱
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
                maxLines = 1 // 避免名字太長換行太醜
            )

            // 價格
            Text("$${product.price}", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)

            Button(
                onClick = onClick,
                modifier = Modifier.padding(top=8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE0B2), contentColor = Color(0xFFE65100))
            ) {
                Text("+ 加入")
            }
        }
    }
}

// 2. 員工任務卡片 (員工端輔具)
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
                Column {
                    Text(order.customerName, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    // 顯示 email，方便員工確認 (如果有的話)
                    if (order.email.isNotEmpty()) {
                        Text(order.email, fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // 訂單內容
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ 呼叫共用的圖示判斷函式
                    val icon = getBakeryIcon(item.name)

                    Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color(0xFFFF9800))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(item.name, fontSize = 22.sp, modifier = Modifier.weight(1f))
                    // 數量顯示特大
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