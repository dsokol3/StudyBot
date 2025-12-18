package com.chatbot.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter(autoApply = false)
public class FloatArrayToStringConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(attribute[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        String trimmed = dbData.trim();
        if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.isBlank()) return new float[0];
        String[] parts = trimmed.split(",");
        List<Float> values = new ArrayList<>(parts.length);
        for (String p : parts) {
            try {
                values.add(Float.parseFloat(p.trim()));
            } catch (NumberFormatException e) {
                // skip invalid
            }
        }
        float[] arr = new float[values.size()];
        for (int i = 0; i < values.size(); i++) arr[i] = values.get(i);
        return arr;
    }
}
