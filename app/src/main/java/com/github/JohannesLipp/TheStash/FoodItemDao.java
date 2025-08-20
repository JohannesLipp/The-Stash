package com.github.JohannesLipp.TheStash;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FoodItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FoodItem item);

    @Query("SELECT * FROM food_items ORDER BY expiryYear ASC, expiryMonth ASC")
    List<FoodItem> getAllItemsSorted();

    @Query("UPDATE food_items SET count = count - :reduceBy WHERE id = :id AND count >= :reduceBy")
    void reduceQuantity(int id, int reduceBy);

    @Update
    void update(FoodItem foodItem);

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    FoodItem getItemById(int id);

    @Delete
    void delete(FoodItem item);
}