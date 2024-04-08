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
            console.log(clientId);
        }
    );
    setTimeout(function() {
        if (clientId == null) {
            startClient();
        }
    }, 1000);
}
