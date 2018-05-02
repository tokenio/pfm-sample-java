'use strict';

function initiateAccess() {
    // prevent multiple clicking
    unbindClick();

    var XHR = new XMLHttpRequest();

    //set up the access request
    XHR.open('POST', 'http://localhost:3000/request-balances', true);

    XHR.setRequestHeader("Content-Type", "application/json; charset=utf-8");

    var data = $.param({
        resources: [ // the button asks for permission to:
            { type: Token.RESOURCE_TYPE_ALL_ACCOUNTS }, // get list of accounts
            { type: Token.RESOURCE_TYPE_ALL_BALANCES }, // get balance of each account
        ]
     });

     // Define what happens on successful data submission
     XHR.addEventListener("load", function(event) {
       window.open(event.target.responseURL, "Token Web App", "width=400,height=600");
     });

    // Send the data; HTTP headers are set automatically
    XHR.send(data);
}

function bindClick() {
    // Add click listener
    el.addEventListener('click', initiateAccess);
}

function unbindClick() {
    // Remove click listener
    el.removeEventListener('click', initiateAccess);
}

var el = document.getElementById("tokenAccessBtn");
bindClick();
