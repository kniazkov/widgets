/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Only widgets with requested relationships are tracked; no DOM references are stored as targets.
const accessibilityConsumers = new Set();

function semanticRoot() {
    return typeof page === "undefined" ? document.body : page.root;
}

function semanticTarget(source, property) {
    const id = source._accessibleReferences?.[property];
    const target = id && widgets[id];
    const root = semanticRoot();
    if (
        !target ||
        target === source ||
        !root?.contains(source) ||
        !root.contains(target) ||
        source._treePaused ||
        target._treePaused
    )
        return null;
    if (!target.id) target.id = "widgets-semantic-" + target._id;
    return target.id;
}

function assignSemanticAttribute(widget, attribute, value) {
    if (value) widget.setAttribute(attribute, value);
    else widget.removeAttribute(attribute);
}

function refreshAccessibleRelationsFor(widget) {
    const covered = widget._backdrop && widget.getAttribute("role") === "presentation";
    const label = covered ? null : semanticTarget(widget, "accessible label");
    const help = covered ? null : semanticTarget(widget, "accessible description widget");
    const ownDescription = covered ? null : widget._modalDescription?.id;
    const error =
        covered || !widget._states?.invalid ? null : semanticTarget(widget, "accessible error");
    assignSemanticAttribute(widget, "aria-labelledby", label);
    // Include active errors in describedby for assistive technology without errormessage support.
    const descriptions = new Set([help, ownDescription, error].filter(Boolean));
    assignSemanticAttribute(widget, "aria-describedby", [...descriptions].join(" "));
    assignSemanticAttribute(widget, "aria-errormessage", error);
}

function refreshAccessibleRelations() {
    for (const widget of accessibilityConsumers) refreshAccessibleRelationsFor(widget);
}

function forgetAccessibleWidget(widget) {
    accessibilityConsumers.delete(widget);
    for (const consumer of accessibilityConsumers) {
        for (const property of Object.keys(consumer._accessibleReferences)) {
            if (consumer._accessibleReferences[property] === widget._id) {
                delete consumer._accessibleReferences[property];
            }
        }
        if (Object.keys(consumer._accessibleReferences).length === 0) {
            accessibilityConsumers.delete(consumer);
            refreshAccessibleRelationsFor(consumer);
        }
    }
}

function setAccessibleReference(data, property) {
    const widget = widgets[data.widget];
    const target = data[property];
    if (
        !widget ||
        typeof target !== "string" ||
        (target !== "" && !/^#[1-9][0-9]*$/.test(target)) ||
        target === widget._id
    )
        return false;
    widget._accessibleReferences ??= Object.create(null);
    if (target) widget._accessibleReferences[property] = target;
    else delete widget._accessibleReferences[property];
    if (Object.keys(widget._accessibleReferences).length) accessibilityConsumers.add(widget);
    else accessibilityConsumers.delete(widget);
    refreshAccessibleRelationsFor(widget);
    return true;
}

function setWidgetAccessibleName(data) {
    const widget = widgets[data.widget];
    const name = data["accessible name"];
    if (!widget || typeof name !== "string") return false;
    if (widget._choiceType) return setChoiceAccessibleName(data);
    if (widget._backdrop) return setModalAccessibleName(widget, name);
    assignSemanticAttribute(widget, "aria-label", name.trim());
    return true;
}

function setWidgetAccessibleDescription(data) {
    const widget = widgets[data.widget];
    const description = data["accessible description"];
    if (!widget || typeof description !== "string") return false;
    if (widget._backdrop) return setModalAccessibleDescription(data);
    assignSemanticAttribute(widget, "aria-description", description.trim());
    return true;
}

function setHeadingLevel(data) {
    const widget = widgets[data.widget];
    const level = data["heading level"];
    if (!widget?._headingCapable || !Number.isInteger(level) || level < 0 || level > 6) {
        return false;
    }
    assignSemanticAttribute(widget, "role", level ? "heading" : null);
    assignSemanticAttribute(widget, "aria-level", level);
    return true;
}

function refreshImageAlternative(widget) {
    widget.alt = widget._decorativeImage ? "" : widget._alternativeText || "";
    assignSemanticAttribute(widget, "aria-hidden", widget._decorativeImage ? "true" : null);
}

function setAlternativeText(data) {
    const widget = widgets[data.widget];
    const text = data["alternative text"];
    if (widget?.tagName !== "IMG" || typeof text !== "string") return false;
    widget._alternativeText = text;
    refreshImageAlternative(widget);
    return true;
}

function setDecorativeImage(data) {
    const widget = widgets[data.widget];
    const flag = data["decorative image"];
    if (widget?.tagName !== "IMG" || typeof flag !== "boolean") return false;
    widget._decorativeImage = flag;
    refreshImageAlternative(widget);
    return true;
}
