package com.example.ex02_mapssdk_gustavoantunes

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ex02_mapssdk_gustavoantunes.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.ex02_mapssdk_gustavoantunes.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.*
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var locationListener: LocationListener? = null // Responsável por "rastrear" eventos relacionados a geolocalização
    private var locationManager: LocationManager? = null // Reponsável por gerenciar / configurar o rastreamento da geolocalização;
    private var usermaker : Marker? = null;
    private var isMapReady: Boolean = false;
    private var polyline: Polyline? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)

        binding.btnAdd.setOnClickListener{
            if(binding.editDestino.text.toString() != "" && isMapReady){
                var geoloc : LatLng? = geocoding(binding.editDestino.text.toString())

                if(geoloc != null){
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(geoloc, 19.0f))
                    mMap.addMarker(
                        MarkerOptions().position(geoloc)
                            .title("Ponto de Drone")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.drone)))

                    setLinha(usermaker!!.position, geoloc);
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Local não encontrado",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Digite o nome do local",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.btnClear.setOnClickListener{
            mMap.clear();
            usermaker = mMap.addMarker(
                MarkerOptions()
                    .position(usermaker!!.position)
                    .title("Minha Localização")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.home))
            )
        }

        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 1) Definir tipo do mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID)

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val usrPosition = LatLng(location.latitude, location.longitude)

                if (usermaker != null) {
                    usermaker!!.remove()
                } // Remover marcadores anteriores

                usermaker = mMap.addMarker(
                    MarkerOptions()
                        .position(usrPosition)
                        .title("Minha Localização")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.home))
                )

                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        usrPosition,
                        19.0f // Zoom de 2 até 21
                    )
                )
            }
        }

        isMapReady = true
        checkPermission()
        setupLocation()

        mMap.setOnMapClickListener { latLang ->
            val lati = latLang.latitude
            val longi = latLang.longitude

            mMap.addMarker(
                MarkerOptions().position(latLang)
                    .title("Heliponto")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.heliponto))
            )

            mMap.addCircle(
                CircleOptions()
                    .center(latLang)
                    .radius(2000.0)
                    .strokeWidth(5.0f)
                    .strokeColor(Color.WHITE)
                    .fillColor(Color.argb(110, 100, 200, 200))
            )
        }
    }

    fun setLinha(startPoint: LatLng, endPoint: LatLng){
        val polylineOptions = PolylineOptions()
        polylineOptions.add(startPoint)
        polylineOptions.add(endPoint)

        val results = FloatArray(1)
        Location.distanceBetween(startPoint.latitude, startPoint.longitude, endPoint.latitude, endPoint.longitude, results)

        if (results[0] <= 500.0f){
            polylineOptions?.color(Color.GREEN).width(20.0f)
        } else{
            polylineOptions?.color(Color.RED).width(20.0f)
        }

        polyline = mMap.addPolyline(polylineOptions)
    }

    //Geocoding (Geocodificação): Transformação de endereço ou nome do local em coordenadas (latitude e longitude)
    fun geocoding(descricaoLocal: String): LatLng? {
        val geocoder = Geocoder(applicationContext, Locale.getDefault()) //Locale representa uma região específica.
        try {
            val local = geocoder.getFromLocationName(descricaoLocal, 1)
            if (local != null && local.size > 0) {
                var destino = LatLng(local[0].latitude, local[0].longitude)
                return destino
            }
        } catch (e: IOException) {
            e.message
        }
        return null
    }

    // Reverse Geocoding (geocodificação reversa): Transformação de coordenadas (latitude e longitude) em endereço ou descrição do local.
    fun reverseGeocodiing(latLng: LatLng): String? {
        val geocoder = Geocoder(applicationContext, Locale.getDefault()) //Locale representa uma região específica.
        try {
            val local = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            //val local = geocoder.getFromLocationName(descricaoLocal, 1)
            if (local != null && local.size > 0) {
                return local[0].getAddressLine(0).toString()
            }
        } catch (e: IOException) {
            e.message
        }
        return null
    }

    // 1 ) Validar permissões em tempo de execução (necessário para API 23 ou superior)
    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Permissões Ativadas", Toast.LENGTH_SHORT).show()
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
            ) {
                /* Em uma IU educacional, explique ao usuário por que seu aplicativo requer esta permissão para um recurso específico
                se comportar conforme o esperado. Nesta IU, inclua um botão "cancelar" ou "não, obrigado" que permite ao usuário continue usando
                 seu aplicativo sem conceder a permissão. */
                alertaPermissaoNegada()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                //Pedir permissão diretamente. O ActivityResultCallback registrado obtém o resultado desta solicitação (abaixo).
            }
        }
    }

    // 2) Calback que exibe a janela de solicitação de permissão
    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupLocation() // Método acoplado para setar a localização
        }
    }
    // 3) Exibir IU educacional (recomendação do Google)
    fun alertaPermissaoNegada() {
        val alert = AlertDialog.Builder(this)
        alert.setTitle("Permissões Requeridas")
        alert.setMessage("Para continuar utilizando todos os recursos do aplicativo, é altamente recomendado autorizar o acesso a sua localização.")
        alert.setCancelable(false)
        alert.setPositiveButton(
            "Corrigir"
        ) { dialog, which ->
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        alert.setNegativeButton(
            "Cancelar"
        ) { dialog, which ->
            Toast.makeText(getApplicationContext(), "Algumas das funcionalidades do app foram desabilitadas.", Toast.LENGTH_LONG).show();
            // Nunca osar o comendo finish(). Fechar o app é uma prática pouco recomendada.
        }
        val alertDialog = alert.create()
        alertDialog.show()
    }
    // 4) Configurar a Geolocalização.
    fun setupLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            /* requestLocationUpdates(String provider, long minTimeMs, float minDistanceM, LocationListener listener)
               1) provider - String: um provedor listado por getAllProviders() Este valor não pode ser null.(LocationManager.GPS_PROVIDER neste caso)
               2) miTimeMs - long: Intervalo mínimo de tempo entre as atualizações de localização em milissegundos(1000 ms neste caso)
               3) minDistanceM: float: distância mínima entre atualizações de localização em metros
               4) listener: LocationListener: o ouvinte que receberá atualizações de localização Este valor não pode ser null.
               https://developer.android.com/reference/android/location/LocationManager#requestLocationUpdates(java.lang.String,%20long,%20float,%20android.location.LocationListener)*/
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000, 10f, locationListener!!
            )
        }
    }
}