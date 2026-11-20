# Validate and save a form

[MVC in one click](mvc-counter.md) · [Русский](../ru/form-models.md)

Build a form for a phone extension. The form checks the text as you type. Even when you have
entered all four digits correctly, **Saved extension** still shows the previous value.
It changes when you click **Save** or press Enter.

## Run the form

1. Open the Maven project from the [quick start](quick-start.md). If the counter is running, stop it with Ctrl+C.
2. Save [ExtensionForm.java](../examples/ExtensionForm.java) as `src/main/java/example/ExtensionForm.java`.
3. In your `pom.xml`, change `example.Counter` to `example.ExtensionForm` inside `<mainClass>`.
4. Run `mvn compile exec:java` and open [localhost:8080](http://localhost:8080/).

Try these steps in order. The message **Enter exactly 4 digits (0–9).** tells you how to correct the input.

| Action | Message | Save button | Saved extension |
| --- | --- | --- | --- |
| Open the form with the field empty | Visible | Disabled | Empty |
| Type `12a4` | Visible | Disabled | Empty |
| Replace the text with `0042` | Hidden | Enabled | Empty |
| Click **Save** | Hidden | Enabled | `0042` |
| Clear the field and press Enter | Visible | Disabled | `0042` |
| Type `1234` and press Enter | Hidden | Enabled | `1234` |

## Where the text lives

The input and the result are bound to different models. `input.setTextModel(draft)` binds the
input to the draft in both directions. Typing in the browser updates `draft` on the server.
Calling `draft.setData("0042")` from Java would put those digits in the field. The result
uses `value.setTextModel(saved)`, so it continues to show the previously saved extension as
you edit.

`ExtensionModel` inherits string storage and change notifications from `DefaultModel<String>`.
Keeping the extension as text preserves leading zeros. The `isValid()` method defines the rule.

```java
@Override
public boolean isValid() {
    return FORMAT.matcher(getData()).matches();
}
```

`getData()` returns the current text, even when it is `12a4`. The `FORMAT` pattern checks
whether the entire string contains exactly four digits from 0 to 9. Calling
`draft.getValidFlagModel()` gives you a **derived model**. Its value follows the result of
that check whenever the draft changes. The message uses `setHiddenStateModel(valid)`, so a
valid extension hides it. The button uses `setDisabledStateModel(valid.invert())`, so an
invalid extension disables saving.

## When the extension is saved

The button’s controller is the lambda registered with `onClick`. Extracting the action into
a regular Java method lets you call it when the user presses Enter in the field, too.

```java
button.onClick(event -> save(draft, saved));
input.onEnter(event -> save(draft, saved));
```

Both handlers check the draft in one place on the server.

```java
private static void save(ExtensionModel draft, StringModel saved) {
    if (draft.isValid()) {
        saved.setData(draft.getData());
    }
}
```

The save happens at `saved.setData(...)`. It copies the validated extension and notifies the
bound label. In this example both models live in memory for one page instance. If the number
must survive an application restart, write it to your storage in this handler and update
`saved` after the write succeeds.

**Try changing the rule.** Change `DIGITS` from `4` to `5`, restart the application and
reopen the form. The message now asks for five digits. Typing `0042` still leaves the button
disabled. Enter `00042` and click **Save** to see all five digits in the result.

Widgets in this example · [InputField](widgets/forms.md#inputfield) · [Label](widgets/forms.md#label) · [Button](widgets/display.md#button)

[Next: build a page from cards](page-composition.md)
