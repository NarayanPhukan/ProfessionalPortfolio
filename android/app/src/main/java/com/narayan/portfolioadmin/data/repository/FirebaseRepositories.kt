package com.narayan.portfolioadmin.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.narayan.portfolioadmin.data.model.ContactMessage
import com.narayan.portfolioadmin.data.model.Profile
import com.narayan.portfolioadmin.data.model.Project
import com.narayan.portfolioadmin.data.model.Skill
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Authentication returned empty user")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}

class ProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getProfileFlow(): Flow<Profile?> = callbackFlow {
        val listener = firestore.collection("profile").document("main")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(Profile::class.java)?.let {
                        if (it.id.isBlank()) it.copy(id = snapshot.id) else it
                    }
                    trySend(profile)
                } else {
                    // Fallback to query first document in profile collection if "main" is not yet populated
                    firestore.collection("profile").limit(1).get()
                        .addOnSuccessListener { querySnap ->
                            val fallbackProfile = querySnap.documents.firstOrNull()?.let { doc ->
                                doc.toObject(Profile::class.java)?.let {
                                    if (it.id.isBlank()) it.copy(id = doc.id) else it
                                }
                            }
                            trySend(fallbackProfile)
                        }
                        .addOnFailureListener {
                            trySend(null)
                        }
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateProfile(profile: Profile): Result<Unit> {
        return try {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val updated = profile.copy(updated_at = now)
            // Update the primary 'main' profile doc
            firestore.collection("profile").document("main").set(updated).await()
            // If the profile had another document ID, sync it as well
            if (profile.id.isNotBlank() && profile.id != "main") {
                firestore.collection("profile").document(profile.id).set(updated).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ProjectsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getProjectsFlow(): Flow<List<Project>> = callbackFlow {
        val listener = firestore.collection("projects")
            .orderBy("display_order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    firestore.collection("projects").get().addOnSuccessListener { s ->
                        val list = s.documents.mapNotNull { doc ->
                            doc.toObject(Project::class.java)?.let {
                                if (it.id.isBlank()) it.copy(id = doc.id) else it
                            }
                        }.sortedBy { it.display_order }
                        trySend(list)
                    }
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Project::class.java)?.let {
                        if (it.id.isBlank()) it.copy(id = doc.id) else it
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveProject(project: Project): Result<Unit> {
        return try {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val docId = if (project.id.isNotBlank()) project.id else firestore.collection("projects").document().id
            val toSave = project.copy(
                id = docId,
                created_at = if (project.created_at.isNotBlank()) project.created_at else now
            )
            firestore.collection("projects").document(docId).set(toSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(id: String): Result<Unit> {
        return try {
            firestore.collection("projects").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SkillsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getSkillsFlow(): Flow<List<Skill>> = callbackFlow {
        val listener = firestore.collection("skills")
            .orderBy("display_order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    firestore.collection("skills").get().addOnSuccessListener { s ->
                        val list = s.documents.mapNotNull { doc ->
                            doc.toObject(Skill::class.java)?.let {
                                if (it.id.isBlank()) it.copy(id = doc.id) else it
                            }
                        }.sortedBy { it.display_order }
                        trySend(list)
                    }
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Skill::class.java)?.let {
                        if (it.id.isBlank()) it.copy(id = doc.id) else it
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveSkill(skill: Skill): Result<Unit> {
        return try {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val docId = if (skill.id.isNotBlank()) skill.id else firestore.collection("skills").document().id
            val toSave = skill.copy(
                id = docId,
                created_at = if (skill.created_at.isNotBlank()) skill.created_at else now
            )
            firestore.collection("skills").document(docId).set(toSave).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSkill(id: String): Result<Unit> {
        return try {
            firestore.collection("skills").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class MessagesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getMessagesFlow(): Flow<List<ContactMessage>> = callbackFlow {
        val listener = firestore.collection("contacts")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    firestore.collection("contacts").get().addOnSuccessListener { s ->
                        val list = s.documents.mapNotNull { doc ->
                            doc.toObject(ContactMessage::class.java)?.let {
                                if (it.id.isBlank()) it.copy(id = doc.id) else it
                            }
                        }.sortedByDescending { it.created_at }
                        trySend(list)
                    }
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ContactMessage::class.java)?.let {
                        if (it.id.isBlank()) it.copy(id = doc.id) else it
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(id: String): Result<Unit> {
        return try {
            firestore.collection("contacts").document(id).update("is_read", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(id: String): Result<Unit> {
        return try {
            firestore.collection("contacts").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun uploadImage(context: Context, uri: Uri, folder: String): Result<String> {
        // Attempt Firebase Storage first if available
        try {
            val fileName = "$folder/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val ref = storage.reference.child(fileName)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            return Result.success(downloadUrl)
        } catch (storageException: Exception) {
            // If Firebase Storage fails (e.g. absent bucket, billing disabled, network failure),
            // seamlessly fall back to an optimized Base64 data URI so user avatars/images never fail!
            return try {
                val dataUri = convertUriToBase64DataUri(context, uri, folder)
                Result.success(dataUri)
            } catch (fallbackException: Exception) {
                Result.failure(Exception("Image processing failed: ${fallbackException.localizedMessage ?: storageException.localizedMessage}"))
            }
        }
    }

    suspend fun uploadImage(uri: Uri, folder: String): Result<String> {
        return try {
            val fileName = "$folder/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val ref = storage.reference.child(fileName)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun convertUriToBase64DataUri(context: Context, uri: Uri, folder: String): String {
        val maxDim = if (folder == "avatars") 360 else 800
        val quality = if (folder == "avatars") 85 else 75

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw Exception("Could not open image stream")

        var inSampleSize = 1
        val (width, height) = options.outWidth to options.outHeight
        if (width > maxDim || height > maxDim) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / inSampleSize) >= maxDim || (halfHeight / inSampleSize) >= maxDim) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw Exception("Could not decode image bitmap")

        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (newWidth, newHeight) = if (ratio > 1) {
                maxDim to (maxDim / ratio).toInt()
            } else {
                (maxDim * ratio).toInt() to maxDim
            }
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64String"
    }
}
