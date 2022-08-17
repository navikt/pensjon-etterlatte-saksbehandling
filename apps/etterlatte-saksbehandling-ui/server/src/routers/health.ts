import express, { Request, Response } from 'express'

export const healthRouter = express.Router()

healthRouter.get('/isAlive', (req: Request, res: Response) => {
  return res.status(200).send('Ok')
})

healthRouter.get('/isReady', (req: Request, res: Response) => {
  return res.status(200).send('Ok')
})
