import express, { NextFunction, Request, Response } from "express";
import { v4 as uuidv4 } from 'uuid';
import fetch from "node-fetch";
const router = express.Router();

declare module "express-session" {
    interface SessionData {
        csrfToken: string;
        pkceCodes: any
    }
}

export const oauthflow = async () => {
    // Authentication
    router.get('/login', (req: Request, res: Response, next: NextFunction) => {
        console.log('********* hits oauth2 login');
        const csrfToken = uuidv4();
        req.session.csrfToken = csrfToken
        const state = {
            csrfToken: csrfToken,
            redirectTo: 'oauth2/openid/callback'
        }

        req.session.pkceCodes = {
            challengeMethod: 'S256',
            //verifier: verifier,
            //challenge: challenge,
        };

        const statebase64 = Buffer.from(JSON.stringify(state)).toString('base64');
        const azuread = {
            scope: 'openid profile offline_access https://graph.microsoft.com/user.read', // mby api://dev-gcp.etterlatte.etterlatte-saksbehandling-ui/.default
            clientId: 'de3237a1-16a3-4260-add0-8c85034fe394', // azuread-ey-sak-lokal todo: env later
            redirect_uri: 'localhost:8080/oauth2/openid/callback',
            grant_type: 'authorization_code',
            response_type: 'code',
            response_mode: 'query',
            state: statebase64
        }
        const authUrl = "https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/oauth2/authorize?";
        const params = new URLSearchParams({ ...Object.entries(azuread) })
        const url = new URL(`${authUrl}${params}`);
        console.log('redirects to azuread')
        res.redirect(url.toString());

    });

    router.get('/openid/callback', (req: Request, res: Response, next: NextFunction) => {
        console.log('/openid/callback  req:', req)
        //Buffer.from("SGVsbG8gV29ybGQ=", 'base64').toString('ascii')
    });

    router.get('/logout', (req: Request, res: Response, next: NextFunction) => {
        // authenticateAzure(req, res, next);
    });

    return router;
};







/*
export const authenticateAzure = (req: Request, res: Response, next: NextFunction) => {
    const regex: RegExpExecArray | null = /redirectUrl=(.*)/.exec(req.url);
    const redirectUrl = regex ? regex[1] : 'invalid';

    const successRedirect = regex ? redirectUrl : '/';

    if (!req.session) {
        throw new Error('Mangler sesjon på kall');
    }

    req.session.redirectUrl = successRedirect;
    try {
        passport.authenticate('azureOidc', {
            failureRedirect: '/error',
            successRedirect,
        })(req, res, next);
    } catch (err) {
        throw new Error(`Error during authentication: ${err}`);
    }
};

export const authenticateAzureCallback = () => {
    return (req: Request, res: Response, next: NextFunction) => {
        try {

            passport.authenticate('azureOidc', {
                failureRedirect: '/error',
                successRedirect: req.session.redirectUrl || '/',
            })(req, res, next);
        } catch (err) {
            throw new Error(`Error during authentication: ${err}`);
        }
    };
};

export const logout = (req: Request, res: Response) => {
    if (!req.session) {
        throw new Error('Mangler sesjon på kall');
    }

    res.redirect('logout');
    req.session.destroy((error: Error) => {
        if (error) {
            //TODO: log?
        }
    });
};
*/