export function sanitize(value: String | undefined | null) {
  if (value === undefined) {
    return undefined
  }
  if (value === null) {
    return null
  }
  return value.replace(/\n|\r/g, '')
}
