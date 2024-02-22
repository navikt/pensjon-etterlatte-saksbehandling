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
      return 'Annen'
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
    case IBehandlingsType.MANUELT_OPPHOER:
      return `/behandling/${behandlingSammendrag.id}/opphoeroversikt`
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
