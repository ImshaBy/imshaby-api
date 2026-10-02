package by.imsha.rest.passwordless.mapper;

import by.imsha.properties.FusionauthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Выбор приложения FusionAuth для /api/passwordless/start: от него зависит шаблон письма.
 */
class PasswordlessAdapterMapperTest {

    private static final String DEFAULT_APP = "default-application-id";
    private static final String BACKOFFICE_APP = "backoffice-application-id";

    private PasswordlessAdapterMapper mapper;

    @BeforeEach
    void setUp() {
        final FusionauthProperties properties = new FusionauthProperties();
        properties.setApplicationId(DEFAULT_APP);
        properties.setAllowedApplicationIds(List.of(BACKOFFICE_APP));

        // Абстрактный класс: настоящие методы вызываем, генерируемые MapStruct не нужны.
        mapper = Mockito.mock(PasswordlessAdapterMapper.class, Mockito.CALLS_REAL_METHODS);
        mapper.passwordlessApiProperties = properties;
    }

    @Test
    void usesDefaultApplicationWhenNoneRequested() {
        assertThat(mapper.resolveApplicationId(null)).isEqualTo(DEFAULT_APP);
        assertThat(mapper.resolveApplicationId("")).isEqualTo(DEFAULT_APP);
        assertThat(mapper.resolveApplicationId("  ")).isEqualTo(DEFAULT_APP);
    }

    @Test
    void acceptsTheDefaultApplicationExplicitly() {
        assertThat(mapper.resolveApplicationId(DEFAULT_APP)).isEqualTo(DEFAULT_APP);
    }

    @Test
    void acceptsAnAllowedApplication() {
        assertThat(mapper.resolveApplicationId(" " + BACKOFFICE_APP + " ")).isEqualTo(BACKOFFICE_APP);
    }

    @Test
    void rejectsAnApplicationOutsideTheList() {
        assertThatThrownBy(() -> mapper.resolveApplicationId("someone-elses-application"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    /**
     * Через сгенерированный MapStruct-класс, а не только метод: метод выбора приложения не должен
     * применяться к остальным строковым полям (email, код из письма).
     */
    @Test
    void generatedMapperTouchesOnlyTheApplicationId() {
        final FusionauthProperties properties = new FusionauthProperties();
        properties.setApplicationId(DEFAULT_APP);
        properties.setAllowedApplicationIds(List.of(BACKOFFICE_APP));
        final PasswordlessAdapterMapperImpl generated = new PasswordlessAdapterMapperImpl();
        generated.passwordlessApiProperties = properties;

        final var start = new api_specification.by.imsha.server.passwordless.adapter.server.model.StartPasswordlessLoginRequest();
        start.setEmail("admin@imsha.by");
        start.setApplicationId(BACKOFFICE_APP);
        assertThat(generated.map(start).getLoginId()).isEqualTo("admin@imsha.by");
        assertThat(generated.map(start).getApplicationId()).isEqualTo(BACKOFFICE_APP);

        final var finish = new api_specification.by.imsha.server.passwordless.adapter.server.model.FinishPasswordlessLoginRequest();
        finish.setCode("123456789");
        assertThat(generated.map(finish).getCode()).isEqualTo("123456789");
    }
}
