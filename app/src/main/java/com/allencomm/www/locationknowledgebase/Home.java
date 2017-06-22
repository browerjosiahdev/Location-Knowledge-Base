package com.allencomm.www.locationknowledgebase;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.util.SparseArray;
import android.util.Log;
import android.net.Uri;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import com.allencomm.www.locationknowledgebase.requests.KnowledgeRequest;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.appdatasearch.GetRecentContextCall;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.Detector;

import com.android.volley.toolbox.*;

import com.allencomm.www.locationknowledgebase.services.GimbalService;
import com.allencomm.www.locationknowledgebase.data_access.GimbalDAO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class Home extends AppCompatActivity {

    public static final int CAMERA_PERMISSION_REQUEST = 1;
    public static final int BLUETOOTH_PERMISSION_REQUEST = 2;
    public static final String KNOWLEDGE_BASE_URL = "http://dev-cwsandbox.allencomm.com/";// "http://10.200.3.207:63047/";

    public BroadcastReceiver bluetoothReceiver;
    public ArrayList<String> validMacAddresses = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Start listening for bluetooth beacons.
        startService(new Intent(this, GimbalService.class));

        // Check to see if we have to request bluetooth permissions.
        int bluetoothPermissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH);
        int bluetoothAdminPermissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN);
        int accessCoarseLocationPermissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);

        if (bluetoothPermissionCheck != PackageManager.PERMISSION_GRANTED ||
                bluetoothAdminPermissionCheck != PackageManager.PERMISSION_GRANTED ||
                accessCoarseLocationPermissionCheck != PackageManager.PERMISSION_GRANTED
                ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION},
                    BLUETOOTH_PERMISSION_REQUEST);
        }

        // Set the action toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add a click event listener to the action button.
        FloatingActionButton scan = (FloatingActionButton) findViewById(R.id.scan);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Home.this.scan(view);
            }
        });

        final Button discover = (Button) findViewById(R.id.discover);
        discover.setEnabled(false);
        discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Home.this.discover(view);
            }
        });

        // Request the list of valid MAC addresses.
        requestValidAddresses(new KnowledgeRequest() {
            @Override
            public void onResponse(JSONArray response) {
                for (int i = 0; i < response.length(); i++) {
                    try {
                        validMacAddresses.add(i, response.getString(i));
                    } catch (JSONException error) {
                        Log.e("RESPONSE ERROR", error.getMessage());
                    }
                }

                // Enable the discover button if there is at least one
                // valid MAC address.
                if (validMacAddresses.size() > 0) {
                    discover.setEnabled(true);
                }
            }

            @Override
            public void onError(VolleyError error) {
                if (error != null && error.getMessage() != null) {
                    Log.e("RESPONSE ERROR", error.getMessage());
                }
            }
        });
    }

    public void discover(View view) {
        final TextView placeholderTextView = (TextView) findViewById(R.id.placeholder_text);
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        // Create the broadcast receiver.
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                        // TODO: What should we do when the discovery process begins?

                        break;
                    }
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                        adapter.cancelDiscovery();

                        break;
                    }
                    case BluetoothDevice.ACTION_FOUND: {
                        BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        final String address = device.getAddress();

                        // Check to see if the found MAC address is valid.
                        if (validMacAddresses.contains(address)) {
                            Snackbar
                                    .make(findViewById(R.id.content), "Found device: " + address, Snackbar.LENGTH_LONG)
                                    .show();

                            // Get the RSSI value to determine the connection strength to the
                            // Bluetooth device,
                            final int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                            HashMap<String, String> requestData = new HashMap<String, String>();
                            requestData.put("id", address);
                            requestData.put("rssi", Integer.toString(rssi));

                            requestKnowledge(requestData, new KnowledgeRequest() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    Snackbar
                                            .make(findViewById(R.id.content), "Device content retrieved!", Snackbar.LENGTH_LONG)
                                            .show();

                                    String title = "";
                                    String body = "";

                                    try {
                                        title = response.getString("title");
                                        body = response.getString("body");

                                        adapter.cancelDiscovery();
                                    } catch (JSONException error) {
                                        Log.e("RESPONSE JSON", error.getMessage());
                                    }

                                    placeholderTextView.setText(title + " : " + body);
                                }

                                @Override
                                public void onError(VolleyError error) {
                                    Snackbar
                                            .make(findViewById(R.id.content), "Unable to retrieve device content: " + address + " : " + Integer.toString(rssi), Snackbar.LENGTH_LONG)
                                            .show();

                                    placeholderTextView.setText("onError: " + error.toString());

                                    if (error != null && error.getMessage() != null) {
                                        Log.e("RESPONSE ERROR", error.getMessage());
                                    }
                                }
                            });
                        }

                        break;
                    }
                }
            }
        };

        // Register to listen for Bluetooth devices.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver, filter);

        adapter.startDiscovery();
    }

    public void scan(View view) {
        final View scanView = view;
        final Activity activity = this;

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        final CameraSource cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .build();

        final SurfaceView cameraView = (SurfaceView) findViewById(R.id.camera_view);
        final TextView placeholderTextView = (TextView) findViewById(R.id.placeholder_text);

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST);
                } else {
                    startCamera();
                }
            }

            public void startCamera() {
                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException e) {
                    Log.e("CAMERA SOURCE", e.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });
        cameraView.setVisibility(View.VISIBLE);

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (barcodes.size() != 0) {
                    placeholderTextView.post(new Runnable() {
                        @Override
                        public void run() {
                            String qrValue = barcodes.valueAt(0).displayValue;

                            // Check if this is a Location Knowledge Base ID qr code.
                            if (qrValue.contains("AllenCommLKB:")) {
                                String knowledgeBaseId = qrValue.split(":")[1];
                                HashMap<String, String> requestData = new HashMap<String, String>();
                                requestData.put("id", knowledgeBaseId);

                                requestKnowledge(requestData, new KnowledgeRequest() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        String title = "";
                                        String body = "";

                                        try {
                                            title = response.getString("title");
                                            body = response.getString("body");
                                        } catch (org.json.JSONException e) {
                                            Log.e("RESPONSE JSON", e.getMessage());
                                        }

                                        placeholderTextView.setText(title + " : " + body);
                                    }

                                    @Override
                                    public void onError(VolleyError error) {
                                        Log.e("RESPONSE ERROR", error.getMessage());
                                    }
                                });
                            // If the result didn't match, the QR code is invalid and we
                            // should notify the user.
                            } else {
                                View snackbarView = findViewById(R.id.content);

                                Snackbar
                                        .make(snackbarView, "Invalid QR Code", Snackbar.LENGTH_LONG)
                                        .show();
                            }

                            // Hide the camera view.
                            cameraView.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    public void requestKnowledge(HashMap<String, String> uriData, final KnowledgeRequest request) {
        String url = KNOWLEDGE_BASE_URL + "api/knowledge/item?";
        RequestQueue queue = Volley.newRequestQueue(this);

        // Convert the map of URI data to URL parameters.
        if (uriData != null) {
            for (HashMap.Entry<String, String> entry : uriData.entrySet()) {
                url += entry.getKey() + "=" + entry.getValue() + "&";
            }
        }

        url = url.substring(0, url.length() - 1);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        request.onResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        request.onError(error);
                    }
                }
        );

        queue.add(jsonObjectRequest);
    }

    public void requestValidAddresses(final KnowledgeRequest request) {
        String url = KNOWLEDGE_BASE_URL + "api/knowledge/addresses";
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        request.onResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        request.onError(error);
                    }
                }
        );

        queue.add(jsonArrayRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case BLUETOOTH_PERMISSION_REQUEST: {
                break;
            }
            case CAMERA_PERMISSION_REQUEST: {
                SurfaceView cameraView = (SurfaceView) findViewById(R.id.camera_view);
                cameraView.setVisibility(View.GONE);
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(bluetoothReceiver);
    }
}
