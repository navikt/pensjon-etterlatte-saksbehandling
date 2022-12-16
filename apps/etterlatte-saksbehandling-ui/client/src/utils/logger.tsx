import Bowser from 'bowser'
const browser: any = Bowser.getParser(window.navigator.userAgent)
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

export interface IStackLineNoColumnNo {
  readonly lineno: number
  readonly columnno: number
  readonly error: any
}

export const logger = {
  info: (stackLineNoColumnNo: IStackLineNoColumnNo) => {
    const data = { type: 'info', stackInfo: stackLineNoColumnNo, jsonContent: { ...defaultContext } }
    loggFunc(data)
      .then(() => store.dispatch(loggInfo(data)))
      .catch((err) => {
        console.error('Couldnt log info message: ', data, ' err: ', err)
      })
  },
  error: (stackLineNoColumnNo: IStackLineNoColumnNo) => {
    const data = { type: 'error', stackInfo: stackLineNoColumnNo, jsonContent: { ...defaultContext } }
    loggFunc(data)
      .then(() => store.dispatch(loggError(data)))
      .catch((err) => {
        console.error('Couldnt log error message: ', data, ' err: ', err)
      })
  },
  generalError: (info: string) => {
    const data = { type: 'error', data: info, jsonContent: { ...defaultContext } }
    loggFunc(data)
      .then(() => store.dispatch(loggError(data)))
      .catch((err) => {
        console.error('Couldnt log error message: ', data, ' err: ', err)
      })
  },
}

export const setupWindowOnError = () => {
  addEventListener('error', (event) => {
    const { error, lineno, colno } = event
    logger.error({ lineno: lineno, columnno: colno, error: JSON.stringify(error) })
    if (error.stack && error.stack?.indexOf('invokeGuardedCallbackDev') >= 0 && !error.alreadySeen) {
      error.alreadySeen = true
      event.preventDefault()
      return true
    }
    return true
  })
}
