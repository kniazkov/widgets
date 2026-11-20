# Scan product QR codes

[Catalog](../../WIDGETS.md) · [ru](../../ru/widgets/camera.md)

## QrScanner

Starts the camera only after a click and decodes frames locally. Accepts the configured URL prefix followed by a UUID. Validate the code and permissions in the server handler, then acknowledge with `setPaused(false)`. Use `setPaused(true)` while awaiting confirmation. HTTPS or localhost is required. Closing or hiding the scanner stops the camera.

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
