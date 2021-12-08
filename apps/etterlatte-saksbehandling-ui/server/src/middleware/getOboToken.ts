import { parseJwt } from "../utils/parsejwt";

/*
    POST /oauth2/v2.0/token HTTP/1.1
    Host: login.microsoftonline.com/<tenant>
    Content-Type: application/x-www-form-urlencoded

    grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer
    client_id=535fb089-9ff3-47b6-9bfb-4f1264799865
    &client_secret=sampleCredentia1s
    &assertion=eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6InowMzl6ZHNGdWl6cEJmQlZLMVRuMjVRSFlPMCJ9.eyJhdWQiOiIyO{a lot of characters here}
    &scope=https://graph.microsoft.com/user.read+offline_access
    &requested_token_use=on_behalf_of
    */

export const getOboToken = async (auth: any) => {
    if (!auth) {
        throw new Error("Ikke autentisert");
    }
    const bearerToken = auth.split(" ")[1];
    const parsedToken = parseJwt(bearerToken);
    try {
        const response = await fetch("https://login.microsoftonline.com/nav/oauth2/v2.0/token", {
            body: JSON.stringify({
                client_id: "app-id i azure", //62366534-1ec3-4962-8869-9b5535279d0b
                client_secret: "...",
                scope: "scopet til tjenesten vi kaller",
                redirect_uri: "",
                grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
                assertion: bearerToken,
                requested_token_use: "on_behalf_of",
            }),
        });
        if (response.status >= 400) {
            throw new Error("Token-kall feilet");
        }
        const json = response.json();
        console.log(json);
    } catch (e) {
        throw new Error("Det skjedde en feil ved henting av obo-token");
    }
};
