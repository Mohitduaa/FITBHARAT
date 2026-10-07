package com.example.data.remote

import com.example.data.local.UserProfileEntity
import com.example.data.model.AiFoodScanResult
import com.example.data.model.DetectedItem
import com.example.data.model.GoalType
import com.example.data.model.goal
import com.example.platform.PlatformServices
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** A scan that produced no usable result; [message] is shown to the user as-is. */
class FoodScanException(message: String) : Exception(message)

private class GeminiHttpException(val code: Int, body: String) :
    IOException("Gemini HTTP $code: ${body.take(300)}")

@OptIn(ExperimentalEncodingApi::class)
class GeminiService(private val platform: PlatformServices) {
    private val client = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 20_000
            requestTimeoutMillis = 75_000
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val apiKey: String?
        get() = platform.geminiApiKey?.takeUnless { it.isBlank() || it == "MY_GEMINI_API_KEY" }

    /** False when the build has no Gemini key, so the coach can only give offline tips. */
    val isConfigured: Boolean get() = apiKey != null

    suspend fun analyzeFoodImage(jpeg: ByteArray, profile: UserProfileEntity): AiFoodScanResult =
        withContext(Dispatchers.Default) {
            if (apiKey == null) throw FoodScanException("AI scan abhi set up nahi hai. Khana khud add karein.")

            val goalText = when (profile.goal()) {
                GoalType.LOSE -> "weight loss"
                GoalType.GAIN -> "healthy weight gain"
                GoalType.MAINTAIN -> "weight maintenance"
            }
            val prompt = """
                You are an expert Indian dietitian. Identify the food in this photo and estimate its nutrition
                for the portion that is actually visible. Assume typical Indian home-cooking oil and portion sizes.
                The user's goal is $goalText and their daily target is ${profile.calorieTarget} kcal.
                Return ONLY JSON with exactly this shape:
                {
                  "isFood": true,
                  "dishName": "Name of the dish or thali",
                  "estimatedCalories": 450,
                  "proteinG": 18.5,
                  "carbsG": 65.0,
                  "fatG": 12.0,
                  "fiberG": 8.0,
                  "portionDescription": "e.g. 2 phulka + 1 katori dal + sabzi",
                  "desiHealthScore": 8,
                  "desiCoachFeedback": "1-2 sentences of practical Hinglish feedback for the user's goal",
                  "ingredients": ["main ingredients you can see or that this dish is made of, e.g. besan, paneer, onion, oil"],
                  "detectedItems": [
                    {"name": "Item", "portion": "1 katori", "calories": 140, "proteinG": 7.0, "carbsG": 18.0, "fatG": 4.0}
                  ],
                  "smartDesiSwaps": ["Short tip for the user's goal", "Another tip"]
                }
                desiHealthScore is 1-10. If the photo does not show food or drink, return {"isFood": false}.
            """.trimIndent()

            val body = buildJsonObject {
                putJsonArray("contents") {
                    add(buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", prompt) })
                            add(buildJsonObject {
                                putJsonObject("inlineData") {
                                    put("mimeType", "image/jpeg")
                                    put("data", Base64.encode(jpeg))
                                }
                            })
                        }
                    })
                }
                putJsonObject("generationConfig") {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                    put("maxOutputTokens", 8192)
                    putJsonObject("thinkingConfig") { put("thinkingLevel", "low") }
                }
            }

            val text = try {
                generate(SCAN_MODELS, body)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                platform.logError(TAG, "Food scan failed", e)
                throw FoodScanException(
                    if (e is GeminiHttpException) "AI abhi busy hai. Thodi der baad dobara try karein."
                    else "Internet connection check karke dobara try karein."
                )
            }
            parseScanResult(text)
        }

    suspend fun chatWithDesiCoach(
        userPrompt: String,
        chatHistory: List<Pair<String, Boolean>>,
        profile: UserProfileEntity,
        todaySummary: String
    ): String = withContext(Dispatchers.Default) {
        if (apiKey == null) return@withContext getOfflineCoachResponse(userPrompt, profile)

        val goalRule = when (profile.goal()) {
            GoalType.LOSE -> "The user wants to LOSE weight. Promote a sustainable deficit (0.25 to 0.75 kg per week)."
            GoalType.GAIN -> "The user wants to GAIN weight. Promote a clean calorie surplus with high protein and strength training (0.25 to 0.5 kg per week)."
            GoalType.MAINTAIN -> "The user wants to MAINTAIN weight. Focus on balanced meals, protein and consistency."
        }
        val diet = profile.dietPreference.lowercase().replace('_', '-')
        val systemInstruction = """
            You are FitBharat Coach, an empathetic and knowledgeable Indian nutrition and fitness coach.
            User: ${profile.name}, ${profile.age} y, ${profile.gender}, ${profile.heightCm.toInt()} cm, ${profile.currentWeightKg} kg now, target ${profile.targetWeightKg} kg, daily target ${profile.calorieTarget} kcal, $diet diet.
            Today so far: $todaySummary
            $goalRule
            You understand Indian households, rotis, rice, tadka, meetha cravings, festivals and shaadi situations.
            Tone: warm and encouraging, natural Hinglish mixed with English.
            Rules:
            - Never recommend starvation, crash diets or junk-food bulking. For medical conditions, suggest seeing a doctor.
            - Give practical Indian food swaps and portion tips, using the user's numbers where useful.
            - Keep answers short: 3-5 bullets or 2 short paragraphs. Plain text, no markdown headings or bold.
        """.trimIndent()

        // Earlier turns; the history already ends with the message being answered.
        val earlier = chatHistory.dropLast(1).takeLast(6).dropWhile { !it.second }
        val body = buildJsonObject {
            putJsonArray("contents") {
                earlier.forEach { (text, isUser) ->
                    add(buildJsonObject {
                        put("role", if (isUser) "user" else "model")
                        putJsonArray("parts") { add(buildJsonObject { put("text", text) }) }
                    })
                }
                add(buildJsonObject {
                    put("role", "user")
                    putJsonArray("parts") { add(buildJsonObject { put("text", userPrompt) }) }
                })
            }
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { add(buildJsonObject { put("text", systemInstruction) }) }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.7)
                put("maxOutputTokens", 2048)
                putJsonObject("thinkingConfig") { put("thinkingLevel", "minimal") }
            }
        }

        try {
            generate(CHAT_MODELS, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            platform.logError(TAG, "Coach chat failed", e)
            // The short reason helps tell a bad key (HTTP 400/403) from a network problem.
            val reason = when (e) {
                is GeminiHttpException -> "AI error ${e.code}"
                else -> e::class.simpleName ?: "network error"
            }
            "Coach abhi connect nahi ho pa raha ($reason) — internet check karke dobara poochiye. Tab tak ek tip:\n\n" +
                getOfflineCoachResponse(userPrompt, profile)
        }
    }

    /**
     * Tries each model in order: an overloaded / failing model or an empty answer falls through to
     * the next one. Returns the answer text without any "thought" parts.
     */
    private suspend fun generate(models: List<String>, body: JsonObject): String {
        val key = apiKey ?: throw IOException("No API key")
        var lastError: Exception = IOException("No model available")
        for (model in models) {
            try {
                val response = client.post("$BASE_URL/$model:generateContent") {
                    header("x-goog-api-key", key)
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
                val responseBody = response.bodyAsText()
                val status = response.status.value
                if (status !in 200..299) {
                    val error = GeminiHttpException(status, responseBody)
                    if (status != 429 && status < 500) throw error
                    lastError = error
                    continue
                }
                val candidate = json.parseToJsonElement(responseBody).jsonObject["candidates"]
                    ?.jsonArray?.firstOrNull()?.jsonObject
                val parts = candidate?.get("content")?.jsonObject?.get("parts")?.jsonArray.orEmpty()
                val text = parts
                    .map { it.jsonObject }
                    .filter { it["thought"]?.jsonPrimitive?.booleanOrNull != true }
                    .joinToString("") { it["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
                    .trim()
                if (text.isNotEmpty()) return text
                lastError = IOException("Empty answer from $model (${candidate?.get("finishReason")})")
            } catch (e: CancellationException) {
                throw e
            } catch (e: GeminiHttpException) {
                if (e.code != 429 && e.code < 500) throw e
                lastError = e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError
    }

    private fun parseScanResult(jsonString: String): AiFoodScanResult {
        val obj = try {
            json.parseToJsonElement(
                jsonString.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            ).jsonObject
        } catch (e: Exception) {
            platform.logError(TAG, "Unparseable scan result: ${jsonString.take(300)}", e)
            throw FoodScanException("Photo samajh nahi aayi. Khane ki saaf photo ke saath dobara try karein.")
        }
        if (obj["isFood"]?.jsonPrimitive?.booleanOrNull == false) {
            throw FoodScanException("Is photo mein khana nahi dikha. Plate ki saaf photo lein.")
        }
        val calories = obj.int("estimatedCalories", 0)
        if (calories <= 0) {
            throw FoodScanException("Calories estimate nahi ho payi. Dobara try karein ya khud add karein.")
        }

        val detected = obj["detectedItems"].asObjects().map { item ->
            DetectedItem(
                name = item.string("name", "Food item"),
                portion = item.string("portion", "1 serving"),
                calories = item.int("calories", 0),
                proteinG = item.double("proteinG", 0.0),
                carbsG = item.double("carbsG", 0.0),
                fatG = item.double("fatG", 0.0)
            )
        }
        val swaps = (obj["smartDesiSwaps"] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }

        return AiFoodScanResult(
            dishName = obj.string("dishName", "Meal"),
            estimatedCalories = calories,
            proteinG = obj.double("proteinG", 0.0),
            carbsG = obj.double("carbsG", 0.0),
            fatG = obj.double("fatG", 0.0),
            fiberG = obj.double("fiberG", 0.0),
            portionDescription = obj.string("portionDescription", "1 plate"),
            detectedItems = detected,
            ingredients = (obj["ingredients"] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull },
            desiHealthScore = obj.int("desiHealthScore", 5).coerceIn(1, 10),
            desiCoachFeedback = obj.string("desiCoachFeedback", ""),
            smartDesiSwaps = swaps
        )
    }

    private fun JsonObject.string(key: String, default: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull ?: default

    private fun JsonObject.int(key: String, default: Int): Int =
        (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() } ?: default

    private fun JsonObject.double(key: String, default: Double): Double =
        (this[key] as? JsonPrimitive)?.doubleOrNull ?: default

    private fun JsonElement?.asObjects(): List<JsonObject> =
        (this as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }

    private fun getOfflineCoachResponse(prompt: String, profile: UserProfileEntity): String {
        val lower = prompt.lowercase()
        val goal = profile.goal()
        return when {
            "gain" in lower || "badh" in lower || "patla" in lower || "mass" in lower || (goal == GoalType.GAIN && ("weight" in lower || "diet" in lower)) -> {
                "Healthy Weight Gain ke Desi Rules:\n\n" +
                "1. Calorie surplus: Aapka daily target ${profile.calorieTarget} kcal hai. Meals skip kiye bina din mein 4-5 baar khaayein.\n" +
                "2. Calorie-dense desi food: Peanut chikki, banana shake, dry fruits, ghee wali dal, paneer paratha.\n" +
                "3. Protein: Har meal mein paneer, dahi, dal, soya, eggs ya chicken rakhein.\n" +
                "4. Strength training: Hafte mein 3-4 din weights ya bodyweight exercises, taaki weight muscle ke roop mein badhe, fat nahi."
            }
            "shaadi" in lower || "party" in lower || "wedding" in lower -> {
                "Shaadi / Party Survival Rules:\n\n" +
                "1. Pehle Tandoori & Salad: Buffet mein seedha main course mat jao. 2-3 pieces paneer tikka ya chicken tikka + cucumber khao pehle.\n" +
                "2. Naan vs Roti: Naan maida aur butter se bhari hoti hai (350+ kcal). Instead, choose 1 Tandoori Roti (bina makkhan).\n" +
                "3. Meetha Strategy: 3 Gulab Jamun lene ke bajaye, sirf 1 piece lo aur slow chew karke enjoy karo.\n" +
                "4. Hydrate: Drinks mein soda ya alcohol ke beech 1-2 glass paani piyein!"
            }
            "night" in lower || "bhookh" in lower || "late" in lower || "snack" in lower -> {
                "Late Night Bhookh? Top 3 Desi Options:\n\n" +
                "1. Bhuna Makhana: 1 katori roasted makhana with a pinch of black salt (~80 kcal). Crunchy aur light.\n" +
                "2. Haldi Doodh: 150ml toned doodh with pinch of turmeric & cinnamon. Calms mind and helps sleep.\n" +
                "3. Cucumber Slices with Chaat Masala: Zero guilt, ultra hydrating (only 25 kcal)!\n\n" +
                "Tip: Often late night cravings are just thirst or boredom. Pehle 1 bada glass paani pi kar 10 min wait karein."
            }
            "roti" in lower || "rice" in lower || "chawal" in lower -> {
                "Roti vs Chawal:\n\n" +
                "• Calories: 1 Phulka (85 kcal) aur 1 choti katori rice (130-140 kcal) mein zyada difference nahi hai.\n" +
                "• Fiber & Fullness: Roti mein fiber thoda zyada hota hai, isliye pet der tak bhara rehta hai.\n" +
                "• Desi Formula: Agar rice pasand hai, to dal-to-rice ratio 2:1 rakhein (double dal, kam chawal) + kachumber salad.\n\n" +
                "Dono khaye ja sakte hain, bas portion control zaroori hai!"
            }
            "protein" in lower || "veg" in lower || "shakahari" in lower -> {
                "Vegetarian Indian Protein Checklist:\n\n" +
                "1. Paneer / Tofu: 100g paneer = ~18g protein.\n" +
                "2. Soya Chunks: 50g raw soya = ~26g protein (sabse sasta aur powerful source!).\n" +
                "3. Dahi / Greek Yogurt: 1 bowl = 8-10g protein + probiotics.\n" +
                "4. Dal & Sprouts: 1 big katori moong dal or sprouts = 8-10g.\n" +
                "5. Besan Chilla / Sattu: Subah breakfast mein besan ya sattu add karein."
            }
            else -> coachGreeting(profile)
        }
    }

    companion object {
        private const val TAG = "GeminiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        // Primary model first; the second is used when the first is overloaded or fails.
        private val SCAN_MODELS = listOf("gemini-3.5-flash", "gemini-3.5-flash-lite")
        private val CHAT_MODELS = listOf("gemini-3.5-flash-lite", "gemini-3.5-flash")

        fun coachGreeting(profile: UserProfileEntity): String {
            val goalLine = when (profile.goal()) {
                GoalType.LOSE -> "Goal: ${profile.currentWeightKg} kg se ${profile.targetWeightKg} kg tak healthy weight loss."
                GoalType.GAIN -> "Goal: ${profile.currentWeightKg} kg se ${profile.targetWeightKg} kg tak healthy weight gain."
                GoalType.MAINTAIN -> "Goal: ${profile.currentWeightKg} kg pe fit aur strong rehna."
            }
            val name = profile.name.ifBlank { "dost" }
            return "Namaste $name! Main hoon aapka FitBharat Coach.\n\n" +
                "$goalLine Aaj ka target ${profile.calorieTarget} kcal aur ${profile.stepGoal} steps hai.\n\n" +
                "Diet, recipes, cravings, workout ya motivation — kuch bhi poochiye!"
        }
    }
}
