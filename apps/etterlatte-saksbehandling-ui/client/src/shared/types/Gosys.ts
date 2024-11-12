import { OppgaveSaksbehandler } from '~shared/types/oppgave'
import { SakType } from '~shared/types/sak'

export interface GosysOppgave {
  id: number
  versjon: number
  status: string
  tema: GosysTema
  oppgavetype: string
  saksbehandler?: OppgaveSaksbehandler
  enhet: string
  opprettet: string
  frist?: string
  beskrivelse?: string
  journalpostId?: string
  bruker?: GosysOppgaveBruker
}

export interface GosysOppgaveBruker {
  ident?: string
  type?: GosysBrukerType
}

export enum GosysBrukerType {
  PERSON = 'PERSON',
  ARBEIDSGIVER = 'ARBEIDSGIVER',
  SAMHANDLER = 'SAMHANDLER',
}

export enum GosysTema {
  PEN = 'PEN',
  EYO = 'EYO',
  EYB = 'EYB',
}

export const formaterStatus = (status: string) => {
  switch (status) {
    case 'OPPRETTET':
      return 'Opprettet'
    case 'AAPNET':
      return 'Åpnet'
    default:
      return status
  }
}

export const formaterOppgavetype = (type: string) => {
  switch (type) {
    case 'GEN':
      return 'Generell'
    case 'BEH_SED':
      return 'Behandle SED'
    case 'BEH_SAK':
      return 'Behandle sak'
    case 'KRA':
      return 'Krav'
    case 'ATT':
      return 'Attestering'
    case 'JFR':
      return 'Journalføring'
    case 'VURD_HENV':
      return 'Vurder henvendelse'
    case 'VUR_KONS_YTE':
      return 'Vurder konsekvens for ytelsen'
    default:
      return type
  }
}

export const sakTypeFraTema = (tema: GosysTema) => {
  switch (tema) {
    case GosysTema.EYB:
      return SakType.BARNEPENSJON
    case GosysTema.EYO:
      return SakType.OMSTILLINGSSTOENAD
    case GosysTema.PEN:
      return undefined
  }
}
