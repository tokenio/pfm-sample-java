'use strict';

// Here, "Token" comes from https://web-app.sandbox.token.io/token.js :
Token.styleButton({            // Sets up the Link with Token button
    id: 'tokenAccessBtn',
    label: 'Link with Token'
}).bindAccessButton(
    {
        alias: {
            type: 'EMAIL',
            value: '{alias}'        // address filled in by server
        },
        resources: [ // the button asks for permission to:
            { type: Token.RESOURCE_TYPE_ALL_ACCOUNTS }, // get list of accounts
            { type: Token.RESOURCE_TYPE_ALL_BALANCES }, // get balance of each account
        ]
    },
    function(data) { // success, have access token
        console.log('success callback got ' + JSON.stringify(data));
        if (data.tokenId) {
            $.getJSON(
                '/fetch-balances',
                {tokenId: data.tokenId},
                function (balancesJSON) {
                    $('#balances').empty();
                    if (balancesJSON.balances) {
                        for (var accountId in balancesJSON.balances) {
                            const balance = balancesJSON.balances[accountId];
                            $('#balances').append(`<p>${balance.currency}: ${balance.value}`);
                        }
                    }
                }
            );
        }
    },
    function(error) { // fail
        alert('Something\'s wrong! ' + error);
    }
);
