package com.dhimandasgupta.mapoverlay;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, OnTouchListener {

    private GoogleMap mMap;
    private View mOverlayView;
    private AppCompatTextView mTextView;

    private boolean isDrawingModeOn = false;
    private boolean isDrawing = false;

    private List<LatLng> latLngs = new ArrayList<>();
    private PolylineOptions mPolylineOptions;
    private PolygonOptions mPolygonOptions;

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            changeMapStyle();
            mHandler.postDelayed(this, 5000);
        }
    };

    private int mStyleIndex = 0;
    private List<Integer> mStyles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mOverlayView = findViewById(R.id.frame);
        mOverlayView.setOnTouchListener(this);

        mTextView = findViewById(R.id.text);
        mTextView.setText("");

        final String YOUR_MAP_KEY = getString(R.string.google_maps_key);
        if (YOUR_MAP_KEY.equalsIgnoreCase("YOUR_MAP_KEY")) {
            Toast.makeText(this, "Please enter your Map Key", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mTextView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mTextView.getViewTreeObserver().removeOnPreDrawListener(this);

                final int bottom = mTextView.getHeight();

                mMap.setPadding(0, 0, 0, bottom);

                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int menuId = item.getItemId();

        switch (menuId) {
            case R.id.menu_drawing_mode:
                item.setChecked(!item.isChecked());
                setDrawingModeChecked(item.isChecked());
                return true;

            case R.id.menu_map_style:
                item.setChecked(!item.isChecked());
                setMapStyleChecked(item.isChecked());
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int X1 = (int) motionEvent.getX();
        int Y1 = (int) motionEvent.getY();
        Point point = new Point();
        point.x = X1;
        point.y = Y1;

        LatLng firstGeoPoint = mMap.getProjection().fromScreenLocation(point);

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clearMap();
                isDrawing = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDrawing) {
                    X1 = (int) motionEvent.getX();
                    Y1 = (int) motionEvent.getY();
                    point = new Point();
                    point.x = X1;
                    point.y = Y1;
                    LatLng geoPoint = mMap.getProjection()
                            .fromScreenLocation(point);
                    latLngs.add(geoPoint);
                    mPolylineOptions = new PolylineOptions();
                    mPolylineOptions.color(Color.RED);
                    mPolylineOptions.width(3);
                    mPolylineOptions.addAll(latLngs);
                    mMap.addPolyline(mPolylineOptions);
                }
                break;
            case MotionEvent.ACTION_UP:
                latLngs.add(firstGeoPoint);
                mMap.clear();
                mPolylineOptions = null;
                mMap.getUiSettings().setZoomGesturesEnabled(true);
                mMap.getUiSettings().setAllGesturesEnabled(true);
                mPolygonOptions = new PolygonOptions();
                mPolygonOptions.fillColor(Color.GRAY);
                mPolygonOptions.strokeColor(Color.RED);
                mPolygonOptions.strokeWidth(5);
                mPolygonOptions.addAll(latLngs);
                final Polygon polygon = mMap.addPolygon(mPolygonOptions);
                isDrawing = false;
                calculateAreaInPolygon(polygon);
                break;
        }

        return isDrawingModeOn;
    }

    private void setDrawingModeChecked(boolean b) {
        isDrawingModeOn = b;

        mMap.getUiSettings().setZoomGesturesEnabled(!isDrawingModeOn);
        mMap.getUiSettings().setAllGesturesEnabled(!isDrawingModeOn);

        mOverlayView.setVisibility(isDrawingModeOn ? View.VISIBLE : View.GONE);
        mOverlayView.setOnTouchListener(isDrawingModeOn ? this : null);

        if (!isDrawingModeOn) {
            //clearMap();
        }
    }

    private void clearMap() {
        mMap.clear();
        latLngs.clear();
        mPolylineOptions = null;
    }

    private void calculateAreaInPolygon(final Polygon polygon) {
        final double area = SphericalUtil.computeArea(polygon.getPoints());
        final String areaString = area + " Sq Meters";

        Toast.makeText(this, "Area : " + area, Toast.LENGTH_LONG).show();

        mTextView.setText(areaString);
    }

    private void setMapStyleChecked(boolean b) {
        if (b) {
            mHandler.postDelayed(mRunnable, 5000);
        } else {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    private void changeMapStyle() {
        if (mMap == null) {
            return;
        }

        if (mStyles.size() == 0) {
            mStyles.add(R.raw.aubergine_style_json);
            mStyles.add(R.raw.dark_style_json);
            mStyles.add(R.raw.night_style_json);
            mStyles.add(R.raw.retro_style_json);
            mStyles.add(R.raw.silver_style_json);
            mStyles.add(R.raw.standerd_style_json);
        }

        if (mStyleIndex == mStyles.size() - 1) {
            mStyleIndex = 0;
        } else {
            mStyleIndex++;
        }

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, mStyles.get(mStyleIndex)));

            if (!success) {
                //Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            //Log.e(TAG, "Can't find style. Error: ", e);
        }
    }
}
