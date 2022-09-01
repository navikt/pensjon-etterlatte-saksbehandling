const path = process.env.REACT_APP_VEDTAK_URL

type Success<T> = { status: 'ok'; data: T; statusCode: number }
type Error = { status: 'error'; statusCode: number }

export type ApiResponse<T> = Success<T> | Error

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH'

interface Options {
  url: string
  method: Method
  body?: Record<string, unknown>
}

async function apiFetcher<T>(props: Options): Promise<ApiResponse<T>> {
  const { url, method, body } = props

  const trimmedUrl = url.startsWith('/') ? url.slice(1) : url

  const response = await fetch(`${path}/api/${trimmedUrl}`, {
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
      data: await response.json(),
    }
  }

  console.error(response)
  return { status: 'error', statusCode: response.status }
}

export const apiClient = {
  get: <T>(url: string) => apiFetcher<T>({ url, method: 'GET' }),
  post: <T>(url: string, body: Record<string, unknown>) => apiFetcher<T>({ url: url, body: body, method: 'POST' }),
} as const
