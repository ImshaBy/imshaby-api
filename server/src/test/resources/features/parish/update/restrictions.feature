#language: ru
#encoding: UTF-8
#noinspection NonAsciiCharacters

Функционал: REST-API приложения imshaby-api. Редактирование парафии

  Как авторизованный пользователь API,
  Я хочу быть уверен, что приложение отклоняет невалидные запросы для редактирования парафии

  Предыстория:
    Допустим для авторизации используется api-токен

  Сценарий: Отправляем запрос с пустым обязательным телом и отсутствующим в системе идентификатором
    Когда подготавливаем запрос Редактирование парафии
    И телом запроса
    """
    {}
    """
    И с переменными
      | идентификатор | 123 |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

  Сценарий: Отправляем запрос с отсутствующим в системе идентификатором
    Когда подготавливаем запрос Редактирование парафии
    И поле "Наименование" заполнено значением "Новое наименование парафии"
    И с переменными
      | идентификатор | 123 |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

  Сценарий: Отправляем запрос с пустым обязательным телом и отсутствующим в системе идентификатором
    Когда подготавливаем запрос Редактирование статуса парафии
    И телом запроса
    """
    {}
    """
    И с переменными
      | идентификатор | 123 |
    Затем выполняем запрос

    Тогда код ответа равен 400
    И в ответе содержится время ошибки
    И в ответе содержится ошибка с кодом PARISH.401

  Сценарий: Отправляем запрос с отсутствующим в системе идентификатором
    Когда подготавливаем запрос Редактирование статуса парафии
    И поле "Статус" заполнено значением "APPROVED"
    И с переменными
      | идентификатор | 123 |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

  Сценарий: Отправляем запрос с пустым обязательным телом и отсутствующим в системе идентификатором
    Когда подготавливаем запрос Редактирование локализации парафии
    И телом запроса
    """
    {}
    """
    И с переменными
      | идентификатор | 123 |
      | локаль | en |
    Затем выполняем запрос

    Тогда код ответа равен 400
    И в ответе содержится время ошибки
    И в ответе содержится ошибка с кодом PARISH.002

  Сценарий: Отправляем запрос с отсутствующим в системе идентификатором
    Когда подготавливаем запрос Редактирование локализации парафии
    И поле "Наименование" заполнено значением "наименование"
    И поле "Краткое наименование" заполнено значением "наим."
    И поле "Адрес" заполнено значением "адрес"
    И с переменными
      | идентификатор | 123 |
      | локаль | en |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

  Сценарий: Отправляем запрос с невальдной локалью и отсутствующим в системе идентификатором
    Когда подготавливаем запрос Редактирование локализации парафии
    И поле "Наименование" заполнено значением "наименование"
    И поле "Краткое наименование" заполнено значением "наим."
    И поле "Адрес" заполнено значением "адрес"
    И с переменными
      | идентификатор | 123 |
      | локаль | en123 |
    Затем выполняем запрос

    Тогда код ответа равен 400
    И в ответе содержится время ошибки
    И в ответе содержится ошибка с кодом PARISH.001