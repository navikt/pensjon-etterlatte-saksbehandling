import express, { Request, Response } from 'express'
import { logger } from '../monitoring/logger'
import { requireEnvValue } from '../config/config'
import crypto from 'crypto'

const KRYPTERINGSNOEKKEL_FNR = requireEnvValue('krypteringsnoekkelFnr')

export const krypterRouter = express.Router()

interface KrypterRequest {
  request: string
}
interface KrypterResponse {
  respons: string
}

interface DekrypterRequest {
  request: string
}
interface DekrypterResponse {
  respons: string
}

function encrypt(tekst: string, ENCRYPTION_KEY: string) {
  const iv = crypto.randomBytes(16)
  const cipher = crypto.createCipheriv('aes-256-cbc', Buffer.from(ENCRYPTION_KEY), iv)
  let encrypted = cipher.update(tekst)

  encrypted = Buffer.concat([encrypted, cipher.final()])

  return `${iv.toString('base64')}:${encrypted.toString('base64')}`
}

krypterRouter.post(`/krypter/`, express.json(), async (req, res) => {
  try {
    const fnr = req.body as KrypterRequest
    const response: KrypterResponse = {
      respons: encrypt(fnr.request, KRYPTERINGSNOEKKEL_FNR),
    }
    return res.json(response)
  } catch (e) {
    logger.error('Feil oppsto ved kryptering', e)
    return res.sendStatus(500)
  }
})

function decrypt(tekst: string, ENCRYPTION_KEY: string) {
  const [ivStreng, tekstStreng] = tekst.split(':')
  const iv = Buffer.from(ivStreng, 'base64')
  const kryptertTekst = Buffer.from(tekstStreng, 'base64')
  const decipher = crypto.createDecipheriv('aes-256-cbc', Buffer.from(ENCRYPTION_KEY), iv)
  let decrypted = decipher.update(kryptertTekst)

  decrypted = Buffer.concat([decrypted, decipher.final()])

  return decrypted.toString()
}

krypterRouter.post(`/dekrypter/`, express.json(), async (req: Request, res: Response) => {
  try {
    const fnr = req.body as DekrypterRequest
    const response: DekrypterResponse = {
      respons: decrypt(fnr.request, KRYPTERINGSNOEKKEL_FNR),
    }
    return res.json(response)
  } catch (e) {
    logger.error('Feil oppsto ved dekryptering', e)
    return res.sendStatus(500)
  }
})
