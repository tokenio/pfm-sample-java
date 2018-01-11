## Token PFM Sample: Java

Simple personal finance app that illustrates Token.io's Access Tokens

This sample app shows how to request Token's Access Tokens, useful
for fetching account information.

To build this code, you need Java Development Kit (JDK) version 8 or later.

To build, `./gradlew shadowJar`.

To run, `java -jar app/build/libs/app-*.jar`

This starts up a server.

The server operates against Token's Sandbox environment by default.
This testing environment lets you try out UI and account flows without
exposing real bank accounts.

The server shows a web page at `localhost:3000`. The page has a Link with Token button.
Clicking the button displays Token UI that requests an Access Token.
When the app has an Access Token, it uses that Access Token to get account balances.
