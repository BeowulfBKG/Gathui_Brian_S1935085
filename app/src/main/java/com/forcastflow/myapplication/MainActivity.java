package com.forcastflow.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TEMPERATURE = "Temperature";
    private static final String WIND_DIRECTION = "Wind Direction";
    private static final String WIND_SPEED = "Wind Speed";
    private static final String HUMIDITY = "Humidity";
    private static final String PRESSURE = "Pressure";
    private static final String VISIBILITY = "Visibility";
    private static final String MINIMUM_TEMPERATURE = "Minimum Temperature";
    private static final String MAXIMUM_TEMPERATURE = "Maximum Temperature";

    private ArrayAdapter<String> adapter;
    private List<String> weatherDataList;
    private String latitude;
    private String longitude;
    private GoogleMap mMap;
    private Spinner locationSpinner;
    private Map<String, String[]> locationUrls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();
        window.setNavigationBarColor(ContextCompat.getColor(this, android.R.color.white));
        window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));

        // Ensure icons and text are visible on a white background
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        ListView listView = findViewById(R.id.listView);
        weatherDataList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, weatherDataList);
        listView.setAdapter(adapter);

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MainActivity", "Error: mapFragment is null");
        }

        // Initialize the spinner and location URLs
        locationSpinner = findViewById(R.id.locationSpinner);
        initializeLocationUrls();

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(locationUrls.keySet()));
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(spinnerAdapter);

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLocation = parent.getItemAtPosition(position).toString();
                startProgress(locationUrls.get(selectedLocation));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Display the parsed GeoRSS location if available
        if (latitude != null && longitude != null) {
            double lat = Double.parseDouble(latitude);
            double lon = Double.parseDouble(longitude);
            LatLng location = new LatLng(lat, lon);
            mMap.addMarker(new MarkerOptions().position(location).title("GeoRSS Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10));
        }
    }

    private void initializeLocationUrls() {
        locationUrls = new HashMap<>();
        locationUrls.put("Glasgow, Scotland", new String[]{
                "https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/2648579",
                "https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/2648579"});
        locationUrls.put("London, England", new String[]{
                "https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/2643743",
                "https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/2643743"});
        locationUrls.put("New York, USA", new String[]{
                "https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/5128581",
                "https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/5128581"});
        locationUrls.put("Muscat, Oman", new String[]{
                "https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/287286",
                "https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/287286"});
        locationUrls.put("Port Louis, Mauritius", new String[]{
                "https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/934154",
                "https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/934154"});
        locationUrls.put("Bangladesh", new String[]{
                "https://weather-broker-cdn.api.bbci.co.uk/en/forecast/rss/3day/1185241",
                "https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/1185241"});
    }

    public void startProgress(String[] urls) {
        String threeDayForecastUrl = urls[0];
        String latestForecastUrl = urls[1];

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(new FetchWeatherDataTask(latestForecastUrl, true));
        executor.execute(new FetchWeatherDataTask(threeDayForecastUrl, false));
    }

    private String validateUrl(String urlString) throws MalformedURLException {
        URL url = new URL(urlString);
        String protocol = url.getProtocol();
        String host = url.getHost();
        String path = url.getPath();

        if (!protocol.equals("https")) {
            throw new IllegalArgumentException("Invalid protocol: " + protocol);
        }

        if (!host.equals("weather-broker-cdn.api.bbci.co.uk")) {
            throw new IllegalArgumentException("Invalid host: " + host);
        }

        return urlString;
    }

    private boolean isServerReachable(String urlString) {
        try {
            URL url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private class FetchWeatherDataTask implements Runnable {
        private final String url;
        private final boolean isLatestForecast;

        public FetchWeatherDataTask(String url, boolean isLatestForecast) {
            this.url = url;
            this.isLatestForecast = isLatestForecast;
        }

        @Override
        public void run() {
            Log.d("MyTag", "FetchWeatherDataTask started");
            try {
                String validatedUrl = validateUrl(url);
                Log.d("MyTag", "URL validated: " + validatedUrl);

                if (!isServerReachable(validatedUrl)) {
                    Log.e("MyTag", "Server is not reachable");
                    return;
                }

                URL aurl = new URL(validatedUrl);
                URLConnection yc = aurl.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(in);
                int eventType = xpp.getEventType();
                StringBuilder resultBuilder = new StringBuilder();

                parseForecast(xpp, eventType, resultBuilder, isLatestForecast);

                String result = resultBuilder.toString();

                in.close();

                Log.d("MyTag", "FetchWeatherDataTask completed");
                updateUI(result);
            } catch (Exception ae) {
                Log.e("MyTag", "exception", ae);
                ae.printStackTrace();
            }
        }

        private void parseForecast(XmlPullParser xpp, int eventType, StringBuilder resultBuilder, boolean isLatestForecast) throws Exception {
            if (isLatestForecast) {
                parseLatestForecast(xpp, eventType, resultBuilder);
            } else {
                parseThreeDayForecast(xpp, eventType, resultBuilder);
            }
        }

        private void parseLatestForecast(XmlPullParser xpp, int eventType, StringBuilder resultBuilder) throws Exception {
            String location = "Location";
            String dayAndDate = null;
            String time = null;
            String temperature = null;
            String windDirection = null;
            String windSpeed = null;
            String humidity = null;
            String pressure = null;
            String visibility = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equalsIgnoreCase("pubDate")) {
                        String pubDate = xpp.nextText();
                        String[] pubDateParts = pubDate.split(" ");
                        dayAndDate = pubDateParts[0] + " " + pubDateParts[1] + " " + pubDateParts[2] + " " + pubDateParts[3];
                        time = pubDateParts[4] + " " + pubDateParts[5];
                    } else if (xpp.getName().equalsIgnoreCase("description")) {
                        String description = xpp.nextText();
                        String[] descriptionParts = description.split(", ");
                        for (String part : descriptionParts) {
                            if (part.contains("Temperature")) {
                                temperature = part.split(": ")[1];
                            } else if (part.contains("Wind Direction")) {
                                windDirection = part.split(": ")[1];
                            } else if (part.contains("Wind Speed")) {
                                windSpeed = part.split(": ")[1];
                            } else if (part.contains("Humidity")) {
                                humidity = part.split(": ")[1];
                            } else if (part.contains("Pressure")) {
                                pressure = part.split(": ")[1];
                                if (part.contains(",")) {
                                    pressure += "," + part.split(",")[1];
                                }
                            } else if (part.contains("Visibility")) {
                                visibility = part.split(": ")[1];
                            }
                        }
                    } else if (xpp.getName().equalsIgnoreCase("georss:point")) {
                        if (latitude == null && longitude == null) {
                            String geoPoint = xpp.nextText();
                            String[] latLon = geoPoint.split(" ");
                            latitude = latLon[0];
                            longitude = latLon[1];
                            Log.d("MyTag", "GeoRSS found: " + latitude + ", " + longitude);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "GeoRSS found: " + latitude + ", " + longitude, Toast.LENGTH_LONG).show());
                        }
                    }
                }
                eventType = xpp.next();
            }

            resultBuilder.append("Location: ").append(location);
            resultBuilder.append("\nDay and Date: ").append(dayAndDate);
            resultBuilder.append("\nTime: ").append(time);
            resultBuilder.append("\n").append(TEMPERATURE).append(": ").append(temperature);
            resultBuilder.append("\n").append(WIND_DIRECTION).append(": ").append(windDirection);
            resultBuilder.append("\n").append(WIND_SPEED).append(": ").append(windSpeed);
            resultBuilder.append("\n").append(HUMIDITY).append(": ").append(humidity);
            resultBuilder.append("\n").append(PRESSURE).append(": ").append(pressure);
            resultBuilder.append("\n").append(VISIBILITY).append(": ").append(visibility);
        }

        private void parseThreeDayForecast(XmlPullParser xpp, int eventType, StringBuilder resultBuilder) throws Exception {
            String dayAndDate = null;
            String weather = null;
            String minTemp = null;
            String maxTemp = null;
            String windDirection = null;
            String windSpeed = null;
            String humidity = null;
            String pressure = null;
            String visibility = null;
            String uvRisk = null;
            String pollution = null;
            boolean geoRssFoundInItem = false;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equalsIgnoreCase("item")) {
                        dayAndDate = null;
                        weather = null;
                        minTemp = null;
                        maxTemp = null;
                        windDirection = null;
                        windSpeed = null;
                        humidity = null;
                        pressure = null;
                        visibility = null;
                        uvRisk = null;
                        pollution = null;
                        geoRssFoundInItem = false;
                    }
                    if (xpp.getName().equalsIgnoreCase("title")) {
                        String title = xpp.nextText();
                        if (title != null) {
                            String[] titleParts = title.split(": ");
                            if (titleParts.length > 1) {
                                dayAndDate = titleParts[0];
                                weather = titleParts[1];
                            }
                        }
                    } else if (xpp.getName().equalsIgnoreCase("description")) {
                        String description = xpp.nextText();
                        if (description != null) {
                            String[] descriptionParts = description.split(", ");
                            for (String part : descriptionParts) {
                                if (part.contains("Minimum Temperature")) {
                                    minTemp = part.split(": ")[1];
                                } else if (part.contains("Maximum Temperature")) {
                                    maxTemp = part.split(": ")[1];
                                } else if (part.contains("Wind Direction")) {
                                    windDirection = part.split(": ")[1];
                                } else if (part.contains("Wind Speed")) {
                                    windSpeed = part.split(": ")[1];
                                } else if (part.contains("Humidity")) {
                                    humidity = part.split(": ")[1];
                                } else if (part.contains("Pressure")) {
                                    pressure = part.split(": ")[1];
                                } else if (part.contains("Visibility")) {
                                    visibility = part.split(": ")[1];
                                } else if (part.contains("UV Risk")) {
                                    uvRisk = part.split(": ")[1];
                                } else if (part.contains("Pollution")) {
                                    pollution = part.split(": ")[1];
                                }
                            }
                        }
                    } else if (xpp.getName().equalsIgnoreCase("georss:point")) {
                        String geoPoint = xpp.nextText();
                        String[] latLon = geoPoint.split(" ");
                        latitude = latLon[0];
                        longitude = latLon[1];
                        geoRssFoundInItem = true;
                        Log.d("MyTag", "GeoRSS found: " + latitude + ", " + longitude);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "GeoRSS found: " + latitude + ", " + longitude, Toast.LENGTH_LONG).show());
                    }
                }
                eventType = xpp.next();
                if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")) {
                    resultBuilder.append("\nDay and Date: ").append(dayAndDate);
                    resultBuilder.append("\nKind of Weather: ").append(weather);
                    resultBuilder.append("\n").append(MINIMUM_TEMPERATURE).append(": ").append(minTemp);
                    resultBuilder.append("\n").append(MAXIMUM_TEMPERATURE).append(": ").append(maxTemp);
                    resultBuilder.append("\n").append(WIND_DIRECTION).append(": ").append(windDirection);
                    resultBuilder.append("\n").append(WIND_SPEED).append(": ").append(windSpeed);
                    resultBuilder.append("\n").append(HUMIDITY).append(": ").append(humidity);
                    resultBuilder.append("\n").append(PRESSURE).append(": ").append(pressure);
                    resultBuilder.append("\n").append(VISIBILITY).append(": ").append(visibility);
                    resultBuilder.append("\nUV Risk: ").append(uvRisk);
                    resultBuilder.append("\nPollution: ").append(pollution);
                    resultBuilder.append("\n\n");

                    if (!geoRssFoundInItem && latitude == null && longitude == null) {
                        Log.d("MyTag", "GeoRSS not found in this item.");
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "GeoRSS not found in this item.", Toast.LENGTH_LONG).show());
                    }
                }
            }
        }

        private void updateUI(String result) {
            runOnUiThread(() -> {
                if (result != null) {
                    weatherDataList.add(result + "\n");
                    adapter.notifyDataSetChanged();

                    if (latitude != null && longitude != null) {
                        Log.d("MyTag", "GeoRSS successfully stored: " + latitude + ", " + longitude);
                        Toast.makeText(MainActivity.this, "GeoRSS successfully stored: " + latitude + ", " + longitude, Toast.LENGTH_LONG).show();

                        // Update the map with the new GeoRSS location
                        if (mMap != null) {
                            double lat = Double.parseDouble(latitude);
                            double lon = Double.parseDouble(longitude);
                            LatLng location = new LatLng(lat, lon);
                            mMap.addMarker(new MarkerOptions().position(location).title("GeoRSS Location"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10));
                        }
                    }
                }
            });
        }
    }
}
