 # T-bankBalanceChecker
 
 ## Описание
**T-bankBalanceChecker** предназначен для опроса
состояния банковских счетов (Т-Банк) пользователя
и автоматичекого оповещения о снижении баланса на
контролируемых счетах ниже заданного порога.

Для запроса перечня счетов пользователей
используется REST API Т-Банк Инвестиции

 ## Сборка проекта

1. Клонировать репозиторий: ```git clone ...```
2. Задать [токен доступа](https://developer.tbank.ru/invest/intro/intro/token)
   к Invest API:
   - создать файл ```local.properties``` в корне проекта
   - добавить в него строку вида ```TBANK_INVEST_TOKEN=<Токен>```, где
   ```<токен>``` - токен доступа, полученный в личном кабинете пользователя
   Т-Ивестиции, путь к SDK Android, например
   ```sdk.dir=C\:\\Users\\User\\AppData\\Local\\Android\\Sdk```
    и порог для индикации пониженного баланса
   ```BALANCE_THRESHOLD_RUB=<порог>```
   
3. Открыть проект в Android Studio и нажать "Run"

Пример содержимого файла ```local.properties```:

```
sdk.dir=C\:\\Users\\User\\AppData\\Local\\Android\\Sdk

TBANK_INVEST_TOKEN=14235234346456ар

BALANCE_THRESHOLD_RUB=10000
```

 ## Требования и установка
 * Минимальные требования: ОС Android, версия не ниже 8.0 (Oreo)
 * Min SDK: API 26+