import { Kilde } from '~shared/types/kilde'

export interface AktivitetspliktOppfolging {
  behandlingId: string
  aktivitet: string
  opprettet: string
  opprettetAv: string
}

export interface IAktivitet {
  id?: string
  sakId: number
  behandlingId: string
  type: AktivitetspliktType
  fom: Date
  tom?: Date
  opprettet: Kilde
  endret: Kilde
  beskrivelse: string
}

export enum AktivitetspliktType {
  ARBEIDSTAKER = 'Arbeidstaker',
  SELVSTENDIG_NAERINGSDRIVENDE = 'Selvstendig Næringsdrivende',
  ETABLERER_VIRKSOMHET = 'Etablerer virksomhet',
  ARBEIDSSOEKER = 'Arbeidssøker',
  UTDANNING = 'Utdanning',
}
