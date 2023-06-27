import winston, { format, transports } from 'winston'
import { PrometheusTransport } from './transport'

const { colorize, combine, timestamp, simple, json } = format
const { Console } = transports

const consoleTransport = new Console()
const prometheusTransport = new PrometheusTransport()

const production = combine(timestamp(), json())
const dev = combine(colorize(), simple())

export const logger = winston.createLogger({
  level: 'info',
  format: process.env.NAIS_CLUSTER_NAME ? production : dev,
  defaultMeta: { service: 'etterlatte-saksbehandling-ui' },
  transports: [consoleTransport, prometheusTransport],
})

export const frontendLogger = winston.createLogger({
  level: 'info',
  format: process.env.NAIS_CLUSTER_NAME ? production : dev,
  defaultMeta: {
    service: 'etterlatte-saksbehandling-ui-client',
  },
  transports: [consoleTransport, prometheusTransport],
})
