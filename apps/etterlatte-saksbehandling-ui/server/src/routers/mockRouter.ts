import express, { Request, Response } from 'express'
import '../mockdata/oppgaverMockData.json'
import personsok from '../mockdata/personsok.json'
import personsokUtenSak from '../mockdata/personsokUtenSak.json'

export const mockRouter = express.Router() // for å støtte dekoratør for innloggede flater

mockRouter.get('/personer/:fnr', (req: Request, res: Response) => {
  if (req.params.fnr === '26117512737') {
    return res.json(personsokUtenSak)
  }
  return res.json(personsok)
})

mockRouter.get('/oppgaver', (req: Request, res: Response) => {
  let oppgaver = require('../mockdata/oppgaverMockData.json')
  setTimeout(() => {
    res.json({ oppgaver })
  }, 1000)
})

mockRouter.get(`/behandling/:id`, (req: Request, res: Response) => {
  const id = req.params.id;
  let behandling = require(`../mockdata/detaljertBehandling_${id}.json`)
  setTimeout(() => {
    res.json(behandling)
  }, 1000)
})

mockRouter.post(`/vedtak/:id`, (req: Request, res: Response) => {
  const id = req.params.id;
  // let vedtakIverksatt = require(`../mockdata/detaljertBehandling_${id}.json`)
  setTimeout(() => {
    res.json("her kommer det data")
  }, 1000)
})