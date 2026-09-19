# Color strings

`Color.fromString(String)` parses concrete colors for configuration, widget properties and image
processing. It returns the same immutable eight-bit RGBA `Color` used by constructors; there is no
browser dependency. HEX alpha is **last**, whereas `Color.pack()` produces **ARGB** for image pixels.

| Format | Examples |
| --- | --- |
| Short HEX | `#abc`, `#abcd` (each digit is doubled) |
| Full HEX | `#C0935C`, `#C0935C80` (last byte is alpha) |
| RGB / RGBA, commas | `rgb(192,147,92)`, `rgba(192,147,92,0.5)` |
| RGB percentages | `rgb(100%,50%,0%)` |
| RGB / RGBA, spaces | `rgb(192 147 92)`, `rgb(192 147 92 / 50%)` |
| HSL / HSLA | `hsl(120,100%,50%)`, `hsla(120,100%,50%,0.5)` |
| HSL, spaces | `hsl(120deg 100% 50% / 50%)` |
| Existing names | `red`, `LIGHT_GRAY`, `LightGrey`, `transparent` |

For example, a configuration string `"#C0935C"` and `"rgb(192,147,92)"` both yield
`new Color(192, 147, 92)`.

## Rules and compatibility

- HEX digits, function names and predefined names are case-insensitive, independently of JVM locale.
  Leading/trailing whitespace and whitespace between function arguments are accepted.
- RGB accepts decimal numbers (including fractions and scientific notation) and percentages.
  Comma syntax requires all three RGB channels to use the same unit; space syntax permits mixing.
- `rgb`/`rgba` and `hsl`/`hsla` are aliases. Comma syntax accepts three or four arguments;
  space syntax accepts three channels and optional `/ alpha`. Do not mix separator styles.
- HSL saturation and lightness require percentages. Hue accepts a unitless degree value or `deg`,
  `grad`, `rad`, `turn`; negative values and full turns wrap around the color wheel.
- Alpha accepts a number (0–1) or percentage. RGB, saturation, lightness and alpha values outside
  their ranges are clamped. Conversion to eight-bit components rounds to the nearest integer.
- Existing predefined names retain their historical widgets values, including `green` = `(0,127,0)`
  and `darkgray` = `(64,64,64)`. This release does not add the full CSS named-color catalog.
- Null, empty, malformed and unsupported strings return `Color.BLACK`, as before. Non-finite
  numbers, extra arguments and trailing junk are rejected. Previously tolerated malformed comma
  strings with empty trailing arguments are now rejected as well.
- This is a supported subset of [CSS Color](https://www.w3.org/TR/css-color-4/), not a CSS evaluator.
  `currentColor`, `var()`, `calc()`, `none`, relative colors, HWB, Lab/LCH and `color()` are not
  supported. Serialization (`toString`, `toJsonObject`, `pack`) is unchanged.
