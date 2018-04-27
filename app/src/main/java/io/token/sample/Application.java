package io.token.sample;

import static com.google.common.base.Charsets.UTF_8;
import static io.token.TokenIO.TokenCluster.SANDBOX;
import static io.token.TokenRequest.TokenRequestOptions.REDIRECT_URL;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.DOMAIN;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.util.Util.generateNonce;

import com.google.common.io.Resources;
import io.token.AccessTokenBuilder;
import io.token.Account;
import io.token.Member;
import io.token.TokenIO;
import io.token.TokenRequest;
import io.token.proto.ProtoJson;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource;
import io.token.proto.common.token.TokenProtos.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import spark.Spark;

/**
 * Application main entry point.
 * To execute, one needs to run something like:
 * <p>
 * <pre>
 * ./gradlew :app:shadowJar
 * java -jar ./app/build/libs/app-1.0.0-all.jar
 * </pre>
 */
public class Application {
    /**
     * Main function.
     *
     * @param args command line arguments
     * @throws IOException thrown on errors
     */
    public static void main(String[] args) throws IOException {
        // Connect to Token's development sandbox
        TokenIO tokenIO = initializeSDK();

        // Create a Member (Token user account). A "real world" server would
        // use the same member instead of creating a new one for each run;
        // this demo creates a a new member for easier demos/testing.
        Member pfmMember = initializeMember(tokenIO);

        // Initializes the server
        Spark.port(3000);

        // Endpoint for requesting access to account balances
        Spark.post("/request-balances", (req, res) -> {
            //Create an AccessTokenBuilder
            AccessTokenBuilder tokenBuilder = AccessTokenBuilder.create(
                    pfmMember.firstAlias())
                    .forAll();
            //Create a token request to be stored
            TokenRequest request = TokenRequest.create(tokenBuilder)
                    .setOption(REDIRECT_URL, "http://localhost:3000/fetch-balances");

            String requestId = pfmMember.storeTokenRequest(request);

            //generate the Token request URL to redirect to
            String tokenRequestUrl = tokenIO.generateTokenRequestUrl(requestId);

            //send a 302 redirect
            res.status(302);
            res.redirect(tokenRequestUrl);
            return null;
        });

        // Endpoint for transfer payment, called by client side after user approves payment.
        Spark.get("/fetch-balances", (req, res) -> {
            //String tokenId = req.queryMap("tokenId").value();
            String callbackUri = req.raw().getRequestURL().toString()
                    + "?"
                    + req.raw().getQueryString();
            String tokenId = tokenIO.parseTokenRequestCallbackUrl(callbackUri).getTokenId();

            // use access token's permissions from now on, set true if customer initiated request
            pfmMember.useAccessToken(tokenId, false);
            Token token = pfmMember.getToken(tokenId);
            // extract the account ids token grants access to from the token
            List<String> accounts = token.getPayload().getAccess().getResourcesList()
                    .stream()
                    .map(Resource::getAccount)
                    .map(Resource.Account::getAccountId)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toList());
            List<String> balanceJsons = new ArrayList<>();
            for (int i = 0; i < accounts.size(); i++) {
                //for each account, get its balance
                Account account = pfmMember.getAccount(accounts.get(i));
                Money balance = account.getCurrentBalance(STANDARD);
                balanceJsons.add(ProtoJson.toJson(balance));
            }

            // when done using access, clear token from the client.
            pfmMember.clearAccessToken();

            // respond to script.js with JSON
            return String.format("{\"balances\":[%s]}", String.join(",", balanceJsons));
        });

        // Serve the web page, stylesheet and JS script:
        String script = Resources.toString(Resources.getResource("script.js"), UTF_8)
                .replace("{alias}", pfmMember.firstAlias().getValue());
        Spark.get("/script.js", (req, res) -> script);
        String style = Resources.toString(Resources.getResource("style.css"), UTF_8);
        Spark.get("/style.css", (req, res) -> {
            res.type("text/css");
            return style;
        });
        String page = Resources.toString(Resources.getResource("index.html"), UTF_8);
        Spark.get("/", (req, res) -> page);
    }

    /**
     * Initializes the SDK, pointing it to the specified environment.
     *
     * @return TokenIO SDK instance
     * @throws IOException
     */
    private static TokenIO initializeSDK() throws IOException {
        return TokenIO.builder()
                .connectTo(SANDBOX)
                .devKey("4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI")
                .build();
    }

    /**
     * Log in existing member or create new member.
     *
     * @param tokenIO Token SDK client
     * @return Logged-in member
     */
    private static Member initializeMember(TokenIO tokenIO) {
        // An alias is a human-readable way to identify a member, e.g., a domain or email address.
        String domain = "asjava-" + generateNonce().toLowerCase() + ".com";
        Alias alias = Alias.newBuilder()
                .setType(DOMAIN)
                .setValue(domain)
                .build();
        Member member = tokenIO.createMember(alias);
        // A member's profile has a display name and picture.
        // The Token UI shows this (and the alias) to the user when requesting access.
        member.setProfile(Profile.newBuilder()
                .setDisplayNameFirst("Info Demo")
                .build());
        return member;
    }
}
