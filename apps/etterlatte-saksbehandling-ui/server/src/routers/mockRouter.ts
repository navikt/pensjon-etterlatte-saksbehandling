import express, { Request, Response } from 'express'
import '../mockdata/oppgaverMockData.json'
import personsokUtenSak from '../mockdata/personsokUtenSak.json'

export const mockRouter = express.Router() // for å støtte dekoratør for innloggede flater

mockRouter.get('/modiacontextholder/decorator', (req: Request, res: Response) => {
  res.json({
    ident: 'Z81549300',
    navn: 'Truls Veileder',
    fornavn: 'Truls',
    etternavn: 'Veileder',
    rolle: 'attestant',
    enheter: [
      {
        enhetId: '0315',
        navn: 'NAV Grünerløkka',
      },
      {
        enhetId: '0316',
        navn: 'NAV Gamle Oslo',
      },
    ],
  })
})

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

mockRouter.get(`/behandling/:id/manueltopphoer`, (_req: Request, res: Response) => {
  let svar = require(`../mockdata/manueltOpphoer_detaljer.json`)
  setTimeout(() => {
    res.json(svar)
  }, 300)
})

mockRouter.get(`/behandling/:id`, (req: Request, res: Response) => {
  const id = req.params.id
  let behandling = require(`../mockdata/detaljertBehandling_${id}.json`)
  setTimeout(() => {
    res.json(behandling)
  }, 1000)
})

mockRouter.post(`/behandling/:id/kommerbarnettilgode`, (req: Request, res: Response) => {
  setTimeout(() => {
    res.json(req.body)
  }, 300)
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

  if (id == '14') {
    setTimeout(() => {
      res.sendStatus(412) // Precondition Failed
    })
  } else {
    const vilkaarsproving = require(`../mockdata/hentVilkaarsvurdering_${id}.json`)
    setTimeout(() => {
      res.json(vilkaarsproving)
    }, 1000)
  }
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

mockRouter.get(`/trygdetid`, (req: Request, res: Response) => {
  let trygdetid = require(`../mockdata/trygdetid.json`)
  setTimeout(() => {
    res.json(trygdetid)
  }, 1000)
})
