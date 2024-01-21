@file:OptIn(ExperimentalComposeUiApi::class)

package com.example.appvacaciones2

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import com.example.appvacaciones2.ui.theme.AppVacaciones2Theme
import java.io.File
import java.time.LocalDateTime
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageSavedCallback

import androidx.camera.core.ImageCaptureException

import androidx.camera.view.PreviewView
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appvacaciones2.db.DBHelper
import com.example.appvacaciones2.db.Lugar
import com.example.appvacaciones2.db.LugarDAO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker


class MainActivity : ComponentActivity() {
    val camaraVM: AppVM by viewModels()
    lateinit var cameraController: LifecycleCameraController
    val lanzadorPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        when {
            (it[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false)
                    or (it[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false) -> {
                Log.v("callback RequestMultiplePermissions", "permiso ubicacion granted")
                camaraVM.onPermisoUbicacionOk()
            }
            (it[android.Manifest.permission.CAMERA] ?: false) -> {
                Log.v("callback RequestMultiplePermissions", "permiso camara granted")
                camaraVM.onPermisoCamaraOk()
            }
            else -> {
                Log.v("lanzador permisos callback" , "Sin permisos")
            }
        }
    }

    //CONFIGURACION DE CAMARA
    private fun setupCamara() {
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        camaraVM.lanzadorPermisos = lanzadorPermisos
        setupCamara()
        lanzadorPermisos.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))

        lifecycleScope.launch(Dispatchers.IO) {
            val dbHelper = DBHelper(this@MainActivity)
            val dao = LugarDAO(dbHelper)
            val cantidadRegistros = dao.getCantidadRegistros()
            if (cantidadRegistros < 1) {
                dao.insertar(Lugar(0,"Sitio 1","URL",40.5,40.5,1,100.1,200.2,"Bonito"))
                dao.insertar(Lugar(0,"Sitio 2","URL",40.5,40.5,1,100.1,200.2,"Bonito"))
            }

        }
        setContent {
            AppUI(cameraController)
        }
    }
}

//ENUM CLASS
enum class Pantalla{
    FORM,
    FOTO,
    LISTA,
    EDITAR,
    MAPA
}

// VIEWMODELS
class AppVM : ViewModel(){
    val pantallaActual = mutableStateOf(Pantalla.LISTA)
    // callbacks
    var onPermisoCamaraOk:() -> Unit = {}
    var onPermisoUbicacionOk:() -> Unit = {}

    var lanzadorPermisos: ActivityResultLauncher<Array<String>>? = null

    fun cambiarPantallaFoto() { pantallaActual.value = Pantalla.FOTO}
    fun cambiarPantallaForm() { pantallaActual.value = Pantalla.FORM}
    fun cambiarPantallaLista() { pantallaActual.value = Pantalla.LISTA}
    fun cambiarPantallaEditar() { pantallaActual.value = Pantalla.EDITAR}
    fun cambiarPantallaMapa() { pantallaActual.value = Pantalla.MAPA}
}
class FormRegistroVM : ViewModel() {
    val nombre = mutableStateOf("")
    val latitud = mutableStateOf(0.0)
    val longitud = mutableStateOf(0.0)
    var longitude = mutableStateOf(0.0)
    var orden = mutableStateOf(0)
    var costoAlojamiento = mutableStateOf(0.0)
    var costoTransporte = mutableStateOf(0.0)
    var comentarios = mutableStateOf("")
    val foto = mutableStateOf<Uri?>(null)
}

//MANEJO DE INTERFACES

@Composable
fun AppUI(cameraController: LifecycleCameraController) {
    val contexto = LocalContext.current
    val formRegistroVM:FormRegistroVM = viewModel()
    val appVM:AppVM =viewModel()
    when(appVM.pantallaActual.value) {
        Pantalla.FORM -> {
            LugarFormUI(
                appVM
            )
        }
        Pantalla.FOTO -> {
            PantallaCamaraUI(FormRegistroVM(), appVM, cameraController)
        }
        Pantalla.LISTA -> {
            PantallaInicioListaUI(appVM)

        }
        Pantalla.EDITAR -> {
            PantallaInicioListaUI(appVM)

        }
        Pantalla.MAPA -> {
            MapaFullScreenUI(
                latitud = formRegistroVM.latitud.value,
                longitud = formRegistroVM.longitud.value,
                salirMapa = {
                    appVM.cambiarPantallaLista()
                }
            )
        }
        else -> {
            Log.v("AppUI()", "NO DEBERIA ESTAR AQUI")
        }
    }

}

//FIN APPUI

//PARA USO DE CAMARA

fun generarNombreSegunFechaHastaSegundo():String = LocalDateTime
    .now().toString().replace(Regex("[T:.-]"), "").substring(0,14)

fun crearArchivoImagenPrivado(contexto: Context): File = File(
    contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    "${generarNombreSegunFechaHastaSegundo()}.jpg"
)

fun uri2imageBitmap(uri:Uri, contexto: Context) = BitmapFactory.decodeStream(
    contexto.contentResolver.openInputStream(uri)
).asImageBitmap()

fun capturarFotografia(
    cameraController: LifecycleCameraController,
    archivo: File,
    contexto: Context,
    onImagenGuardada: (uri: Uri) -> Unit)
{
    val opciones = OutputFileOptions.Builder(archivo).build()
    cameraController.takePicture(
        opciones,
        ContextCompat.getMainExecutor(contexto),
        object: OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.also {// also era let
                    Log.v("tomarFotografia()::onImageSaved", "Foto guardada en ${it.toString()}")
                    guardarImagenMediaStore(contexto, it)
                    onImagenGuardada(it)
                }
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("capturarFotografia::OnImageSavedCallback::onError", exception.message?:"Error")
            }

        }
    )
}

private fun guardarImagenMediaStore(context: Context, imageUri: Uri) {
    try {
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Imagen_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.DATA, imageUri.path)
        }

        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val insertedUri = contentResolver.insert(contentUri, contentValues)

        Log.v("guardarImagenMediaStore", "Imagen guardada en MediaStore: $insertedUri")
    } catch (e: Exception) {
        Log.e("guardarImagenMediaStore", "Error al guardar la imagen en MediaStore: ${e.message}")
    }
}

class SinPermisoException(mensaje:String) : Exception(mensaje)

@Composable
fun PantallaCamaraUI(formRegistroVM: FormRegistroVM, appVM: AppVM, cameraController: LifecycleCameraController){

    val contexto = LocalContext.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PreviewView(it).apply {
                controller = cameraController
            }
        }
    )
    Button(onClick = {
        capturarFotografia(
            cameraController,
            crearArchivoImagenPrivado(contexto),
            contexto
        ) {
            //formRegistroVM.foto.value = it

            appVM.cambiarPantallaLista()
        }
    }) {
        Text ("Captura Foto")
    }
}

//FIN DE CAMARA

// PANTALLA DE LISTA


@Composable
fun Fila(lugar: Lugar, onClick: (Lugar) -> Unit = {}, onDelete:(Lugar) -> Unit = {} ){
    val alcanceCorrutina = rememberCoroutineScope()
    val contexto = LocalContext.current

    Column (
        modifier = Modifier.padding(10.dp)
    ){
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(lugar) }
        ){
            Text(lugar.imagen)
            Spacer(modifier = Modifier.width(20.dp))
            Column{
                Text(lugar.nombre)
                Text(lugar.costoAlojamiento.toString())
                Text(lugar.costoTransporte.toString())
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row {
            Icon(
                Icons.Filled.LocationOn ,
                contentDescription = "Ubicacion"
            )
            Icon(
                Icons.Filled.Edit ,
                contentDescription = "Editar lugar"
            )
            Icon(
                Icons.Filled.Delete ,
                contentDescription = "Eliminar lugar",
                modifier = Modifier.clickable {
                    alcanceCorrutina.launch( Dispatchers.IO ){
                        val dbHelper = DBHelper(contexto)
                        val dao = LugarDAO(dbHelper)
                        dao.borrar(lugar)
                        onDelete(lugar)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun DetalleUI(lugar:Lugar, onDelete:(Lugar) -> Unit = {}) {
    val alcanceCorrutina = rememberCoroutineScope()
    val contexto = LocalContext.current
    val appVM: AppVM = viewModel()
    val datosLugar: FormRegistroVM = viewModel()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp)
    ) {
        Text(lugar.nombre)
        Text(lugar.imagen)
        Text("Costo por noche")
        Text(lugar.costoAlojamiento.toString())
        Text("Traslados")
        Text(lugar.costoTransporte.toString())
        Text("Comentarios:")
        Text(lugar.comentarios)

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = "Tomar foto del lugar",
                modifier = Modifier.clickable {
                    appVM.cambiarPantallaFoto()
                    appVM.lanzadorPermisos?.launch(arrayOf(Manifest.permission.CAMERA))
                }
            )
            Spacer(modifier = Modifier.width(20.dp))
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Editar lugar"
            )
            Spacer(modifier = Modifier.width(20.dp))
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar lugar",
                modifier = Modifier.clickable {
                    alcanceCorrutina.launch(Dispatchers.IO) {
                        val dbHelper = DBHelper(contexto)
                        val dao = LugarDAO(dbHelper)
                        dao.borrar(lugar)
                        onDelete(lugar)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                datosLugar.latitud.value = lugar.latitude
                datosLugar.longitud.value = lugar.longitude
                appVM.cambiarPantallaMapa()
                      },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Ver en Mapa")
        }
        //MapaOsmUI(lugar.latitude,lugar.longitude, modifier = Modifier.size(200.dp,200.dp))
        //MapaFullScreenUI(lugar.latitude,lugar.longitude)

    }
}

@Composable
fun PantallaInicioListaUI(appVM: AppVM){
    val contexto = LocalContext.current
    val (lugares, setLugares) = remember { mutableStateOf(emptyList<Lugar>())}
    val (lugarSeleccionado,setLugarSeleccionado) = remember { mutableStateOf<Lugar?>(null) }
    val appVM:AppVM = viewModel()

    LaunchedEffect(lugares){ //tareas era Unit
        withContext( Dispatchers.IO ) {
            val dbHelper = DBHelper(contexto)
            val dao = LugarDAO(dbHelper)
            setLugares(dao.findAll())
        }
    }
    Column (
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally

    ){
        //Aqui va la lista
        if (lugarSeleccionado == null){
            LazyColumn() {
                items(lugares) { lugar ->
                    Fila(lugar,
                        onClick = {
                            setLugarSeleccionado(it)
                            //setLugares(emptyList<Lugar>())
                        },
                        onDelete = {
                            setLugares(emptyList<Lugar>())
                        })
                }
            }
            Button(onClick = {
                appVM.cambiarPantallaForm()
            }) {
                Text("Agregar Lugar")
            }
        } else {
            DetalleUI(lugarSeleccionado){
                setLugarSeleccionado(null)
                setLugares(emptyList<Lugar>())
            }
            Button(onClick = {
                val intent = Intent(contexto, MainActivity::class.java)
                contexto.startActivity(intent)
            }) {
                Text("Volver a Lista")
            }
        }
        //Fin de la lista
    }
}

//FIN PANTALLA LISTA

//FORMULARIO
@Composable
fun LugarFormUI(appVM: AppVM, onSave:(Lugar) -> Unit ={}){

    var nombre by remember{ mutableStateOf("")}
    var imagen by remember{ mutableStateOf("")}
    var latitude by remember{ mutableStateOf(0.0)}
    var longitude by remember{ mutableStateOf(0.0)}
    var orden by remember{ mutableStateOf(0)}
    var costoAlojamiento by remember{ mutableStateOf(0.0)}
    var costoTransporte by remember{ mutableStateOf(0.0)}
    var comentarios by remember{ mutableStateOf("")}
    val contexto = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()

    val mapaTextos = mapOf(
        "textoNombre" to contexto.resources.getString(R.string.nombreLugar),
        "imgRef" to contexto.resources.getString(R.string.imgRef),
        "latitud" to contexto.resources.getString(R.string.latitud),
        "longitud" to contexto.resources.getString(R.string.longitud),
        "orden" to contexto.resources.getString(R.string.orden),
        "costoAlojamiento" to contexto.resources.getString(R.string.costoAlojamiento),
        "costoTransporte" to contexto.resources.getString(R.string.costoTransporte),
        "comentarios" to contexto.resources.getString(R.string.comentarios)
    )

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        // Nombre

        TextField(
            value = nombre,
            //value = place?.nombre ?: "",
            onValueChange = {
                nombre = it
                //place?.nombre = it
            },
            label = { mapaTextos["textoNombre"]?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) {
                        keyboardController?.hide()
                    }
                }
        )

        // Imagen de referencia
        TextField(
            value = imagen,
            //value = place?.imagen ?: "",
            onValueChange = {
                //place?.name = it
                imagen = it
            },
            label = { mapaTextos["imgRef"]?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) {
                        keyboardController?.hide()
                    }
                }
        )

        // Latitud y longitud
        TextField(
            value = latitude.toString(),
            //value = place?.latitude.toString() ?: "",
            onValueChange = {
                //place?.latitude = it.toDoubleOrNull()
                latitude = it.toDoubleOrNull() ?: 0.0
            },
            label = { mapaTextos["latitud"]?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) {
                        keyboardController?.hide()
                    }
                }
        )

        TextField(
            value =longitude.toString(),
            //value = place?.longitude.toString() ?: "",
            onValueChange = {
                //place?.longitude = it.toDoubleOrNull()
                longitude = it.toDoubleOrNull() ?: 0.0
            },
            label = { mapaTextos["longitud"]?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) {
                        keyboardController?.hide()
                    }
                }
        )

        // Orden
        TextField(
            value =orden.toString(),
            //value = place?.order.toString() ?: "",
            onValueChange = {
                //place?.order = it.toIntOrNull()
                orden = it.toIntOrNull() ?: 0
            },
            label = { mapaTextos["orden"]?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) {
                        keyboardController?.hide()
                    }
                }
        )

        // Costo de alojamiento
        TextField(
            value =costoAlojamiento.toString(),
            //value = place?.accommodationCost.toString() ?: "",
            onValueChange = {
                //place?.accommodationCost = it.toDoubleOrNull()
                costoAlojamiento = it.toDoubleOrNull() ?: 0.0
            },
            label = { mapaTextos["costoAlojamiento"]?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) {
                        keyboardController?.hide()
                    }
                }
        )

        // Costo de transporte
        TextField(
            value=costoTransporte.toString(),
            //value = place?.transportationCost.toString() ?: "",
            onValueChange = {
                //place?.transportationCost = it.toDoubleOrNull()
                costoTransporte = it.toDoubleOrNull() ?: 0.0
            },
            label = { mapaTextos["costoTransporte"]?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) {
                        keyboardController?.hide()
                    }
                }
        )

        // Comentarios
        TextField(
            value=comentarios,
            //value = place?.comments ?: "",
            onValueChange = {
                comentarios = it
                //place?.comments = it
            },
            label = { mapaTextos["comentarios"]?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (!it.isFocused) {
                        keyboardController?.hide()
                    }
                }
        )

        // Botón de guardar
        Button(
            onClick = {
                val lugar = Lugar(0,nombre,imagen,latitude,longitude,orden, costoAlojamiento,costoTransporte,comentarios)
                //onSave(place ?: Place())
                alcanceCorrutina.launch( Dispatchers.IO ){
                    val dbHelper = DBHelper(contexto)
                    val dao = LugarDAO(dbHelper)
                    dao.insertar(lugar)

                    onSave(lugar)
                    appVM.cambiarPantallaLista()

                }
            },
        ) {
            Text("Guardar")
        }
        Button(
            onClick = {
                //onSave(place ?: Place())
                appVM.cambiarPantallaLista()
            },
        ) {
            Text("Cancelar")
        }
    }


}

//VISTA DE MAPA
@Composable
fun MapaFullScreenUI(latitud: Double, longitud: Double, salirMapa: () -> Unit) {

    MapaOsmUI(latitud = latitud, longitud = longitud)

    Button(
        onClick = { salirMapa() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Salir del Mapa")
    }
}

@Composable
fun MapaOsmUI(latitud:Double, longitud:Double) {
    val contexto = LocalContext.current

    AndroidView(
        factory = {
            org.osmdroid.views.MapView(it).also {
                it.setTileSource(TileSourceFactory.MAPNIK)
                org.osmdroid.config.Configuration.getInstance().userAgentValue = contexto.packageName
            }
        },
        update = {
            it.overlays.removeIf{true}
            it.invalidate()
            it.controller.setZoom(18.0)
            val geoPoint = GeoPoint(latitud, longitud)
            it.controller.animateTo(geoPoint)

            val marcador = Marker(it)
            marcador.position = geoPoint
            marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            it.overlays.add(marcador)
        }
    )

}