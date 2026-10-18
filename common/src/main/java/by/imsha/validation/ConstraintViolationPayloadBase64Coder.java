package by.imsha.validation;


import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

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
        //минифицируем результирующую строку, не изменяя общий HTTP mapper
        this.objectMapper = objectMapper.rebuild()
                .disable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    /**
     * Конвертировать объект в json и кодировать в Base64
     */
    public String encode(Object source) throws JacksonException {
        return Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(source));
    }

    /**
     * Декодировать base64 строку и конвертировать в JsonNode
     */
    public JsonNode decode(String encodedValue) throws JacksonException {
        return objectMapper.readTree(new String(Base64.getDecoder().decode(encodedValue), StandardCharsets.UTF_8));
    }
}
