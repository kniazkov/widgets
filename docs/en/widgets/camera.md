# Scan QR codes

[Catalog](../../WIDGETS.md) · [ru](../../ru/widgets/camera.md)

## QrScanner

The scanner renders only video and starts disabled. Use the standard disabled property:
`enable()` requests camera access, `disable()` stops decoding and releases the camera, and
`setDisabledFlag(boolean)` or `getDisabledStateModel()` provides reactive control.
Buttons, instructions, results and localized status messages belong to the application.
Register `onStatus` to receive camera states such as `READY`, `DENIED` and `UNAVAILABLE`.

The one-argument constructor accepts arbitrary QR text, including packaging links and Wi-Fi data.
The constructor with a string prefix retains the optional URL-prefix-plus-UUID filter.
Validate codes and permissions in the server handler, then acknowledge with `setPaused(false)`.
Use `setPaused(true)` while awaiting confirmation: it pauses recognition but keeps the camera open.
Acknowledging a code never enables a disabled camera. HTTPS or localhost is required.
Hiding the page stops the camera; removing the scanner or disposing the client releases it too.
Late permission grants after disabling or removing the scanner are immediately released.

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

Render QR results as text; do not execute them or automatically open URLs. Map status enum values
to your own localized messages, as shown in `AllWidgets`. On errors or `CLOSED`, disable the
scanner before offering another enable attempt.

The old constructors accepting a message map remain source-compatible, but no longer render
those messages or a built-in button. Existing callers must add their own controls and enable
the scanner explicitly.

[Java](../../../src/main/java/com/kniazkov/widgets/view/QrScanner.java) · [Screenshot](#shot-qr-scanner)

<a id="shot-qr-scanner"></a>

![QrScanner with external controls](../../images/widgets/qr-scanner.png)
