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
    for (const name of [
        "title",
        "description",
        "robots",
        "canonical",
        "og:title",
        "og:description",
        "og:url",
        "og:image"
    ]) {
        const social = name.startsWith("og:");
        const selector =
            name === "title"
                ? "title"
                : name === "canonical"
                  ? 'link[rel="canonical"]'
                  : `meta[${social ? "property" : "name"}="${name}"]`;
        const elements = [...document.head.querySelectorAll(selector)];
        const value = metadata[name] || "";
        let element = elements.shift();
        elements.forEach(duplicate => duplicate.remove());
        if (!value) {
            element?.remove();
            continue;
        }
        if (!element) {
            element = document.createElement(
                name === "title" ? "title" : name === "canonical" ? "link" : "meta"
            );
            if (name === "canonical") element.rel = "canonical";
            else if (name !== "title") element.setAttribute(social ? "property" : "name", name);
            document.head.appendChild(element);
        }
        if (name === "title") element.textContent = value;
        else if (name === "canonical") element.href = value;
        else element.content = value;
    }
}

// Public content is data, never HTML supplied by an application or a URL parameter.
function renderPublicDocument(value) {
    const article = document.createElement("article");
    article.className = "widgets-public";
    for (const block of value.blocks) {
        if (!/^(h[1-6]|p|a|img)$/.test(block.tag)) throw new Error("Invalid public block");
        const node = document.createElement(block.tag);
        if (block.tag === "a" || block.tag === "img") {
            const url = new URL(block.url, window.location.origin);
            if (!["http:", "https:"].includes(url.protocol)) throw new Error("Invalid public URL");
            if (block.tag === "a") node.href = block.url;
            else {
                node.src = block.url;
                node.alt = block.text;
                node.width = block.width;
                node.height = block.height;
            }
        }
        if (block.tag !== "img") node.textContent = block.text;
        if (block.tag === "a") {
            const paragraph = document.createElement("p");
            paragraph.append(node);
            article.append(paragraph);
        } else article.append(node);
    }
    return article;
}

function publicDocumentMetadata(value) {
    return {
        canonical: value.canonical,
        "og:title": value.title,
        "og:description": value.description,
        "og:url": value.canonical,
        "og:image": value.image
    };
}
