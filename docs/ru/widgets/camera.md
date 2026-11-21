# Сканирование QR-кодов

[Каталог](../../WIDGET_GALLERY.ru.md) · [en](../../en/widgets/camera.md)

## QrScanner

Сканер показывает только видео и изначально отключён. Управление — через стандартное
свойство disabled: `enable()` запрашивает доступ к камере, `disable()` останавливает
распознавание и освобождает камеру. Для реактивного управления есть
`setDisabledFlag(boolean)` и `getDisabledStateModel()`.
Кнопки, инструкции, результат и локализованные сообщения рисует приложение.
Обработчик `onStatus` получает состояния камеры, например `READY`, `DENIED`, `UNAVAILABLE`.

Конструктор с одним аргументом принимает любой QR-текст, включая ссылки с упаковки и Wi-Fi.
Конструктор со строковым префиксом сохраняет необязательный фильтр «префикс URL + UUID».
Проверяйте содержимое и права на сервере, затем подтверждайте код через `setPaused(false)`.
`setPaused(true)` приостанавливает распознавание, сохраняя камеру включённой.
Подтверждение кода не включает отключённую камеру. Нужен HTTPS или localhost.
Скрытие страницы останавливает камеру; удаление сканера и завершение клиента также её освобождают.
Если разрешение пришло уже после отключения или удаления сканера, поток сразу закрывается.

```java
final TextWidget result = new TextWidget("No QR received");
final TextWidget status = new TextWidget("Camera is disabled.");
final QrScanner[] scanner = new QrScanner[1];
scanner[0] = new QrScanner(code -> {
    result.setText(code);
    scanner[0].setPaused(false);
});
scanner[0].onStatus(state -> status.setText(state.name()));
final Button enable = new Button("Enable camera");
enable.onClick(event -> scanner[0].enable());
final Button disable = new Button("Disable camera");
disable.onClick(event -> {
    scanner[0].disable();
    status.setText("Camera is disabled.");
});
root.add(scanner[0]);
root.add(new Section(enable, disable));
root.add(new Section(status));
root.add(new Section(result));
```

Результат QR показывайте как текст: не исполняйте его и не открывайте ссылки автоматически.
Преобразуйте состояния enum в свои локализованные сообщения, как в `AllWidgets`.
При ошибках и `CLOSED` отключайте сканер перед новой попыткой включения.

Старые конструкторы с картой сообщений сохранены для совместимости исходного кода,
но встроенных сообщений и кнопки больше нет. Приложению нужно добавить собственное управление
и явно включать сканер.

[Java](../../../src/main/java/com/kniazkov/widgets/view/QrScanner.java) · [Screenshot](#shot-qr-scanner)

<a id="shot-qr-scanner"></a>

![QrScanner with external controls](../../images/widgets/qr-scanner.png)
