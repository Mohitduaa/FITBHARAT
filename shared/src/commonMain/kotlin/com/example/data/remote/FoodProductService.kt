package com.example.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/** Nutrition of a packaged food, per 100 g. */
data class FoodProduct(
    val barcode: String,
    val name: String,
    val brand: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double,
    /** Grams in one serving as printed on the pack, when known. */
    val servingGrams: Double?
)

class FoodProductException(message: String) : Exception(message)

/**
 * Looks up packaged foods by barcode in Open Food Facts (open database, ODbL).
 * No account or key is needed; the API asks clients to identify themselves via User-Agent.
 */
class FoodProductService {
    private val client = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 20_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun lookup(barcode: String): FoodProduct {
        val body = try {
            client.get("https://world.openfoodfacts.org/api/v2/product/$barcode.json") {
                url.parameters.append("fields", "product_name,product_name_en,brands,serving_quantity,nutriments")
                header("User-Agent", "FitBharat/1.0 (nutrition tracker app)")
            }.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw FoodProductException("Could not reach the food database. Check your internet connection.")
        }

        val root = try {
            json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw FoodProductException("The food database returned an unexpected response.")
        }
        val product = (root["product"] as? JsonObject)
            ?.takeIf { (root["status"] as? JsonPrimitive)?.intOrNull != 0 }
            ?: throw FoodProductException("This product is not in the food database yet. Add it as a custom food.")
        val nutriments = product["nutriments"] as? JsonObject ?: JsonObject(emptyMap())

        fun nutrient(key: String): Double? =
            nutriments.number("${key}_100g") ?: nutriments.number("${key}_prepared_100g")

        val kcal = nutrient("energy-kcal") ?: nutrient("energy")?.let { it / 4.184 }
            ?: throw FoodProductException("This product has no calorie information. Add it as a custom food.")

        val name = product.text("product_name").ifBlank { product.text("product_name_en") }.ifBlank { "Packaged food" }
        return FoodProduct(
            barcode = barcode,
            name = name,
            brand = product.text("brands").substringBefore(',').trim(),
            kcalPer100g = kcal,
            proteinPer100g = nutrient("proteins") ?: 0.0,
            carbsPer100g = nutrient("carbohydrates") ?: 0.0,
            fatPer100g = nutrient("fat") ?: 0.0,
            fiberPer100g = nutrient("fiber") ?: 0.0,
            servingGrams = product.number("serving_quantity")?.takeIf { it > 0 }
        )
    }

    private fun JsonObject.number(key: String): Double? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        return primitive.doubleOrNull ?: primitive.contentOrNull?.toDoubleOrNull()
    }

    private fun JsonObject.text(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
}
