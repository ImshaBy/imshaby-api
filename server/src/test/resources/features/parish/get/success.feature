#language: ru
#encoding: UTF-8
#noinspection NonAsciiCharacters

Функционал: REST-API приложения imshaby-api. Получение парафии

  Как авторизованный пользователь API,
  Я хочу быть уверен, что приложение принимает валидные запросы для получения паарафии

  Предыстория:
    Допустим для авторизации используется api-токен

  Сценарий: Создаем парафию для последующих манипуляций
    Когда подготавливаем запрос Создание парафии
    И поле "Статус" заполнено значением "PENDING"
    И поле "Наименование" заполнено значением "ПолучениеПарафииУспех_ФШДжвлф2"
    И поле "Идентификатор города" заполнено уникальным значением
    И поле "Идентификатор пользователя" заполнено уникальным значением и сохранено с ключом "get_api_parish_user_userId"
    И поле "Ключ" заполнено уникальным значением
    Затем выполняем запрос

    Тогда код ответа равен 201
    И сохранить "Идентификатор" из ответа с ключом getParish_id

  Сценарий: Получение парафии по идентификатору
    Когда подготавливаем запрос Получение парафии
    И с переменными
      | идентификатор | [ключ] getParish_id |
    Затем выполняем запрос

    Тогда код ответа равен 200
    И полученное поле "Идентификатор" заполнено значением "[ключ] getParish_id"

  Сценарий: Получение статуса парафии по идентификатору
    Когда подготавливаем запрос Получение статуса парафии
    И с переменными
      | идентификатор | [ключ] getParish_id |
    Затем выполняем запрос

    Тогда код ответа равен 200
    И полученное поле "Статус" заполнено значением "INITIAL"

  Сценарий: Получение парафии по идентификатору пользователя
    Когда подготавливаем запрос Получение парафии по идентификатору пользователя
    И с переменными
      | идентификатор пользователя | [ключ] get_api_parish_user_userId |
    Затем выполняем запрос

    Тогда код ответа равен 200
    И полученное поле "Идентификатор пользователя" заполнено значением "[ключ] get_api_parish_user_userId"

  Сценарий: Получение списка парафий
    Когда подготавливаем запрос Получение списка парафий
    И с параметрами
      | фильтр | state==INITIAL |
      | номер страницы | 0 |
      | размер страницы | 10 |
      | сортировка | +name |
    Затем выполняем запрос

    Тогда код ответа равен 200
    И полученный ответ является массивом с размером "1"