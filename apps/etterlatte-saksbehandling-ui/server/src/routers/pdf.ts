import { Request, RequestHandler, Response } from 'express'
import fetch from 'node-fetch'

export default function pdf(): RequestHandler {
  return async (req: Request, res: Response) => {
    const path = `http://localhost:8085/brev${req.path}`

    const data = await fetch(path, { method: 'GET' })
        .then(res => res.buffer())

    res.contentType('application/pdf')
    res.send(await data)
  }
}
