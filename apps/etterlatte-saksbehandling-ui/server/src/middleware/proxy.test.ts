import assert from 'node:assert/strict'
import { once } from 'node:events'
import { createServer, Server } from 'node:http'
import { test, TestContext } from 'node:test'
import express from 'express'
import { proxy } from './proxy'

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

test('proxy bevarer metode, body og query-parametere ved bytte til behandling', async (context) => {
  const backend = createServer(async (req, res) => {
    let body = ''
    for await (const chunk of req) {
      body += chunk
    }
    res.setHeader('Content-Type', 'application/json')
    res.end(
      JSON.stringify({
        url: req.url,
        method: req.method,
        body,
        authorization: req.headers.authorization,
      })
    )
  })
  const backendUrl = await startServer(backend, context)
  const app = express()
  app.use((_req, res, next) => {
    res.locals.token = 'test-token'
    next()
  })
  app.use('/api/beregning', proxy(backendUrl))
  app.use('/api/beregning_v2', proxy(backendUrl, { '^/api/beregning_v2': '/api/beregning' }))
  const proxyUrl = await startServer(createServer(app), context)

  for (const prefiks of ['/api/beregning', '/api/beregning_v2']) {
    for (const method of ['GET', 'POST', 'DELETE']) {
      const body = method === 'POST' ? JSON.stringify({ inntekter: [] }) : undefined
      const response = await fetch(`${proxyUrl}${prefiks}/avkorting/behandling-id/liste?aar=2026&fra=a%2Fb`, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body,
      })

      assert.equal(response.status, 200)
      assert.deepEqual(await response.json(), {
        url: '/api/beregning/avkorting/behandling-id/liste?aar=2026&fra=a%2Fb',
        method,
        body: body ?? '',
        authorization: 'Bearer test-token',
      })
    }
  }
})
