import { ISak } from '~shared/types/sak'

export interface IEtteroppgjoerBehandling {
  id: string
  status: string
  sak: ISak
  aar: number
  opprettet: string // Mottatt?
  opplysninger: IEtteroppgjoerOpplysninger
}

export interface IEtteroppgjoerOpplysninger {
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
