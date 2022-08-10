type ApiResponse<TSuccess> = Success<TSuccess> | Error
type Success<T> = { status: 'ok'; data: T }
type Error = { status: 'error'; error: { statusCode: number } }

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH'

export async function apiClient<TSuccess>(url: string, options: { method: Method }): Promise<ApiResponse<TSuccess>> {
  const response = await fetch(url, { method: options.method })

  if (response.ok) {
    return success<TSuccess>(await response.json())
  }

  return error(404)
}

function success<T>(data: T): Success<T> {
  return { status: 'ok', data }
}

function error(statusCode: number): Error {
  return { status: 'error', error: { statusCode } }
}
