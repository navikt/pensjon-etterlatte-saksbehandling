import express, { Request, Response } from "express";

const app = express();

app.use(express.json());
app.use(function (req, res, next) {
    res.header("Access-Control-Allow-Origin", "http://localhost:3000");
    res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
    res.header("Access-Control-Allow-Headers", "Content-Type");
    res.header("Access-Control-Allow-Credentials", "true");
    next();
});

app.get("/modiacontextholder/api/decorator", (req: Request, res: Response) => {
    const saksbehandler = {
        ident: "Z999999",
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
    return res.json(saksbehandler);
});

app.get("/modiacontextholder/api/context/aktivbruker", (req: Request, res: Response) => {
    return res.json({
        aktivBruker: null,
        aktivEnhet: null,
    });
});

app.get("/modiacontextholder/api/context/aktivenhet", (req: Request, res: Response) => {
    return res.json({ enhetId: "0315", navn: "NAV Grünerløkka" });
});

app.delete("/modiacontextholder/api/context/aktivbruker", (req: Request, res: Response) => {
    return res.json({
        aktivBruker: null,
        aktivEnhet: null,
    });
});

app.post("/modiacontextholder/api/context", (req: Request, res: Response) => {
    return res.json({});
});

app.listen(4000, () => {
    console.log("Mock-server kjører på port 4000");
});
