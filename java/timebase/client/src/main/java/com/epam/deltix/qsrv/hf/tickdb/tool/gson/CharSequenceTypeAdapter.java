package com.epam.deltix.qsrv.hf.tickdb.tool.gson;

import com.google.gson.*;

import java.lang.reflect.Type;

public class CharSequenceTypeAdapter implements JsonDeserializer<CharSequence>, JsonSerializer<CharSequence> {

    @Override
    public CharSequence deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return json.getAsString();
    }

    @Override
    public JsonElement serialize(CharSequence src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.toString());
    }
}
