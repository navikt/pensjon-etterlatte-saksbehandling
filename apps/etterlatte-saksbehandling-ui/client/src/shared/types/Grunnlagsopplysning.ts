export interface Grunnlagsopplysning<T> {
  id: string
  kilde: string
  opplysningsType: string
  opplysning: T
}
