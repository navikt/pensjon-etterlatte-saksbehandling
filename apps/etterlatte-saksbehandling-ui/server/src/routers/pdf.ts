import { Request, RequestHandler, Response } from 'express'
import fetch from 'node-fetch'

const apiUrl = process.env.API_URL || 'http://localhost:8085'

export default function pdf(): RequestHandler {
  return async (req: Request, res: Response) => {
    const path = `${apiUrl}/brev${req.path}`

    const result = await fetch(path)
        .then(res => ({ status: res.status, data: res.buffer() }))

    if (result.status == 200) {
      res.contentType('application/pdf')
      res.send(await result.data)
    } else {
      res.sendStatus(result.status)
    }
  }
}
