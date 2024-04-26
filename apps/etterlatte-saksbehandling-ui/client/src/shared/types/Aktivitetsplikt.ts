import { KildeSaksbehandler } from '~shared/types/kilde'

export interface AktivitetspliktOppfolging {
  behandlingId: string
  aktivitet: string
  opprettet: string
  opprettetAv: string
}

export interface IAktivitet {
  id: string
  sakId: number
  behandlingId: string
  type: AktivitetspliktType
  fom: string
  tom?: string
  opprettet: KildeSaksbehandler
  endret: KildeSaksbehandler
  beskrivelse: string
}

export interface IOpprettAktivitet {
  sakId: number
  type: AktivitetspliktType
  fom: string
  tom?: string
  beskrivelse: string
}

export enum AktivitetspliktType {
  ARBEIDSTAKER = 'ARBEIDSTAKER',
  SELVSTENDIG_NAERINGSDRIVENDE = 'SELVSTENDIG_NAERINGSDRIVENDE',
  ETABLERER_VIRKSOMHET = 'ETABLERER_VIRKSOMHET',
  ARBEIDSSOEKER = 'ARBEIDSSOEKER',
  UTDANNING = 'UTDANNING',
}
