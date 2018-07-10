"use strict";

var elementId = "tokenAccessBtn";
var tokenController;
var button;

function createButton() {
    // create TokenPopupController to handle Popup messages
    tokenController = window.Token.createPopupController();

    // get button placeholder element
    var element = document.getElementById(elementId);

    // create the button
    button = window.Token.createTokenButton(element, {
        label: "Token Access",
    });

    // setup onLoad callback
    tokenController.onLoad(function(controller) {
        // execute bindButton when controller is ready
        bindButton();
    });

    // setup onSuccess callback
    tokenController.onSuccess(function(data) { // Success Callback
        // build success URL
        var successURL = "/fetch-balances"
           + "?tokenId=" + window.encodeURIComponent(data.tokenId);
        // navigate to success URL
        window.location.assign(successURL);
    });

    // setup onError callback
    tokenController.onError(function(error) { // Failure Callback
        throw error;
    });
}

function bindButton() {
    // bind the Token Button to the Popup Controller
    tokenController.bindButtonClick(button, function(action) {
        // Each time the button is clicked, a new tokenRequestUrl is created
        getTokenRequestUrl(function(tokenRequestUrl) {
            // Initialize popup using the tokenRequestUrl
            action(tokenRequestUrl);
        });
    });
    // enable button after binding
    button.enable();
}

function getTokenRequestUrl(done) {
    var XHR = new XMLHttpRequest();

    //set up the access request
    XHR.open("POST", "http://localhost:3000/request-balances", true);

    XHR.setRequestHeader("Content-Type", "application/json; charset=utf-8");

    var data = $.param({
        resources: [ // the button asks for permission to:
            { type: Token.RESOURCE_TYPE_ALL_ACCOUNTS }, // get list of accounts
            { type: Token.RESOURCE_TYPE_ALL_BALANCES }, // get balance of each account
        ]
     });

     // Define what happens on successful data submission
     XHR.addEventListener("load", function(event) {
         // execute callback once response is received
         done(event.target.responseURL);
     });

    // Send the data; HTTP headers are set automatically
    XHR.send(data);
}

createButton();
