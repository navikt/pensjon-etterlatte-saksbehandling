import express, { Request, Response } from 'express'
import '../mockdata/oppgaverMockData.json'
import personsokUtenSak from '../mockdata/personsokUtenSak.json'

export const mockRouter = express.Router() // for å støtte dekoratør for innloggede flater

mockRouter.get(`/personer/:fnr`, (req: Request, res: Response) => {
  const fnr = req.params.fnr

  if (req.params.fnr === '26117512737') {
    return res.json(personsokUtenSak)
  }
  let person = require(`../mockdata/personsok_${fnr}.json`)
  setTimeout(() => {
    res.json(person)
  }, 1000)
  // return res.json(personsok)
})

mockRouter.get('/oppgaver', (req: Request, res: Response) => {
  let oppgaver = require('../mockdata/oppgaverMockData.json')
  setTimeout(() => {
    res.json({ oppgaver })
  }, 1000)
})

mockRouter.get(`/behandling/:id`, (req: Request, res: Response) => {
  const id = req.params.id
  let behandling = require(`../mockdata/detaljertBehandling_${id}.json`)
  setTimeout(() => {
    res.json(behandling)
  }, 1000)
})

mockRouter.post(`/vedtak/:id`, (req: Request, res: Response) => {
  // let vedtakIverksatt = require(`../mockdata/detaljertBehandling_${id}.json`)
  setTimeout(() => {
    res.json('her kommer det data')
  }, 1000)
})

mockRouter.post(`/avbrytBehandling/:id`, (req: Request, res: Response) => {
  setTimeout(() => {
    res.json('avbryter')
  }, 1000)
})

mockRouter.get(`/vilkaarsvurdering/:id`, (req: Request, res: Response) => {
  const id = req.params.id
  const vilkaarsproving = require(`../mockdata/hentVilkaarsvurdering_${id}.json`)
  setTimeout(() => {
    res.json(vilkaarsproving)
  }, 1000)
})

mockRouter.post(`/vilkaarsvurdering/:id`, (req: Request, res: Response) => {
  const id = req.params.id
  const vilkaarsproving = require(`../mockdata/hentVilkaarsvurdering_${id}.json`)
  setTimeout(() => {
    res.json(vilkaarsproving)
  }, 1000)
})

mockRouter.delete(`/vilkaarsvurdering/:behandlingId/:type`, (req: Request, res: Response) => {
  setTimeout(() => {
    res.status(200).send()
  }, 1000)
})

mockRouter.delete(`/vilkaarsvurdering/resultat/:behandlingId`, (req: Request, res: Response) => {
  setTimeout(() => {
    res.status(200).send()
  }, 1000)
})

mockRouter.post(`/vilkaarsvurdering/resultat/:id`, (req: Request, res: Response) => {
  const id = req.params.id
  const vilkaarsproving = require(`../mockdata/hentVilkaarsvurdering_${id}.json`)
  setTimeout(() => {
    res.json(vilkaarsproving)
  }, 1000)
})
