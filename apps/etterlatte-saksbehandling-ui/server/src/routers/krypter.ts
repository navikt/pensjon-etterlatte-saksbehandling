import express from 'express'
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

const algoritme = 'aes-256-cbc'

function tilBase64Url(tekst: Buffer) {
  return tekst
      .toString('base64')
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/g, '')
}

function fraBase64Url(tekst: string) {
  return Buffer.from(tekst
      .replace(/-/g, '+')
      .replace(/_/g, '/'),
      'base64')
}

function encrypt(tekst: string, ENCRYPTION_KEY: string) {
  const iv = crypto.randomBytes(16)
  const cipher = crypto.createCipheriv(algoritme, Buffer.from(ENCRYPTION_KEY), iv)
  let encrypted = cipher.update(tekst)

  encrypted = Buffer.concat([encrypted, cipher.final()])

  return `${tilBase64Url(iv)}:${tilBase64Url(encrypted)}`
}

function decrypt(tekst: string, ENCRYPTION_KEY: string) {
  const [ivStreng, tekstStreng] = tekst.split(':')
  const iv = fraBase64Url(ivStreng)
  const kryptertTekst = fraBase64Url(tekstStreng)
  const decipher = crypto.createDecipheriv(algoritme, Buffer.from(ENCRYPTION_KEY), iv)
  let decrypted = decipher.update(kryptertTekst)

  decrypted = Buffer.concat([decrypted, decipher.final()])

  return decrypted.toString()
}

krypterRouter.post(`/dekrypter/`, express.json(), async (req, res) => {
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