package io.token.sample;

import static com.google.common.base.Charsets.UTF_8;
import static io.token.TokenIO.TokenCluster.SANDBOX;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static io.token.util.Util.generateNonce;

import com.google.common.io.Resources;
import io.token.Account;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.ProtoJson;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.money.MoneyProtos.Money;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        Member originalMember = initializeMember(tokenIO);

        // Initializes the server
        Spark.port(3000);

        // Endpoint for transfer payment, called by client side after user approves payment.
        Spark.get("/fetch-balances", (req, res) -> {
            String tokenId = req.queryMap("tokenId").value();

            // "log in" as service member
            Member fetchMember = tokenIO.getMember(originalMember.memberId());
            fetchMember.useAccessToken(tokenId); // use access token's permissions from now on

            List<Account> accounts = fetchMember.getAccounts(); // get list of accounts
            List<String> balanceJsons = new ArrayList<>();
            for (int i = 0; i < accounts.size(); i++) { // for each account...
                Money balance = fetchMember.getAvailableBalance(accounts
                        .get(i)
                        .id()); // ...get its balance
                balanceJsons.add(ProtoJson.toJson(balance));
            }

            // respond to script.js with JSON
            return String.format("{\"balances\":[%s]}", String.join(",", balanceJsons));
        });

        // Serve the web page and JS script:
        String script = Resources.toString(Resources.getResource("script.js"), UTF_8)
                .replace("{alias}", originalMember.firstAlias().getValue());
        Spark.get("/script.js", (req, res) -> script);
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
        // An alias is a human-readable way to identify a member, e.g., an email address.
        // When we tell Token UI to request an Access Token, we use this address.
        // Normally, aliases are verified; in test environments like Sandbox, email addresses
        // that contain "+noverify" are automatically verified.
        String email = "asjava-" + generateNonce().toLowerCase() + "+noverify@example.com";
        Alias alias = Alias.newBuilder()
                .setType(EMAIL)
                .setValue(email)
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
