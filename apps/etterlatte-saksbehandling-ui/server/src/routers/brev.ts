import { NextFunction, Request, Response, Router } from 'express'
import fetch from 'node-fetch'
import request from 'request'

const router = Router({ mergeParams: true })

const apiUrl = process.env.BREV_API_URL || 'http://localhost:8085'

// Hent alle brev tilknyttet behandling ID
router.get('/behandling/:behandlingId', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const path = `${apiUrl}/brev/behandling/${req.params.behandlingId}`
    const response = await fetch(path)
    const json = await response.json()
    res.send(json)
  } catch (e) {
    next(e)
  }
})

router.delete('/:brevId', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const path = `${apiUrl}/brev/${req.params.brevId}`
    const response = await fetch(path, { method: 'DELETE' })
    res.sendStatus(response.status)
  } catch (e) {
    next(e)
  }
})

router.get('/maler', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const response = await fetch(`${apiUrl}/brev/maler`)
    const data = await response.json()

    res.send(await data)
  } catch (e) {
    next(e)
  }
})

router.get('/mottakere', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const response = await fetch(`${apiUrl}/brev/mottakere`)
    const data = await response.json()

    res.send(data)
  } catch (e) {
    next(e)
  }
})

router.post('/behandling/:behandlingId', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const path = `${apiUrl}/brev/behandling/${req.params.behandlingId}`

    const response = await fetch(path, {
      method: 'POST',
      body: JSON.stringify(req.body),
      headers: {
        'Content-Type': 'application/json',
      },
    })
    const data = await response.json()

    res.send(data)
  } catch (e) {
    next(e)
  }
})

router.post('/behandling/:behandlingId/vedtak', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const path = `${apiUrl}/brev/behandling/${req.params.behandlingId}/vedtak`

    const response = await fetch(path, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    })
    const data = await response.text()

    res.send(data)
  } catch (e) {
    next(e)
  }
})

router.post('/:brevId/ferdigstill', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const path = `${apiUrl}/brev/${req.params.brevId}/ferdigstill`

    const response = await fetch(path, { method: 'POST' })
    const data = await response.json()

    res.send(data)
  } catch (e) {
    next(e)
  }
})

router.post('/:brevId/pdf', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const path = `${apiUrl}/brev/${req.params.brevId}/pdf`

    const result = await fetch(path, { method: 'POST' })
    const data = await result.buffer()

    if (result.status == 200) {
      res.contentType('application/pdf')
      res.send(data)
    } else {
      res.sendStatus(result.status)
    }
  } catch (e) {
    next(e)
  }
})

router.post('/pdf/:behandlingId', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const path = `${apiUrl}/brev/pdf/${req.params.behandlingId}`
    req.pipe(request.post(path, next)).pipe(res)
  } catch (e) {
    next(e)
  }
})

router.post('/forhaandsvisning', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const path = `${apiUrl}/brev/forhaandsvisning`

    const result = await fetch(path, {
      method: 'POST',
      body: JSON.stringify(req.body),
      headers: {
        'Content-Type': 'application/json',
      },
    })
    const data = await result.buffer()

    if (result.status == 200) {
      res.contentType('application/pdf')
      res.send(data)
    } else {
      res.sendStatus(result.status)
    }
  } catch (e) {
    next(e)
  }
})

router.get('/dokumenter/:fnr', async (req: Request, res: Response, next: NextFunction) => {
    try {
        const path = `${apiUrl}/brev/dokumenter/${req.params.fnr}`
        const response = await fetch(path)
        const json = await response.json()
        res.send(json)
    } catch (e) {
        next(e)
    }
})

router.post('/brev/dokumenter/:journalpostId/:dokumentInfoId', async (req: Request, res: Response, next) => {
    try {
        const path = `${apiUrl}/brev/dokumenter/${req.params.journalpostId}/${req.params.dokumentInfoId}`
        const response = await fetch(path, { method: 'POST' })
        const data = await response.buffer()

        if (response.status == 200) {
            res.contentType('application/pdf')
            res.send(data)
        } else {
            res.sendStatus(response.status)
        }
    } catch (e) {
        next(e)
    }
})

export default router
