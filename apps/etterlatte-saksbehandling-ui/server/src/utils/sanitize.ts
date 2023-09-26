export function sanitize(value: String | undefined) {
  if (!value) {
    return ''
  }
  return value.replace(/\n|\r/g, '')
}
