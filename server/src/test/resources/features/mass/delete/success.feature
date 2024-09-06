#language: ru
#encoding: UTF-8
#noinspection NonAsciiCharacters

Функционал: REST-API приложения imshaby-api. Удаление Служб

  Как авторизованный пользователь API,
  Я хочу быть уверен, что приложение принимает валидные запросы для удаления Служб

  Предыстория:
    Допустим для авторизации используется api-токен

  Сценарий: Создаем парафию для последующих манипуляций
    Когда подготавливаем запрос Создание парафии
    И поле "Статус" заполнено значением "PENDING"
    И поле "Наименование" заполнено значением "ПолучениеПарафииУспех_ФШДжвлф1"
    И поле "Идентификатор города" заполнено уникальным значением
    И поле "Идентификатор пользователя" заполнено уникальным значением
    И поле "Ключ" заполнено уникальным значением
    Затем выполняем запрос

    Тогда код ответа равен 201
    И сохранить "Идентификатор" из ответа с ключом parishIdForDeleteMass

  Сценарий: Создаем Службу для последующих манипуляций
    Когда подготавливаем запрос Создание службы
    И поле "Идентификатор города" заполнено уникальным значением
    И поле "Код языка" заполнено значением "123"
    И поле "Время" заполнено значением "23:59"
    И поле "Дни" заполнено значением "1"
    И поле "Идентификатор парафии" заполнено уникальным значением
    Затем выполняем запрос

    Тогда код ответа равен 201
    И сохранить "Идентификатор" из ответа с ключом deleteMass_id

  Сценарий: Отправляем запрос на удаление Службы
    Когда подготавливаем запрос Удаление службы
    И с переменными
      | идентификатор | [ключ] deleteMass_id |
    Затем выполняем запрос

    Тогда код ответа равен 200
    И полученное поле "Идентификатор" заполнено значением "[ключ] deleteMass_id"
    И полученное поле "Статус" заполнено значением "DELETED"

  Сценарий: Пытаемся получить службу по идентификатору которая была удалена
    Когда подготавливаем запрос Получение службы
    И с переменными
      | идентификатор | [ключ] deleteMass_id |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

  Сценарий: Создаем Службу для последующих манипуляций
    Когда подготавливаем запрос Создание службы
    И поле "Идентификатор города" заполнено уникальным значением
    И поле "Код языка" заполнено значением "123"
    И поле "Время" заполнено значением "23:59"
    И поле "Дни" заполнено значением "1"
    И поле "Идентификатор парафии" заполнено значением "[ключ] parishIdForDeleteMass"
    Затем выполняем запрос

    Тогда код ответа равен 201
    И сохранить "Идентификатор" из ответа с ключом deleteMassByParishId_id

  Сценарий: Отправляем запрос на удаление Службы
    Когда подготавливаем запрос Удаление службы по идентификатору парафии
    И с параметрами
      | идентификатор парафии | [ключ] parishIdForDeleteMass |
    Затем выполняем запрос

    Тогда код ответа равен 200
    И полученный массив "Идентификаторы" содержит в себе значение "[ключ] deleteMassByParishId_id"
    И полученное поле "Статус" заполнено значением "DELETED"

  Сценарий: Пытаемся получить службу по идентификатору которая была удалена
    Когда подготавливаем запрос Получение службы
    И с переменными
      | идентификатор | [ключ] deleteMassByParishId_id |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

#//////////////////////////////////////////////////////////////////////////////////////////////////////////

  Сценарий: Создаем Службу для последующих манипуляций
    Когда подготавливаем запрос Создание службы
    И поле "Идентификатор города" заполнено уникальным значением
    И поле "Код языка" заполнено значением "123"
    И поле "Время" заполнено значением "23:59"
    И поле "Дни" заполнено значением "1"
    И поле "Идентификатор парафии" заполнено уникальным значением
    И поле "Дата начала" заполнено значением "06/24/2024"
    И поле "Дата окончания" заполнено значением "06/30/2024"
    Затем выполняем запрос

    Тогда код ответа равен 201
    И сохранить "Идентификатор" из ответа с ключом deleteMassByTimeInterval1_id

  Сценарий: Отправляем запрос на удаление Службы
    Когда подготавливаем запрос Удаление служб за интервал времени
    И с переменными
      | идентификатор | [ключ] deleteMassByTimeInterval1_id |
    И с параметрами
      | начало интервала дат | 24-06-2024 |
      | конец интервала дат | 26-06-2024 |
    Затем выполняем запрос

    Тогда код ответа равен 200
#    И полученное поле "Идентификатор" заполнено значением "[ключ] deleteMassByTimeInterval1_id"
    И полученное поле "Статус" заполнено значением "[DELETED]"

  Сценарий: Пытаемся получить службу по идентификатору которая была удалена
    Когда подготавливаем запрос Получение службы
    И с переменными
      | идентификатор | [ключ] deleteMassByTimeInterval1_id |
    Затем выполняем запрос

    Тогда код ответа равен 404
    И в ответе содержится время ошибки

    #//////////////////////////////////////////////////////////////////////////////////////////////////////////

  Сценарий: Создаем Службу для последующих манипуляций
    Когда подготавливаем запрос Создание службы
    И поле "Идентификатор города" заполнено уникальным значением
    И поле "Код языка" заполнено значением "123"
    И поле "Время" заполнено значением "23:59"
    И поле "Дни" заполнено значением "4"
    И поле "Идентификатор парафии" заполнено уникальным значением
    И поле "Дата начала" заполнено значением "06/24/2024"
    И поле "Дата окончания" заполнено значением "06/30/2024"
    Затем выполняем запрос

    Тогда код ответа равен 201
    И сохранить "Идентификатор" из ответа с ключом deleteMassByTimeInterval2_id

  Сценарий: Отправляем запрос на удаление Службы за интервал времени
    Когда подготавливаем запрос Удаление служб за интервал времени
    И с переменными
      | идентификатор | [ключ] deleteMassByTimeInterval2_id |
    И с параметрами
      | начало интервала дат | 24-06-2024 |
      | конец интервала дат | 26-06-2024 |
    Затем выполняем запрос

    Тогда код ответа равен 200
#    И полученное поле "Идентификатор" заполнено значением "[ключ] deleteMassByTimeInterval2_id"
    И полученное поле "Статус" заполнено значением "[UPDATED]"

  Сценарий: Проверяем Службу которой сместили дату начала при удалении по периоду
    Когда подготавливаем запрос Получение службы
    И с переменными
      | идентификатор | [ключ] deleteMassByTimeInterval2_id |
    Затем выполняем запрос

    Тогда код ответа равен 200
    И полученное поле "Дата начала" заполнено значением "06/27/2024"

    #//////////////////////////////////////////////////////////////////////////////////////////////////////////

  Сценарий: Создаем Службу для последующих манипуляций
    Когда подготавливаем запрос Создание службы
    И поле "Идентификатор города" заполнено уникальным значением
    И поле "Код языка" заполнено значением "123"
    И поле "Время" заполнено значением "23:59"
    И поле "Дни" заполнено значением "1,7"
    И поле "Идентификатор парафии" заполнено уникальным значением
    И поле "Дата начала" заполнено значением "06/24/2024"
    И поле "Дата окончания" заполнено значением "06/30/2024"
    Затем выполняем запрос

    Тогда код ответа равен 201
    И сохранить "Идентификатор" из ответа с ключом deleteMassByTimeInterval3_id

  Сценарий: Отправляем запрос на удаление Службы
    Когда подготавливаем запрос Удаление служб за интервал времени
    И с переменными
      | идентификатор | [ключ] deleteMassByTimeInterval3_id |
    И с параметрами
      | начало интервала дат | 26-06-2024 |
      | конец интервала дат | 27-06-2024 |
    Затем выполняем запрос

    Тогда код ответа равен 200
    И полученное поле "Статус" заполнено значением "[UPDATED, CREATED]"