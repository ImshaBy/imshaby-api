package by.imsha.exception;

import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

/**
 * @author Alena Misan
 */
public class ValidationErrorBuilder {

    public static ValidationError fromBindingErrors(Errors errors) {
        ValidationError error = new ValidationError("Validation failed. " + errors.getErrorCount() + " error(s)");
        for (ObjectError objectError : errors.getAllErrors()) {
            objectError.getObjectName();
            error.addValidationError(objectError.getDefaultMessage(), objectError.getObjectName());
        }
        return error;
    }
    
    
}
