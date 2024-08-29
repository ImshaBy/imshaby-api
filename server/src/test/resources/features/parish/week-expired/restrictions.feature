#language: ru
#encoding: UTF-8
#noinspection NonAsciiCharacters

Функционал: REST-API приложения imshaby-api. Получение списка парафий имеющие неактуальные Службы в определённую неделю

  Как авторизованный пользователь API,
  Я хочу быть уверен, что приложение отклоняет невалидные запросы для получения списка парафий имеющие неактуальные Службы

  Предыстория:
    Допустим для авторизации используется api-токен

  Сценарий: Отправляем запрос содержащий дату с неверным форматом
    Когда подготавливаем запрос Получение списка парафий имеющие неактуальные Службы
    И с параметрами
      | дата | 2018-06-23 |
    Затем выполняем запрос

    Тогда код ответа равен 400
    И в ответе содержится время ошибки