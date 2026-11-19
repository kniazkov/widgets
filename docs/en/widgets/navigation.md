# Navigate sections and hierarchies

[Widget catalog](../../WIDGETS.md) · [Русский](../../ru/widgets/navigation.md)

Tabs and Accordion select content within the current page. Breadcrumbs link to other pages, while TreeView loads a hierarchy as branches open.

[Page navigation](../page-navigation.md)

[Tabs](#tabs) · [Accordion](#accordion) · [Breadcrumbs](#breadcrumbs) · [TreeView](#treeview)

## Tabs

Switch between views in the same page. A stable key selects a tab through `getSelectedKeyModel()`. Its factory builds the content on first activation and retains it when another tab is shown.

```java
var tabs = new Tabs();
tabs.addTab("log", "Log", () -> new Section(new TextWidget("Observations")));
```

[Breadcrumbs, tabs and expandable sections](#shot-navigation-desktop) · [Java](../../../src/main/java/com/kniazkov/widgets/view/Tabs.java)

## Accordion

Let readers expand titled sections. The expanded-keys model holds a set. Choose single- or multiple-section mode at construction; hidden sections retain their content after the first opening.

```java
var faq = new Accordion(false);
faq.addSection("when", "When?", () -> new Section(new TextWidget("After sunset")));
```

[Breadcrumbs, tabs and expandable sections](#shot-navigation-desktop) · [Java](../../../src/main/java/com/kniazkov/widgets/view/Accordion.java)

## Breadcrumbs

Show the path to the current page. Ancestors are navigable links and the final item is the current-page label. Replace the ordered path with `setItems` when the location changes.

```java
var path = new Breadcrumbs("Location", List.of(
    new Breadcrumbs.Item("Home", "/"), new Breadcrumbs.Item("Log", "")));
```

[Breadcrumbs, tabs and expandable sections](#shot-navigation-desktop) · [Java](../../../src/main/java/com/kniazkov/widgets/view/Breadcrumbs.java)

## TreeView

Browse a hierarchy with stable node keys. Supply children asynchronously when a branch opens; read the selected key and expanded keys from their models. A leaf has no expansion action.

```java
var tree = new TreeView(List.of(new TreeView.Node("sky", "Sky", true)),
    key -> CompletableFuture.completedFuture(List.of(new TreeView.Node("moon", "Moon", false))),
    new TreeView.Messages("Loading", "Empty", "Try again"));
```

[Catalog hierarchy with expandable branches](#shot-tree-desktop) · [Java](../../../src/main/java/com/kniazkov/widgets/view/TreeView.java)

## AllWidgets screenshots

<a id="shot-navigation-desktop"></a>

**Breadcrumbs, tabs and expandable sections**

![Breadcrumbs, tabs and expandable sections](../../images/widgets/navigation-desktop.png)

<a id="shot-navigation-mobile-tabs"></a>

**Tabs and wrapping breadcrumbs in a narrow window**

![Tabs and wrapping breadcrumbs in a narrow window](../../images/widgets/navigation-mobile-tabs.png)

<a id="shot-navigation-mobile-accordion"></a>

**Expanded accordion in a narrow window**

![Expanded accordion in a narrow window](../../images/widgets/navigation-mobile-accordion.png)

<a id="shot-tree-desktop"></a>

**Catalog hierarchy with expandable branches**

![Catalog hierarchy with expandable branches](../../images/widgets/tree-desktop.png)

<a id="shot-tree-mobile"></a>

**Catalog tree on a narrow screen**

![Catalog tree on a narrow screen](../../images/widgets/tree-mobile.png)
