export interface ISanksjon {
  id?: string
  behandlingId: string
  sakId: number
  type: SanksjonType
  fom: string
  tom?: string
  opprettet: {
    tidspunkt: string
    ident: string
  }
  endret?: {
    tidspunkt: string
    ident: string
  }
  beskrivelse: string
}

export interface ISanksjonLagre {
  id?: string
  sakId: number
  type: SanksjonType
  fom: string
  tom?: string
  beskrivelse: string
}

export enum SanksjonType {
  BORTFALL = 'BORTFALL',
  OPPHOER = 'OPPHOER',
  STANS = 'STANS',
  UTESTENGING = 'UTESTENGING',
}

export const tekstSanksjon: Record<SanksjonType, string> = {
  BORTFALL: 'Bortfall',
  OPPHOER: 'Opphør',
  STANS: 'Stans',
  UTESTENGING: 'Utestenging',
}
