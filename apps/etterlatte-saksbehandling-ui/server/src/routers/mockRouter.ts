import express, { Request, Response } from 'express'

export const mockRouter = express.Router() // for å støtte dekoratør for innloggede flater


mockRouter.get("/personer/:fnr", (req: Request, res: Response) => {
  res.json({
    fornavn: "Mocket",
    etternavn: "Bruker",
    ident: "11057523044",
    saker: {behandlinger: []}
  });
})