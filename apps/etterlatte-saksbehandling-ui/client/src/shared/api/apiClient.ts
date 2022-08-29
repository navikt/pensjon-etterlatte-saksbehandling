const path = process.env.REACT_APP_VEDTAK_URL

type Success<T> = { status: 'ok'; data: T; statusCode: number }
type Error = { status: 'error'; statusCode: number }

export type ApiResponse<T> = Success<T> | Error

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH'

interface options {
  url: string
  method: Method
  body?: Record<string, unknown>
}

export async function apiClient<T>(props: options): Promise<ApiResponse<T>> {
  const { url, method, body } = props

  const trimmedUrl = url.startsWith('/') ? url.slice(1) : url

  const response = await fetch(`${path}/api/${trimmedUrl}`, {
    method: method,
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
