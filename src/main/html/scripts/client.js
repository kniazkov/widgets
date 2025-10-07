/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

var clientId = null;
var period = 250;
var mainCycleTask = null;
var events = [];
var lastEventId = 0;
var lastProcessedUpdateId = 0;

var parseId = function(str) {
    if (typeof str !== "string" || !str.startsWith("#")) {
        throw new Error("Invalid ID format");
    }
    const num = Number(str.slice(1));
    if (!Number.isInteger(num) || num < 0) {
        throw new Error("Invalid numeric part of ID");
    }
    return num;
}

var initClient = function() {
    window.addEventListener("beforeunload", function() {
        if (clientId != null) {
            sendRequest(
                {
                    action : "kill",
                    client : clientId
                }
            );
        }
    });
    startClient();
};

var startClient = function() {
    sendRequest(
        {
            action : "new instance"
        },
        function(data) {
            var json = JSON.parse(data);
            clientId = json.id;
            log("Client created, id: " + clientId + '.');
            mainCycleTask = setInterval(mainCycle, period);
            mainCycle();
        }
    );
    setTimeout(function() {
        if (clientId == null) {
            startClient();
        }
    }, 1000);
}

var createEvent = function(widget, type, data) {
    var eventId = "#" + ++lastEventId;
    var obj = {
        id : eventId,
        widget : widget._id,
        type   : type
    };
    if (data) {
        obj.data = data;
    }
    log("The widget " + widget._id + " triggered the event " + eventId + " '" + type + "'.");
    events.push(obj);
};

var processUpdates = function(updates) {
    if (!updates || updates.length == 0) {
        return;
    }
    if (updates.length == 1) {
        log("Received 1 update.");
    } else {
        log("Received " + updates.length + " updates.");
    }
    for (var i = 0; i < updates.length; i++) {
        var result = false;
        var update = updates[i];
        var id = parseId(update.id);
        if (id <= lastProcessedUpdateId) {
            log("Update " + update.id + " skipped.");
            continue;
        }
        var handler = actionHandlers[update.action];
        if (handler) {
            result = handler(update);
        } else {
            log("Unknown action: '" + update.action + "'.");
        }
        lastProcessedUpdateId = id;
    }
};

var removeProcessedEvents = function(id) {
    var i;
    for (i = events.length - 1; i >= 0; i--) {
        if (events[i].id == id) {
            break;
        }
    }
    events.splice(0, i + 1);
};

var sendSynchronizeRequest = function() {
    sendRequest(
        {
            action : "synchronize",
            client : clientId,
            events : events,
            lastUpdate : "#" + lastProcessedUpdateId
        },
        function(data) {
            if (!data) {
                log("Network error.");
                return;
            }
            var json = JSON.parse(data);
            processUpdates(json.updates);
            removeProcessedEvents(json.lastEvent);
        }
    );
};

var mainCycle = function() {
    sendSynchronizeRequest();
};

var reset = function() {
    log("The server initiated the client reset.");
    clientId = null;
    clearInterval(mainCycleTask);
    document.body.innerHTML = "";
    startClient();
};

var actionHandlers = {
    "create widget" : createWidget,
    "reset" : reset,
    "subscribe" : subscribeToEvent,
    "set child" : setChildWidget,
    "append child" : appendChildWidget,
    "set text" : setText,
    "set color" : setColor,
    "set background color" : setBackgroundColor,
    "set width" : setWidth,
    "set height" : setHeight,
    "set font face": setFontFace,
    "set font size": setFontSize,
    "set font weight": setFontWeight,
    "set italic": setItalic
};

var ALWAYS_ALLOWED_EVENTS = ["text input"];

var sendEventToServer = function(widget, type, data) {
    if (widget._events[type] || ALWAYS_ALLOWED_EVENTS.includes(type)) {
        createEvent(widget, type, data);
        sendSynchronizeRequest();
    }
};
