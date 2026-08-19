package com.rishaanlabs.ros.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "finance_categories",
    indices = [Index(value = ["groupName", "name"], unique = true)]
)
data class FinanceCategory(
    @PrimaryKey val id: String,
    val groupName: String,
    val name: String,
    val isEssential: Boolean = false,
    val monthlyBudgetMinor: Long? = null,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)

fun defaultFinanceCategories(): List<FinanceCategory> = listOf(
    FinanceCategory("essential-housing", "Essentials", "Housing", true, sortOrder = 10),
    FinanceCategory("essential-bills", "Essentials", "Bills", true, sortOrder = 20),
    FinanceCategory("essential-groceries", "Essentials", "Groceries", true, sortOrder = 30),
    FinanceCategory("essential-transport", "Essentials", "Transport", true, sortOrder = 40),
    FinanceCategory("essential-healthcare", "Essentials", "Healthcare", true, sortOrder = 50),
    FinanceCategory("essential-insurance", "Essentials", "Insurance", true, sortOrder = 60),
    FinanceCategory("lifestyle-eating-out", "Lifestyle", "Eating Out", false, sortOrder = 110),
    FinanceCategory("lifestyle-shopping", "Lifestyle", "Shopping", false, sortOrder = 120),
    FinanceCategory("lifestyle-entertainment", "Lifestyle", "Entertainment", false, sortOrder = 130),
    FinanceCategory("lifestyle-subscriptions", "Lifestyle", "Subscriptions", false, sortOrder = 140),
    FinanceCategory("lifestyle-personal-care", "Lifestyle", "Personal Care", false, sortOrder = 150),
    FinanceCategory("lifestyle-gifts", "Lifestyle", "Gifts", false, sortOrder = 160),
    FinanceCategory("travel-transport", "Travel", "Transport", false, sortOrder = 210),
    FinanceCategory("travel-hotel", "Travel", "Hotel", false, sortOrder = 220),
    FinanceCategory("travel-food", "Travel", "Food", false, sortOrder = 230),
    FinanceCategory("travel-activities", "Travel", "Activities", false, sortOrder = 240),
    FinanceCategory("work-software", "Work", "Software", false, sortOrder = 310),
    FinanceCategory("work-equipment", "Work", "Equipment", false, sortOrder = 320),
    FinanceCategory("work-other", "Work", "Other", false, sortOrder = 330),
    FinanceCategory("other-fees", "Other", "Fees", false, sortOrder = 410),
    FinanceCategory("other-misc", "Other", "Miscellaneous", false, sortOrder = 420)
)
