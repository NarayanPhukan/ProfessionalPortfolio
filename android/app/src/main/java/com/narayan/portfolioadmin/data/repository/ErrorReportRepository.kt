package com.narayan.portfolioadmin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.narayan.portfolioadmin.data.model.ErrorReport
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ErrorReportRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("error_reports")

    fun getErrorReportsFlow(): Flow<List<ErrorReport>> = callbackFlow {
        val listener = collection
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ErrorReport(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            error_type = doc.getString("error_type") ?: "USER_REPORT",
                            stack_trace = doc.getString("stack_trace") ?: "",
                            device_info = doc.getString("device_info") ?: "",
                            app_version = doc.getString("app_version") ?: "1.0",
                            screen_name = doc.getString("screen_name") ?: "",
                            created_at = doc.getString("created_at") ?: "",
                            status = doc.getString("status") ?: "new"
                        )
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitReport(report: ErrorReport): Result<Unit> = runCatching {
        val docId = report.id.ifBlank { UUID.randomUUID().toString() }
        val toSave = report.copy(id = docId)
        collection.document(docId).set(toSave).await()
    }

    suspend fun deleteReport(id: String): Result<Unit> = runCatching {
        collection.document(id).delete().await()
    }

    suspend fun markResolved(id: String): Result<Unit> = runCatching {
        collection.document(id).update("status", "resolved").await()
    }
}
