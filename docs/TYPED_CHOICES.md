# Typed choices

Use `ChoiceOption<T>` to associate a stable key, caption and server-side typed
value. Captions may repeat and order may change without moving the selection.

```java
record Product(int id) {}
var paper = new ChoiceOption<>("paper-17", "Paper", new Product(17));
var other = new ChoiceOption<>("paper-23", "Paper", new Product(23));
var select = new Select<>(List.of(paper, other));
select.setAccessibleName("Product");
select.setPlaceholder("Choose a product");
select.onChoose(keys -> save(select.getSelectedValues()));
select.setOptions(List.of(other, paper));
```

- `Select<T>` (native single-choice dropdown) lets users clear via the
  placeholder option. `MultiSelect<T>` (native multiple list) uses platform
  gestures; pair it with an explicit button invoking `clear()` if needed.
  Its placeholder is an advisory title, never a selectable key.
- `getSelectionModel()` is reactive and copies caller lists, and
  `getSelectedValues()` resolves keys to the current typed values. Replacing
  options keeps enabled keys that still exist. The model rejects unknown,
  disabled or duplicate keys, and an option snapshot rejects duplicate keys.
- A strict `ComboBox<T>(options, false)` never commits the query as a value.
  Selecting a real option gives a typed value, while dismissing restores the
  selected caption (if any). In free-text mode, `getFreeText()` is separate
  from selected values; editing deselects the key. The clear button or
  Ctrl+Delete when closed clears both states.
- Name each control. The composite uses combobox/listbox/option semantics,
  active descendant and a live status for loading, empty results and errors
  (built-in status messages are English). Arrow keys navigate enabled results,
  Enter chooses, Escape cancels and Tab can leave. IME search waits until
  composition ends.
- The optional provider returns a `CompletableFuture<ComboBox.Page<T>>`
  promptly. The first cursor is empty; the next cursor is provider-owned;
  return an empty next cursor for the final page. A newer query, cancellation
  or disposal asks the future to cancel and discards any late result even if
  the provider cannot stop. Scope options to 512 per accumulated snapshot,
  including any retained choice, with distinct stable keys (128 UTF-16 units)
  and search inputs up to 256 units. Result filtering retains committed
  choices; explicitly replace the catalogue with `setOptions` to drop them.
- Existing `DropDownList`, `SuggestionField` and `FormField` retain
  their APIs. Use typed controls separately from the current
  `InputField`-based FormField; use the application value for validation and
  call `setValidState(false)` to expose invalid state on the control.

See the [real browser examples](en/widgets/choices.md#shot-typed-choices-wide).
