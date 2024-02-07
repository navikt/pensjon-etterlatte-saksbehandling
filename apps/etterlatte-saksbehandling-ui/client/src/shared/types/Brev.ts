export interface IBrev {
  id: number
  sakId: number
  tittel: string
  behandlingId: string
  prosessType: BrevProsessType
  soekerFnr: string
  status: BrevStatus
  statusEndret: string
  mottaker: Mottaker
  opprettet: string
  brevtype: Brevtype
}

export interface Mottaker {
  navn: string
  foedselsnummer?: {
    value: string
  }
  orgnummer?: string
  adresse: Adresse
}

export interface Adresse {
  adresseType?: AdresseType
  adresselinje1?: string
  adresselinje2?: string
  adresselinje3?: string
  postnummer?: string
  poststed?: string
  landkode?: string
  land?: string
}

export enum AdresseType {
  NORSKPOSTADRESSE = 'NORSKPOSTADRESSE',
  UTENLANDSKPOSTADRESSE = 'UTENLANDSKPOSTADRESSE',
}

export enum BrevStatus {
  OPPRETTET = 'OPPRETTET',
  OPPDATERT = 'OPPDATERT',
  FERDIGSTILT = 'FERDIGSTILT',
  JOURNALFOERT = 'JOURNALFOERT',
  DISTRIBUERT = 'DISTRIBUERT',
  SLETTET = 'SLETTET',
}

export function kanBrevRedigeres(status: BrevStatus): boolean {
  return [BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(status)
}

export enum BrevProsessType {
  AUTOMATISK = 'AUTOMATISK',
  REDIGERBAR = 'REDIGERBAR',
  MANUELL = 'MANUELL',
  OPPLASTET_PDF = 'OPPLASTET_PDF',
}

export enum Spraak {
  NB = 'nb',
  NN = 'nn',
  EN = 'en',
}

export enum Brevtype {
  VEDTAK = 'VEDTAK',
  VARSEL = 'VARSEL',
  INFORMASJON = 'INFORMASJON',
  OPPLASTET_PDF = 'OPPLASTET_PDF',
  MANUELT = 'MANUELT',
  VEDLEGG = 'VEDLEGG',
}
