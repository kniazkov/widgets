# Как проверить Widgets на облачном компьютере

Для приёмки собираем чистую ветку и отдельно проверяем работу в настоящем браузере
того же компьютера. Успех jsdom или старого CI не заменяет ни текущий браузерный
прогон, ни ручную приёмку. Результаты Windows относятся к CI текущего PR.

## Штатная сборка и автопроверки

На Linux нужны JDK 21, Maven, Node.js не ниже версии из `package.json` и браузеры,
которые устанавливает Playwright. Не переносите в репозиторий настройки прокси,
хранилищ сертификатов и другие данные окружения.

```bash
git status --short                            # чистый checkout
git rev-parse HEAD 'HEAD^{tree}'               # SHA коммита и дерева в отчёт
java -version; mvn -version
node --version; npm --version
npm ci
npx playwright --version
npx playwright install --with-deps chromium webkit
mvn -B -ntp clean verify
npm run test:browser
```

Запишите версии обеих сборок браузеров из
`node_modules/playwright-core/browsers.json` и версию открытого вручную браузера.
Сохраните stdout, stderr и код выхода отдельно для Maven и npm. Если отправляете
вывод через `tee`, в Bash сначала включите `set -o pipefail`; иначе статус
`tee` может скрыть падение тестов. В отчёте отдельно укажите Java-тесты,
Checkstyle, ESLint, Prettier, проверку каталога, JS-тесты и Playwright по проектам.
`webkit-suggestions` действительно запускает WebKit, но только для файлов из
списка `testMatch` в `playwright.config.js`: это не полная копия Chromium-проекта.
Если хост не поддерживает один из движков, так и отметьте; прямой прогон
доступного движка не считается успехом полного `npm run test:browser`.

Для работы над отдельным тестом runner принимает параметры Playwright после `--`:

```bash
npm run test:e2e -- cloud-verification.spec.js
```

Без аргументов runner, как и прежде, собирает и запускает оба локальных Java-сервера,
выполняет все настроенные браузерные тесты и останавливает серверы. Проверка с
фильтром не заменяет полный прогон.

### Проверка кода выхода

В **отдельной временной копии** того же коммита, где уже установлен npm-пакетный
набор, создайте `src/test/e2e/probe-runner-exit.spec.js`:

```js
import { expect, test } from "@playwright/test";
test("intentional exit probe", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByText("Waiting for browser event", { exact: true })).toBeVisible();
    expect(1, "INTENTIONAL_E2E_RUNNER_EXIT_PROBE").toBe(2);
});
```

```bash
exit_code=0
npm run test:e2e -- --project=chromium probe-runner-exit.spec.js >probe.log 2>&1 || exit_code=$?
test "$exit_code" -ne 0
grep -F 'INTENTIONAL_E2E_RUNNER_EXIT_PROBE' probe.log
```

Убедитесь, что браузер дошёл до намеренного assertion, а лог сообщает об одном
падении Playwright и ненулевом коде дочернего процесса runner. Ошибка установки,
сборки или запуска сервера такой проверкой не считается. Не переносите временный
тест в рабочую ветку и не подмешивайте его логи к зелёному полному прогону.

## Ручной проход AllWidgets

Соберите проверяемый коммит и запустите на том же облачном компьютере:

```bash
./run.sh com.kniazkov.widgets.example.AllWidgets 18087
```

Откройте `http://127.0.0.1:18087/` в настоящем браузере облачного компьютера.
Если порт занят, выберите свободный, не меняйте сеть и не выводите тестовый сервер
наружу. Автотесты используют ту же галерею по пути `/all-widgets` в E2E-сервере.

1. В обычном окне нажмите **Change separator color**, убедитесь, что цвет линии
   изменился в ответ на обработчик Java.
2. В **SuggestionField** введите `rhodium`, нажмите **↓**, затем **Enter**.
   В поле должно стать `Rhodium-plated brass`.
3. Нажмите **Show message**, проверьте текст сообщения, нажмите **Close**.
4. Перетяните первый слайд **Carousel** влево: подпись от сервера меняется с
   `Selected image: 1` на `Selected image: 2`.
5. В **FileLoader** нажмите **Choose file** и выберите маленький искусственный
   файл без личных данных. Проверьте `File uploaded: <имя> (<размер> bytes)`:
   сообщение подтверждает завершение загрузки на сервере, а не только выбор файла.
6. Сузьте окно до мобильной ширины, перезагрузите галерею, проверьте читаемость
   и повторите взаимодействие с popup. Укажите, если узкое окно моделирует размер,
   но не мобильное устройство. Сенсорный ввод и мобильный viewport проверяются
   отдельно в `cloud-verification.spec.js`; не называйте эмуляцию ручным тач-тестом.

В отчёте перечислите наблюдавшиеся результаты и непройденные действия. Для
нового видимого виджета приложите сделанный в реальном облачном браузере и
вырезанный скриншот согласно [правилам каталога](../../CONTRIBUTING.md#development-workflow).
