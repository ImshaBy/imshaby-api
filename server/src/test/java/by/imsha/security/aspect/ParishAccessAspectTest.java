package by.imsha.security.aspect;

import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.security.ParishAuthorizationService;
import by.imsha.security.annotation.RequireParishAccess;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParishAccessAspectTest {

    @Mock
    private ParishAuthorizationService parishAuthorizationService;

    @Mock
    private ParishService parishService;

    @Mock
    private MassService massService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private ParishAccessAspect parishAccessAspect;

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Test
    void shouldSkipCheckForInternalRole() {
        // Given
        RequireParishAccess annotation = createAnnotation("parishId", false, "parishId", "");
        when(parishAuthorizationService.isInternalRole()).thenReturn(true);

        // When/Then
        assertThatCode(() -> parishAccessAspect.checkParishAccess(joinPoint, annotation))
                .doesNotThrowAnyException();

        // Verify no parish service calls were made
        verify(parishService, never()).getParish(anyString());
        verify(parishAuthorizationService, never()).checkParishKeyAccess(anyString());
    }

    @Test
    void shouldCheckAccessByPathVariable() throws NoSuchMethodException {
        // Given
        String parishId = "parish123";
        String parishKey = "parish-key";
        RequireParishAccess annotation = createAnnotation("parishId", false, "parishId", "");
        
        Parish parish = new Parish();
        parish.setId(parishId);
        parish.setKey(parishKey);

        when(parishAuthorizationService.isInternalRole()).thenReturn(false);
        when(joinPoint.getArgs()).thenReturn(new Object[]{parishId});
        
        Method method = TestController.class.getMethod("updateParish", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        
        when(parishService.getParish(parishId)).thenReturn(Optional.of(parish));
        doNothing().when(parishAuthorizationService).checkParishKeyAccess(parishKey);

        // When
        parishAccessAspect.checkParishAccess(joinPoint, annotation);

        // Then
        verify(parishService).getParish(parishId);
        verify(parishAuthorizationService).checkParishKeyAccess(parishKey);
    }

    @Test
    void shouldThrowExceptionWhenParishNotFound() throws NoSuchMethodException {
        // Given
        String parishId = "nonexistent";
        RequireParishAccess annotation = createAnnotation("parishId", false, "parishId", "");

        when(parishAuthorizationService.isInternalRole()).thenReturn(false);
        when(joinPoint.getArgs()).thenReturn(new Object[]{parishId});
        
        Method method = TestController.class.getMethod("updateParish", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        
        when(parishService.getParish(parishId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> parishAccessAspect.checkParishAccess(joinPoint, annotation))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Приход не найден");
    }

    @Test
    void shouldCheckAccessFromRequestBody() throws NoSuchMethodException {
        // Given
        String parishId = "parish123";
        String parishKey = "parish-key";
        RequireParishAccess annotation = createAnnotation("parishId", true, "parishId", "");
        
        TestRequestBody requestBody = new TestRequestBody();
        requestBody.setParishId(parishId);
        
        Parish parish = new Parish();
        parish.setId(parishId);
        parish.setKey(parishKey);

        when(parishAuthorizationService.isInternalRole()).thenReturn(false);
        when(joinPoint.getArgs()).thenReturn(new Object[]{requestBody});
        
        Method method = TestController.class.getMethod("createWithBody", TestRequestBody.class);
        when(methodSignature.getMethod()).thenReturn(method);
        
        when(parishService.getParish(parishId)).thenReturn(Optional.of(parish));
        doNothing().when(parishAuthorizationService).checkParishKeyAccess(parishKey);

        // When
        parishAccessAspect.checkParishAccess(joinPoint, annotation);

        // Then
        verify(parishService).getParish(parishId);
        verify(parishAuthorizationService).checkParishKeyAccess(parishKey);
    }

    @Test
    void shouldCheckAccessByMassId() throws NoSuchMethodException {
        // Given
        String massId = "mass123";
        String parishId = "parish123";
        String parishKey = "parish-key";
        RequireParishAccess annotation = createAnnotation("massId", false, "parishId", "massId");
        
        Mass mass = new Mass();
        mass.setId(massId);
        mass.setParishId(parishId);
        
        Parish parish = new Parish();
        parish.setId(parishId);
        parish.setKey(parishKey);

        when(parishAuthorizationService.isInternalRole()).thenReturn(false);
        when(joinPoint.getArgs()).thenReturn(new Object[]{massId});
        
        Method method = TestController.class.getMethod("updateMass", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        
        when(massService.getMass(massId)).thenReturn(Optional.of(mass));
        when(parishService.getParish(parishId)).thenReturn(Optional.of(parish));
        doNothing().when(parishAuthorizationService).checkParishKeyAccess(parishKey);

        // When
        parishAccessAspect.checkParishAccess(joinPoint, annotation);

        // Then
        verify(massService).getMass(massId);
        verify(parishService).getParish(parishId);
        verify(parishAuthorizationService).checkParishKeyAccess(parishKey);
    }

    @Test
    void shouldThrowExceptionWhenMassNotFound() throws NoSuchMethodException {
        // Given
        String massId = "nonexistent";
        RequireParishAccess annotation = createAnnotation("massId", false, "parishId", "massId");

        when(parishAuthorizationService.isInternalRole()).thenReturn(false);
        when(joinPoint.getArgs()).thenReturn(new Object[]{massId});
        
        Method method = TestController.class.getMethod("updateMass", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        
        when(massService.getMass(massId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> parishAccessAspect.checkParishAccess(joinPoint, annotation))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Месса не найдена");
    }

    private RequireParishAccess createAnnotation(String parishIdParam, boolean fromRequestBody, 
                                                  String bodyField, String massIdParam) {
        return new RequireParishAccess() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return RequireParishAccess.class;
            }

            @Override
            public String parishIdParam() {
                return parishIdParam;
            }

            @Override
            public boolean fromRequestBody() {
                return fromRequestBody;
            }

            @Override
            public String bodyField() {
                return bodyField;
            }

            @Override
            public String massIdParam() {
                return massIdParam;
            }
        };
    }

    // Test controller class for method signatures
    static class TestController {
        public void updateParish(@PathVariable("parishId") String parishId) {}
        public void createWithBody(@RequestBody TestRequestBody body) {}
        public void updateMass(@PathVariable("massId") String massId) {}
    }

    // Test request body class
    static class TestRequestBody {
        private String parishId;

        public String getParishId() {
            return parishId;
        }

        public void setParishId(String parishId) {
            this.parishId = parishId;
        }
    }
}
