export interface IBrev {
  id: number
  behandlingId: string
  tittel: string
  status: string
  mottaker: Mottaker
  erVedtaksbrev: boolean
}

export interface Mottaker {
  navn?: string
  foedselsnummer?: string
  orgnummer?: string
  adresse?: Adresse
}

export interface Adresse {
  adresseType?: string
  adresselinje1?: string
  adresselinje2?: string
  adresselinje3?: string
  postnummer?: string
  poststed?: string
  landkode?: string
  land?: string
}
