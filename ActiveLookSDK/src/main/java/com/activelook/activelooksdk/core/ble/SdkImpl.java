/*

Copyright 2021 Microoled
Licensed under the Apache License, Version 2.0 (the “License”);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an “AS IS” BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package com.activelook.activelooksdk.core.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.activelook.activelooksdk.DiscoveredGlasses;
import com.activelook.activelooksdk.Glasses;
import com.activelook.activelooksdk.Sdk;
import com.activelook.activelooksdk.exceptions.UnsupportedBleException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

class SdkImpl implements Sdk {

    private final Context context;
    private final BluetoothManager manager;
    final BluetoothAdapter adapter;
    @NonNull private final BluetoothLeScanner scanner;
    private final HashMap<String, GlassesImpl> connectedGlasses = new HashMap<>();
    private ScanCallback scanCallback;

    SdkImpl(Context context) throws UnsupportedBleException {
        this.context = context;
        this.manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.adapter = this.manager.getAdapter();
        if (this.adapter == null) {
            throw new UnsupportedBleException();
        }
        BluetoothLeScanner scanner = this.adapter.getBluetoothLeScanner();
        if (scanner == null) {
            throw new RuntimeException("Bluetooth turned off");
        }
        this.scanner = scanner;
    }

    Context getContext() {
        return this.context;
    }

    private void toast(String text) {
        Toast.makeText(this.context, text, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void startScan(@Nullable String address, Consumer<DiscoveredGlasses> onDiscoverGlasses) {
        this.scanCallback = new ScanCallbackImpl(onDiscoverGlasses);
        if (address != null) {
            ScanFilter scanFilterMac = new ScanFilter.Builder().setDeviceAddress(address).build();
            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
            this.scanner.startScan(Collections.singletonList(scanFilterMac), scanSettings, scanCallback);
        } else {
            this.scanner.startScan(this.scanCallback);
        }
    }

    public void startScan(Consumer<DiscoveredGlasses> onDiscoverGlasses) {
        startScan(null, onDiscoverGlasses);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void stopScan() {
        this.scanner.stopScan(this.scanCallback);
        this.scanCallback = null;
    }

    @Override
    public boolean isScanning() {
        return this.scanCallback != null;
    }

    @Override
    public void connect(
            String address,
            Consumer<Glasses> onConnected,
            Consumer<String> onConnectionFail,
            Consumer<Glasses> onDisconnected
    ) {
        registerConnectedGlasses(new GlassesImpl(address, onConnected, onConnectionFail, onDisconnected));
    }

    @Override
    public void stopConnect(String address) {
        GlassesImpl gls = connectedGlasses.get(address);
        if (gls != null) {
            gls.disconnect();
        }
    }

    void registerConnectedGlasses(GlassesImpl bleGlasses) {
        this.connectedGlasses.put(bleGlasses.getAddress(), bleGlasses);
    }

    void unregisterConnectedGlasses(GlassesImpl bleGlasses) {
        this.connectedGlasses.remove(bleGlasses.getAddress());
    }

    GlassesImpl getConnectedBleGlasses(final String address) {
        return this.connectedGlasses.get(address);
    }

}
