# Local actions and IME

[Full contract, integration example and limitations (Russian)](LOCAL_ACTIONS_IME.ru.md).

Use `button.onClickOnClient(new Focus(editor))`, `ScrollIntoView`, `SelectText`,
`OpenPage` or `OpenPageInNewTab`. Local actions run synchronously in the click handler,
including keyboard button activation, through a strict allowlist separate from server updates.
Only HTTP(S)/relative navigation URLs are allowed. No code strings are evaluated.
Local UI effects do not authorize business operations and are not rolled back on server rejection.
A new tab may still be blocked by browser policy; delayed/server-triggered opening cannot
preserve the original user activation.

`InputField.setInputDebounce(0..2000)` configures trailing-edge delivery, immediate by default.
Committed text flushes before blur and business commands. Intermediate IME input stays local;
compositionend and the duplicate final input submit once. Deferred normalization cannot replace
newer unacknowledged input or move its caret. Disposal cancels scheduled input.

Run `com.kniazkov.widgets.example.LocalActions` for the integration example. Existing widgets
are reused without visual changes. Browser tests exercise composition event sequences; actual
OS IME/virtual keyboard behavior still requires manual verification on the target platform.
