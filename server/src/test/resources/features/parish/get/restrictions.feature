#language: ru
#encoding: UTF-8
#noinspection NonAsciiCharacters

Функционал: REST-API приложения imshaby-api. Получение парафии

  Как авторизованный пользователь API,
  Я хочу быть уверен, что приложение отклоняет невалидные запросы для получения парафии

  Предыстория:
    Допустим для авторизации используется api-токен

  Сценарий: Получение парафии по отсутствующему в системе идентификатору
    Когда подготавливаем запрос Получение парафии
    И с переменными
      | идентификатор | 123 |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

  Сценарий: Получение статуса парафии по отсутствующему в системе идентификатору
    Когда подготавливаем запрос Получение статуса парафии
    И с переменными
      | идентификатор | 123 |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

  Сценарий: Получение парафии по отсутствующему в системе идентификатору пользователя
    Когда подготавливаем запрос Получение парафии по идентификатору пользователя
    И с переменными
      | идентификатор пользователя | 123 |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

  Сценарий: Получение списка парафий без обязательного параметра фильтр
    Когда подготавливаем запрос Получение списка парафий
    И с параметрами
      | номер страницы | 0 |
      | размер страницы | 10 |
      | сортировка | +name |
    Затем выполняем запрос

    Тогда код ответа равен 400
    И в ответе содержится время ошибки

  Сценарий: Получение списка парафий с невалидным фильтром
    Когда подготавливаем запрос Получение списка парафий
    И с параметрами
      | фильтр | state===PENDING |
      | номер страницы | 0 |
      | размер страницы | 10 |
      | сортировка | +name |
    Затем выполняем запрос

    Тогда код ответа равен 500
    И в ответе содержится время ошибки