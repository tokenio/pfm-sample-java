"use strict";
var tokenController;
var button;

function clean() {
    if (button) {
        button.destroy();
        button = null;
    }

    if (tokenController && tokenController.destroy) {
        tokenController.destroy();
        tokenController = null;
    }
}

function createRedirectButton() {
    // clean up instances
    clean();

    // Client side Token object for creating the Token button, handling the popup, etc
    var token = new window.Token({
        env: 'sandbox',
    });

    // get button placeholder element
    var element = document.getElementById('tokenAccessBtn');

    // create the button
    button = token.createTokenButton(element, {
        label: 'Token Access',
    });

    // create TokenController to handle messages
    tokenController = token.createController();

    // bind the Token Button to the Token Controller when ready
    tokenController.bindButtonClick(
        button, // Token Button
        redirectTokenRequest, // redirect token request function
        function(error) { // bindComplete callback
            if (error) throw error;
            // enable button after binding
            button.enable();
        },
    );
}

function createPopupButton() {
    // clean up instances
    clean();

    // Client side Token object for creating the Token button, handling the popup, etc
    var token = new window.Token({
        env: 'sandbox',
    });

    // get button placeholder element
    var element = document.getElementById('tokenAccessBtn');

    // create the button
    button = token.createTokenButton(element, {
        label: 'Token Access',
    });

    // create TokenController to handle messages
    tokenController = token.createController({
        onSuccess: function(data) { // Success Callback
            // ideally you should POST 'data' to your endpoint, but for simplicity's sake
            // we are simply putting it in the URL
            var successURL = "/fetch-balances-popup"
                + "?data=" + window.encodeURIComponent(JSON.stringify(data));
            // navigate to success URL
            window.location.assign(successURL);
        },
        onError: function(error) { // Failure Callback
            throw error;
        },
    });

    // bind the Token Button to the Token Controller when ready
    tokenController.bindButtonClick(
        button, // Token Button
        getTokenRequestUrl, // token request function
        function(error) { // bindComplete callback
            if (error) throw error;
            // enable button after binding
            button.enable();
        },
        { // options
            desktop: 'POPUP',
        }
    );
}

function redirectTokenRequest() {
    // go to request balances
    document.location.assign("/request-balances");
}

// set up a function to fetch the Token Request Function
function getTokenRequestUrl(done) {
    fetch('/request-balances-popup', {
        method: 'POST',
        mode: 'no-cors',
        headers: {
            'Content-Type': 'application/json; charset=utf-8',
        },
    })
    .then(function(response) {
        if (response.ok) {
            response.text()
                .then(function(data) {
                    // execute callback when successful response is received
                    done(data);
                    console.log('data: ', data);
                });
        }
    });
}

function setupButtonTypeSelector() {
    var selector = document.getElementsByName('buttonTypeSelector');
    var selected;
    for (var i = 0; i < selector.length; i++) {
        if (selector[i].checked) {
            selected = selector[i].value;
        }
        selector[i].addEventListener('click', function(e) {
            var value = e.target.value;
            if (value === selected) return;
            selected = value;
            if (value === 'POPUP') {
                createPopupButton();
            } else if (value === 'REDIRECT') {
                createRedirectButton();
            }
        });
    }
    createRedirectButton();
}

setupButtonTypeSelector();
