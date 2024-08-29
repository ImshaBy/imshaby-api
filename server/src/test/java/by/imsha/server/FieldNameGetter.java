package by.imsha.server;

public interface FieldNameGetter {

    String getFieldName(String fieldQualifier);

    class Default implements FieldNameGetter {

        @Override
        public String getFieldName(String fieldQualifier) {
            throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа");
        }
    }
}
