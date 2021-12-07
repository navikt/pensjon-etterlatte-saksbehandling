import { NextFunction, Request, Response } from "express";
import fetch from "node-fetch";

export const authMiddleware = async (req: Request, res: Response, next: NextFunction) => {
    //console.log(req.headers.authorization);
    // håndter token obo

    //pseudokode
    try {
        const response = await fetch("https://login.microsoftonline.com/nav/oauth2/v2.0/token", {
            body: JSON.stringify({
                client_id: "",
                scope: "",
                redirect_uri: "",
                grant_type: "",
                client_secret: "",
                code: "",
            }),
        });
        if(response.status >= 400) {
          throw new Error("Token-kall feilet");
        }
        const json = response.json();
        console.log(json);
        // Gjør noe med headers auth
        //req.
    } catch (e) {
        console.log(e); //TODO:  lage en logger (winston)
        return res.status(500).send("Det skjedde en feil");
    }
    return next();
};


export const authenticateUser = (req: Request, res: Response, next: NextFunction) => {
    // 1. Validate the Authorization header
    console.log(req.headers);
    const auth = req.headers.authorization;
    if(!auth) {
        return res.redirect("/oauth2/login");
    }
    console.log(auth);

    /*
        Token Validation
        Your application should also validate the claims and signature for the Azure AD JWT access_token attached by the sidecar.

        That is, validate the standard claims such as iss, iat, exp, and aud.

        aud must be equal to your application's client ID in Azure AD.
    */
    
    return next();

}