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
            console.log("Client " + clientId + "created.");
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
            var json = JSON.parse(data);
            console.log(json);
        }
    );
}
