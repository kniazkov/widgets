/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Snapshot site defaults before any page runtime starts changing the document head.
function readDocumentMetadata() {
    return {
        title: document.querySelector("title")?.textContent || "",
        description: document.querySelector('meta[name="description"]')?.content || "",
        robots: document.querySelector('meta[name="robots"]')?.content || ""
    };
}

// Use DOM text/attributes rather than HTML parsing for application-provided metadata.
function applyDocumentMetadata(metadata) {
    for (const name of ["title", "description", "robots"]) {
        const selector = name === "title" ? "title" : `meta[name="${name}"]`;
        const elements = [...document.head.querySelectorAll(selector)];
        const value = metadata[name] || "";
        let element = elements.shift();
        elements.forEach(duplicate => duplicate.remove());
        if (!value) {
            element?.remove();
            continue;
        }
        if (!element) {
            element = document.createElement(name === "title" ? "title" : "meta");
            if (name !== "title") element.name = name;
            document.head.appendChild(element);
        }
        if (name === "title") element.textContent = value;
        else element.content = value;
    }
}
