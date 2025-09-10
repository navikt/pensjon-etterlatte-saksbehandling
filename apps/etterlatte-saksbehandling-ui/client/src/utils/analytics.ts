import { JaNei } from '~shared/types/ISvar'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { IBehandlingsType, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { sendHendelseTilUmami } from '~shared/umami/umami'

export enum LogEvents {
  CLICK = 'klikk',
}

export enum ClickEvent {
  // Sak
  VIS_SAKSHISTORIKK = 'vis sakshistorikk',
  MANUELT_ENDRET_ENHET = 'manuelt endret enhet',

  //Aktivitetsplikt
  SJEKKER_SISTE_BEREGNING = 'sjekker siste beregning',

  // Vilkaarsvurdering
  SLETT_VILKAARSVURDERING = 'slett vilkaarsvurdering',

  // Gosys
  FERDIGSTILL_GOSYS_OPPGAVE = 'ferdigstill gosys oppgave',
  FLYTT_GOSYS_OPPGAVE = 'flytt gosys oppgave',

  // Journalpost
  FEILREGISTRER_JOURNALPOST = 'feilregistrer journalpost',
  OPPHEV_FEILREGISTRERING_JOURNALPOST = 'opphev feilregistrering journalpost',
  FLYTT_JOURNALPOST = 'flytt journalpost',
  KNYTT_JOURNALPOST_TIL_ANNEN_SAK = 'knytt journalpost til annen sak',

  // Brev
  OPPRETT_NYTT_BREV = 'opprett nytt brev',
  SLETT_BREV = 'slett brev',
  LAST_OPP_BREV = 'last opp brev',
  TILBAKESTILL_MOTTAKERE_BREV = 'tilbakestill mottakere for brev',

  // Notat
  OPPRETT_NYTT_NOTAT = 'opprett nytt notat',
  SLETT_NOTAT = 'slett notat',
  JOURNALFOER_NOTAT = 'journalfoer notat',

  // Oppgave
  OPPRETT_GENERELL_OPPGAVE = 'opprett generell oppgave',
  OPPRETT_JOURNALFOERINGSOPPGAVE = 'opprett journalfoeringsoppgave',
  OPPRETT_FRIKOBLET_JOURNALFOERINGSOPPGAVE = 'opprett journalfoeringsoppgave frikoblet',
  TILDEL_TILKNYTTEDE_OPPGAVER = 'tildel tilknyttede oppgaver',
  AAPNE_OPPFOELGINGSOPPGAVE_MODAL = 'åpne opprett oppfølgingsoppgave modal',
  OPPRETT_OPPFOELGINGSOPPGAVE = 'opprett oppfølgingsoppgave',

  // Avkorting
  AVKORTING_FORVENTET_INNTEKT_HJELPETEKST = 'avkorting forventet inntekt hjelpetekst',
  AVKORTING_INNVILGA_MAANEDER_HJELPETEKST = 'avkorting innvilga måneder hjelpetekst',

  // Trygdetid
  KOPIER_TRYGDETIDSGRUNNLAG_FRA_BEHANDLING_MED_SAMME_AVDOEDE = 'kopier trygdetidsgrunnlag fra behandling med samme avdoede',

  // Vilkårsvurdering
  KOPIER_VILKAAR_FRA_BEHANDLING_MED_SAMME_AVDOED = 'kopier vilkår fra behandling med samme avdoed',

  // Tilbakemeldinger
  TILBAKEMELDING_SAKSBEHANDLING_UTLAND_FOERSTEGANGSBEHANDLING = 'tilbakemelding saksbehandling utland førstegangsbehandling',
  TILBAKEMELDING_SAKSBEHANDLING_UTLAND_AVSLAG = 'tilbakemelding saksbehandling utland avslag',
  TILBAKEMELDING_SAKSBEHANDLING_UTLAND_SLUTTBEHANDLING = 'tilbakemelding saksbehandling utland sluttbehandling',

  // Personopplysninger
  AAPNE_PERSONOPPLYSNINGER_FRA_STATUS_BAR = 'åpne personopplysninger fra status bar',

  // Bytt sak i sakoversikten
  BYTT_SAK_SAKOVERSIKT = 'bytt sak sakoversikt',

  // Sidemeny
  KOLLAPS_SIDEMENY = 'kollaps sidmeny',

  // Generelt
  VIS_VARSLINGER = 'vis varslinger',
  VIS_BEHANDLING_HISTORIKK = 'vis behandling historikk',
}

export const trackClick = (name: ClickEvent) => {
  sendHendelseTilUmami(LogEvents.CLICK, { tekst: name })
}

export const trackClickJaNei = (name: ClickEvent, svar: JaNei) => trackClickMedSvar(name, svar)

export const trackClickMedSvar = (name: ClickEvent, svar: string) => {
  sendHendelseTilUmami(LogEvents.CLICK, { tekst: name, svar })
}

export function velgTilbakemeldingClickEventForUtlandsbehandling(
  behandling: IBehandlingReducer
): ClickEvent | undefined {
  // Hvis vi har en førstegangsbehandling utland
  const erFoerstegangsbehandling = behandling.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING
  const utlandstilknytningErUtland =
    !!behandling.utlandstilknytning && behandling.utlandstilknytning.type !== UtlandstilknytningType.NASJONAL
  if (erFoerstegangsbehandling && utlandstilknytningErUtland) {
    const vilkaarsvurderingGirAvslag =
      behandling.vilkaarsvurdering?.resultat?.utfall === VilkaarsvurderingResultat.IKKE_OPPFYLT
    if (vilkaarsvurderingGirAvslag) {
      // avslag er et eget event som vi sjekker først
      return ClickEvent.TILBAKEMELDING_SAKSBEHANDLING_UTLAND_AVSLAG
    } else if (behandling.erSluttbehandling) {
      // Hvis ikke avslag -- er dette en sluttbehandling
      return ClickEvent.TILBAKEMELDING_SAKSBEHANDLING_UTLAND_SLUTTBEHANDLING
    } else {
      // Det er en utland førstegangsbehandling
      return ClickEvent.TILBAKEMELDING_SAKSBEHANDLING_UTLAND_FOERSTEGANGSBEHANDLING
    }
  } else if (behandling.revurderingsaarsak === Revurderingaarsak.SLUTTBEHANDLING) {
    // Revurderinger er sluttbehandlinger hvis det er revurderingsårsak sluttbehandling utland
    return ClickEvent.TILBAKEMELDING_SAKSBEHANDLING_UTLAND_SLUTTBEHANDLING
  }
  return undefined
}
