package com.example.mariaapp.model

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BakeryRepository {
    private val db = FirebaseFirestore.getInstance()

    // 取得即時訂單流 (回傳 Flow 給 ViewModel 監聽)
    fun getOrdersStream(): Flow<List<BakeryOrder>> = callbackFlow {
        val listener = db.collection("orders")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(BakeryOrder::class.java)?.copy(id = doc.id)
                    }
                    trySend(orders) // 發送資料給觀察者
                }
            }
        awaitClose { listener.remove() } // 當 ViewModel 不聽時，斷開連線
    }

    // 新增訂單
    suspend fun addOrder(order: BakeryOrder) {
        db.collection("orders").add(order).await()
    }

    // 更新訂單狀態
    suspend fun updateOrderStatus(orderId: String, newStatus: String) {
        db.collection("orders").document(orderId).update("status", newStatus).await()
    }
}