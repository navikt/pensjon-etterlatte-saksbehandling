import winston, { format, transports } from 'winston'
const { colorize, combine, timestamp, simple, json } = format

const { Console } = transports

export const logger = winston.createLogger({
  level: 'info',
  format: winston.format.json(),
  defaultMeta: { service: 'etterlatte-saksbehandling-ui' },
  transports: [new Console()],
})

const production = combine(timestamp(), json())
const dev = combine(colorize(), simple())

export const frontendLogger = winston.createLogger({
  level: 'info',
  format: process.env.NAIS_CLUSTER_NAME ? production : dev,
  defaultMeta: {
    service: 'etterlatte-saksbehandling-ui-client',
  },
  transports: [new Console()],
})
