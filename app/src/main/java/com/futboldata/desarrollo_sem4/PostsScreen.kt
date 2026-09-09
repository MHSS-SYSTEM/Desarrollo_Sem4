package com.futboldata.desarrollo_sem4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Locale

// Logica de red social con Firestore, sin ViewModel: todo el estado vive
// directamente en el Composable con remember/mutableStateOf, y las
// funciones cargarNuevosPosts()/cargarMasPosts() usan listeners de
// Firestore igual que en los ejemplos de consultas avanzadas (orderBy +
// limit + startAfter con el ultimo DocumentSnapshot como cursor).

private val db = Firebase.firestore

@Composable
fun PostsScreen(modifier: Modifier = Modifier) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var ultimoDocumento by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var hayMasPosts by remember { mutableStateOf(true) }
    var cargando by remember { mutableStateOf(false) }
    var textoNuevoPost by remember { mutableStateOf("") }

    fun cargarNuevosPosts() {
        cargando = true
        db.collection("posts")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { resultado ->
                posts = resultado.documents.map { documento ->
                    documento.toObject(Post::class.java)?.copy(id = documento.id) ?: Post()
                }
                ultimoDocumento = resultado.documents.lastOrNull()
                hayMasPosts = resultado.documents.size == 5
                cargando = false
            }
            .addOnFailureListener {
                cargando = false
            }
    }

    fun cargarMasPosts() {
        val cursor = ultimoDocumento ?: return
        cargando = true
        db.collection("posts")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .startAfter(cursor)
            .limit(5)
            .get()
            .addOnSuccessListener { resultado ->
                val nuevos = resultado.documents.map { documento ->
                    documento.toObject(Post::class.java)?.copy(id = documento.id) ?: Post()
                }
                posts = posts + nuevos
                if (resultado.documents.isNotEmpty()) {
                    ultimoDocumento = resultado.documents.last()
                }
                hayMasPosts = nuevos.size == 5
                cargando = false
            }
            .addOnFailureListener {
                cargando = false
            }
    }

    fun publicarPost() {
        if (textoNuevoPost.isBlank()) return
        val nuevoPost = hashMapOf(
            "texto" to textoNuevoPost,
            "fecha" to Timestamp.now()
        )
        db.collection("posts")
            .add(nuevoPost)
            .addOnSuccessListener {
                textoNuevoPost = ""
                cargarNuevosPosts()
            }
    }

    LaunchedEffect(Unit) {
        cargarNuevosPosts()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = textoNuevoPost,
            onValueChange = { textoNuevoPost = it },
            label = { Text("Nuevo post") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { publicarPost() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Publicar")
        }

        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(posts) { post ->
                PostItem(post)
                HorizontalDivider()
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        cargando -> CircularProgressIndicator()
                        hayMasPosts -> Button(onClick = { cargarMasPosts() }) {
                            Text("Cargar más")
                        }
                        else -> Text("No hay más posts")
                    }
                }
            }
        }
    }
}

@Composable
private fun PostItem(post: Post) {
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = post.texto, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = formato.format(post.fecha.toDate()),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
