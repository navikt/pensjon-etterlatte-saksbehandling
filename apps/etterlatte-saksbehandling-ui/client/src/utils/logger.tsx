import { apiClient } from '~shared/api/apiClient'
import { store } from '~store/Store'
import { loggError, loggInfo } from '~store/reducers/BehandlingReducer'
import { ErrorInfo } from 'react'

const GYLDIG_FNR = (input: string | undefined) => /^\d{11}$/.test(input ?? '')

function sanitizeUrlPossibleFnr(url?: string): string {
  if (url) {
    const splittedUrl = url.split('/')
    return splittedUrl
      .map((urlpart) => {
        if (GYLDIG_FNR(urlpart)) {
          return urlpart.substring(0, 5).concat('******')
        }
        return urlpart
      })
      .join('/')
  }
  return ''
}

const defaultContext = {
  url: sanitizeUrlPossibleFnr(window.location.href),
  userAgent: window.navigator.userAgent,
}

const loggFunc = (data: any) => apiClient.post('/logg', data, true, true)

export interface IStackLineNoColumnNo {
  readonly lineno: number
  readonly columnno: number
  readonly message: any
  readonly error: any
}

interface ErrorData {
  msg: string
  errorInfo?: ErrorInfo
  apiErrorInfo?: ApiErrorInfo
  err?: Error
}

interface ApiErrorInfo {
  url: string
  method: string
  error?: JsonError
}

interface JsonError {
  status: number
  detail: string
  code?: string
  meta?: Record<string, unknown>
}

export const logger = {
  info: (stackLineNoColumnNo: IStackLineNoColumnNo) => {
    const data = { type: 'info', stackInfo: stackLineNoColumnNo, jsonContent: { ...defaultContext } }
    loggFunc(data)
      .then(() => store.dispatch(loggInfo(data)))
      .catch((err) => {
        console.error('Unable to log info message: ', data, ' err: ', err)
      })
  },
  error: (stackLineNoColumnNo: IStackLineNoColumnNo) => {
    const data = { type: 'error', stackInfo: stackLineNoColumnNo, jsonContent: { ...defaultContext } }
    loggFunc(data)
      .then(() => store.dispatch(loggError(data)))
      .catch((err) => {
        console.error('Unable to log error message: ', data, ' err: ', err)
      })
  },
  generalError: (info: ErrorData) => {
    const data = { type: 'Error', data: info, jsonContent: { ...defaultContext } }
    loggFunc(data)
      .then(() => store.dispatch(loggError(data)))
      .catch((err) => {
        console.error('Unable to log error message: ', data, ' err: ', err)
      })
  },
  generalWarning: (info: ErrorData) => {
    const data = { type: 'warning', data: info, jsonContent: { ...defaultContext } }
    loggFunc(data)
      .then(() => store.dispatch(loggError(data)))
      .catch((err) => {
        console.error('Unable to log error message: ', data, ' err: ', err)
      })
  },
}

export const setupWindowOnError = () => {
  addEventListener('error', (event) => {
    const { error: kanskjeError, lineno, colno, message } = event

    const error = kanskjeError || {}
    if (import.meta.env.MODE === 'development') {
      console.error(error.message, error.stack)
    } else {
      if (message !== 'ResizeObserver loop completed with undelivered notifications.') {
        logger.error({ lineno, columnno: colno, message, error: JSON.stringify(error) })
      }

      if (error.stack && error.stack?.indexOf('invokeGuardedCallbackDev') >= 0 && !error.alreadySeen) {
        error.alreadySeen = true
        event.preventDefault()
        return true
      }
    }
    return true
  })
}

/* eslint no-var: off */
declare global {
  interface Window {
    windowOpen: boolean
  }
}

window.windowOpen = true
export const setupOnUnloadEventhandler = () => {
  addEventListener('beforeunload', () => {
    window.windowOpen = false
  })
}
