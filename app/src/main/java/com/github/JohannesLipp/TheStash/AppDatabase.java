package com.github.JohannesLipp.TheStash;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {FoodItem.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract FoodItemDao foodItemDao();
}