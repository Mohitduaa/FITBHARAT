package com.example.data.model

enum class MealType(val displayName: String, val hindiName: String) {
    BREAKFAST("Breakfast", "Nashta"),
    LUNCH("Lunch", "Dopahar Ka Khana"),
    SNACKS("Evening Snack", "Chai / Snacks"),
    DINNER("Dinner", "Raat Ka Khana")
}

enum class DietPreference(val displayName: String) {
    VEGETARIAN("Vegetarian (Shakahari)"),
    NON_VEGETARIAN("Non-Vegetarian"),
    EGGETARIAN("Eggetarian"),
    JAIN("Jain (No onion/garlic)"),
    VEGAN("Vegan")
}

data class IndianFoodPreset(
    val name: String,
    val hindiName: String,
    val servingSize: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val category: String,
    val isVeg: Boolean = true,
    val healthTip: String = ""
)

object IndianFoodDatabase {
    val items = listOf(
        IndianFoodPreset("Roti (Phulka - No Ghee)", "Roti (Bina Ghee)", "1 medium (35g)", 85, 3.0, 17.5, 0.4, 2.3, "Breads", true, "High complex carbs; 2 rotis per meal is ideal."),
        IndianFoodPreset("Roti (With Ghee)", "Ghee Roti", "1 medium (40g)", 115, 3.0, 17.5, 4.0, 2.3, "Breads", true, "Good healthy fats, limit to 1/2 tsp ghee."),
        IndianFoodPreset("Dal Tadka (Yellow Moong/Toor)", "Dal Tadka", "1 katori (150g)", 140, 7.5, 18.0, 4.0, 4.2, "Dals & Lentils", true, "Great source of plant protein and folate."),
        IndianFoodPreset("Moong Dal Khichdi", "Moong Khichdi", "1 bowl (200g)", 210, 8.5, 36.0, 3.5, 5.0, "Rice & Khichdi", true, "Light on stomach, perfect post-workout or dinner."),
        IndianFoodPreset("Brown Rice (Steamed)", "Brown Rice", "1 katori (130g)", 145, 3.2, 30.0, 1.2, 2.8, "Rice & Khichdi", true, "Lower glycemic index than polished white rice."),
        IndianFoodPreset("White Basmati Rice", "Chawal", "1 katori (130g)", 165, 3.0, 36.0, 0.4, 0.8, "Rice & Khichdi", true, "Control portion to 1 small katori paired with double dal."),
        IndianFoodPreset("Paneer Bhurji", "Paneer Bhurji", "1 bowl (150g)", 240, 14.5, 6.0, 18.0, 1.8, "Paneer & Dairy", true, "Excellent vegetarian protein, keeps you full."),
        IndianFoodPreset("Palak Paneer", "Palak Paneer", "1 katori (150g)", 195, 10.0, 7.0, 14.0, 3.5, "Paneer & Dairy", true, "Iron from spinach + calcium & protein from paneer."),
        IndianFoodPreset("Rajma Masala", "Rajma Curry", "1 bowl (180g)", 220, 11.0, 32.0, 5.0, 8.5, "Dals & Lentils", true, "Rich in soluble fiber that lowers cholesterol."),
        IndianFoodPreset("Chole Masala (Chickpeas)", "Chole", "1 bowl (180g)", 240, 10.5, 34.0, 6.5, 7.8, "Dals & Lentils", true, "Pair with 1 roti or brown rice, skip bhature."),
        IndianFoodPreset("Poha with Peanuts & Veggies", "Poha", "1 plate (160g)", 220, 5.5, 38.0, 6.0, 3.2, "Breakfast", true, "Add more peas and carrots to lower carb density."),
        IndianFoodPreset("Upma (Sooji with Veggies)", "Sooji Upma", "1 plate (160g)", 210, 5.0, 36.0, 5.5, 2.5, "Breakfast", true, "Add roasted chana dal or sprouts for extra protein."),
        IndianFoodPreset("Besan Chilla with Paneer", "Besan Chilla", "2 chillas (140g)", 250, 13.0, 24.0, 10.0, 5.0, "Breakfast", true, "Top weight-loss breakfast: high protein & fiber."),
        IndianFoodPreset("Idli with Sambhar", "Idli Sambhar", "2 idlis + 1 bowl sambhar", 190, 7.0, 36.0, 2.0, 4.0, "South Indian", true, "Steamed, fermented, gut-healthy."),
        IndianFoodPreset("Plain Masala Dosa", "Masala Dosa", "1 medium", 260, 6.0, 42.0, 8.0, 3.0, "South Indian", true, "Enjoy with extra sambhar instead of coconut chutney."),
        IndianFoodPreset("Egg Bhurji (2 Eggs + Veggies)", "Andaa Bhurji", "1 plate (140g)", 185, 13.5, 4.0, 12.0, 1.2, "Eggs & Non-Veg", false, "High satiety, complete amino acid profile."),
        IndianFoodPreset("Boiled Eggs (Whole)", "Ubla Andaa", "2 whole eggs", 155, 12.5, 1.0, 10.5, 0.0, "Eggs & Non-Veg", false, "Zero sugar, rich in choline and vitamin B12."),
        IndianFoodPreset("Boiled Egg Whites", "Andaa Safedi", "3 whites", 51, 11.0, 0.7, 0.2, 0.0, "Eggs & Non-Veg", false, "Pure lean protein for calorie deficit."),
        IndianFoodPreset("Tandoori Chicken (Breast)", "Tandoori Chicken", "2 pieces (160g)", 220, 32.0, 4.0, 7.5, 1.0, "Eggs & Non-Veg", false, "Lean, high-protein muscle builder with minimal fat."),
        IndianFoodPreset("Chicken Tikka", "Chicken Tikka", "5-6 pieces (150g)", 210, 28.0, 5.0, 8.0, 1.2, "Eggs & Non-Veg", false, "Grilled without heavy cream."),
        IndianFoodPreset("Fish Curry (Rohu/Pomfret)", "Machhi Curry", "1 bowl (180g)", 190, 22.0, 6.0, 8.0, 1.0, "Eggs & Non-Veg", false, "Omega-3 fatty acids for metabolic health."),
        IndianFoodPreset("Soya Chunks Curry", "Soya Curry", "1 bowl (160g)", 180, 17.0, 16.0, 4.0, 6.0, "Vegetables & Curries", true, "Highest plant protein (52g protein per 100g raw soya)."),
        IndianFoodPreset("Bhindi Masala (Okra)", "Bhindi Ki Sabzi", "1 katori (120g)", 110, 2.5, 10.0, 6.5, 3.8, "Vegetables & Curries", true, "Low calorie, rich in soluble dietary fiber."),
        IndianFoodPreset("Aloo Gobi Matar (Less Oil)", "Aloo Gobi", "1 katori (130g)", 135, 3.0, 18.0, 5.5, 3.2, "Vegetables & Curries", true, "Keep potato portion moderate, load on gobi."),
        IndianFoodPreset("Lauki / Bottle Gourd Sabzi", "Ghia / Lauki", "1 katori (140g)", 75, 1.8, 8.0, 3.5, 2.5, "Vegetables & Curries", true, "Extremely hydrating and low calorie (96% water)."),
        IndianFoodPreset("Mixed Sprout Salad", "Sprouts Salad", "1 bowl (150g)", 130, 9.0, 19.0, 1.5, 6.5, "Snacks & Salads", true, "Enzyme rich, keeps cravings away for hours."),
        IndianFoodPreset("Cucumber Tomato Onion Salad", "Kachumber Salad", "1 plate (150g)", 45, 1.5, 8.5, 0.4, 2.8, "Snacks & Salads", true, "Eat before lunch to fill stomach volume."),
        IndianFoodPreset("Roasted Makhana (Foxnuts)", "Bhuna Makhana", "1 big bowl (30g)", 110, 3.2, 22.0, 1.0, 2.5, "Snacks & Salads", true, "Crunchy healthy alternative to potato chips."),
        IndianFoodPreset("Roasted Chana (With Skin)", "Bhuna Chana", "1 handful (30g)", 115, 6.0, 17.0, 2.0, 4.5, "Snacks & Salads", true, "Great portable desk snack, low GI."),
        IndianFoodPreset("Dahi / Plain Curd", "Dahi", "1 katori (100g)", 70, 3.5, 4.5, 4.0, 0.0, "Paneer & Dairy", true, "Probiotic for healthy digestion and gut microbiome."),
        IndianFoodPreset("Chaas / Salted Buttermilk", "Chaas / Mattha", "1 tall glass (250ml)", 45, 2.8, 4.0, 1.8, 0.0, "Beverages", true, "Cools body, hydrates and aids fat metabolism."),
        IndianFoodPreset("Masala Chai (Less Sugar)", "Ghar Ki Chai", "1 cup (150ml)", 65, 2.0, 9.0, 2.2, 0.0, "Beverages", true, "Limit sugar to 1/2 tsp (10 kcal)."),
        IndianFoodPreset("Green Tea / Lemon Ginger", "Green Tea", "1 cup (200ml)", 4, 0.2, 0.8, 0.0, 0.0, "Beverages", true, "EGCG antioxidants to boost resting metabolic rate.")
    )
}
