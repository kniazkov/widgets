# Сканировать QR-коды

[Catalog](../../WIDGET_GALLERY.ru.md) · [en](../../en/widgets/camera.md)

## QrScanner

Включает камеру только по нажатию и распознаёт кадры локально. Конструктор с двумя аргументами принимает произвольный текст QR, в том числе ссылки с упаковок и настройки Wi-Fi. Результат показывается текстом, без исполнения или автоматического открытия ссылки. Конструктор с тремя аргументами сохраняет фильтр по префиксу ссылки и UUID. В серверном обработчике проверьте код и права, затем подтвердите событие через `setPaused(false)`. На время подтверждения используйте `setPaused(true)`. Нужен HTTPS или localhost. Закрытие или скрытие сканера останавливает камеру.

```java
final Map<String, String> messages = Map.of(
    "start", "Enable camera",
    "idle", "Allow camera access to scan any QR code.",
    "opening", "Opening camera…",
    "ready", "Show a QR code. Remove it before scanning it again.",
    "waiting", "Processing QR…",
    "https", "Camera access requires HTTPS.",
    "denied", "Camera permission was denied.",
    "unavailable", "Camera unavailable.",
    "invalid", "No readable QR text.",
    "closed", "Camera stopped. Reload to start again.");
final TextWidget result = new TextWidget("No QR received");
final QrScanner[] scanner = new QrScanner[1];
scanner[0] = new QrScanner(messages, code -> {
    result.setText(code);
    scanner[0].setPaused(false);
});
root.add(scanner[0]);
root.add(new Section(result));
```

[Java](../../../src/main/java/com/kniazkov/widgets/view/QrScanner.java) · [Screenshot](#shot-qr-scanner)

<a id="shot-qr-scanner"></a>

![QrScanner](../../images/widgets/qr-scanner.png)
