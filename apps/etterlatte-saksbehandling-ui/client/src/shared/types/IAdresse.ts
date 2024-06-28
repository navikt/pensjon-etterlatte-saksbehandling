export interface IAdresse {
  adresseLinje1: string
  adresseLinje2?: string
  adresseLinje3?: string
  aktiv: boolean
  coAdresseNavn?: string
  gyldigFraOgMed?: string
  gyldigTilOgMed?: string
  kilde: string
  land?: string
  postnr: string
  poststed?: string
  type: string // adresseType
}
