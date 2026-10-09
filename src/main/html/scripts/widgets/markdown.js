/*
 * Copyright (c) 2026 Ivan Kniazkov
 */

// Markdown is rendered with DOM nodes only: source text never becomes executable HTML.
function createMarkdown() {
    const widget = document.createElement("div");
    widget.className = "widgets-markdown";
    widget.setText = text => {
        const fragment = document.createDocumentFragment();
        markdownBlocks(fragment, text.replace(/\r\n?/g, "\n").split("\n"));
        widget.replaceChildren(fragment);
        return true;
    };
    return widget;
}

// Only explicit safe schemes and relative references may become active links.
function markdownSafeLink(value) {
    if (/[\u0000-\u0020\u007f]/u.test(value)) return false;
    const scheme = value.match(/^([a-z][a-z0-9+.-]*):/i);
    return !scheme || /^(https?|mailto|tel)$/i.test(scheme[1]);
}

// Skip escaped markers, code spans and runs belonging to nested emphasis.
function markdownClosingDelimiter(text, delimiter, start) {
    for (let i = start; i < text.length; i++) {
        if (text[i] === "\\") {
            i++;
            continue;
        }
        if (text[i] === "`") {
            const run = text.slice(i).match(/^`+/)[0];
            const close = text.indexOf(run, i + run.length);
            if (close >= 0) i = close + run.length - 1;
            continue;
        }
        if (text[i] !== delimiter[0]) continue;
        let end = i + 1;
        while (text[end] === delimiter[0]) end++;
        const count = end - i;
        if (
            (count === delimiter.length || count === 3) &&
            count >= delimiter.length &&
            !/\s/.test(text[i - 1])
        ) {
            return end - delimiter.length;
        }
        i = end - 1;
    }
    return -1;
}

function markdownInline(parent, text, depth = 0, links = true) {
    if (depth >= 32) {
        parent.append(document.createTextNode(text));
        return;
    }
    // Deliberately bounded subset: inline destinations have no spaces or parentheses.
    const tokens =
        /(?: {2,}|\\)\n|\\[!"#$%&'()*+,\-./:;<=>?@[\]\\^_`{|}~]|`+|!?\[([^\]\n]+)\]\(([^\s()]*)\)|(\*\*\*|___|\*\*|__|~~|\*|_)/g;
    let cursor = 0;
    let match;
    while ((match = tokens.exec(text))) {
        const start = match.index;
        const token = match[0];
        parent.append(document.createTextNode(text.slice(cursor, start).replace(/\n/g, " ")));
        let end = tokens.lastIndex;
        let node;
        if (token.endsWith("\n")) {
            node = document.createElement("br");
        } else if (token.startsWith("\\")) {
            node = document.createTextNode(token.slice(1));
        } else if (token.startsWith("`")) {
            // A code span closes only with a run of exactly the same length.
            const runs = /`+/g;
            runs.lastIndex = end;
            let closing;
            while ((closing = runs.exec(text)) && closing[0].length !== token.length) {}
            if (closing) {
                node = document.createElement("code");
                node.textContent = text.slice(end, closing.index).replace(/\n/g, " ");
                end = runs.lastIndex;
            }
        } else if (match[1] !== undefined) {
            if (links && !token.startsWith("!") && markdownSafeLink(match[2])) {
                node = document.createElement("a");
                node.setAttribute("href", match[2]);
                markdownInline(node, match[1], depth + 1, false);
            }
        } else {
            // Underscores inside words (account_name) remain literal.
            const inWord = token.startsWith("_") && /[\p{L}\p{N}]/u.test(text[start - 1] || "");
            const close =
                !inWord && !/\s/.test(text[end] || " ")
                    ? markdownClosingDelimiter(text, token, end)
                    : -1;
            if (close > end && !/\s/.test(text[close - 1])) {
                const tag = token === "~~" ? "del" : token.length === 1 ? "em" : "strong";
                node = document.createElement(tag);
                let content = node;
                if (token.length === 3) {
                    content = document.createElement("em");
                    node.append(content);
                }
                markdownInline(content, text.slice(end, close), depth + 1, links);
                end = close + token.length;
            }
        }
        parent.append(node || document.createTextNode(token));
        cursor = end;
        tokens.lastIndex = end;
    }
    parent.append(document.createTextNode(text.slice(cursor).replace(/\n/g, " ")));
}

function markdownListItem(line) {
    return line.match(/^( *)([-+*]|\d{1,9}[.)]) +(.*)$/);
}

function markdownSeparator(line) {
    return /^ {0,3}(?:(?:\* *){3,}|(?:- *){3,}|(?:_ *){3,})$/.test(line);
}

function markdownBlockStart(line) {
    return (
        /^ {0,3}(?:#{1,6}(?:\s|$)|>|`{3,}|~{3,})/.test(line) ||
        markdownSeparator(line) ||
        markdownListItem(line)
    );
}

function markdownBlocks(parent, lines, depth = 0) {
    // Bound recursion for deeply nested, untrusted documents.
    if (depth >= 32) {
        const fallback = document.createElement("p");
        fallback.textContent = lines.join("\n");
        parent.append(fallback);
        return;
    }
    let index = 0;
    while (index < lines.length) {
        const line = lines[index];
        if (!line.trim()) {
            index++;
            continue;
        }
        const fence = line.match(/^ {0,3}(`{3,}|~{3,})(.*)$/);
        const heading = line.match(/^ {0,3}(#{1,6})(?:\s+(.*)|$)/);
        const item = markdownListItem(line);
        let node;
        if (fence) {
            const body = [];
            const closing = new RegExp("^ {0,3}" + fence[1][0] + "{" + fence[1].length + ",} *$");
            index++;
            while (index < lines.length && !closing.test(lines[index])) body.push(lines[index++]);
            if (index < lines.length) index++;
            node = document.createElement("pre");
            const code = document.createElement("code");
            code.textContent = body.join("\n");
            node.append(code);
        } else if (heading) {
            node = document.createElement("h" + heading[1].length);
            markdownInline(node, (heading[2] || "").replace(/\s+#+\s*$/, ""));
            index++;
        } else if (markdownSeparator(line)) {
            node = document.createElement("hr");
            index++;
        } else if (/^ {0,3}>/.test(line)) {
            const body = [];
            while (index < lines.length && /^ {0,3}>/.test(lines[index])) {
                body.push(lines[index++].replace(/^ {0,3}> ?/, ""));
            }
            node = document.createElement("blockquote");
            markdownBlocks(node, body, depth + 1);
        } else if (item) {
            const ordered = /^\d/.test(item[2]);
            const indent = item[1].length;
            node = document.createElement(ordered ? "ol" : "ul");
            if (ordered) node.setAttribute("start", String(parseInt(item[2], 10)));
            while (index < lines.length) {
                const next = markdownListItem(lines[index]);
                if (
                    !next ||
                    next[1].length !== indent ||
                    /^\d/.test(next[2]) !== ordered ||
                    markdownSeparator(lines[index])
                )
                    break;
                const body = [next[3]];
                const contentIndent = lines[index].length - next[3].length;
                index++;
                while (
                    index < lines.length &&
                    lines[index].trim() &&
                    lines[index].startsWith(" ".repeat(contentIndent))
                ) {
                    body.push(lines[index++].slice(contentIndent));
                }
                const child = document.createElement("li");
                markdownBlocks(child, body, depth + 1);
                node.append(child);
            }
        } else {
            const body = [line];
            index++;
            while (
                index < lines.length &&
                lines[index].trim() &&
                !markdownBlockStart(lines[index])
            ) {
                body.push(lines[index++]);
            }
            node = document.createElement("p");
            markdownInline(node, body.join("\n"));
        }
        parent.append(node);
    }
}
