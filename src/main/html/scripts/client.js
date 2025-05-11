/*
 * Copyright (c) 2025 Ivan Kniazkov
 */

var clientId = null;
var period = 250;
var mainCycleTask = null;

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

var processUpdates = function(json) {
    if (json.updates && json.updates.length > 0) {
        if (json.updates.length == 1) {
            log("Received 1 update.");
        } else {
            log("Received " + json.updates.length + " updates.");
        }
        for (var i = 0; i < json.updates.length; i++) {
            var result = false;
            var update = json.updates[i];
            var handler = actionHandlers[update.action];
            if (handler) {
                result = handler(update);
            }
        }
    }
};

var composeSyncData = function(){

};

var mainCycle = function() {
    sendRequest(
        {
            action : "synchronize",
            client : clientId
        },
        function(data) {
            if (!data) {
                log("Network error.");
                return;
            }
            var json = JSON.parse(data);
            processUpdates(json);
        }
    );
};

var reset = function() {
    log("The server initiated the client reset.");
    clientId = null;
    clearInterval(mainCycleTask);
    document.body.innerHTML = "";
    startClient();
};

var actionHandlers = {
    "create" : createWidget,
    "reset" : reset,
    "set child" : setChildWidget,
    "append child" : appendChildWidget,
    "set text" : setText,
    "set color" : setColor,
    "set width" : setWidth,
    "set height" : setHeight,
    "set font face": setFontFace,
    "set font size": setFontSize,
    "set font weight": setFontWeight,
    "set italic": setItalic
};

var lastEventId = 0;
var events = new Set();

var sendEventToServer = function(widget, type, data) {
    var eventId = "#" + ++lastEventId;
    var request = {
        action : "process event",
        event : eventId,
        client : clientId,
        widget : widget._id,
        type   : type
    };
    if (data) {
        request.data = data;
    }
    log("The widget " + widget._id + " triggered the event " + eventId + " '" + type + "'.");
    events.add(eventId);
    sendRequest(request, function(data) {
        if (!data) {
            log("Network error.");
            return;
        }
        var json = JSON.parse(data);
        if (json.result && json.event) {
            events.delete(eventId);
            log("The event " + json.event + " was processed, unprocessed events: "
                + events.size + ".");
            processUpdates(json);
        } else {
            log("The event has not been processed.");
        }
    });
};
