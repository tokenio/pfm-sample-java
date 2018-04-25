'use strict';

function initiateAccess() {
    console.log("I am here");
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
       window.location.replace(event.target.responseURL);
       //window.open(event.target.responseURL, 'Token Web App', 'height=600,width=400');
     });

    // Send the data; HTTP headers are set automatically
    XHR.send(data);
}

document.getElementById("tokenAccessBtn").onclick = initiateAccess;
