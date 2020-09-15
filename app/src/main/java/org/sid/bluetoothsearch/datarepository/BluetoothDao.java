package org.sid.bluetoothsearch.datarepository;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BluetoothDao {
    @Query(value = "SELECT * FROM Bluetooth")
    public LiveData<List<Bluetooth>> getBluetooths();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(Bluetooth bluetooth);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    public void update(Bluetooth bluetooth);
}
