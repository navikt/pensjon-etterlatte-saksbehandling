import express, { Request, Response } from "express";
import { parseJwt, Token } from "../utils/parsejwt";

export const modiaRouter = express.Router(); // for å støtte dekoratør for innloggede flater

interface IEnhet {
    enhetId: string;
    navn: string;
}
interface ISaksbehandler {
    ident: string;
    navn: string;
    fornavn: string;
    etternavn: string;
    enheter: IEnhet[];
}
const getSaksbehandler = (req: Request): ISaksbehandler | null => {
    if (process.env.DEVELOPMENT === "true") {
        return {
            ident: "Z81549300",
            navn: "Truls Veileder",
            fornavn: "Truls",
            etternavn: "Veileder",
            enheter: [
                {
                    enhetId: "0315",
                    navn: "NAV Grünerløkka",
                },
                {
                    enhetId: "0316",
                    navn: "NAV Gamle Oslo",
                },
            ],
        };
    }

    const auth = req.headers.authorization;
    if(!auth) { return null }

    const bearerToken = auth.split(" ")[1];
    const parsedToken = parseJwt(bearerToken);

    return {
        ident: parsedToken.NAVident,
        navn: parsedToken.name,
        fornavn: parsedToken.name.split(", ")[1],
        etternavn: parsedToken.name.split(", ")[0],
        enheter: [
            {
                enhetId: "0315",
                navn: "NAV Grünerløkka",
            },
            {
                enhetId: "0316",
                navn: "NAV Gamle Oslo",
            },
        ],
    };
};

modiaRouter.get("/decorator", (req: Request, res: Response) => {
    const saksbehandler = getSaksbehandler(req);
    return res.json(saksbehandler);
});

modiaRouter.get("/context/aktivbruker", (req: Request, res: Response) => {
    return res.json({
        aktivBruker: null,
        aktivEnhet: null,
    });
});

modiaRouter.get("/context/aktivenhet", (req: Request, res: Response) => {
    return res.json({ enhetId: "0315", navn: "NAV Grünerløkka" });
});

modiaRouter.delete("/context/aktivbruker", (req: Request, res: Response) => {
    return res.json({
        aktivBruker: null,
        aktivEnhet: null,
    });
});

modiaRouter.post("/context", (req: Request, res: Response) => {
    return res.json({});
});
