import express, { Request, Response } from 'express'
import personsok from '../mockdata/personsok.json';
import personsokUtenSak from '../mockdata/personsokUtenSak.json';

export const mockRouter = express.Router() // for å støtte dekoratør for innloggede flater


mockRouter.get("/personer/:fnr", (req: Request, res: Response) => {
  if(req.params.fnr === '26117512737') {
    return res.json(personsokUtenSak);
  }
  return res.json(personsok);
})

