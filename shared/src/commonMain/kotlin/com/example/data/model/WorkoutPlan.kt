package com.example.data.model

data class WorkoutExercise(
    val name: String,
    val hindiName: String,
    val durationSeconds: Int,
    val repsOrDetails: String,
    val description: String,
    val tips: String,
    /** Id of the exercise photos bundled with the app; empty when there is none. */
    val imageId: String = "",
    /** Full step-by-step instructions. */
    val steps: List<String> = emptyList(),
    /** Muscles worked, e.g. "Chest, Triceps". */
    val muscles: String = ""
)

data class WorkoutRoutine(
    val id: String,
    val title: String,
    val hindiTitle: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val level: String, // Beginner, Intermediate, Gentle
    val equipment: String, // No Equipment
    val description: String,
    val iconName: String,
    val exercises: List<WorkoutExercise>,
    val category: WorkoutCategory = WorkoutCategory.FAT_BURN
)

enum class WorkoutCategory(val label: String) {
    YOGA("Yoga"),
    FAT_BURN("Home Fat Burn")
}

object WorkoutDatabase {
    val routines = listOf(
        WorkoutRoutine(
            id = "surya_namaskar",
            title = "Morning Suryanamaskar Flow",
            hindiTitle = "Pratahkalin Surya Namaskar",
            durationMinutes = 12,
            caloriesBurned = 70,
            level = "Beginner Friendly",
            equipment = "Yoga Mat / Floor",
            description = "Traditional 12-step Sun Salutation flow for full body flexibility, spinal health, and activating morning metabolism.",
            iconName = "wb_sunny",
            exercises = listOf(
                WorkoutExercise("Pranamasana (Prayer Pose)", "Pranam Mudra", 30, "Slow deep breathing", "Stand upright with feet together, palms joined at chest.", "Inhale calm, exhale tension.", imageId = "ai_prayer"),
                WorkoutExercise("Hastauttanasana (Raised Arms)", "Hastauttanasana", 30, "Inhale & stretch", "Raise arms overhead, gentle arch backwards.", "Keep shoulders relaxed, feel abdominal stretch.", imageId = "ai_raised_arms"),
                WorkoutExercise("Padahastasana (Forward Fold)", "Padahastasana", 40, "Bend forward", "Bend from waist touching fingers/palms to feet.", "Soft knees if hamstrings are tight.", imageId = "ai_forward_fold"),
                WorkoutExercise("Ashwa Sanchalanasana (Equestrian)", "Ashwa Sanchalan", 40, "Right leg back", "Step right leg back, sink hips, gaze slightly upward.", "Opens tight hip flexors from long sitting.", imageId = "ai_low_lunge"),
                WorkoutExercise("Dandasana (Plank Pose)", "Santolanasana", 35, "Hold core tight", "Keep spine straight from head to heels, core engaged.", "Do not let lower back sag.", imageId = "ai_plank"),
                WorkoutExercise("Ashtanga Namaskara (8-Point Pose)", "Ashtanga Namaskar", 30, "Knees, chest, chin to floor", "Lower knees, chest and chin to ground while hips stay slightly lifted.", "Builds upper body and core stability.", imageId = "ai_eight_point"),
                WorkoutExercise("Bhujangasana (Cobra Pose)", "Bhujangasana", 40, "Gentle chest lift", "Slide forward, lift chest using back muscles without forcing arms.", "Great for lower back relief.", imageId = "ai_cobra"),
                WorkoutExercise("Adho Mukha Svanasana (Downward Dog)", "Parvatasana", 45, "Inverted V-shape", "Push hips high to the sky, press palms firmly.", "Calms nervous system and lengthens calves.", imageId = "ai_down_dog")
            ),
            category = WorkoutCategory.YOGA
        ),
        WorkoutRoutine(
            id = "yoga_belly_fat",
            title = "Yoga for Belly Fat",
            hindiTitle = "Pet Ki Charbi Ke Liye Yoga",
            durationMinutes = 15,
            caloriesBurned = 90,
            level = "Beginner Friendly",
            equipment = "Yoga Mat",
            description = "Core-focused poses that strengthen the abdomen, improve digestion and build steady heat.",
            iconName = "self_improvement",
            exercises = listOf(
                WorkoutExercise("Kapalbhati Breathing", "Kapalbhati", 60, "Quick exhales", "Sit tall and push short, sharp breaths out through the nose by pulling the belly in.", "Stop if you feel dizzy; skip during pregnancy.", imageId = "ai_breathing"),
                WorkoutExercise("Naukasana (Boat Pose)", "Naukasana", 30, "Hold 20-30 s", "Sit, lean back, lift both legs and reach arms forward so the body forms a V.", "Bend the knees to make it easier.", imageId = "ai_boat"),
                WorkoutExercise("Bhujangasana (Cobra Pose)", "Bhujangasana", 40, "Hold & breathe", "Lie on your stomach, press palms beside the chest and lift the chest.", "Keep elbows soft and shoulders down.", imageId = "ai_cobra"),
                WorkoutExercise("Dhanurasana (Bow Pose)", "Dhanurasana", 30, "Hold 20 s", "Lie on your stomach, hold your ankles and lift chest and thighs together.", "Breathe evenly; rock gently if comfortable.", imageId = "ai_bow"),
                WorkoutExercise("Pawanmuktasana (Wind-Relieving)", "Pawanmuktasana", 45, "Hug knees", "Lie on your back and hug both knees to the chest, lifting the head.", "Eases bloating and gas.", imageId = "ai_knees_to_chest"),
                WorkoutExercise("Plank Hold", "Phalakasana", 30, "Hold steady", "Hold a straight line from head to heels on palms or forearms.", "Squeeze the glutes; don't let hips drop.", imageId = "ai_plank"),
                WorkoutExercise("Ardha Matsyendrasana (Seated Twist)", "Ardha Matsyendrasana", 40, "20 s each side", "Sit, cross one foot over the other knee and twist toward the bent knee.", "Lengthen the spine on each inhale.", imageId = "ai_seated_twist"),
                WorkoutExercise("Shavasana (Rest)", "Shavasana", 60, "Relax", "Lie flat, arms by your sides, and breathe slowly.", "Let the heart rate settle before getting up.", imageId = "ai_resting")
            ),
            category = WorkoutCategory.YOGA
        ),
        WorkoutRoutine(
            id = "yoga_power_flow",
            title = "Power Yoga Fat Burn",
            hindiTitle = "Power Yoga",
            durationMinutes = 20,
            caloriesBurned = 140,
            level = "Intermediate",
            equipment = "Yoga Mat",
            description = "A faster, flowing sequence that raises the heart rate while building strength and balance.",
            iconName = "self_improvement",
            exercises = listOf(
                WorkoutExercise("Surya Namaskar Rounds", "Surya Namaskar", 180, "4 brisk rounds", "Move through the 12 Sun Salutation steps at a steady pace.", "Match each move with one breath.", imageId = "ai_down_dog"),
                WorkoutExercise("Utkatasana (Chair Pose)", "Utkatasana", 40, "Hold 30-40 s", "Bend the knees as if sitting on a chair, arms raised overhead.", "Weight in the heels, chest lifted.", imageId = "ai_chair"),
                WorkoutExercise("Virabhadrasana II (Warrior II)", "Virabhadrasana", 60, "30 s each side", "Wide stance, front knee bent over the ankle, arms stretched out.", "Keep the front knee in line with the toes.", imageId = "ai_warrior"),
                WorkoutExercise("Trikonasana (Triangle)", "Trikonasana", 60, "30 s each side", "Straight legs wide, reach one hand down to the shin and the other up.", "Keep both sides of the waist long.", imageId = "ai_triangle"),
                WorkoutExercise("Chaturanga to Cobra Flow", "Chaturanga", 60, "8 slow reps", "From plank lower halfway, then roll into cobra and push back to plank.", "Drop the knees if needed.", imageId = "ai_cobra"),
                WorkoutExercise("Vrikshasana (Tree Pose)", "Vrikshasana", 60, "30 s each leg", "Stand on one leg, place the other foot on the inner thigh or calf, palms together.", "Fix your gaze on one point for balance.", imageId = "ai_tree"),
                WorkoutExercise("Setu Bandhasana (Bridge)", "Setu Bandhasana", 45, "Hold 30 s", "Lie on your back, feet flat, lift the hips high.", "Press evenly through both feet.", imageId = "ai_bridge"),
                WorkoutExercise("Shavasana (Rest)", "Shavasana", 60, "Relax", "Lie flat and breathe slowly.", "Let the body cool down.", imageId = "ai_resting")
            ),
            category = WorkoutCategory.YOGA
        ),
        WorkoutRoutine(
            id = "yoga_bedtime",
            title = "Bedtime Relax Yoga",
            hindiTitle = "Sone Se Pehle Yoga",
            durationMinutes = 10,
            caloriesBurned = 40,
            level = "All Levels",
            equipment = "Bed or Mat",
            description = "Gentle stretches that calm the mind and help you sleep better. Good sleep controls cravings the next day.",
            iconName = "self_improvement",
            exercises = listOf(
                WorkoutExercise("Balasana (Child's Pose)", "Balasana", 60, "Slow breathing", "Kneel, sit back on the heels and fold forward with arms stretched.", "Rest the forehead on the floor or a pillow.", imageId = "ai_child"),
                WorkoutExercise("Marjaryasana-Bitilasana (Cat-Cow)", "Marjari Asana", 60, "10 slow rounds", "On hands and knees, arch and round the back with the breath.", "Move slowly; it releases the lower back.", imageId = "ai_cat_cow"),
                WorkoutExercise("Supta Baddha Konasana (Butterfly)", "Supta Titli Asana", 60, "Relax hips", "Lie on your back, soles of the feet together, knees open.", "Put pillows under the knees if needed.", imageId = "ai_butterfly"),
                WorkoutExercise("Supine Spinal Twist", "Supta Matsyendrasana", 60, "30 s each side", "Lie on your back and drop both knees to one side, arms out.", "Keep both shoulders on the floor.", imageId = "Lying_Crossover"),
                WorkoutExercise("Viparita Karani (Legs Up the Wall)", "Viparita Karani", 90, "Breathe deeply", "Lie with your legs resting straight up a wall.", "Eases tired legs after a long day.", imageId = "ai_legs_up_wall"),
                WorkoutExercise("Anulom Vilom Breathing", "Anulom Vilom", 90, "Alternate nostrils", "Breathe in through one nostril and out through the other, switching each time.", "Keep the breath smooth and quiet.", imageId = "ai_breathing")
            ),
            category = WorkoutCategory.YOGA
        ),
        WorkoutRoutine(
            id = "fat_burn_7min",
            title = "7-Minute Fat Burner",
            hindiTitle = "7 Minute Fat Burner",
            durationMinutes = 7,
            caloriesBurned = 75,
            level = "All Levels",
            equipment = "No Equipment",
            description = "The classic quick full-body circuit: 30 seconds of work, 10 seconds of rest. Fits into any busy day.",
            iconName = "fitness_center",
            exercises = listOf(
                WorkoutExercise("Jumping Jacks", "Jumping Jacks", 30, "30 s fast", "Jump the feet out while raising arms overhead, then back together.", "Step instead of jumping for low impact.", imageId = "Star_Jump"),
                WorkoutExercise("Wall Sit", "Diwaar Kursi", 30, "Hold 30 s", "Back against a wall, slide down until knees are at 90 degrees.", "Keep knees over the ankles.", imageId = "Sit_Squats"),
                WorkoutExercise("Push-ups", "Dand", 30, "As many as you can", "Hands under shoulders, lower the chest and press back up.", "Do them on your knees to make it easier.", imageId = "Pushups"),
                WorkoutExercise("Crunches", "Crunches", 30, "Controlled reps", "On your back, knees bent, curl the shoulders off the floor.", "Don't pull on your neck.", imageId = "Crunches"),
                WorkoutExercise("Squats", "Baithak", 30, "Steady reps", "Push the hips back and down, then stand up.", "Chest up, heels down.", imageId = "Bodyweight_Squat"),
                WorkoutExercise("High Knees", "Tez Ghutne", 30, "30 s fast", "Run in place driving knees to hip height.", "Pump the arms for extra burn.", imageId = "Fast_Skipping"),
                WorkoutExercise("Lunges", "Lunges", 30, "Alternate legs", "Step forward and lower until both knees bend to 90 degrees.", "Front knee stays behind the toes.", imageId = "Bodyweight_Walking_Lunge"),
                WorkoutExercise("Plank", "Plank", 30, "Hold 30 s", "Straight line from head to heels on forearms.", "Brace the core and breathe.", imageId = "Plank")
            )
        ),
        WorkoutRoutine(
            id = "fat_burn_full_body",
            title = "20-Min Full-Body Fat Burn",
            hindiTitle = "Poore Shareer Ka Fat Burn",
            durationMinutes = 20,
            caloriesBurned = 200,
            level = "Intermediate",
            equipment = "No Equipment",
            description = "Higher-intensity intervals for the fastest calorie burn at home. 45 seconds on, 15 seconds off, two rounds.",
            iconName = "fitness_center",
            exercises = listOf(
                WorkoutExercise("Burpees", "Burpees", 45, "Steady pace", "Squat, jump the feet back to plank, return and jump up.", "Step back instead of jumping to make it easier.", imageId = "wm_burpee"),
                WorkoutExercise("Mountain Climbers", "Mountain Climbers", 45, "Fast knees", "From plank, drive the knees to the chest one after the other.", "Keep hips level with shoulders.", imageId = "Mountain_Climbers"),
                WorkoutExercise("Jump Squats", "Jump Baithak", 45, "Land softly", "Squat down, then jump up explosively.", "Do regular squats if your knees hurt.", imageId = "Freehand_Jump_Squat"),
                WorkoutExercise("Skaters", "Skater Jumps", 45, "Side to side", "Leap sideways onto one leg, swinging the arms across.", "Touch the floor lightly for balance.", imageId = "Skating"),
                WorkoutExercise("Plank Jacks", "Plank Jacks", 45, "Quick feet", "In plank, jump the feet wide and back together.", "Don't let the lower back sag.", imageId = "Plank"),
                WorkoutExercise("Butt Kicks", "Aedi Peeche", 45, "Fast pace", "Jog in place, kicking heels toward the glutes.", "Stay on the balls of the feet.", imageId = "Single_Leg_Butt_Kick"),
                WorkoutExercise("Russian Twists", "Russian Twist", 45, "Twist side to side", "Sit leaning back, feet up or down, rotate the torso left and right.", "Move from the waist, not just the arms.", imageId = "Russian_Twist"),
                WorkoutExercise("Cool-down Stretch", "Stretching", 60, "Breathe slowly", "Stretch the legs, hips and shoulders gently.", "Never skip the cool-down.", imageId = "Hamstring_Stretch")
            )
        ),
        WorkoutRoutine(
            id = "post_meal_walk",
            title = "15-Min Post-Meal Fat Burn Walk",
            hindiTitle = "Khane Ke Baad Ki 15-Min Walk",
            durationMinutes = 15,
            caloriesBurned = 80,
            level = "All Levels / Beginner",
            equipment = "Comfortable Slippers/Shoes",
            description = "Shatapavali (100 steps tradition) updated for modern science. Lowers post-meal blood sugar spikes by 35% and accelerates fat burn.",
            iconName = "directions_walk",
            exercises = listOf(
                WorkoutExercise("Gentle Pace Walking", "Dheemi Chal", 180, "Easy breath", "Walk at comfortable, relaxed conversational pace.", "Start within 15 minutes after eating."),
                WorkoutExercise("Brisk Walking", "Tez Chal", 300, "Slightly elevated pulse", "Pick up speed, swing arms naturally beside body.", "Focus on breathing rhythmically through nose."),
                WorkoutExercise("Side Step Strides", "Side Kadam", 120, "Side to side", "Step side-to-side along living room or hallway.", "Activates outer glutes and adductors."),
                WorkoutExercise("Calf Raise Walks", "Aedi Utha Kar Chalna", 120, "On ball of feet", "Walk gently on tiptoes for 10-15 paces, then normal.", "Strengthens ankles and calves."),
                WorkoutExercise("Cool-down Stroll", "Dheema Halka Walk", 180, "Slow deep breaths", "Wind down, hands on belly, mindful steps.", "Promotes gastric motility and prevents acid reflux.")
            )
        ),
        WorkoutRoutine(
            id = "desi_hiit_beginner",
            title = "Desi Home Fat-Loss HIIT",
            hindiTitle = "Ghar Par Desi Fat Loss HIIT",
            durationMinutes = 20,
            caloriesBurned = 160,
            level = "Beginner to Intermediate",
            equipment = "No Equipment",
            description = "High energy intervals that burn visceral fat without needing a gym or dumbbells. 40s work, 20s rest.",
            iconName = "fitness_center",
            exercises = listOf(
                WorkoutExercise("Desi High Knees", "Tez Ghutne Upar", 40, "40s work / 20s rest", "Drive knees up rhythmically with light bouncy steps.", "Land softly on balls of your feet.", imageId = "Fast_Skipping"),
                WorkoutExercise("Baithak (Desi Bodyweight Squats)", "Baithak", 45, "15-20 reps", "Feet shoulder width, push hips back like sitting on a low stool.", "Keep chest proud, weight on heels.", imageId = "Bodyweight_Squat"),
                WorkoutExercise("Pehlwani Arm Windmills", "Hath Ghoomana", 40, "Full rotations", "Circular arm rotations forward and backward engaging shoulders.", "Warms up rotator cuff and upper torso.", imageId = "Arm_Circles"),
                WorkoutExercise("Incline Desk / Wall Pushups", "Diwaar Pushups", 45, "12-15 reps", "Hands on wall or sturdy table, press chest towards it.", "Joint-safe way to build chest and triceps.", imageId = "Incline_Push-Up"),
                WorkoutExercise("Butt Kicks with Arm Swings", "Aedi Peeche Maro", 40, "40s work / 20s rest", "Jog in place bringing heels toward glutes.", "Great hamstring activation and cardio burn.", imageId = "Single_Leg_Butt_Kick"),
                WorkoutExercise("Standing Oblique Twists", "Kamar Ka Mod", 45, "Twist left & right", "Hands behind head, twist elbow toward opposite knee.", "Tones love handles and waistline.", imageId = "Torso_Rotation")
            )
        ),
        WorkoutRoutine(
            id = "belly_core_burn",
            title = "Flat Tummy & Core Strengthener",
            hindiTitle = "Pet Aur Kamar Ki Mazbooti",
            durationMinutes = 15,
            caloriesBurned = 110,
            level = "Beginner Friendly",
            equipment = "Mat or Bed",
            description = "Safe floor exercises targeted to strengthen deep core muscles (transverse abdominis) without straining the neck.",
            iconName = "accessibility_new",
            exercises = listOf(
                WorkoutExercise("Glute Bridge", "Setu Bandhasana", 45, "12-15 reps", "Lie on back, lift hips until knees, hips, and shoulders form a straight line.", "Squeeze glutes at top for 2 seconds.", imageId = "Butt_Lift_Bridge"),
                WorkoutExercise("Dead Bug Core Hold", "Dead Bug", 45, "Alternate arms & legs", "Opposite arm and leg lower slowly while lower back presses into floor.", "Never let your lower back arch off the floor.", imageId = "Dead_Bug"),
                WorkoutExercise("Bird Dog Balance", "Santulan", 45, "10 reps each side", "On hands and knees, reach opposite arm and leg straight out.", "Improves posture and relieves desk backache.", imageId = "wm_bird_dog"),
                WorkoutExercise("Forearm Plank Hold", "Plank", 40, "Breathe steadily", "Hold elbows directly under shoulders, keep body straight like a plank.", "Tighten abs as if expecting a punch in belly.", imageId = "Plank"),
                WorkoutExercise("Seated Knee Tucks", "Baith Kar Knee Tuck", 45, "12-15 reps", "Sit on chair edge or floor, bring knees toward chest with control.", "Burns lower abdominal pouch.", imageId = "Bent-Knee_Hip_Raise")
            )
        ),
        WorkoutRoutine(
            id = "knee_safe_cardio",
            title = "Low-Impact Knee-Safe Cardio",
            hindiTitle = "Ghutno Ke Liye Surakshit Cardio",
            durationMinutes = 18,
            caloriesBurned = 120,
            level = "Gentle / Overweight Safe",
            equipment = "No Equipment",
            description = "Designed specially for heavier individuals or those with sensitive knees. Zero jumping, high calorie burn.",
            iconName = "favorite",
            exercises = listOf(
                WorkoutExercise("Step Jacks (No Jump)", "Bina Jump Jacks", 45, "Continuous tempo", "Step one foot out while raising arms overhead, alternate sides.", "Zero impact on knees and ankles.", imageId = "Star_Jump"),
                WorkoutExercise("Boxer Shuffle & Jab", "Boxer Punch", 45, "Light punches", "Shift weight gently side to side while throwing light controlled punches.", "Tones shoulders and elevates heart rate."),
                WorkoutExercise("Standing Knee Pull-Downs", "Ghutna Upar Khinch", 45, "Rhythmic pulls", "Reach high overhead, pull hands down as knee lifts to hip height.", "Contracts entire core and lat muscles.", imageId = "Step-up_with_Knee_Raise"),
                WorkoutExercise("Speed Skater Taps", "Skater Tap", 45, "Side reach", "Step wide to side, tap trailing toe behind, sway arms.", "Shapes outer thighs and glutes safely.", imageId = "Skating"),
                WorkoutExercise("Standing Calf Pumps", "Pindli Pump", 45, "Up and down", "Rise high on toes, lower down slowly with control.", "Boosts venous return from legs to heart.")
            )
        )
    )
}
