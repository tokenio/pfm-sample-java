package io.token.sample;

import static com.google.common.base.Charsets.UTF_8;
import static io.grpc.Status.Code.NOT_FOUND;
import static io.token.TokenClient.TokenCluster.SANDBOX;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.ACCOUNTS;
import static io.token.proto.common.token.TokenProtos.TokenRequestPayload.AccessBody.ResourceType.BALANCES;
import static io.token.util.Util.generateNonce;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Resources;
import io.grpc.StatusRuntimeException;
import io.token.proto.ProtoJson;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.member.MemberProtos.Profile;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.security.UnsecuredFileSystemKeyStore;
import io.token.tokenrequest.TokenRequest;
import io.token.tpp.Account;
import io.token.tpp.ExternalMetadata;
import io.token.tpp.Member;
import io.token.tpp.Representable;
import io.token.tpp.TokenClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String CSRF_TOKEN_KEY = "csrf_token";

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
        Spark.get("/request-balances", (req, res) -> {
            // generate CSRF token
            String csrfToken = generateNonce();

            // generate a reference ID for the token
            String refId = generateNonce();

            // set CSRF token in browser cookie
            res.cookie(CSRF_TOKEN_KEY, csrfToken);

            // generate redirect URL
            String redirectUrl = req.scheme() + "://" + req.host() + "/fetch-balances";

            // Create a token request to be stored
            TokenRequest tokenRequest = TokenRequest.accessTokenRequestBuilder(ACCOUNTS, BALANCES)
                    .setToMemberId(pfmMember.memberId())
                    .setToAlias(pfmMember.firstAliasBlocking())
                    .setRefId(refId)
                    .setRedirectUrl(redirectUrl)
                    .setCsrfToken(csrfToken)
                    .setBankId("wood")
                    .build();

            String requestId = pfmMember.storeTokenRequestBlocking(tokenRequest);

            // generate the Token request URL
            String tokenRequestUrl = pfmMember.getBankAuthUrlBlocking("wood", requestId);

            //send a 302 redirect
            res.status(302);
            res.redirect(tokenRequestUrl);
            return null;
        });

        // Endpoint for requesting access to account balances
        Spark.post("/request-balances-popup", (req, res) -> {
            // generate CSRF token
            String csrfToken = generateNonce();

            // generate a reference ID for the token
            String refId = generateNonce();

            // set CSRF token in browser cookie
            res.cookie(CSRF_TOKEN_KEY, csrfToken);

            // generate redirect URL
            String redirectUrl = req.scheme() + "://" + req.host() + "/fetch-balances-popup";

            // Create a token request to be stored
            TokenRequest tokenRequest = TokenRequest.accessTokenRequestBuilder(ACCOUNTS, BALANCES)
                    .setToMemberId(pfmMember.memberId())
                    .setToAlias(pfmMember.firstAliasBlocking())
                    .setRefId(refId)
                    .setRedirectUrl(redirectUrl)
                    .setCsrfToken(csrfToken)
                    .build();

            String requestId = pfmMember.storeTokenRequestBlocking(tokenRequest);

            // generate the Token request URL
            String tokenRequestUrl = pfmMember.getBankAuthUrlBlocking("wood", requestId);

            // return the generated Token Request URL
            res.status(200);
            return tokenRequestUrl;
        });

        // Endpoint for transfer payment, called by client side after user approves payment.
        Spark.get("/fetch-balances", (req, res) -> {
            String tokenRequestId = pfmMember
                    .onBankAuthCallbackBlocking("wood", req.queryString());
            String tokenId = tokenClient.getTokenRequestResultBlocking(tokenRequestId).getTokenId();

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
            ExternalMetadata metadata = pfmMember.getExternalMetadataBlocking(tokenRequestId);
            // respond to script.js with JSON
            return String.format("{\"balances\":[%s]}<br/><br/>", String.join(",", balanceJsons))
                    + String.format(
                    "standard: %s<br/>consent-id: %s<br/>consent: %s<bar/>",
                    metadata.getOpenBankingStandard(),
                    metadata.getConsentId().orElse("UNKNOWN"),
                    metadata.getConsent()
                            .map(c -> new String(BaseEncoding.base64().decode(c)))
                            .orElse("UNKNOWN"));
        });

        // Endpoint for transfer payment, called by client side after user approves payment.
        Spark.get("/fetch-balances-popup", (req, res) -> {
            String tokenRequestId = pfmMember
                    .onBankAuthCallbackBlocking("wood", req.queryString());
            String tokenId = tokenClient.getTokenRequestResultBlocking(tokenRequestId).getTokenId();

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
     * Initializes the SDK, pointing it to the specified environment and the
     * directory where keys are being stored.
     *
     * @return TokenClient SDK instance
     * @throws IOException
     */
    private static TokenClient initializeSDK() throws IOException {
        Path keys = Files.createDirectories(Paths.get("./keys"));
        return TokenClient.builder()
                .connectTo(SANDBOX)
                .withKeyStore(new UnsecuredFileSystemKeyStore(
                        keys.toFile()))
                .build();

    }

    /**
     * Log in existing member or create new member.
     *
     * @param tokenClient Token SDK client
     * @return Logged-in member
     */
    private static Member initializeMember(TokenClient tokenClient) {
        // The UnsecuredFileSystemKeyStore stores keys in a directory
        // named on the member's memberId, but with ":" replaced by "_".
        // Look for such a directory.
        //   If found, try to log in with that memberId
        //   If not found, create a new member.
        File keysDir = new File("./keys");
        String[] paths = keysDir.list();

        return Arrays.stream(paths)
                .filter(p -> p.contains("_")) // find dir names containing "_"
                .map(p -> p.replace("_", ":")) // member ID
                .findFirst()
                .map(memberId -> loadMember(tokenClient, memberId))
                .orElseGet(() -> createMember(tokenClient));
    }

    /**
     * Using a TokenClient SDK client and the member ID of a previously-created
     * Member (whose private keys we have stored locally).
     *
     * @param tokenClient SDK
     * @param memberId ID of member
     * @return Logged-in member.
     */
    private static Member loadMember(TokenClient tokenClient, String memberId) {
        try {
            return tokenClient.getMemberBlocking(memberId);
        } catch (StatusRuntimeException sre) {
            if (sre.getStatus().getCode() == NOT_FOUND) {
                // We think we have a member's ID and keys, but we can't log in.
                // In the sandbox testing environment, this can happen:
                // Sometimes, the member service erases the test members.
                throw new RuntimeException(
                        "Couldn't log in saved member, not found. Remove keys dir and try again.");
            } else {
                throw new RuntimeException(sre);
            }
        }
    }

    /**
     * Using a TokenClient SDK client, create a new Member.
     * This has the side effect of storing the new Member's private
     * keys in the ./keys directory.
     *
     * @param tokenClient Token SDK client
     * @return newly-created member
     */
    private static Member createMember(TokenClient tokenClient) {
        // Generate a random username, or alias, which is a human-readable way
        // to identify a member, e.g., a domain or email address.
        // If we try to create a member with an already-used name,
        // it will fail.
        // If a domain alias is used instead of an email, please contact Token
        // with the domain and member ID for verification.
        // See https://developer.token.io/sdk/#aliases for more information.
        String email = "asjava-" + generateNonce().toLowerCase() + "+noverify@example.com";
        Alias alias = Alias.newBuilder()
                .setType(EMAIL)
                .setValue(email)
                .build();
        Member member = tokenClient.createMemberBlocking(alias);
        // A member's profile has a display name and picture.
        // The Token UI shows this (and the alias) to the user when requesting access.
        member.setProfile(Profile.newBuilder()
                .setDisplayNameFirst("Demo")
                .setDisplayNameLast("PFM")
                .build());
        try {
            byte[] pict = Resources.toByteArray(Resources.getResource("southside.png"));
            member.setProfilePictureBlocking("image/png", pict);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return member;
        // The newly-created member is automatically logged in.
    }

}
