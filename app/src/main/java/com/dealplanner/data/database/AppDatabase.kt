package com.dealplanner.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dealplanner.data.dao.*
import com.dealplanner.data.model.*

@Database(
    entities = [
        PantryItem::class,
        DealItem::class,
        ReceiptItem::class,
        MealPlan::class,
        BudgetState::class,
        Params::class,
        CouponModifier::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun dealDao(): DealDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun budgetDao(): BudgetDao
    abstract fun paramsDao(): ParamsDao
    abstract fun couponModifierDao(): CouponModifierDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "snap_optimizer_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default params and budget on first run
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Example migration for future versions
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add migration logic here when needed
            }
        }
    }
}
