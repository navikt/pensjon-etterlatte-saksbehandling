import express, { Request, Response } from 'express'
import personsok from '../mockdata/personsok.json';

export const mockRouter = express.Router() // for å støtte dekoratør for innloggede flater


mockRouter.get("/personer/:fnr", (req: Request, res: Response) => {
  res.json(personsok);
})

