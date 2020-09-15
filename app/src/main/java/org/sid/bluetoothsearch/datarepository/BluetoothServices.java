package org.sid.bluetoothsearch.datarepository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import android.os.AsyncTask;

public class BluetoothServices {
    private BluetoothDao bluetoothDao;
    //private LiveData<List<Bluetooth>> Bluetooths;
    private BluetoothDatabase db;

    public BluetoothServices(Application application) {
        db = BluetoothDatabase.getInstance(application); // On cree une instance de la base de donne
        bluetoothDao = db.bluetoothDao(); // On connecte notre classe DAO a la base de donnee
    }

    // On redefinit les methodes de la classe DAO

    public LiveData<List<Bluetooth>> getAll() {
        return bluetoothDao.getBluetooths();
    }

    public void insert(Bluetooth bluetooth) {
        new InsertAsyncTask(bluetoothDao).execute(bluetooth);
    }

    public void update(Bluetooth bluetooth){
        new UpdateAsyncTask(bluetoothDao).execute(bluetooth);
    }

    private static class InsertAsyncTask extends AsyncTask<Bluetooth, Void, Void> {
        private BluetoothDao asyncTaskDao;

        InsertAsyncTask(BluetoothDao bluetoothDao) {
            asyncTaskDao = bluetoothDao;
        }

        @Override
        protected Void doInBackground(final Bluetooth... params) {
            asyncTaskDao.insert(params[0]);
            return null;
        }
    }

    private static class UpdateAsyncTask extends AsyncTask<Bluetooth, Void, Void> {

        private BluetoothDao asyncTaskDao;

        UpdateAsyncTask(BluetoothDao bluetoothDao) {
            asyncTaskDao = bluetoothDao;
        }

        @Override
        protected Void doInBackground(final Bluetooth... params) {
            asyncTaskDao.update(params[0]);
            return null;
        }
    }


}
