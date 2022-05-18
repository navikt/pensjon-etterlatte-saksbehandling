import { Request, Response, Router } from 'express'
import fetch from 'node-fetch'

const router = Router({ mergeParams: true })

const apiUrl = process.env.API_URL || 'http://localhost:8085'

// Hent alle brev tilknyttet behandling ID
router.get('/behandling/:behandlingId', async (req: Request, res: Response) => {
  const path = `${apiUrl}/brev/behandling/${req.params.behandlingId}`

  const data = await fetch(path)
      .then(res => res.json())

  res.send(await data)
})

router.delete('/:brevId', async (req: Request, res: Response) => {
  const path = `${apiUrl}/brev/${req.params.brevId}`

  const data = await fetch(path, { method: 'DELETE' })
      .then(res => {
        console.log(`${res.status}: ${res.statusText}`)
        return res.status
      })

  res.send(data)
})

router.get('/maler', async (req: Request, res: Response) => {
  const data = await fetch(`${apiUrl}/brev/maler`)
      .then(res => res.json())

  res.send(await data)
})

router.post('/behandling/:behandlingId', async (req: Request, res: Response) => {
  const path = `${apiUrl}/brev/behandling/${req.params.behandlingId}`

  const data = await fetch(path, {
    method: 'POST',
    body: JSON.stringify(req.body),
    headers: {
      'Content-Type': 'application/json'
    }
  })
      .then(res => res.json())

  res.send(await data)
})

router.post('/:brevId/ferdigstill', async (req: Request, res: Response) => {
  const path = `${apiUrl}/brev/${req.params.brevId}/ferdigstill`

  const data = await fetch(path, { method: 'POST' })
      .then(res => res.json())

  res.send(await data)
})

router.post('/:brevId/pdf', async (req: Request, res: Response) => {
  const path = `${apiUrl}/brev/${req.params.brevId}/pdf`

  const result = await fetch(path, { method: 'POST' })
      .then(res => ({ status: res.status, data: res.buffer() }))

  if (result.status == 200) {
    res.contentType('application/pdf')
    res.send(await result.data)
  } else {
    res.sendStatus(result.status)
  }
})

export default router
