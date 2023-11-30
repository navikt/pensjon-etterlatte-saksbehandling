import tokenCache from './token-cache'

export function flush(): void {
  tokenCache.flushAll()
}

export function getTokenInCache(cacheKey: string): [cacheHit: true, value: string] | [cacheHit: false] {
  const tokenInCache: string | undefined = tokenCache.get(cacheKey)
  if (tokenInCache) {
    return [true, tokenInCache]
  }

  return [false]
}

export function setTokenInCache(cacheKey: string, access_token: string, expires_in: number): void {
  if (access_token == null) return

  let calculatedExpiration = (expires_in ?? 65) - 5
  tokenCache.set(cacheKey, access_token, calculatedExpiration)
}
