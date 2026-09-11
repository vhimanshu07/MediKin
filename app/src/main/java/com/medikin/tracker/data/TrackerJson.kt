package com.medikin.tracker.data

import com.medikin.tracker.domain.DoseTime
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

fun trackerGson(): Gson = GsonBuilder()
    .registerTypeAdapter(DoseTime::class.java, DoseTimeJsonAdapter)
    .create()

object DoseTimeJsonAdapter : JsonSerializer<DoseTime>, JsonDeserializer<DoseTime> {
    override fun serialize(
        source: DoseTime,
        typeOfSource: Type,
        context: JsonSerializationContext,
    ): JsonElement = JsonPrimitive(source.key)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): DoseTime {
        if (json.isJsonPrimitive) {
            return DoseTime.parse(json.asString)
                ?: throw JsonParseException("Invalid reminder time: ${json.asString}")
        }
        val value = json.asJsonObject
        return runCatching {
            DoseTime(value["hour"].asInt, value["minute"].asInt)
        }.getOrElse { throw JsonParseException("Invalid reminder time", it) }
    }
}
