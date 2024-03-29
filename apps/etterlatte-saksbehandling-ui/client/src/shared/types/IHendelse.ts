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
  BEHANDLING_VILKAARSVURDERT = 'BEHANDLING:VILKAARSVURDERT',
  BEHANDLING_TRYGDETID_OPPDATERT = 'BEHANDLING:TRYGDETID_OPPDATERT',
  BEHANDLING_BEREGNET = 'BEHANDLING:BEREGNET',
  BEHANDLING_AVKORTET = 'BEHANDLING:AVKORTET',
  VEDTAK_FATTET = 'VEDTAK:FATTET',
  VEDTAK_UNDERKJENT = 'VEDTAK:UNDERKJENT',
  VEDTAK_ATTESTERT = 'VEDTAK:ATTESTERT',
}
