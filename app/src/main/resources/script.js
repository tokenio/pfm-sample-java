"use strict";

var elementId = "tokenAccessBtn";

function createButton() {
    // Create button
    window.Token.styleButton({
        id: elementId,
        label: "Token Access",
    }, bindButton); // execute bindButton when styling button is finished
}

function bindButton(button) {
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
         button.bindAccessButton(
             event.target.responseURL, // request token URL
             function(data) { // success callback
                 // build success URL
                 var successURL = "/fetch-balances"
                    + "?tokenId=" + window.encodeURIComponent(data.tokenId);
                 // navigate to success URL
                 window.location.assign(successURL);
             },
             function(error) { // fail callback
                 throw error;
             }
         );
     });

    // Send the data; HTTP headers are set automatically
    XHR.send(data);
}

createButton();
