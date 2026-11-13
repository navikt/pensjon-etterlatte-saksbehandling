import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { once } from 'node:events'
import { createServer, Server } from 'node:http'
import { test, TestContext } from 'node:test'
import express from 'express'
import { proxy } from './proxy'
import { beregningConfig, hentBeregningApiConfig } from '../config/beregning'

const startServer = async (server: Server, context: TestContext): Promise<string> => {
  server.listen(0, '127.0.0.1')
  await once(server, 'listening')
  context.after(
    () =>
      new Promise<void>((resolve, reject) => {
        server.closeAllConnections()
        server.close((error) => (error ? reject(error) : resolve()))
      })
  )
  const address = server.address()
  assert(address && typeof address !== 'string')
  return `http://127.0.0.1:${address.port}`
}

test('miljøvariabelen velger backend og scope, med beregningsappen som standard', () => {
  const apiConfig = {
    beregning: { url: 'http://etterlatte-beregning', scope: 'beregning-scope' },
    behandling: { url: 'http://etterlatte-behandling', scope: 'behandling-scope' },
  }
  for (const verdi of [undefined, '', 'false', 'true', 'TRUE', 'ugyldig']) {
    const env = { ...process.env }
    if (verdi === undefined) {
      delete env.BRUK_INTERN_BEREGNING
    } else {
      env.BRUK_INTERN_BEREGNING = verdi
    }
    const resultat = execFileSync(
      process.execPath,
      [
        '--require',
        'ts-node/register/files',
        '-e',
        `const { hentBeregningApiConfig } = require(${JSON.stringify(require.resolve('../config/beregning'))});
         process.stdout.write(JSON.stringify(hentBeregningApiConfig(${JSON.stringify(apiConfig)})));`,
      ],
      { env, encoding: 'utf8' }
    )
    assert.deepEqual(JSON.parse(resultat), verdi === 'true' ? apiConfig.behandling : apiConfig.beregning)
  }
})

test('proxy velger backend og scope uten å endre sti, metode, body eller query-parametere', async (context) => {
  const backend = (navn: string) =>
    createServer(async (req, res) => {
      let body = ''
      for await (const chunk of req) {
        body += chunk
      }
      res.setHeader('Content-Type', 'application/json')
      res.end(
        JSON.stringify({
          backend: navn,
          url: req.url,
          method: req.method,
          body,
          authorization: req.headers.authorization,
        })
      )
    })
  const apiConfig = {
    beregning: { url: await startServer(backend('beregning'), context), scope: 'beregning-scope' },
    behandling: { url: await startServer(backend('behandling'), context), scope: 'behandling-scope' },
  }

  for (const brukBehandling of [false, true]) {
    await context.test(`brukBehandling=${brukBehandling}`, async (context) => {
      const opprinneligBrukBehandling = beregningConfig.brukBehandling
      context.after(() => {
        beregningConfig.brukBehandling = opprinneligBrukBehandling
      })
      beregningConfig.brukBehandling = brukBehandling
      const beregningApiConfig = hentBeregningApiConfig(apiConfig)
      const backendNavn = brukBehandling ? 'behandling' : 'beregning'
      assert.strictEqual(beregningApiConfig, apiConfig[backendNavn])

      const app = express()
      app.use((_req, res, next) => {
        res.locals.token = 'test-token'
        next()
      })
      app.use('/api/beregning', proxy(beregningApiConfig.url))
      const proxyUrl = await startServer(createServer(app), context)

      for (const method of ['GET', 'POST', 'DELETE']) {
        const body = method === 'POST' ? JSON.stringify({ inntekter: [] }) : undefined
        const response = await fetch(`${proxyUrl}/api/beregning/avkorting/behandling-id/liste?aar=2026&fra=a%2Fb`, {
          method,
          headers: { 'Content-Type': 'application/json' },
          body,
        })

        assert.equal(response.status, 200)
        assert.deepEqual(await response.json(), {
          backend: backendNavn,
          url: '/api/beregning/avkorting/behandling-id/liste?aar=2026&fra=a%2Fb',
          method,
          body: body ?? '',
          authorization: 'Bearer test-token',
        })
      }
    })
  }
})
