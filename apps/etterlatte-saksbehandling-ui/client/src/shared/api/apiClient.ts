type ApiSuccess<T> = { status: 'ok'; data: T; statusCode: number }
export type ApiError = { status: 'error'; statusCode: number; error?: any }

export type ApiResponse<T> = ApiSuccess<T> | ApiError

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface Options {
  url: string
  method: Method
  body?: Record<string, unknown>
  noData?: boolean
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
      return {
        status: 'ok',
        statusCode: response.status,
        data: !props.noData ? await response.json() : null,
      }
    }
    console.error(response)
    return { status: 'error', statusCode: response.status }
  } catch (e) {
    console.error('Rejection i fetch / utlesing av data', e)
    return { status: 'error', statusCode: 500, error: e }
  }
}

export const apiClient = {
  get: <T>(url: string) => apiFetcher<T>({ url, method: 'GET' }),
  post: <T>(url: string, body: Record<string, unknown>, noData = false) =>
    apiFetcher<T>({ url: url, body: body, method: 'POST', noData: noData }),
  delete: <T>(url: string, noData?: boolean) => apiFetcher<T>({ url, method: 'DELETE', noData }),
} as const
