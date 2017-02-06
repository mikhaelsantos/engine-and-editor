/**
 *
 */
SignalPath.VariadicOutput = function(json, parentDiv, module, type, pub) {
    pub = pub || {};
    pub = SignalPath.Output(json, parentDiv, module, type, pub);

    var div;
    var variadicContext = null;

    pub.registerToVariadic = function(variadicCtx) {
        variadicContext = variadicCtx;
    };

    pub.makeNewPlaceholder = function(newName) {
        var jsonCopy = jQuery.extend(true, {}, json); // deep-copy object

        delete jsonCopy.id
        delete jsonCopy.longName
        delete jsonCopy.sourceId
        delete jsonCopy.displayName
        jsonCopy.connected = false
        jsonCopy.export = false
        jsonCopy.targets = []
        jsonCopy.name = newName

        return module.addOutput(jsonCopy);
    };

    pub.markPlaceholder = function(hide) {
        div.addClass("last-variadic");
        div.find(".ioname").append("&nbsp;<i class='variadic-plus fa fa-plus'>");
        if (hide) {
            div.css("display", "none");
        }
    };

    pub.unmarkPlaceholder = function() {
        div.css("display", "");
        div.removeClass("last-variadic");
        json.connected = div.data("spObject").isConnected();
    };

    pub.deleteMe = function() {
        module.removeOutput(pub.getName());
    };

    var super_createDiv = pub.createDiv
    pub.createDiv = function() {
        div = super_createDiv()

        div.bind("spConnect spExport", function(event, output) {
            variadicContext.onConnectOrSetExport(pub);
        })

        div.bind("spDisconnect spUnexport", function(event, output) {
            variadicContext.onDisconnectOrUnexport(pub, div);
        })

        return div
    }

    return pub;
}