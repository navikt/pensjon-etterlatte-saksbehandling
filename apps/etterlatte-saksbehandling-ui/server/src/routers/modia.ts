import express, { Request, Response } from 'express';

export const modiaRouter = express.Router(); // for å støtte dekoratør for innloggede flater


modiaRouter.get("/decorator", (req: Request, res: Response) => {
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