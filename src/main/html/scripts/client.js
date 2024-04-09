/*
 * Copyright (c) 2024 Ivan Kniazkov
 */

var clientId = null;
var period = 20;

var startClient = function() {
    sendRequest(
        {
            action : "new instance"
        },
        function(data) {
            var json = JSON.parse(data);
            clientId = json.id;
            console.log("Client created, id: " + clientId + '.');
            setInterval(mainCycle, period);
            window.addEventListener("beforeunload", function() {
                sendRequest(
                    {
                        action : "kill",
                        client : clientId
                    }
                );
            });
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

var actionHandlers = {
    "create" : createWidget,
    "set child" : setChildWidget,
    "append child" : appendChildWidget,
    "set text" : setText
};
