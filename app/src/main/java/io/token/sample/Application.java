package io.token.sample;

import static com.google.common.base.Charsets.UTF_8;
import static io.token.TokenClient.TokenCluster.SANDBOX;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.ACCOUNTS;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.BALANCES;
import static io.token.util.Util.generateNonce;

import com.google.common.io.Resources;
import io.token.proto.ProtoJson;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.token.TokenProtos.AccessBody.Resource;
import io.token.proto.common.token.TokenProtos.Token;
import io.token.tokenrequest.TokenRequest;
import io.token.tpp.Account;
import io.token.tpp.Member;
import io.token.tpp.Representable;
import io.token.tpp.TokenClient;
import io.token.tpp.util.Util;

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
        TokenClient tokenClient = initializeSDK();

        // Create a Member (Token user account). A "real world" server would
        // use the same member instead of creating a new one for each run;
        // this demo creates a a new member for easier demos/testing.
        Member pfmMember = initializeMember(tokenClient);

        // Initializes the server
        Spark.port(3000);

        // Endpoint for requesting access to account balances
        Spark.post("/request-balances", (req, res) -> {
            //Create a token request to be stored
            TokenRequest tokenRequest = TokenRequest.accessTokenRequestBuilder(ACCOUNTS, BALANCES)
                    .setToMemberId(pfmMember.memberId())
                    .setToAlias(pfmMember.firstAliasBlocking())
                    .setRedirectUrl("http://localhost:3000/fetch-balances")
                    .setCallbackState(generateNonce())
                    .build();

            String requestId = pfmMember.storeTokenRequestBlocking(tokenRequest);

            //generate the Token request URL to redirect to
            String tokenRequestUrl = tokenClient.generateTokenRequestUrlBlocking(requestId);

            //send a 302 redirect
            res.status(302);
            res.redirect(tokenRequestUrl);
            return null;
        });

        // Endpoint for transfer payment, called by client side after user approves payment.
        Spark.get("/fetch-balances", (req, res) -> {
            String tokenId = req.queryMap("tokenId").value();

            // use access token's permissions from now on, set true if customer initiated request
            Representable representable = pfmMember.forAccessToken(tokenId, false);
            List<Account> accounts = representable.getAccountsBlocking();
            List<String> balanceJsons = new ArrayList<>();
            for (int i = 0; i < accounts.size(); i++) {
                //for each account, get its balance
                Account account = accounts.get(i);
                Money balance = account.getBalanceBlocking(STANDARD).getCurrent();
                balanceJsons.add(ProtoJson.toJson(balance));
            }

            // respond to script.js with JSON
            return String.format("{\"balances\":[%s]}", String.join(",", balanceJsons));
        });

        // Serve the web page, stylesheet and JS script:
        String script = Resources.toString(Resources.getResource("script.js"), UTF_8)
                .replace("{alias}", pfmMember.firstAliasBlocking().getValue());
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
    private static TokenClient initializeSDK() throws IOException {
        return TokenClient.builder()
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
    private static Member initializeMember(TokenClient tokenIO) {
        // An alias is a human-readable way to identify a member, e.g., a domain or email address.
        // If a domain alias is used instead of an email, please contact Token
        // with the domain and member ID for verification.
        // See https://developer.token.io/sdk/#aliases for more information.
        String email = "asjava-" + generateNonce().toLowerCase() + "+noverify@example.com";
        Alias alias = Alias.newBuilder()
                .setType(EMAIL)
                .setValue(email)
                .build();
        Member member = tokenIO.createMemberBlocking(alias);
        // A member's profile has a display name and picture.
        // The Token UI shows this (and the alias) to the user when requesting access.
        member.setProfile(Profile.newBuilder()
                .setDisplayNameFirst("Info Demo")
                .build());
        return member;
    }
}
