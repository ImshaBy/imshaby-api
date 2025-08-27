package by.imsha.server;

import by.imsha.server.mapping.request.dto.CreateCityDtoMapping;
import by.imsha.server.mapping.request.dto.UpdateCityDtoMapping;
import by.imsha.server.mapping.request.dto.UpdateCityLocalizationDtoMapping;
import by.imsha.server.mapping.request.dto.auth.GenerateAndGetConfirmationCodeRequestDtoMapping;
import by.imsha.server.mapping.request.dto.auth.VerifyConfirmationCodeRequestDtoMapping;
import by.imsha.server.mapping.request.dto.mass.MassDtoMapping;
import by.imsha.server.mapping.request.dto.mass.RefreshMassDtoMapping;
import by.imsha.server.mapping.request.dto.parish.CreateParishDtoMapping;
import by.imsha.server.mapping.request.dto.parish.UpdateParishDtoMapping;
import by.imsha.server.mapping.request.dto.parish.UpdateParishLocalizationDtoMapping;
import by.imsha.server.mapping.request.dto.parish.UpdateParishStateDtoMapping;
import by.imsha.server.mapping.request.dto.passwordless.ExchangeAuthenticationCodeForTokenRequestDtoMapping;
import by.imsha.server.mapping.request.dto.passwordless.GenerateAndGetAuthenticationCodeRequestDtoMapping;
import by.imsha.server.mapping.request.dto.passwordless.GenerateAndSendAuthenticationCodeRequestDtoMapping;
import by.imsha.server.mapping.request.dto.webhook.CreateWebHookDtoMapping;
import by.imsha.server.mapping.response.dto.CreateCityResponseDtoMapping;
import by.imsha.server.mapping.response.dto.auth.VerifyConfirmationCodeResponseDtoMapping;
import by.imsha.server.mapping.response.dto.mass.ConfirmRelevanceMassResponseDtoMapping;
import by.imsha.server.mapping.response.dto.mass.DeleteMassByTimeIntervalResponseDtoMapping;
import by.imsha.server.mapping.response.dto.mass.DeleteMassResponseDtoMapping;
import by.imsha.server.mapping.response.dto.mass.MassResponseDtoMapping;
import by.imsha.server.mapping.response.dto.mass.UpdateMassResponseDtoMapping;
import by.imsha.server.mapping.response.dto.parish.ConfirmRelevanceParishResponseDtoMapping;
import by.imsha.server.mapping.response.dto.parish.GetParishStateResponseDtoMapping;
import by.imsha.server.mapping.response.dto.parish.ParishResponseDtoMapping;
import by.imsha.server.mapping.response.dto.parish.UpdateParishResponseDtoMapping;
import by.imsha.server.mapping.response.dto.parish.WeekExpiredParishResponseDtoMapping;
import by.imsha.server.mapping.response.dto.passwordless.ExchangeAuthenticationCodeForTokenResponseDtoMapping;
import by.imsha.server.mapping.response.dto.passwordless.GenerateAndGetAuthenticationCodeResponseDtoMapping;
import by.imsha.server.mapping.response.dto.ping.PingResponseDtoMapping;
import by.imsha.server.mapping.response.dto.webhook.DeleteWebhookResponseDtoMapping;
import by.imsha.server.mapping.response.dto.webhook.GetAllWebHookResponseDtoMapping;
import by.imsha.server.mapping.response.dto.webhook.WebHookResponseDtoMapping;
import io.restassured.http.Method;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Перечисление со всеми endpoint приложения imshaby-api
 */
@Getter
public enum ImshabyApiEndpoint {
    CREATE_CITY("Создание города", "/api/cities", Method.POST, new CreateCityDtoMapping(),
            new CreateCityResponseDtoMapping()),
    GET_CITY("Получение города", "/api/cities/{идентификатор}", Method.GET, new FieldValueSetter.Default(),
            new FieldNameGetter.Default()),
    UPDATE_CITY("Редактирование города", "/api/cities/{идентификатор}", Method.PUT, new UpdateCityDtoMapping(),
            new FieldNameGetter.Default()),
    UPDATE_CITY_LOCALIZATION("Изменение локализации города", "/api/cities/{идентификатор}/lang/{локаль}",
            Method.PUT, new UpdateCityLocalizationDtoMapping(), new FieldNameGetter.Default()),
    DELETE_CITY("Удаление города", "/api/cities/{идентификатор}", Method.DELETE, new FieldValueSetter.Default(),
            new FieldNameGetter.Default()),
    GET_ALL_CITY("Просмотр списка городов", "/api/cities", Method.GET, new FieldValueSetter.Default(),
            new FieldNameGetter.Default(), RequestParam.PAGE, RequestParam.SiZE),
    CREATE_PARISH("Создание парафии", "/api/parish", Method.POST, new CreateParishDtoMapping(),
            new ParishResponseDtoMapping()),
    GET_PARISH("Получение парафии", "/api/parish/{идентификатор}", Method.GET, new FieldValueSetter.Default(),
            new ParishResponseDtoMapping()),
    GET_PARISH_STATE("Получение статуса парафии", "/api/parish/{идентификатор}/state", Method.GET, new FieldValueSetter.Default(),
            new GetParishStateResponseDtoMapping()),
    GET_PARISH_BY_USER_ID("Получение парафии по идентификатору пользователя", "/api/parish/user/{идентификатор пользователя}",
            Method.GET, new FieldValueSetter.Default(), new ParishResponseDtoMapping()),
    GET_ALL_PARISH("Получение списка парафий", "/api/parish", Method.GET, new FieldValueSetter.Default(),
            new ParishResponseDtoMapping(), RequestParam.FILTER, RequestParam.OFFSET, RequestParam.LIMIT, RequestParam.SORT),
    UPDATE_PARISH("Редактирование парафии", "/api/parish/{идентификатор}",
            Method.PUT, new UpdateParishDtoMapping(), new UpdateParishResponseDtoMapping()),
    UPDATE_PARISH_STATE("Редактирование статуса парафии", "/api/parish/{идентификатор}/state",
            Method.PUT, new UpdateParishStateDtoMapping(), new UpdateParishResponseDtoMapping()),
    UPDATE_PARISH_LOCALIZATION("Редактирование локализации парафии", "/api/parish/{идентификатор}/lang/{локаль}",
            Method.PUT, new UpdateParishLocalizationDtoMapping(), new UpdateParishResponseDtoMapping()),
    PARISH_CONFIRM_RELEVANCE("Подтверждение актуальности парафии", "/api/parish/{идентификатор}/confirm-relevance",
            Method.POST, new FieldValueSetter.Default(), new ConfirmRelevanceParishResponseDtoMapping()),
    DELETE_PARISH("Удаление парафии", "/api/parish/{идентификатор}", Method.DELETE, new FieldValueSetter.Default(),
            new UpdateParishResponseDtoMapping(), RequestParam.CASCADE),
    PARISH_WEEK_EXPIRED("Получение списка парафий имеющие неактуальные Службы", "/api/parish/week/expired",
            Method.GET, new FieldValueSetter.Default(), new WeekExpiredParishResponseDtoMapping(), RequestParam.DATE),
    CREATE_MASS("Создание службы", "/api/mass", Method.POST, new MassDtoMapping(),
            new MassResponseDtoMapping()),
    GET_MASS("Получение службы", "/api/mass/{идентификатор}", Method.GET, new FieldValueSetter.Default(),
            new MassResponseDtoMapping()),
    GET_ALL_MASS("Получение списка служб", "/api/mass", Method.GET, new FieldValueSetter.Default(),
            new MassResponseDtoMapping(), RequestParam.FILTER, RequestParam.OFFSET, RequestParam.LIMIT, RequestParam.SORT),
    GET_MASS_WEEK("Получение расписания служб", "/api/mass/week", Method.GET, new FieldValueSetter.Default(),
            new MassResponseDtoMapping(), RequestParam.DATE, RequestParam.PARISH_ID, RequestParam.ONLINE, RequestParam.RORATE,
            RequestParam.MASS_LANG, RequestParam.X_SHOW_PENDING),
    GET_MASS_WEEK_PARAM("Получение расписания служб по ключу парафии в параметрах", "/api/mass/parish-week", Method.GET, new FieldValueSetter.Default(),
            new MassResponseDtoMapping(), RequestParam.API_KEY),
    GET_MASS_WEEK_HEADER("Получение расписания служб по ключу парафии в заголовках", "/api/mass/parish-week", Method.GET, new FieldValueSetter.Default(),
            new MassResponseDtoMapping()),
    UPDATE_MASS("Редактирование службы", "/api/mass/{идентификатор}",
            Method.PUT, new MassDtoMapping(), new UpdateMassResponseDtoMapping()),
    REFRESH_MASS("Обновление службы", "/api/mass/refresh/{идентификатор}",
            Method.PUT, new RefreshMassDtoMapping(), new UpdateMassResponseDtoMapping()),
    // todo должны удалить и использовать /api/parish/{parishId}/confirm-relevance
    MASS_CONFIRM_RELEVANCE("Подтверждение актуальности служб (неактуально)", "/api/mass",
            Method.PUT, new FieldValueSetter.Default(), new ConfirmRelevanceMassResponseDtoMapping(), RequestParam.PARISH_ID),
    DELETE_MASS("Удаление службы", "/api/mass/{идентификатор}", Method.DELETE, new FieldValueSetter.Default(),
            new UpdateMassResponseDtoMapping()),
    DELETE_MASSES_BY_PARISH_ID("Удаление службы по идентификатору парафии", "/api/mass",
            Method.DELETE, new FieldValueSetter.Default(), new DeleteMassResponseDtoMapping(), RequestParam.PARISH_ID),
    DELETE_MASS_BY_TIME_INTERVAL("Удаление служб за интервал времени", "/api/mass/{идентификатор}", Method.DELETE, new FieldValueSetter.Default(),
            new DeleteMassByTimeIntervalResponseDtoMapping(), RequestParam.START_DATE_INTERVAL, RequestParam.END_DATE_INTERVAL),
    CREATE_CITY_WEBHOOK("Создание Webhook города", "/hook/city", Method.POST, new CreateWebHookDtoMapping(),
            new WebHookResponseDtoMapping()),
    CREATE_PARISH_WEBHOOK("Создание Webhook парафии", "/hook/parish", Method.POST, new CreateWebHookDtoMapping(),
            new WebHookResponseDtoMapping()),
    GET_WEBHOOK("Получение Webhook", "/hook/{идентификатор}", Method.GET, new FieldValueSetter.Default(),
            new WebHookResponseDtoMapping()),
    GET_ALL_WEBHOOK("Просмотр списка Webhook", "/hook", Method.GET, new FieldValueSetter.Default(),
            new GetAllWebHookResponseDtoMapping(), RequestParam.PAGE, RequestParam.SiZE),
    DELETE_WEBHOOK("Удаление Webhook", "/hook/{идентификатор}", Method.DELETE, new FieldValueSetter.Default(),
            new DeleteWebhookResponseDtoMapping()),
    GET_PING("Получение ping", "/", Method.GET, new FieldValueSetter.Default(),
            new PingResponseDtoMapping()),
    GENERATE_AND_GET_PASSWORDLESS_CODE("Генерация и получение кода аутентификации", "/api/passwordless/code",
            Method.POST, new GenerateAndGetAuthenticationCodeRequestDtoMapping(), new GenerateAndGetAuthenticationCodeResponseDtoMapping()),
    GENERATE_AND_SEND_PASSWORDLESS_CODE("Генерация и отправка кода аутентификации на почту", "/api/passwordless/start",
            Method.POST, new GenerateAndSendAuthenticationCodeRequestDtoMapping(), new FieldNameGetter.Default()),
    EXCHANGE_PASSWORDLESS_CODE_FOR_TOKEN("Завершение процесса беспарольного входа", "/api/passwordless/login",
            Method.POST, new ExchangeAuthenticationCodeForTokenRequestDtoMapping(), new ExchangeAuthenticationCodeForTokenResponseDtoMapping()),
    GENERATE_AND_SEND_CONFIRMATION_CODE("Генерации и получения кода верификации по email", "/api/auth/request-code",
            Method.POST, new GenerateAndGetConfirmationCodeRequestDtoMapping(), new FieldNameGetter.Default()),
    VERIFY_CONFIRMATION_CODE("Верификация кода подтверждения", "/api/auth/verify-code",
            Method.POST, new VerifyConfirmationCodeRequestDtoMapping(), new VerifyConfirmationCodeResponseDtoMapping()),
    ;

    private static final Map<String, ImshabyApiEndpoint> cyrillicCodeEndpointMap;

    static {
        cyrillicCodeEndpointMap = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(ImshabyApiEndpoint::getCyrillicCode, Function.identity()));
    }

    private final String cyrillicCode;
    private final String path;
    private final Method method;
    private final Map<String, String> cyrillicCodeParamNameMap;
    private final FieldValueSetter fieldValueSetter;
    private final FieldNameGetter responseFieldNameGetter;

    ImshabyApiEndpoint(String cyrillicCode, String path, Method method, FieldValueSetter fieldValueSetter, FieldNameGetter responseFieldNameGetter, RequestParam... availableRequestParams) {
        this.cyrillicCode = cyrillicCode;
        this.path = path;
        this.method = method;
        this.fieldValueSetter = fieldValueSetter;
        this.responseFieldNameGetter = responseFieldNameGetter;
        this.cyrillicCodeParamNameMap = Arrays.stream(availableRequestParams)
                .collect(Collectors.toUnmodifiableMap(RequestParam::getCyrillicCode, RequestParam::getParamName));
    }

    public static ImshabyApiEndpoint fromCyrillicCode(final String cyrillicCode) {
        return cyrillicCodeEndpointMap.get(cyrillicCode);
    }

    public Map<String, String> validateAndGetRequestParams(final Map<String, String> params) {
        return params.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                entry -> ofNullable(cyrillicCodeParamNameMap.get(entry.getKey()))
                                        .orElseThrow(() -> new IllegalArgumentException("у запроса %s нет параметра %s"
                                                .formatted(this.cyrillicCode, entry.getKey()))),
                                Map.Entry::getValue)
                );
    }

    /**
     * Для внутреннего использования при описании эндпоинта
     * (маппинг имен параметров в виде енама)
     */
    @RequiredArgsConstructor
    @Getter
    private enum RequestParam {
        PAGE("номер страницы", "page"),
        SiZE("размер страницы", "size"),
        FILTER("фильтр", "filter"),
        OFFSET("номер страницы", "offset"),
        LIMIT("размер страницы", "limit"),
        SORT("сортировка", "sort"),
        CASCADE("каскад", "cascade"),
        DATE("дата", "date"),
        PARISH_ID("идентификатор парафии", "parishId"),
        API_KEY("ключ", "apiKey"),
        ONLINE("онлайн", "online"),
        RORATE("рораты", "rorate"),
        MASS_LANG("язык Службы", "massLang"),
        X_SHOW_PENDING("показать в статусе ожидания", "x-show-pending"),
        START_DATE_INTERVAL("начало интервала дат", "from"),
        END_DATE_INTERVAL("конец интервала дат", "to"),
        ;

        private final String cyrillicCode;
        private final String paramName;

    }
}
