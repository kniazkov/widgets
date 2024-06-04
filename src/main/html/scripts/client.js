/*
 * Copyright (c) 2024 Ivan Kniazkov
 */

var clientId = null;
var period = 100;
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
            console.log("Client created, id: " + clientId + '.');
            mainCycleTask = setInterval(mainCycle, period);
        }
    );
    setTimeout(function() {
        if (clientId == null) {
            startClient();
        }
    }, 1000);
}

var mainCycle = function() {
    sendRequest(
        {
            action : "synchronize",
            client : clientId
        },
        function(data) {
            var i;
            var json = JSON.parse(data);
            if (json.updates) {
                for (var i = 0; i < json.updates.length; i++) {
                    var result = false;
                    var update = json.updates[i];
                    var handler = actionHandlers[update.action];
                    if (handler) {
                        result = handler(update);
                    }
                }
            }
        }
    );
};

var reset = function() {
    console.log("The server initiated the client reset.");
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
    "set color" : setColor
};

var sendEventToServer = function(widget, type, data) {
    var request = {
        action : "process event",
        client : clientId,
        widget : widget._id,
        type   : type
    };
    if (data) {
        request.data = data;
    }
    console.log("The widget " + widget._id + " triggered the event '" + type + "'.");
    sendRequest(request);
};
