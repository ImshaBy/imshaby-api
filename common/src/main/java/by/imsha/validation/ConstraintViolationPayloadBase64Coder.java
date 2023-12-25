package by.imsha.validation;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Кодировщик объектов полезной нагрузки в строку формата Base64 и из строки Base64 в UTF-8 строку
 *
 * FIXME острой нужды в нем нет, но messageinterpolator не нравится json, его фигурные скобки
 *  ( чтобы не писал в логи warn зря )
 */
public class ConstraintViolationPayloadBase64Coder {

    private final ObjectMapper objectMapper;

    public ConstraintViolationPayloadBase64Coder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        //минифицируем результирующую строку, чтобы максимально уменьшить ее размер
        this.objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Конвертировать объект в json и кодировать в Base64
     */
    public String encode(Object source) throws JsonProcessingException {
        return Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(source));
    }

    /**
     * Декодировать base64 строку и конвертировать в JsonNode
     */
    public JsonNode decode(String encodedValue) throws JsonProcessingException {
        return objectMapper.readTree(new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8));
    }
}
