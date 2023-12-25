package by.imsha.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alena Misan
 */
public class ValidationError {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> errors = new HashMap<String, String>();

    private final String errorMessage;

    public ValidationError( String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void addValidationError(String errorMsg, String rootField) {
        errors.put(rootField, errorMsg);
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
