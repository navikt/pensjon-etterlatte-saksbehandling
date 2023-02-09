import winston, { format, transports } from 'winston'

const { colorize, combine, timestamp, simple, json } = format
const { Console } = transports

const production = combine(timestamp(), json())
const dev = combine(colorize(), simple())

export const logger = winston.createLogger({
  level: 'info',
  format: process.env.NAIS_CLUSTER_NAME ? production : dev,
  defaultMeta: { service: 'etterlatte-saksbehandling-ui' },
  transports: [new Console()],
})

export const frontendLogger = winston.createLogger({
  level: 'info',
  format: process.env.NAIS_CLUSTER_NAME ? production : dev,
  defaultMeta: {
    service: 'etterlatte-saksbehandling-ui-client',
  },
  transports: [new Console()],
})
