import { NextFunction, Request, Response } from "express";
import { AdConfig } from "../config/config";
import { utcSecondsSinceEpoch } from "../utils/date";
import { parseJwt } from "../utils/parsejwt";
import { getOboToken } from "./getOboToken";

export const authMiddleware = async (req: Request, res: Response, next: NextFunction) => {
    //pseudokode
    try {
        const token = await getOboToken(req.headers.authorization);
    } catch (e) {
        console.log(e); //TODO:  lage en logger (winston)
        return res.status(500).send("Det skjedde en feil");
    }
    return next();
};

export const hasBeenIssued = (issuedAtTime: number) => issuedAtTime < utcSecondsSinceEpoch(); // sjekker at issued-date har vært
export const hasExpired = (expires: number) => expires < utcSecondsSinceEpoch();

export const authenticateUser = (req: Request, res: Response, next: NextFunction) => {
    /* NAIS notes
        Token Validation
        Your application should also validate the claims and signature for the Azure AD JWT access_token attached by the sidecar.
        That is, validate the standard claims such as iss, iat, exp, and aud.
        aud must be equal to your application's client ID in Azure AD.
    */

    const auth = req.headers.authorization;
    if (!auth) {
        return res.redirect("/oauth2/login");
    }
    const bearerToken = auth.split(" ")[1];
    const parsedToken = parseJwt(bearerToken);

    console.log(process.env)
    /*
    try {
        if (parsedToken.aud !== AdConfig.audience) {
            throw new Error("Ugyldig audience");
        }
        if (parsedToken.iss !== AdConfig.issuer) {
            throw new Error("Ugyldig issuer");
        }
        if (hasBeenIssued(parsedToken.iat)) {
            throw new Error("Ugyldig iat");
        }
        if (hasExpired(parsedToken.exp)) {
            throw new Error("Token expired");
        }
    } catch (e) {
        console.log(e);
        return res.redirect("/oauth2/login");
    }
    */

    return next();
};
