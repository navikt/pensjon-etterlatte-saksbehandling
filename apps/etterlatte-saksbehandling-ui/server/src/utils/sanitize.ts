export function sanitize(value: String | undefined | null) {
  if (value === undefined) {
    return undefined
  }
  if (value === null) {
    return null
  }
  return value.replace(/\n|\r/g, '')
}

/**
 * String som består av nøyaktig 11 siffer kan potensielt være et fnr
 **/
const erPotensieltFnr = (input: string | undefined) => /^\d{11}$/.test(input ?? '')

/**
 * Fjerner potensielle fnr fra en URL
 **/
export function sanitizeUrl(url?: string): string {
  if (url) {
    const splittedUrl = url.split('/')
    return splittedUrl
      .map((urlpart) => {
        if (erPotensieltFnr(urlpart)) {
          return urlpart.substring(0, 5).concat('******')
        }
        return urlpart
      })
      .join('/')
  }
  return ''
}
