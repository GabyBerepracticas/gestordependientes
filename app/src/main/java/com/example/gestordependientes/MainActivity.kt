package com.example.gestordependientes

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

// ==========================================
// PALETA DE COLORES LILA Y ROJO
// ==========================================
val LilaPrincipal = Color(0xFF7E57C2)
val LilaOscuro = Color(0xFF512DA8)
val LilaSuave = Color(0xFFEDE7F6)
val RojoAcento = Color(0xFFE53935)
val RojoOscuro = Color(0xFFC62828)
val RojoFondoSuave = Color(0xFFFFEBEE)

// MODELOS DE DATOS
data class Usuario(
    val nombre: String,
    val correo: String,
    val password: String
)

data class Tarea(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val fechaLimite: String,
    val categoria: String
)

// ==========================================
// HELPER DE PERSISTENCIA LOCAL (SHAREDPREFERENCES + GSON)
// ==========================================
object GestorAlmacenamiento {
    private const val PREFS_NAME = "GestorPendientesPrefs"

    // Guardar la lista de tareas de un usuario específico
    fun guardarTareas(context: Context, correoUsuario: String, tareas: List<Tarea>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(tareas)
        prefs.edit().putString("tareas_$correoUsuario", json).apply()
    }

    // Cargar las tareas guardadas de un usuario
    fun cargarTareas(context: Context, correoUsuario: String): List<Tarea> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("tareas_$correoUsuario", null) ?: return emptyList()
        val type = object : TypeToken<List<Tarea>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = LilaPrincipal,
                    onPrimary = Color.White,
                    primaryContainer = LilaSuave,
                    secondary = RojoAcento,
                    error = RojoOscuro
                )
            ) {
                GestorApp()
            }
        }
    }
}

@Composable
fun GestorApp() {
    var pantallaActual by remember { mutableStateOf("LOGIN") }
    val usuariosRegistrados = remember { mutableStateListOf<Usuario>() }
    var usuarioLogueado by remember { mutableStateOf<Usuario?>(null) }

    when (pantallaActual) {
        "LOGIN" -> PantallaLogin(
            usuarios = usuariosRegistrados,
            onLoginExitoso = { usuario ->
                usuarioLogueado = usuario
                pantallaActual = "MAIN"
            },
            onIrARegistro = { pantallaActual = "REGISTRO" }
        )
        "REGISTRO" -> PantallaRegistro(
            usuarios = usuariosRegistrados,
            onRegistroExitoso = { pantallaActual = "LOGIN" },
            onVolverLogin = { pantallaActual = "LOGIN" }
        )
        "MAIN" -> PantallaTareas(
            usuario = usuarioLogueado,
            onCerrarSesion = {
                usuarioLogueado = null
                pantallaActual = "LOGIN"
            }
        )
    }
}

// ==========================================
// PANTALLA DE REGISTRO
// ==========================================
@Composable
fun PantallaRegistro(
    usuarios: MutableList<Usuario>,
    onRegistroExitoso: () -> Unit,
    onVolverLogin: () -> Unit
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Registro de Usuario",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LilaOscuro
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (nombre.isBlank() || correo.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                } else if (usuarios.any { it.correo == correo }) {
                    Toast.makeText(context, "El correo ya está registrado", Toast.LENGTH_SHORT).show()
                } else if (password.length < 6) {
                    Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                } else {
                    usuarios.add(Usuario(nombre, correo, password))
                    Toast.makeText(context, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                    onRegistroExitoso()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = LilaPrincipal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse", color = Color.White)
        }

        TextButton(onClick = onVolverLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión", color = RojoAcento)
        }
    }
}

// ==========================================
// PANTALLA DE LOGIN
// ==========================================
@Composable
fun PantallaLogin(
    usuarios: List<Usuario>,
    onLoginExitoso: (Usuario) -> Unit,
    onIrARegistro: () -> Unit
) {
    val context = LocalContext.current
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gestor de Pendientes", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = LilaOscuro)
        Text("Iniciar Sesión", fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val usuarioEncontrado = usuarios.find { it.correo == correo && it.password == password }
                if (usuarioEncontrado != null) {
                    onLoginExitoso(usuarioEncontrado)
                } else {
                    Toast.makeText(context, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = LilaPrincipal),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ingresar", color = Color.White)
        }

        TextButton(onClick = onIrARegistro) {
            Text("¿No tienes cuenta? Regístrate aquí", color = RojoAcento)
        }
    }
}

// ==========================================
// PANTALLA PRINCIPAL DE TAREAS CON PERSISTENCIA
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTareas(
    usuario: Usuario?,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val correo = usuario?.correo ?: ""

    // Cargar tareas guardadas al iniciar la pantalla
    val listaTareas = remember {
        mutableStateListOf<Tarea>().apply {
            addAll(GestorAlmacenamiento.cargarTareas(context, correo))
        }
    }

    // Función auxiliar para persistir en disco
    fun persistirTareas() {
        GestorAlmacenamiento.guardarTareas(context, correo, listaTareas)
    }

    var mostrarDialogoTarea by remember { mutableStateOf(false) }
    var tareaAEditar by remember { mutableStateOf<Tarea?>(null) }
    var tareaAEliminar by remember { mutableStateOf<Tarea?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hola, ${usuario?.nombre ?: "Usuario"}", color = LilaOscuro, fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onCerrarSesion) {
                        Text("Salir", color = RojoAcento, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LilaSuave)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    tareaAEditar = null
                    mostrarDialogoTarea = true
                },
                containerColor = RojoAcento,
                contentColor = Color.White
            ) {
                Text("+", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Mis Pendientes (${listaTareas.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LilaOscuro,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (listaTareas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes tareas pendientes. Presiona '+' para agregar una.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(listaTareas) { tarea ->
                        ItemTarea(
                            tarea = tarea,
                            onEditar = {
                                tareaAEditar = tarea
                                mostrarDialogoTarea = true
                            },
                            onEliminar = {
                                tareaAEliminar = tarea
                            }
                        )
                    }
                }
            }
        }
    }

    if (mostrarDialogoTarea) {
        DialogoCrearEditarTarea(
            tareaEditar = tareaAEditar,
            onDismiss = { mostrarDialogoTarea = false },
            onGuardar = { titulo, descripcion, fecha, categoria ->
                if (tareaAEditar == null) {
                    val nuevaTarea = Tarea(
                        id = (0..9999).random(),
                        titulo = titulo,
                        descripcion = descripcion,
                        fechaLimite = fecha,
                        categoria = categoria
                    )
                    listaTareas.add(nuevaTarea)
                    persistirTareas()
                    Toast.makeText(context, "Tarea creada correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    val index = listaTareas.indexOfFirst { it.id == tareaAEditar!!.id }
                    if (index != -1) {
                        listaTareas[index] = tareaAEditar!!.copy(
                            titulo = titulo,
                            descripcion = descripcion,
                            fechaLimite = fecha,
                            categoria = categoria
                        )
                        persistirTareas()
                        Toast.makeText(context, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                    }
                }
                mostrarDialogoTarea = false
            }
        )
    }

    if (tareaAEliminar != null) {
        AlertDialog(
            onDismissRequest = { tareaAEliminar = null },
            title = { Text("Confirmar eliminación", color = RojoOscuro) },
            text = { Text("¿Estás seguro de eliminar la tarea '${tareaAEliminar?.titulo}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        listaTareas.remove(tareaAEliminar)
                        persistirTareas()
                        tareaAEliminar = null
                        Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoAcento)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { tareaAEliminar = null }) {
                    Text("Cancelar", color = LilaOscuro)
                }
            }
        )
    }
}

@Composable
fun ItemTarea(
    tarea: Tarea,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tarea.titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LilaOscuro)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (tarea.categoria) {
                        "Universidad" -> LilaSuave
                        "Trabajo" -> RojoFondoSuave
                        else -> Color(0xFFF3E5F5)
                    }
                ) {
                    Text(
                        text = tarea.categoria,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (tarea.categoria == "Trabajo") RojoOscuro else LilaOscuro
                    )
                }
            }

            if (tarea.descripcion.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(tarea.descripcion, color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Fecha Límite: ${tarea.fechaLimite}", fontSize = 12.sp, color = Color.Gray)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEditar) {
                    Text("Editar", color = LilaPrincipal)
                }
                TextButton(onClick = onEliminar) {
                    Text("Eliminar", color = RojoAcento)
                }
            }
        }
    }
}

@Composable
fun DialogoCrearEditarTarea(
    tareaEditar: Tarea?,
    onDismiss: () -> Unit,
    onGuardar: (titulo: String, descripcion: String, fecha: String, categoria: String) -> Unit
) {
    val context = LocalContext.current

    var titulo by remember { mutableStateOf(tareaEditar?.titulo ?: "") }
    var descripcion by remember { mutableStateOf(tareaEditar?.descripcion ?: "") }
    var fechaLimite by remember { mutableStateOf(tareaEditar?.fechaLimite ?: "") }

    val categorias = listOf("Universidad", "Trabajo", "Hogar", "Personal")
    var categoriaSeleccionada by remember { mutableStateOf(tareaEditar?.categoria ?: "Universidad") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            fechaLimite = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tareaEditar == null) "Nueva Tarea" else "Editar Tarea", color = LilaOscuro) },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título (Obligatorio)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fechaLimite,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Fecha Límite (Toca para seleccionar)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text("Categoría:", fontWeight = FontWeight.SemiBold, color = LilaOscuro)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    categorias.take(3).forEach { cat ->
                        FilterChip(
                            selected = (categoriaSeleccionada == cat),
                            onClick = { categoriaSeleccionada = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isBlank() || fechaLimite.isBlank()) {
                        Toast.makeText(context, "Título y Fecha son obligatorios", Toast.LENGTH_SHORT).show()
                    } else {
                        onGuardar(titulo, descripcion, fechaLimite, categoriaSeleccionada)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LilaPrincipal)
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = RojoAcento)
            }
        }
    )
}