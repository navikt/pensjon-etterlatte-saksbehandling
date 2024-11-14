export interface AvbrytBehandlingRequest {
  aarsakTilAvbrytelse: AarsakTilAvsluttingFoerstegangsbehandling | AarsakTilAvsluttingRevurdering
  kommentar: string
}

export enum AarsakTilAvsluttingFoerstegangsbehandling {
  SOEKNADEN_ER_IKKE_AKTUELL = 'SOEKNADEN_ER_IKKE_AKTUELL',
  FEILREGISTRERT = 'FEILREGISTRERT',
  ANNET = 'ANNET',
}

export enum AarsakTilAvsluttingRevurdering {
  IKKE_LENGER_AKTUELL = 'IKKE_LENGER_AKTUELL',
  FEILREGISTRERT = 'FEILREGISTRERT',
  AVBRUTT_PAA_GRUNN_AV_FEIL = 'AVBRUTT_PAA_GRUNN_AV_FEIL',
  ANNET = 'ANNET',
}

export const teksterAarsakTilAvsluttingFoerstegangsbehandling: Record<
  AarsakTilAvsluttingFoerstegangsbehandling,
  string
> = {
  SOEKNADEN_ER_IKKE_AKTUELL: 'Søknaden er ikke aktuell',
  FEILREGISTRERT: 'Søknaden er feilregistrert',
  ANNET: 'Annet',
}

export const teksterAarsakTilAvsluttingRevurdering: Record<AarsakTilAvsluttingRevurdering, string> = {
  IKKE_LENGER_AKTUELL: 'Revurderingen er ikke lengre aktuell',
  FEILREGISTRERT: 'Revurderingen er feilregistrert',
  AVBRUTT_PAA_GRUNN_AV_FEIL: 'Revurderingen er avbrutt på grunn av feil i Gjenny',
  ANNET: 'Annet',
}
