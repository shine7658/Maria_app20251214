package com.example.mariaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
// 引用我們剛寫好的三個部分
import com.example.mariaapp.viewmodel.BakeryViewModel
import com.example.mariaapp.view.CustomerScreen
import com.example.mariaapp.view.StaffScreen

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
    // 建立 ViewModel (這是整個 App 的資料核心)
    val viewModel: BakeryViewModel = viewModel()

    // 狀態：現在是誰在使用？
    var isStaffMode by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            // 切換按鈕
            FloatingActionButton(
                onClick = { isStaffMode = !isStaffMode },
                containerColor = Color.Black.copy(alpha = 0.5f)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Mode", tint = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // 根據模式切換顯示的 Screen，並傳入同一個 ViewModel
            if (isStaffMode) {
                StaffScreen(viewModel = viewModel)
            } else {
                CustomerScreen(viewModel = viewModel)
            }
        }
    }
}