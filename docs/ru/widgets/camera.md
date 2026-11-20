# Сканировать QR-коды изделий

[Catalog](../../WIDGET_GALLERY.ru.md) · [en](../../en/widgets/camera.md)

## QrScanner

Включает камеру только по нажатию и распознаёт кадры локально. Принимает заданный префикс ссылки и UUID. В серверном обработчике проверьте код и права, затем подтвердите событие через `setPaused(false)`. На время подтверждения используйте `setPaused(true)`. Нужен HTTPS или localhost. Закрытие или скрытие сканера останавливает камеру.

```java
final Map<String, String> messages = Map.of(
    "start", "Start camera", "stop", "Stop camera",
    "ready", "Point the camera at a product QR code",
    "scanning", "Scanning", "paused", "Confirm the action",
    "denied", "Allow camera access", "missing", "Camera not found",
    "insecure", "HTTPS is required", "unavailable", "Camera unavailable",
    "invalid", "Use a product QR code");
final QrScanner[] scanner = new QrScanner[1];
scanner[0] = new QrScanner("https://shop.example/product?id=", messages, code -> {
    System.out.println(code);
    scanner[0].setPaused(false);
});
root.add(scanner[0]);
```

[Java](../../../src/main/java/com/kniazkov/widgets/view/QrScanner.java) · [Screenshot](#shot-qr-scanner)

<a id="shot-qr-scanner"></a>

![QrScanner](../../images/widgets/qr-scanner.png)
