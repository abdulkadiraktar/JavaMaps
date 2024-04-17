package com.abdulkadiraktar.javamaps.room;


import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.abdulkadiraktar.javamaps.model.Place;

@Database(entities = {Place.class}, version = 1)
public abstract class PlaceDatabase extends RoomDatabase { //soyut bir sınıf abstract

public abstract PlaceDao placeDao();

}
