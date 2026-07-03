export interface AvbrytBehandlingRequest {
  aarsakTilAvbrytelse: AarsakTilAvslutting //TODO
  kommentar: string
}

export enum AarsakTilAvslutting {
  SOEKNAD_TRUKKET = 'SOEKNAD_TRUKKET',
  SAKEN_HENLEGGES_ELLER_SOEKNADEN_ER_IKKE_AKTUELL = 'SAKEN_HENLEGGES_ELLER_SOEKNADEN_ER_IKKE_AKTUELL',
  AVSLUTTE_SAK_VENTER_SVAR_FRA_UTLAND = 'AVSLUTTE_SAK_VENTER_SVAR_FRA_UTLAND',
  ANNET = 'ANNET',
}

export const teksterAArsakTilAvslutting: Record<AarsakTilAvslutting, string> = {
  SOEKNAD_TRUKKET: 'Søknad trukket',
  SAKEN_HENLEGGES_ELLER_SOEKNADEN_ER_IKKE_AKTUELL: 'Saken henlegges / søknaden er ikke aktuell',
  AVSLUTTE_SAK_VENTER_SVAR_FRA_UTLAND: 'Avslutt sak i påvente av opplysninger fra utlandet',
  ANNET: 'Annet',
}
