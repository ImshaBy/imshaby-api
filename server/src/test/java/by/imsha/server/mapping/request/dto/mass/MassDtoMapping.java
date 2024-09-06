package by.imsha.server.mapping.request.dto.mass;

import by.imsha.server.FieldValueSetter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class MassDtoMapping implements FieldValueSetter {

    public static final String IDENTIFIER_FIELD_QUALIFIER = "Идентификатор";
    public static final String CITY_ID_FIELD_QUALIFIER = "Идентификатор города";
    public static final String LANG_CODE_FIELD_QUALIFIER = "Код языка";
    public static final String DURATION_FIELD_QUALIFIER = "Продолжительность";
    public static final String TIME_FIELD_QUALIFIER = "Время";
    public static final String DAYS_FIELD_QUALIFIER = "Дни";
    public static final String ONLINE_FIELD_QUALIFIER = "Онлайн";
    public static final String RORATE_FIELD_QUALIFIER = "Рораты";
    public static final String PARISH_ID_FIELD_QUALIFIER = "Идентификатор парафии";
    public static final String DELETED_FIELD_QUALIFIER = "Удален";
    public static final String NOTES_FIELD_QUALIFIER = "Примечания";
    public static final String SINGLE_START_TIMESTAMP_FIELD_QUALIFIER = "Начало времени";
    public static final String LAST_MODIFIED_DATE_FIELD_QUALIFIER = "Дата последнего изменения";
    public static final String START_DATE_FIELD_QUALIFIER = "Дата начала";
    public static final String END_DATE_FIELD_QUALIFIER = "Дата окончания";

    private String cityId;
    private String langCode;
    private String time;
    private int[] days;
    private String parishId;
    private long singleStartTimestamp;

    @Override
    public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
        if (fieldName.equals(IDENTIFIER_FIELD_QUALIFIER)) {
            return jsonNode.put("id", (String) fieldValue);
        }
        if (fieldName.equals(CITY_ID_FIELD_QUALIFIER)) {
            return jsonNode.put("cityId", (String) fieldValue);
        }
        if (fieldName.equals(LANG_CODE_FIELD_QUALIFIER)) {
            return jsonNode.put("langCode", (String) fieldValue);
        }
        if (fieldName.equals(DURATION_FIELD_QUALIFIER)) {
            return jsonNode.put("duration", (String) fieldValue);
        }
        if (fieldName.equals(TIME_FIELD_QUALIFIER)) {
            return jsonNode.put("time", (String) fieldValue);
        }
        if (fieldName.equals(DAYS_FIELD_QUALIFIER)) {
            ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
            Arrays.stream(StringUtils.split((String) fieldValue, ","))
                    .toList()
                    .forEach(arrayNode::add);
            jsonNode.set("days", arrayNode);
            return jsonNode;
        }
        if (fieldName.equals(ONLINE_FIELD_QUALIFIER)) {
            jsonNode.put("online", (String) fieldValue);
            return jsonNode;
        }
        if (fieldName.equals(RORATE_FIELD_QUALIFIER)) {
            jsonNode.put("rorate", (String) fieldValue);
            return jsonNode;
        }
        if (fieldName.equals(PARISH_ID_FIELD_QUALIFIER)) {
            return jsonNode.put("parishId", (String) fieldValue);
        }
        if (fieldName.equals(DELETED_FIELD_QUALIFIER)) {
            return jsonNode.put("deleted", (String) fieldValue);
        }
        if (fieldName.equals(NOTES_FIELD_QUALIFIER)) {
            return jsonNode.put("notes", (String) fieldValue);
        }
        if (fieldName.equals(SINGLE_START_TIMESTAMP_FIELD_QUALIFIER)) {
            return jsonNode.put("singleStartTimestamp", (String) fieldValue);
        }
        if (fieldName.equals(LAST_MODIFIED_DATE_FIELD_QUALIFIER)) {
            return jsonNode.put("lastModifiedDate", (String) fieldValue);
        }
        if (fieldName.equals(START_DATE_FIELD_QUALIFIER)) {
            return jsonNode.put("startDate", (String) fieldValue);
        }
        if (fieldName.equals(END_DATE_FIELD_QUALIFIER)) {
            return jsonNode.put("endDate", (String) fieldValue);
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели службы");
    }
}