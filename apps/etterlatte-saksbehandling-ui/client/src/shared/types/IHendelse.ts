export interface IHendelse {
  id: number
  hendelse: IHendelseType
  opprettet: string
  ident?: string
  identType?: null
  kommentar: string
  valgtBegrunnelse: string
}

export enum IHendelseType {
  BEHANDLING_OPPRETTET = 'BEHANDLING:OPPRETTET',
  VEDTAK_VILKAARSVURDERT = 'VEDTAK:VILKAARSVURDERT',
  VEDTAK_BEREGNET = 'VEDTAK:BEREGNET',
  VEDTAK_FATTET = 'VEDTAK:FATTET',
  VEDTAK_UNDERKJENT = 'VEDTAK:UNDERKJENT',
  VEDTAK_ATTESTERT = 'VEDTAK:ATTESTERT',
}
