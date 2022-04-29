import { Request, RequestHandler, Response } from 'express'
import axios, { AxiosResponse } from 'axios'

export default function pdf(): RequestHandler {
  return async (req: Request, res: Response) => {
    const path = `http://localhost:8081${req.path}`

    const data = await axios(path, {
      method: 'POST',
      responseType: 'arraybuffer',
      data: req.body,
    }).then((response: AxiosResponse) => response.data)

    res.contentType('application/pdf')
    res.send(await data)
  }
}
