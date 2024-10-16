export interface IBrev {
  id: number
  sakId: number
  tittel: string
  spraak: Spraak
  behandlingId?: string
  prosessType: BrevProsessType
  soekerFnr: string
  status: BrevStatus
  statusEndret: string
  mottakere: Mottaker[]
  opprettet: string
  brevtype: Brevtype
  journalpostId?: string
  bestillingId?: string
}

export interface Mottaker {
  id: string
  navn: string
  foedselsnummer?: {
    value: string
  }
  orgnummer?: string
  adresse: Adresse
  tvingSentralPrint: boolean
  kopimottaker: boolean
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
  UTGAATT = 'UTGAATT',
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
  OVERSENDELSE_KLAGE = 'OVERSENDELSE_KLAGE',
}

export function formaterBrevtype(brevtype: Brevtype) {
  switch (brevtype) {
    case Brevtype.INFORMASJON:
      return 'Informasjonsbrev'
    case Brevtype.MANUELT:
      return 'Manuelt brev'
    case Brevtype.VARSEL:
      return 'Varselbrev'
    case Brevtype.VEDTAK:
      return 'Vedtaksbrev'
    case Brevtype.OPPLASTET_PDF:
      return 'Opplastet PDF'
    case Brevtype.OVERSENDELSE_KLAGE:
      return 'Oversendelse klage'
  }
}
