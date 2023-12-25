
[![Build Status](https://travis-ci.org/childRon/imshaby-api.svg?branch=master)](https://travis-ci.org/childRon/imshaby-api)

# imshaby-api

Для запуска необходимо добавлять параметр VM

`--add-opens java.base/java.nio.charset=ALL-UNNAMED`

Он необходим для mongo, т.к. там почему-то используется доступ к конструктору и попытка установить setAccessible(true).

