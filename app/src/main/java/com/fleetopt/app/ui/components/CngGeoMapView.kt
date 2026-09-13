package com.fleetopt.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.fleetopt.app.data.local.entity.HcvEntity
import com.fleetopt.app.data.local.entity.StationEntity
import com.fleetopt.app.ui.theme.ThemeMode

/**
 * High-performance hardware-accelerated Geographic Map Engine.
 * Powered by Leaflet & OpenStreetMap tiles with zero API keys required.
 * Explicitly visualizes the full 100 km radius circle around Hyderabad and City Gate Hubs,
 * plotting all Daughter Stations, active HCV tankers, and highway corridors.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CngGeoMapView(
    stations: List<StationEntity>,
    tankers: List<HcvEntity>,
    themeMode: ThemeMode,
    onStationClick: (StationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Re-render HTML when stations or tankers update
    val mapHtml = remember(stations, tankers, isDark) {
        generateMapHtml(stations, tankers, isDark)
    }

    LaunchedEffect(mapHtml) {
        webViewRef?.loadDataWithBaseURL("https://osm.org", mapHtml, "text/html", "UTF-8", null)
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onStationSelected(stationId: String) {
                        val station = stations.firstOrNull { it.id == stationId }
                        if (station != null) {
                            post { onStationClick(station) }
                        }
                    }
                }, "AndroidBridge")

                loadDataWithBaseURL("https://osm.org", mapHtml, "text/html", "UTF-8", null)
                webViewRef = this
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun generateMapHtml(
    stations: List<StationEntity>,
    tankers: List<HcvEntity>,
    isDark: Boolean
): String {
    val tileLayerUrl = if (isDark) {
        "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    } else {
        "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
    }

    val cgsLat = 17.4526
    val cgsLng = 78.3312

    val stationsJson = stations.joinToString(",") { s ->
        """
        {
            "id": "${s.id}",
            "name": "${s.name}",
            "lat": ${s.lat},
            "lng": ${s.lng},
            "pressure": ${s.currentPressureBar.toInt()},
            "traffic": "${s.traffic}",
            "distance": ${s.distanceKm.toInt()},
            "type": "${s.stationType}"
        }
        """.trimIndent()
    }

    val tankersJson = tankers.filter { it.status.equals("on_trip", ignoreCase = true) || it.status.equals("available", ignoreCase = true) }.joinToString(",") { t ->
        """
        {
            "reg": "${t.registration}",
            "driver": "${t.driverName}",
            "phone": "${t.driverPhone}",
            "lat": ${t.lat},
            "lng": ${t.lng},
            "status": "${t.status}",
            "capacity": ${t.capacityKg.toInt()}
        }
        """.trimIndent()
    }

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            html, body, #map {
                width: 100%;
                height: 100%;
                margin: 0;
                padding: 0;
                background-color: ${if (isDark) "#090D16" else "#F8FAFC"};
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            }
            .custom-badge {
                padding: 4px 8px;
                border-radius: 6px;
                font-weight: bold;
                font-size: 11px;
                text-align: center;
                white-space: nowrap;
                box-shadow: 0 2px 6px rgba(0,0,0,0.4);
                cursor: pointer;
            }
            .badge-cgs { background: #06B6D4; color: #000; border: 2px solid #FFF; }
            .badge-critical { background: #EF4444; color: #FFF; border: 2px solid #FFF; animation: pulse 1.2s infinite; }
            .badge-warn { background: #F59E0B; color: #000; border: 2px solid #FFF; }
            .badge-ok { background: #10B981; color: #FFF; border: 2px solid #FFF; }
            .badge-tanker { background: #3B82F6; color: #FFF; border: 1px solid #93C5FD; font-size: 10px; border-radius: 12px; }

            @keyframes pulse {
                0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
                70% { box-shadow: 0 0 0 14px rgba(239, 68, 68, 0); }
                100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
            }
            .leaflet-popup-content-wrapper {
                background: ${if (isDark) "#1E293B" else "#FFFFFF"};
                color: ${if (isDark) "#FFFFFF" else "#0F172A"};
                border-radius: 10px;
                box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            }
            .leaflet-popup-tip {
                background: ${if (isDark) "#1E293B" else "#FFFFFF"};
            }
            .btn-dispatch {
                display: block;
                width: 100%;
                background: #14B8A6;
                color: #000;
                font-weight: bold;
                text-align: center;
                padding: 6px 0;
                margin-top: 8px;
                border-radius: 6px;
                text-decoration: none;
                cursor: pointer;
            }
            .radius-label {
                background: transparent;
                border: none;
                box-shadow: none;
                color: #06B6D4;
                font-weight: bold;
                font-size: 11px;
                text-shadow: 1px 1px 3px rgba(0,0,0,0.8);
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            const map = L.map('map', {
                zoomControl: false,
                attributionControl: false
            }).setView([$cgsLat, $cgsLng], 9);

            L.tileLayer('$tileLayerUrl', {
                maxZoom: 18,
                subdomains: 'abcd'
            }).addTo(map);

            const cgsPos = [$cgsLat, $cgsLng];

            // 1. Full 100 KM Regional Operational Circle around Hyderabad
            const circle100km = L.circle(cgsPos, {
                radius: 100000, // 100 km in meters
                color: '#06B6D4',
                weight: 2,
                dashArray: '8, 8',
                fillColor: '#06B6D4',
                fillOpacity: 0.04
            }).addTo(map);

            // 50 KM Inner Cluster Circle
            const circle50km = L.circle(cgsPos, {
                radius: 50000, // 50 km
                color: '#64748B',
                weight: 1.5,
                dashArray: '6, 6',
                fillColor: 'transparent'
            }).addTo(map);

            // 25 KM Express Buffer Circle
            const circle25km = L.circle(cgsPos, {
                radius: 25000, // 25 km
                color: '#475569',
                weight: 1,
                dashArray: '4, 4',
                fillColor: 'transparent'
            }).addTo(map);

            // 2. CGS Mother Hub Marker
            const cgsIcon = L.divIcon({
                className: 'custom-badge badge-cgs',
                html: '★ CGS SHAMSHABAD HUB',
                iconSize: [160, 26],
                iconAnchor: [80, 13]
            });
            L.marker(cgsPos, { icon: cgsIcon }).addTo(map)
                .bindPopup("<b>City Gate Mother Compression Hub</b><br>Header Pressure: 250 bar<br>Dedicated Fast-Fill Bays: 4<br>Coverage: 100 km Radius");

            // 3. Daughter Stations
            const stations = [$stationsJson];
            stations.forEach(s => {
                let badgeClass = 'badge-ok';
                if (s.pressure < 60) badgeClass = 'badge-critical';
                else if (s.pressure <= 120) badgeClass = 'badge-warn';

                const icon = L.divIcon({
                    className: 'custom-badge ' + badgeClass,
                    html: s.name.replace(' CNG Daughter', '') + ' (' + s.pressure + 'b)',
                    iconSize: [130, 24],
                    iconAnchor: [65, 12]
                });

                const marker = L.marker([s.lat, s.lng], { icon: icon }).addTo(map);

                marker.on('click', () => {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onStationSelected(s.id);
                    }
                });

                const trafficColor = s.traffic === 'low' ? '#22C55E' : (s.traffic === 'moderate' ? '#F59E0B' : '#EF4444');
                L.polyline([cgsPos, [s.lat, s.lng]], {
                    color: trafficColor,
                    weight: 4,
                    opacity: 0.75,
                    dashArray: s.traffic === 'heavy' ? '6, 6' : null
                }).addTo(map);
            });

            // 4. Active HCV Tankers
            const tankers = [$tankersJson];
            tankers.forEach(t => {
                const tankerIcon = L.divIcon({
                    className: 'custom-badge badge-tanker',
                    html: '🚚 ' + t.reg,
                    iconSize: [95, 20],
                    iconAnchor: [47, 10]
                });

                L.marker([t.lat, t.lng], { icon: tankerIcon }).addTo(map)
                    .bindPopup("<b>Tanker: " + t.reg + "</b><br>Driver: " + t.driver + "<br>Status: " + t.status.toUpperCase() + "<br>Capacity: " + t.capacity + " kg");
            });

            // Fit map to full 100 km circle on load
            map.fitBounds(circle100km.getBounds(), { padding: [20, 20] });
        </script>
    </body>
    </html>
    """.trimIndent()
}
