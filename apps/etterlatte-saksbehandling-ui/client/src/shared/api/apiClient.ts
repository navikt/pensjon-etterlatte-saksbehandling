import { logger } from '~utils/logger'

export type ApiSuccess<T = void> = { ok: true; status: number; data: T }
export type ApiResponse<T> = ApiSuccess<T> | ApiError

export interface JsonError {
  status: number
  detail: string
  code?: string
  meta?: Record<string, unknown>
}

export interface ApiError extends JsonError {
  ok: false
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface Options {
  url: string
  method: Method
  body?: Record<string, unknown>
  noData?: boolean
}

export async function retrieveData(response: Response): Promise<any> {
  const type = response.headers.get('content-type')?.toLowerCase()

  // content-type-headeren tillater ekstra meta-informasjon, eks: "text/html; charset=utf-8"
  // eller "multipart/form-data; boundary=something". Derfor sjekker vi på om den inneholder
  // den relevante content-typen vi er ute etter
  if (type?.includes('application/json')) {
    return await response.json()
  }
  if (type?.includes('application/pdf')) {
    return await response.arrayBuffer()
  }
  return await response.text()
}

export const restbodyShouldHaveData = (noDataFlag: boolean | undefined, status: number) => {
  return !noDataFlag && (status === 200 || status === 207 || status === 206)
}

async function apiFetcher<T>(props: Options): Promise<ApiResponse<T>> {
  const { url, method, body } = props

  const trimmedUrl = url.startsWith('/') ? url.slice(1) : url
  try {
    const response = await fetch(`/api/${trimmedUrl}`, {
      method: method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: body ? JSON.stringify(body) : undefined,
    })

    if (response.ok) {
      if (restbodyShouldHaveData(props.noData, response.status)) {
        const data = await retrieveData(response)
        return {
          ok: true,
          status: response.status,
          data,
        }
      }

      return {
        ok: true,
        status: response.status,
        data: null as T,
      }
    } else {
      const error: JsonError = await response.json()
      if (response.status >= 500) {
        const errorobj = {
          msg: 'Fikk feil i kall mot backend',
          errorInfo: JSON.stringify({ url: url, method: method, error: error }),
        }
        logger.generalError(JSON.stringify(errorobj))
        console.error(error, response)
        return { ...error, ok: false }
      } else {
        // logger 3xx og 4xx som info
        const errorobj = {
          msg: `Fikk status=${response.status} i kall mot backend`,
          errorInfo: JSON.stringify({ url: url, method: method, error: error }),
        }
        logger.generalInfo(JSON.stringify(errorobj))
        console.log(error, response)
        return { ...error, ok: false }
      }
    }
  } catch (e) {
    console.error('Rejection i fetch / utlesing av data', e)
    const errorobj = { msg: 'Fikk Rejection i kall mot backend', errorInfo: { url: url, method: method } }
    logger.generalError(JSON.stringify(errorobj))
    return {
      ok: false,
      status: 400,
      code: 'FEIL_I_PARSING_AV_DATA',
      detail: 'fikk en feil i parsing av data / rejection i fetch. Feilen ligger i meta',
      meta: {
        cause: e,
      },
    }
  }
}

export const apiClient = {
  get: <T>(url: string) => apiFetcher<T>({ url, method: 'GET' }),
  post: <T>(url: string, body: Record<string, unknown>, noData = false) =>
    apiFetcher<T>({ url: url, body: body, method: 'POST', noData: noData }),
  delete: <T>(url: string, noData?: boolean) => apiFetcher<T>({ url, method: 'DELETE', noData }),
  put: <T>(url: string, body: Record<string, unknown>) => apiFetcher<T>({ url, method: 'PUT', body: body }),
} as const
