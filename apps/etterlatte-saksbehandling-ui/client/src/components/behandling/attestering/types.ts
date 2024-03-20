export enum IBeslutning {
  godkjenn = 'godkjenn',
  underkjenn = 'underkjenn',
}

export enum IReturTypeBehandling {
  inngangsvilkår_feilvurdert = 'Inngangsvilkår feilvurdert',
  feil_i_beregning = 'Feil i beregning',
  feil_i_brev = 'Feil i brev',
  inntektsavkortning = 'Feil i inntektsavkorting',
  trygdetid = 'Feil i trygdetid',
  feil_i_grunnlag = 'Feil i grunnlag',
  systemfeil = 'Systemfeil',
  dokumentasjon_mangler = 'Dokumentasjon mangler',
  annet = 'Annet',
}

export enum IReturTypeTilbakekreving {
  feil_lovhjemmel = 'Feil lovhjemmel',
  feil_i_beregning = 'Feil i beregning',
  feil_i_brev = 'Feil i brev',
  hent_nytt_grunnlag = 'Hent nytt grunnlag',
}

export enum IReturTypeKlage {
  feil_i_formkrav = 'Feil i formkrav / klagefrist',
  feil_i_utfall = 'Feil i utfall',
  feil_i_brev = 'Feil i brev',
  annet = 'Annet',
}
