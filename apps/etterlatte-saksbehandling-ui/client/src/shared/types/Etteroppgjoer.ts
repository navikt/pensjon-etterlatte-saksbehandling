import { ISak } from '~shared/types/sak'

export interface Etteroppgjoer {
  behandling: EtteroppgjoerBehandling
  opplysninger: EtteroppgjoerOpplysninger
}

export interface EtteroppgjoerBehandling {
  id: string
  status: string
  sak: ISak
  aar: number
  opprettet: string // Mottatt?
}

export interface EtteroppgjoerOpplysninger {
  skatt: OpplysnignerSkatt
  ainntekt: AInntekt
  // TODO..
}

export interface OpplysnignerSkatt {
  aarsinntekt: number
}

export interface AInntekt {
  inntektsmaaneder: AInntektMaaned[]
}

export interface AInntektMaaned {
  maaned: string
  summertBeloep: number
}
