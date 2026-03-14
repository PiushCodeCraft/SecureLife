package com.example.womensafteyapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private String userId;

    // ── 1km radius in degrees (approx) ──
    private static final double RADIUS_KM = 1.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // ── Enable my location blue dot ──
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // ── Zoom to current location then load nearby users ──
        mMap.setOnMyLocationChangeListener(location -> {
            LatLng myLocation = new LatLng(
                    location.getLatitude(),
                    location.getLongitude()
            );

            // ── Zoom to my location ──
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f));

            // ── Draw 1km radius circle around me ──
            mMap.addCircle(new CircleOptions()
                    .center(myLocation)
                    .radius(1000) // 1000 meters = 1km
                    .strokeColor(Color.argb(150, 30, 136, 229))
                    .fillColor(Color.argb(30, 30, 136, 229))
                    .strokeWidth(3f));

            // ── Load nearby users ──
            loadNearbyUsers(location.getLatitude(), location.getLongitude());

            // ── Load active SOS alerts ──
            loadSOSAlerts(location.getLatitude(), location.getLongitude());

            // ── Stop listening after first location fix ──
            mMap.setOnMyLocationChangeListener(null);
        });

        // ── Check if opened from SOS notification ──
        String sosLat = getIntent().getStringExtra("sosLat");
        String sosLng = getIntent().getStringExtra("sosLng");
        if (sosLat != null && sosLng != null) {
            double lat = Double.parseDouble(sosLat);
            double lng = Double.parseDouble(sosLng);
            LatLng sosLocation = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions()
                    .position(sosLocation)
                    .title("🚨 SOS Alert!")
                    .snippet("Someone needs help here!")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED)));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sosLocation, 15f));
        }
    }

    // ── Load nearby users from Firestore ──
    private void loadNearbyUsers(double myLat, double myLng) {
        if (db == null) return;

        db.collection("user_locations")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String docUserId = doc.getString("userId");

                        // ── Skip own location ──
                        if (userId != null && userId.equals(docUserId)) continue;

                        Double userLat = doc.getDouble("latitude");
                        Double userLng = doc.getDouble("longitude");

                        if (userLat == null || userLng == null) continue;

                        // ── Check if within 1km ──
                        double distance = calculateDistance(myLat, myLng, userLat, userLng);
                        if (distance <= RADIUS_KM) {
                            // ── Add blue marker for nearby user ──
                            LatLng userPos = new LatLng(userLat, userLng);
                            mMap.addMarker(new MarkerOptions()
                                    .position(userPos)
                                    .title("👤 Nearby User")
                                    .snippet(String.format("%.0fm away", distance * 1000))
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_BLUE)));
                        }
                    }
                });
    }

    // ── Load active SOS alerts ──
    private void loadSOSAlerts(double myLat, double myLng) {
        if (db == null) return;

        db.collection("sos_alerts")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String alertUserId = doc.getString("userId");

                        // ── Skip own SOS ──
                        if (userId != null && userId.equals(alertUserId)) continue;

                        Double sosLat = doc.getDouble("latitude");
                        Double sosLng = doc.getDouble("longitude");

                        if (sosLat == null || sosLng == null) continue;

                        double distance = calculateDistance(myLat, myLng, sosLat, sosLng);
                        if (distance <= RADIUS_KM) {
                            // ── Add red marker for SOS alert ──
                            LatLng sosPos = new LatLng(sosLat, sosLng);
                            mMap.addMarker(new MarkerOptions()
                                    .position(sosPos)
                                    .title("🚨 SOS Alert!")
                                    .snippet(String.format("%.0fm away - needs help!", distance * 1000))
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_RED)));
                        }
                    }
                });
    }

    // ── Calculate distance between 2 coordinates in km ──
    private double calculateDistance(double lat1, double lng1,
                                     double lat2, double lng2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}