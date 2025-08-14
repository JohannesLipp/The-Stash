package com.github.JohannesLipp.TheStash;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FoodItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FoodItem item);

    @Query("SELECT * FROM food_items ORDER BY expiryYear ASC, expiryMonth ASC")
    List<FoodItem> getAllItemsSorted();

    @Query("UPDATE food_items SET quantity = quantity - :reduceBy WHERE id = :id AND quantity >= :reduceBy")
    void reduceQuantity(int id, int reduceBy);

    @Delete
    void delete(FoodItem item);
}