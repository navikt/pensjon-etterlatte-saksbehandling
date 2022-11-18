import Bowser from 'bowser'
const browser: any = Bowser.getParser(window.navigator.userAgent)
import * as StackTrace from 'stacktrace-js'
import { apiClient } from '~shared/api/apiClient'
import { store } from '~store/Store'
import { loggError, loggInfo } from '~store/reducers/BehandlingReducer'

const defaultContext = {
  url: window.location.href,
  userAgent: window.navigator.userAgent,
  userDeviceInfo: browser.parsedResult,
  appName: 'etterlatte-saksbehandling-ui-client',
}

const loggFunc = (data: any) => apiClient.post('/logg', data)

export const logger = {
  info: (text: string) => {
    const data = { type: 'info', jsonContent: { ...defaultContext, text } }
    loggFunc(data)
      .then(() => {})
      .catch((err) => {
        console.error('Couldnt log info message: ', data, ' err: ', err)
      })
    store.dispatch(loggInfo(data))
  },
  error: (text: string) => {
    const data = { type: 'error', jsonContent: { ...defaultContext, text } }
    loggFunc(data)
      .then(() => {})
      .catch((err) => {
        console.error('Couldnt log error message: ', data, ' err: ', err)
      })
    store.dispatch(loggError(data))
  },
}

export const joinStackTraceAndSendToServer = (stackframes: any) => {
  const stringifiedStack = stackframes.map((sf: any) => sf.toString()).join('\n')
  logger.error(`Sourcemapped stacktrace: \n + ${stringifiedStack}`)
}

export const logErrorWithStacktraceJS = (err: any) => {
  StackTrace.fromError(err)
    .then(joinStackTraceAndSendToServer)
    .catch((err) => {
      logger.error(`Could not parse stack, original: \n ${err}`)
    })
}

export const generateStacktraceAndLog = (
  lineno: number,
  colno: number,
  message: string,
  filename: string,
  error: any
) => {
  if (error) {
    logErrorWithStacktraceJS(error)
  }
}

export const setupWindowOnError = () => {
  addEventListener('error', (event) => {
    const { message, colno, lineno, error, filename } = event
    generateStacktraceAndLog(lineno, colno, message, filename, error)
    if (error.stack?.indexOf('invokeGuardedCallbackDev') >= 0 && !error.alreadySeen) {
      error.alreadySeen = true
      event.preventDefault()
      return true
    }
    return true
  })
}
