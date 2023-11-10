export enum IBeslutning {
  godkjenn = 'godkjenn',
  underkjenn = 'underkjenn',
}

export enum IReturTypeBehandling {
  velg = 'Velg',
  inngangsvilkår_feilvurdert = 'Inngangsvilkår feilvurdert',
  feil_i_beregning = 'Feil i beregning',
  feil_i_brev = 'Feil i brev',
  dokumentasjon_mangler = 'Dokumentasjon mangler',
  annet = 'Annet',
}

export enum IReturTypeTilbakekreving {
  velg = 'Velg',
  feil_lovhjemmel = 'Feil lovhjemmel',
  feil_i_beregning = 'Feil i beregning',
  feil_i_brev = 'Feil i brev',
  hent_nytt_grunnlag = 'Hent nytt grunnlag',
}
