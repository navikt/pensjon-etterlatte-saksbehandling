import { AarsaksTyper, BehandlingOgRevurderingsAarsakerType, IBehandlingsammendrag } from '~components/person/typer'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { GenerellBehandlingType, Status } from '~shared/types/Generellbehandling'

export function mapAarsak(aarsak: BehandlingOgRevurderingsAarsakerType) {
  switch (aarsak) {
    case AarsaksTyper.MANUELT_OPPHOER:
      return 'Manuelt opphør'
    case AarsaksTyper.SOEKNAD:
      return 'Søknad'
    case AarsaksTyper.REVURDERING:
      return 'Revurdering'
    case Revurderingaarsak.REGULERING:
      return 'Regulering'
    case Revurderingaarsak.ANSVARLIGE_FORELDRE:
      return 'Ansvarlige foreldre'
    case Revurderingaarsak.SOESKENJUSTERING:
      return 'Søskenjustering'
    case Revurderingaarsak.UTLAND:
      return 'Ut-/innflytting til Norge'
    case Revurderingaarsak.EKSPORT:
      return 'Eksport / Utvandring'
    case Revurderingaarsak.IMPORT:
      return 'Import / Innvandring'
    case Revurderingaarsak.BARN:
      return 'Barn'
    case Revurderingaarsak.DOEDSFALL:
      return 'Dødsfall'
    case Revurderingaarsak.OMGJOERING_AV_FARSKAP:
      return 'Omgjøring av farskap'
    case Revurderingaarsak.ADOPSJON:
      return 'Adopsjon'
    case Revurderingaarsak.VERGEMAAL_ELLER_FREMTIDSFULLMAKT:
      return 'Institusjonsopphold'
    case Revurderingaarsak.SIVILSTAND:
      return 'Endring av sivilstand'
    case Revurderingaarsak.NY_SOEKNAD:
      return 'Ny Søknad'
    case Revurderingaarsak.FENGSELSOPPHOLD:
      return 'Fengselsopphold'
    case Revurderingaarsak.YRKESSKADE:
      return 'Yrkesskade'
    case Revurderingaarsak.UT_AV_FENGSEL:
      return 'Ut av fengsel'
    case Revurderingaarsak.ANNEN:
      return 'Annen (med brev)'
    case Revurderingaarsak.ANNEN_UTEN_BREV:
      return 'Annen (uten brev)'
    case Revurderingaarsak.RETT_UTEN_TIDSBEGRENSNING:
      return 'Stønad uten tidsbegrensning'
    case Revurderingaarsak.OPPHOER_UTEN_BREV:
      return 'Opphør (uten brev)'
    case Revurderingaarsak.AVKORTING_MOT_UFOERETRYGD:
      return 'Avkorting mot uføretrygd'
    case Revurderingaarsak.ETTEROPPGJOER:
      return 'Etteroppgjør'
    case Revurderingaarsak.FORELDRELOES:
      return 'Fra en til to døde foreldre'
    case Revurderingaarsak.OMGJOERING_ETTER_ANKE:
      return 'Omgjøring etter anke'
    case Revurderingaarsak.OMGJOERING_PAA_EGET_INITIATIV:
      return 'Omgjøring på eget intitiativ'
    case Revurderingaarsak.OMGJOERING_ETTER_KRAV_FRA_BRUKER:
      return 'Omgjøring etter krav fra bruker'
    case Revurderingaarsak.OPPHOER_3_AAR_ETTER_DOEDSFALL:
      return 'Opphør 3 år etter dødsfall'
    case Revurderingaarsak.OPPHOER_AV_2_UTVIDEDE_AAR:
      return 'Opphør av 2 utvidede år'
    case Revurderingaarsak.SANKSJON_PGA_MANGLENDE_OPPLYSNINGER:
      return 'Sanksjon pga manglende opplysninger'
    case Revurderingaarsak.SOEKNAD_OM_GJENOPPTAK:
      return 'Søknad om gjenopptak'
    case Revurderingaarsak.UTSENDELSE_AV_KRAVPAKKE:
      return 'Utsendelse av kravpakke'
    case Revurderingaarsak.UTSENDELSE_AV_SED:
      return 'Utsendelse av SED'
  }
}

export function behandlingStatusTilLesbartnavn(status: IBehandlingStatus) {
  switch (status) {
    case IBehandlingStatus.FATTET_VEDTAK:
      return 'Til attestering'
    case IBehandlingStatus.ATTESTERT:
      return 'Attestert'
    case IBehandlingStatus.TIL_SAMORDNING:
      return 'Til samordning'
    case IBehandlingStatus.SAMORDNET:
      return 'Samordnet'
    case IBehandlingStatus.AVSLAG:
      return 'Avslag'
    case IBehandlingStatus.IVERKSATT:
      return 'Iverksatt'
    default:
      return status
  }
}

export const lenkeTilBehandling = (behandlingSammendrag: IBehandlingsammendrag): string => {
  switch (behandlingSammendrag.behandlingType) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return `/behandling/${behandlingSammendrag.id}/soeknadsoversikt`
    case IBehandlingsType.REVURDERING:
      return `/behandling/${behandlingSammendrag.id}/revurderingsoversikt`
  }
}
export const generellBehandlingsStatusTilLesbartNavn = (status: Status) => {
  switch (status) {
    case Status.RETURNERT:
      return 'Returnert'
    case Status.OPPRETTET:
      return 'Opprettet'
    case Status.FATTET:
      return 'Fattet'
    case Status.ATTESTERT:
      return 'Attestert'
    case Status.AVBRUTT:
      return 'Avbrutt'
  }
}
export const genbehandlingTypeTilLesbartNavn = (type: GenerellBehandlingType) => {
  switch (type) {
    case 'KRAVPAKKE_UTLAND':
      return 'Kravpakke til utland'
    case 'ANNEN':
      return 'Annen'
  }
}
