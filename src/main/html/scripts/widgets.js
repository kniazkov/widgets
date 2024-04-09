/*
 * Copyright (c) 2024 Ivan Kniazkov
 */

var widgets = { };

var widgetsLibrary = {
    "root" : function() {
        return document.body;
    }
};

var createWidget = function(data) {
    var ctor = widgetsLibrary[data.type];
    var id = data.widget;
    if (ctor && id) {
        var widget = ctor();
        widgets[id] = widget;
        console.log("Widget '" + data.type + "' created, id: " + id + '.');
        return true;
    }
    return false;
};
