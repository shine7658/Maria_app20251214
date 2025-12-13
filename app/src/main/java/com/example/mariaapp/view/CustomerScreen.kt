package com.example.mariaapp.view

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mariaapp.model.Product
//import com.example.mariaapp.model.Product
import com.example.mariaapp.viewmodel.BakeryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(viewModel: BakeryViewModel) {
    val cart by viewModel.cart.collectAsState()
    var step by remember { mutableStateOf(1) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var customerName by remember { mutableStateOf("") }
    val context = LocalContext.current

    // 假商品資料
    val products = listOf(
        Product("1", "招牌菠蘿", 35, "bread"),
        Product("2", "紅豆麵包", 30, "bean"),
        Product("3", "蔥花麵包", 35, "onion")
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F0))) {
        TopAppBar(
            title = { Text("瑪利MAMA 手作麵包", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFF9800), titleContentColor = Color.White)
        )

        if (step == 1) {
            // 選單
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
            // 結帳
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

                // 1. 姓名欄位 (保持原樣)
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("您的稱呼") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ 2. 新增 Email 欄位 (依照 PDF 第 9 頁)
                var customerEmail by remember { mutableStateOf("") } // 宣告狀態變數

                OutlinedTextField(
                    value = customerEmail,
                    onValueChange = { customerEmail = it },
                    label = { Text("Email (接收取貨通知)") }, // 企劃書文字
                    placeholder = { Text("example@gmail.com") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        // ✅ 修改呼叫方式，傳入 customerEmail
                        viewModel.submitOrder(customerName, customerEmail, selectedTime!!) {
                            // ✅ 修改提示文字，讓使用者知道 Email 已送出
                            Toast.makeText(context, "預約成功！確認信已寄至 $customerEmail", Toast.LENGTH_LONG).show()
                            step = 1
                        }
                    },
                    // ✅ 修改防呆邏輯：Email 也要填寫才能送出
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