package org.sid.bluetoothsearch.datarepository;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Bluetooth.class}, version = 1)
public abstract class BluetoothDatabase extends RoomDatabase {
    public abstract BluetoothDao bluetoothDao();

    private static final String DB_NAME = "bluetooth-db";
    private static BluetoothDatabase INSTANCE;

    public static BluetoothDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (BluetoothDatabase.class) {
                if(INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(), BluetoothDatabase.class,
                            DB_NAME).build();
                }
            }
        }
        return INSTANCE;
    }
}
