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
  body?: Record<string, unknown> | FormData
  noData?: boolean
  dontLogError?: boolean
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
  return !noDataFlag && (status === 200 || status === 201 || status === 207 || status === 206)
}

async function fetchResponse(options: Options) {
  const { url, method, body } = options
  const trimmedUrl = url.startsWith('/') ? url.slice(1) : url

  if (options.body instanceof FormData) {
    return fetch(`/api/${trimmedUrl}`, {
      method,
      body: body as FormData,
    })
  } else {
    return fetch(`/api/${trimmedUrl}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
      },
      body: body ? JSON.stringify(body) : undefined,
    })
  }
}

async function apiFetcher<T>(props: Options): Promise<ApiResponse<T>> {
  const { url, method, dontLogError } = props

  const shouldLogError = !dontLogError
  try {
    const response = await fetchResponse(props)

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
      if (response.status === 401) {
        console.error('Vi er utlogget.')
        return { status: 401, ok: false, detail: 'Du er utlogget, last på siden på nytt.' }
      }

      const error: JsonError = await errorFrom(response)

      if (response.status >= 500) {
        if (shouldLogError) {
          logger.generalError({
            msg: `Fikk feil i kall mot backend. Url: ${url}`,
            errorInfo: { url, method, error },
          })
        }
        //unleash wrapper kaster ikke det
        console.error(error, response)
        return { ...error, ok: false }
      } else {
        // logger 3xx og 4xx som info
        if (shouldLogError) {
          logger.generalInfo({
            msg: `Fikk status=${response.status} i kall mot backend`,
            errorInfo: { url, method, error },
          })
        }
        console.log(error, response)
        return { ...error, ok: false }
      }
    }
  } catch (e) {
    console.error('Rejection i fetch / utlesing av data', e)
    if (shouldLogError) {
      logger.generalError({ msg: `Fikk Rejection i kall mot backend: ${e}`, errorInfo: { url, method } })
    }
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

async function errorFrom(response: Response): Promise<JsonError> {
  const responseContent = await response.text()

  try {
    return JSON.parse(responseContent)
  } catch (err) {
    logger.generalError({ msg: responseContent || `Feil oppsto ved parsing av JSON-error. HTTP-kode: ${response.status} url: ${response.url}` })

    return { status: response.status, detail: 'Fikk feil i kall mot backend' }
  }
}

export const apiClient = {
  get: <T>(url: string) => apiFetcher<T>({ url, method: 'GET' }),
  post: <T>(url: string, body: Record<string, unknown>, noData?: boolean, dontLogError?: boolean) =>
    apiFetcher<T>({ url, body, method: 'POST', noData, dontLogError }),
  postFormData: <T>(url: string, body: FormData) => apiFetcher<T>({ url, body, method: 'POST' }),
  delete: <T>(url: string, noData?: boolean) => apiFetcher<T>({ url, method: 'DELETE', noData }),
  put: <T>(url: string, body: Record<string, unknown>) => apiFetcher<T>({ url, method: 'PUT', body: body }),
} as const
