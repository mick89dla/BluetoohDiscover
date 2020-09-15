package org.sid.bluetoothsearch.bluetoothviewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import org.sid.bluetoothsearch.datarepository.Bluetooth;
import org.sid.bluetoothsearch.datarepository.BluetoothServices;

import java.util.List;

public class BluetoothViewModel extends AndroidViewModel {
    private BluetoothServices mService;
    private LiveData<List<Bluetooth>> mBluetooths;

    public BluetoothViewModel(@NonNull Application application) {
        super(application);
        mService = new BluetoothServices(application);
        mBluetooths = mService.getAll();
    }

    public LiveData<List<Bluetooth>> getAllBluetooth() { return mBluetooths; }
    public void insert(Bluetooth bluetooth) { mService.insert(bluetooth); }
    public void update(Bluetooth bluetooth) { mService.update(bluetooth); }
}
