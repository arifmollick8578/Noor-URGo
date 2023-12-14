package com.noor.essex.urgo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date

class FormSellActivity : AppCompatActivity() {
    private val REQUEST_CATEGORIA = 100
    private var imagem0: ImageView? = null
    private var imagem1: ImageView? = null
    private var imagem2: ImageView? = null
    private var btn_categoria: Button? = null
    private var edt_titulo: EditText? = null
    private var edt_descricao: EditText? = null

    //    private var edt_valor: CurrencyEditText? = null
//    private var edt_cep: MaskEditText? = null
    private var edt_valor: EditText? = null
    private var edt_cep: EditText? = null
    private var progressBar: ProgressBar? = null
    private var txt_local: TextView? = null
    private var text_toolbar: TextView? = null
    private var btn_salvar: Button? = null
    private var categoriaSelecionada = ""
    private var enderecoUsuario: Address? = null
    private var local: Local? = null
    private var retrofit: Retrofit? = null
    private var currentPhotoPath: String? = null
    private val imagemUploadList: MutableList<ImagemUpload> = ArrayList<ImagemUpload>()
    private var anuncio: Anuncio? = null
    private var novoAnuncio = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_sell)
        iniciaCompnentes()
        val bundle = intent.extras
        if (bundle != null) {
            anuncio = bundle.getSerializable("anuncioSelecionado") as Anuncio?
            configDados()
        }
        iniciaRetrofit()
        recoverAddress()
        configCliques()
    }

    private fun configDados() {
        text_toolbar!!.text = "Editing ad"
        categoriaSelecionada = anuncio!!.categoria ?: ""
        btn_categoria!!.text = categoriaSelecionada
        edt_titulo?.setText(anuncio?.titulo)
//        edt_valor.setText(GetMask.getValor(anuncio.getValor()))
        edt_valor?.setText(anuncio?.valor?.toString())
        edt_descricao?.setText(anuncio?.descricao)
        Picasso.get().load(anuncio?.urlImagens?.get(0)?.caminhoImagem).into(imagem0)
        Picasso.get().load(anuncio?.urlImagens?.get(1)?.caminhoImagem).into(imagem1)
        Picasso.get().load(anuncio?.urlImagens?.get(2)?.caminhoImagem).into(imagem2)
//        Picasso.get().load(anuncio.getUrlImagens().get(2).getCaminhoImagem()).into(imagem2)
        novoAnuncio = false
    }

    fun validarDados(view: View?) {
        val titulo = edt_titulo!!.text.toString()
//        val valor = edt_valor.getRawValue() as Double / 100
        val valor = (edt_valor?.text.toString().toDouble()) / 100
        val descricao = edt_descricao!!.text.toString()
        if (titulo.isNotEmpty()) {
            if (valor > 0) {
                if (categoriaSelecionada.isNotEmpty() || true) {
                    if (!descricao.isEmpty()) {
                        if (true || local != null) {
                            if (true || local!!.localidade != null) {
                                if (anuncio == null) anuncio = Anuncio()
                                anuncio!!.id = FirebaseHelper.authId
                                anuncio!!.titulo = titulo
                                anuncio!!.valor = valor
                                anuncio!!.categoria = categoriaSelecionada
                                anuncio!!.descricao = descricao
                                anuncio!!.local = local
                                if (novoAnuncio) { // Novo Anúncio
                                    if (imagemUploadList.size == 3) {
                                        println("FATAL:: imagelist: $imagemUploadList")
                                        for (i in imagemUploadList.indices) {
                                            salvarImagemFirebase(imagemUploadList[i], i)
                                        }
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Selecione 3 imagens para o anúncio.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else { // Edição
                                    if (imagemUploadList.size > 0) {
                                        for (i in imagemUploadList.indices) {
                                            salvarImagemFirebase(imagemUploadList[i], i)
                                        }
                                    } else {
                                        btn_salvar!!.text = "Salvando..."
                                        anuncio!!.salvar(this, false)
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Digite um CEP válido.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            Toast.makeText(this, "Digite um CEP válido.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        edt_descricao!!.requestFocus()
                        edt_descricao!!.error = "Informe a descrição."
                    }
                } else {
                    Toast.makeText(this, "Selecione uma categoria.", Toast.LENGTH_SHORT).show()
                }
            } else {
                edt_valor!!.requestFocus()
                edt_valor!!.error = "Informe um valor válido."
            }
        } else {
            edt_titulo!!.requestFocus()
            edt_titulo!!.error = "Informe o título."
        }
    }

    private fun salvarImagemFirebase(imagemUpload: ImagemUpload, index: Int) {
        btn_salvar!!.text = "Salvando..."
        val storageReference: StorageReference = FirebaseHelper.storageReference ?: return
        println("FATAL:: save is calling: $storageReference")
        storageReference
            .child("imagens")
            .child("anuncios")
            .child(anuncio?.id!!)
            .child("imagem$index.jpeg")
        println("FATAL:: save image: $imagemUpload")
        val uploadTask = storageReference.putFile(Uri.parse(imagemUpload.caminhoImagem))
        uploadTask.addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot? ->
            storageReference.downloadUrl.addOnCompleteListener { task: Task<Uri> ->
                imagemUpload.caminhoImagem = task.result.toString()
                if (novoAnuncio) {
                    anuncio?.urlImagens?.add(imagemUpload)
                } else {
                    anuncio?.urlImagens?.set(index, imagemUpload)
                }
                if (imagemUploadList.size == index + 1) {
                    anuncio?.salvar(this, novoAnuncio)
                }
            }
        }.addOnFailureListener { e: Exception ->
            println("FATAL:: exception: $e")
            Toast.makeText(
                this,
                e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun configCliques() {
        findViewById<View>(R.id.back_arrow).setOnClickListener { v: View? -> finish() }
        imagem0!!.setOnClickListener { v: View? ->
            showBottomDialog(
                0
            )
        }
        imagem1!!.setOnClickListener { v: View? ->
            showBottomDialog(
                1
            )
        }
        imagem2!!.setOnClickListener { v: View? ->
            showBottomDialog(
                2
            )
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        return image
    }

    private fun dispatchTakePictureIntent(requestCode: Int) {
        var request = 0
        when (requestCode) {
            0 -> request = 3
            1 -> request = 4
            2 -> request = 5
        }
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create the File where the photo should go
        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (ex: IOException) {
            // Error occurred while creating the File
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            val photoURI = FileProvider.getUriForFile(
                this,
                "com.noor.essex.urgo.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(takePictureIntent, request)
        }
    }

    fun selecionarCategoria(view: View?) {
//        val intent = Intent(this, CategoriasActivity::class.java)
//        startActivityForResult(intent, REQUEST_CATEGORIA)
    }

    private fun recoverAddress() {
        configCep()
        val authId = FirebaseHelper.authId
        if (authId == null) return
        val enderecoRef: DatabaseReference = FirebaseHelper.databaseReference ?: return

        enderecoRef
            .child("enderecos")
            .child(authId)
        enderecoRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    enderecoUsuario = snapshot.getValue(Address::class.java)
                    if (enderecoUsuario != null) edt_cep?.setText(enderecoUsuario!!.zipcode)
                    progressBar!!.visibility = View.GONE
                } else {
                    Toast.makeText(baseContext, "We need your address.", Toast.LENGTH_SHORT)
                        .show()
//                    startActivity(Intent(baseContext, EnderecoActivity::class.java))
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showBottomDialog(requestCode: Int) {
        val modalbottomsheet: View = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        bottomSheetDialog.setContentView(modalbottomsheet)
        bottomSheetDialog.show()
        modalbottomsheet.findViewById<View>(R.id.btn_camera).setOnClickListener { v: View? ->
            bottomSheetDialog.dismiss()
            verificaPermissaoCamera(requestCode)
        }
        modalbottomsheet.findViewById<View>(R.id.btn_galeria).setOnClickListener { v: View? ->
            bottomSheetDialog.dismiss()
            verificaPermissaoGaleria(requestCode)
        }
        modalbottomsheet.findViewById<View>(R.id.btn_close).setOnClickListener { v: View? ->
            bottomSheetDialog.dismiss()
            Toast.makeText(this, "Fechando", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificaPermissaoCamera(requestCode: Int) {
        val permissionListener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                dispatchTakePictureIntent(requestCode)
            }

            override fun onPermissionDenied(deniedPermissions: List<String?>?) {
                Toast.makeText(this@FormSellActivity, "Permissão Negada.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        showDialogPermissao(
            permissionListener, arrayOf(Manifest.permission.CAMERA),
            "Se você não aceitar a permissão não poderá acessar a Câmera do dispositivo, deseja ativar a permissão agora ?"
        )
    }

    private fun configUpload(requestCode: Int, caminhoImagem: String) {
        var request = 0
        when (requestCode) {
            0, 3 -> request = 0
            1, 4 -> request = 1
            2, 5 -> request = 2
        }
        val imagemUpload = ImagemUpload(caminhoImagem, request)
        if (imagemUploadList.size > 0) {
            var encontrou = false
            for (i in imagemUploadList.indices) {
                if (imagemUploadList[i].index === request) {
                    encontrou = true
                }
            }
            if (encontrou) {
                imagemUploadList[request] = imagemUpload
            } else {
                imagemUploadList.add(imagemUpload)
            }
        } else {
            imagemUploadList.add(imagemUpload)
        }
    }

    private fun verificaPermissaoGaleria(requestCode: Int) {
        val permissionListener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                abrirGaleria(requestCode)
            }

            override fun onPermissionDenied(deniedPermissions: List<String?>?) {
                Toast.makeText(this@FormSellActivity, "Permissão Negada.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        showDialogPermissao(
            permissionListener, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            "Se você não aceitar a permissão não poderá acessar a Galeria do dispositivo, deseja ativar a permissão agora ?"
        )
    }

    private fun abrirGaleria(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, requestCode)
    }

    private fun showDialogPermissao(
        permissionListener: PermissionListener,
        permissoes: Array<String>,
        msg: String
    ) {
//        TedPermission.create()
//            .setPermissionListener(permissionListener)
//            .setDeniedTitle("Permissão negada")
//            .setDeniedMessage(msg)
//            .setDeniedCloseButtonText("Não")
//            .setGotoSettingButtonText("Sim")
//            .setPermissions(permissoes)
//            .check()

        permissoes.forEach {
            TedPermission.create()
                .setPermissionListener(permissionListener)
                .setDeniedTitle("Permissão negada")
                .setDeniedMessage(msg)
                .setDeniedCloseButtonText("Não")
                .setGotoSettingButtonText("Sim")
                .setPermissions(it)
                .check()
        }


    }

    private fun configCep() {
        edt_cep!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val cep = s.toString().replace("_".toRegex(), "").replace("-", "")
                if (cep.length == 8) {
                    buscarEndereco(cep)
                } else {
                    local = null
                    configEndereco()
                    progressBar!!.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun buscarEndereco(cep: String) {
        progressBar!!.visibility = View.VISIBLE
        val cepService: CEPService = retrofit!!.create(CEPService::class.java)
        val call: Call<Local> = cepService.recuperarCEP(cep) ?: return
        call.enqueue(object : Callback<Local?> {
            override fun onResponse(call: Call<Local?>?, response: Response<Local?>) {
                if (response.isSuccessful) {
                    local = response.body()
                    println("FATAL:: local in response: $local")
                    if (local!!.localidade == null) {
                        Toast.makeText(
                            this@FormSellActivity,
                            "CEP inválido.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@FormSellActivity,
                        "Tente novamente mais tarde.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                configEndereco()
            }

            override fun onFailure(call: Call<Local?>?, t: Throwable?) {
                Toast.makeText(
                    this@FormSellActivity,
                    "Tente novamente mais tarde.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun configEndereco() {
        if (local != null) {
            if (local!!.localidade != null) {
                val endereco: String =
                    (local!!.localidade + ", " + local!!.bairro).toString() + " - DDD " + local!!.ddd
                txt_local!!.text = endereco
            } else {
                txt_local!!.text = ""
            }
        } else {
            txt_local!!.text = ""
        }
        progressBar!!.visibility = View.GONE
    }

    // Então esse é um dos possível erros que a gente pode ter
    private fun iniciaRetrofit() {
        retrofit = Retrofit.Builder()
            .baseUrl("https://viacep.com.br/ws/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun iniciaCompnentes() {
        text_toolbar = findViewById(R.id.text_toolbar)
        text_toolbar?.text = "Novo anúncio"
        imagem0 = findViewById(R.id.imagem0)
        imagem1 = findViewById(R.id.imagem1)
        imagem2 = findViewById(R.id.imagem2)
        edt_titulo = findViewById(R.id.edt_titulo)
        edt_descricao = findViewById(R.id.edt_descricao)
        edt_valor = findViewById(R.id.edt_valor)
//        edt_valor.setLocale(Locale("PT", "br"))
        btn_categoria = findViewById(R.id.btn_categoria)
        edt_cep = findViewById(R.id.edt_cep)
        progressBar = findViewById(R.id.progressBar)
        txt_local = findViewById(R.id.txt_local)
        btn_salvar = findViewById(R.id.btn_salvar)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val bitmap0: Bitmap
            val bitmap1: Bitmap
            val bitmap2: Bitmap
            val imagemSelecionada = data!!.data
            val caminhaImagem: String
            if (requestCode == REQUEST_CATEGORIA) {
//                val categoria: Categoria? =
//                    data.getSerializableExtra("categoriaSelecionada") as Categoria?
//                categoriaSelecionada = categoria.getNome()
                btn_categoria!!.text = categoriaSelecionada
            } else if (requestCode <= 2) { // Galeria
                try {
                    caminhaImagem = imagemSelecionada.toString()
                    when (requestCode) {
                        0 -> {
                            bitmap0 = if (Build.VERSION.SDK_INT < 28) {
                                MediaStore.Images.Media.getBitmap(
                                    contentResolver,
                                    imagemSelecionada
                                )
                            } else {
                                val source = ImageDecoder.createSource(
                                    contentResolver,
                                    imagemSelecionada!!
                                )
                                ImageDecoder.decodeBitmap(source)
                            }
                            imagem0!!.setImageBitmap(bitmap0)
                        }

                        1 -> {
                            bitmap1 = if (Build.VERSION.SDK_INT < 28) {
                                MediaStore.Images.Media.getBitmap(
                                    contentResolver,
                                    imagemSelecionada
                                )
                            } else {
                                val source = ImageDecoder.createSource(
                                    contentResolver,
                                    imagemSelecionada!!
                                )
                                ImageDecoder.decodeBitmap(source)
                            }
                            imagem1!!.setImageBitmap(bitmap1)
                        }

                        2 -> {
                            bitmap2 = if (Build.VERSION.SDK_INT < 28) {
                                MediaStore.Images.Media.getBitmap(
                                    contentResolver,
                                    imagemSelecionada
                                )
                            } else {
                                val source = ImageDecoder.createSource(
                                    contentResolver,
                                    imagemSelecionada!!
                                )
                                ImageDecoder.decodeBitmap(source)
                            }
                            imagem2!!.setImageBitmap(bitmap2)
                        }
                    }
                    configUpload(requestCode, caminhaImagem)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else { // Camera
                val file = File(currentPhotoPath)
                caminhaImagem = file.toURI().toString()
                when (requestCode) {
                    3 -> imagem0!!.setImageURI(Uri.fromFile(file))
                    4 -> imagem1!!.setImageURI(Uri.fromFile(file))
                    5 -> imagem2!!.setImageURI(Uri.fromFile(file))
                }
                configUpload(requestCode, caminhaImagem)
            }
        }
    }
}

data class Address(
    val landmark: String? = null,
    val area: String? = null,
    val country: String? = null,
    val zipcode: String? = null
) {
    fun saveData(idUser: String, context: Context?, progressBar: ProgressBar) {
        val enderecoRef: DatabaseReference = FirebaseHelper.databaseReference ?: return
        enderecoRef
            .child("address")
            .child(idUser)
        enderecoRef.setValue(this).addOnCompleteListener { task: Task<Void?> ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Address save successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, task.exception!!.message, Toast.LENGTH_SHORT).show()
            }
            progressBar.visibility = View.GONE
        }
    }
}

//interface PermissionListener {
//    fun onPermissionGranted()
//    fun onPermissionDenied(deniedPermissions: List<String?>?)
//}


data class Local(
    private var cep: String? = null,
    val bairro: String? = null,
    val localidade: String? = null,
    val uf: String? = null,
    val ddd: String? = null
) : Serializable

data class ImagemUpload(
    var caminhoImagem: String? = null,
    val index: Int = 0
) : Serializable

interface CEPService {
    // o tipo de info que quero recuperar do api https://viacep.com.br/
    @GET("{cep}/json/")
    fun recuperarCEP(@Path("cep") cep: String?): Call<Local>?
}


data class Anuncio(
    var id: String? = null,
    var idUsuario: String? = null,
    var titulo: String? = null,
    var valor: Double = 0.0,
    var descricao: String? = null,
    var categoria: String? = null,
    var local: Local? = null,
    var dataPublicacao: Long = 0,
    var urlImagens: ArrayList<ImagemUpload> = arrayListOf()
) : Serializable {
    fun salvar(activity: Activity, novoAnuncio: Boolean) {
        val anuncioPublicoRef: DatabaseReference = FirebaseHelper.databaseReference ?: return
        anuncioPublicoRef
            .child("anuncios_publicos")
            .child(this.id!!)
        anuncioPublicoRef.setValue(this)
        val meusAnunciosRef: DatabaseReference = FirebaseHelper.databaseReference ?: return
        meusAnunciosRef
            .child("meus_anuncios")
            .child(FirebaseHelper.authId!!)
            .child(this.id!!)
        meusAnunciosRef.setValue(this)
        if (novoAnuncio) {
            val dataAnuncioPublico = anuncioPublicoRef.child("dataPublicacao")
            dataAnuncioPublico.setValue(ServerValue.TIMESTAMP)
            val dataMeusAnuncios = meusAnunciosRef.child("dataPublicacao")
            dataMeusAnuncios.setValue(ServerValue.TIMESTAMP)
                .addOnCompleteListener { task: Task<Void?>? ->
                    activity.finish()
                    val intent = Intent(activity, MainActivity::class.java)
                    intent.putExtra("id", 2) // encaminhar para opcao 2 da bottomShit
                    activity.startActivity(intent)
                }
        } else {
            activity.finish()
        }
    }

    fun remover() {
        val anuncioPublicoRef: DatabaseReference = FirebaseHelper.databaseReference ?: return
        anuncioPublicoRef
            .child("anuncios_publicos")
            .child(this.id!!)
        anuncioPublicoRef.removeValue()
        val meusAnunciosRef: DatabaseReference = FirebaseHelper.databaseReference ?: return
        meusAnunciosRef
            .child("meus_anuncios")
            .child(this.id!!)
        meusAnunciosRef.removeValue()
        for (i in urlImagens.indices) {
            val storageReference: StorageReference = FirebaseHelper.storageReference ?: return
            storageReference
                .child("imagens")
                .child("anuncios")
                .child(id!!)
                .child("imagem$i.jpeg")
            storageReference.delete()
        }
    }
}