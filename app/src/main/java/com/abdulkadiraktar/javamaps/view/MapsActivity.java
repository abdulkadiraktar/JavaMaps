package com.abdulkadiraktar.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.abdulkadiraktar.javamaps.R;
import com.abdulkadiraktar.javamaps.databinding.ActivityMapsBinding;
import com.abdulkadiraktar.javamaps.model.Place;
import com.abdulkadiraktar.javamaps.room.PlaceDao;
import com.abdulkadiraktar.javamaps.room.PlaceDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ActivityResultLauncher<String> permissionLauncher;
    LocationManager locationManager;
    LocationListener locationListener;
    PlaceDatabase db;
    PlaceDao placeDao;
    Double selectedLatitude;
    Double selectedLongitude;
    Place selectedPlace;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        registerLauncher();
        db = Room.databaseBuilder(getApplicationContext(), PlaceDatabase.class, "Place").build();
        //db = Room.databaseBuilder(getApplicationContext(), PlaceDatabase.class,"Place").allowMainThreadQueries().build();
        placeDao = db.placeDao();
        selectedLatitude = 0.0;
        selectedLongitude = 0.0;
        binding.saveButton.setEnabled(false);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        Intent intent = getIntent(); //eğer yeniyse delete kaldır eskiyse save etkisizleştir
        String intentInfo = intent.getStringExtra("info");

        if (intentInfo.equals("new")) {

            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE); //TAMAMEN YOK ETME
            //casting
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    SharedPreferences sharedPreferences = MapsActivity.this.getSharedPreferences("com.abdulkadiraktar.javamaps", MODE_PRIVATE);
                    boolean info = sharedPreferences.getBoolean("info", false);
                    if (!info) {
                        LatLng lastUserLocation = new LatLng(location.getLatitude(), location.getLongitude()); //son bilinen konum
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                        sharedPreferences.edit().putBoolean("info", true).apply();
                    }

                    //LatLng userLocation = new LatLng(location.getLatitude() , location.getLongitude());
                    //  mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15)); // konum değiştikçe yeni konuma zoomlama
                    //System.out.println("Location: " + location.toString()); //konum değiştikçe yazdırıyor !!!!!!!!
                    compositeDisposable.addAll()
                }
            };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Snackbar.make(binding.getRoot(), "izin gerekiyor", Snackbar.LENGTH_INDEFINITE).setAction("izin ver", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //izin iste
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                        }
                    }).show();
                } else {
                    // izin iste
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            } else {
                //izinli
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);//son bilinen konum
                if (lastLocation != null) {

                    LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()); //son bilinen konum
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));

                }
                mMap.setMyLocationEnabled(true);
            }
        }

       /* // enlem boylam lat long konumumuz için
        LatLng galata = new LatLng(41.0252452, 28.9731415);
       //marker= haritada ki imleç
        mMap.addMarker(new MarkerOptions().position(galata).title("Galata Tower"));
        //kameranın başlatıldığı yer
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(galata,15));
        */


        else
    {
        //ESKİDEN KAYDEDİLMİŞ MARKERI GÖRÜNTÜLEME
        mMap.clear();

        selectedPlace = (Place) intent.getSerializableExtra("place");

        LatLng latLng = new LatLng(selectedPlace.latitude,selectedPlace.longitude);
        mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
        binding.placeNameText.setText(selectedPlace.name);
        binding.saveButton.setVisibility(View.GONE);
        binding.deleteButton.setVisibility(View.VISIBLE);

    }


}


    private void registerLauncher() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    //izin verildi
                    if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);//son bilinen konum
                        if (lastLocation != null) {
                            LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()); //son bilinen konum
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                        } else {
                            //izin reddedildi
                            Toast.makeText(MapsActivity.this, "izin gerekiyor", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        mMap.clear();
    mMap.addMarker(new MarkerOptions().position(latLng));
    selectedLatitude = latLng.latitude;
    selectedLongitude = latLng.longitude;
    binding.saveButton.setEnabled(true); //seçtikten sonra kullanılabilir !!!!!!!!!!!!!
    }
    public void save (View view){

        Place place = new Place(binding.placeNameText.getText().toString(),selectedLatitude,selectedLongitude);
//threading -> MAİN (UI) , Default (CPU Intensive yoğun işlemler, IO ( network, database)

       //işlemi nerede yapacağın subscribeon
        //placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();

        // Disposable kullan at çöp torbalı
        compositeDisposable.add(placeDao.insert(place).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::yeniaktivite));
        //ioda yap main threade göster

    }
    private void yeniaktivite(){
        Intent intent = new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    public void delete (View view) {
        if (selectedPlace != null) {
            compositeDisposable.add(placeDao.delete(selectedPlace).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(MapsActivity.this :: yeniaktivite));

        }
    }

    @Override //UYGULAMA YOK EDİLDİĞİNDE
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}

