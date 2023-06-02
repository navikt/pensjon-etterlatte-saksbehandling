import { isAfter } from 'date-fns'
import { IAdresse } from '~shared/types/IAdresse'
import { IBehandlingStatus, IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { IBehandlingsammendrag } from '~components/person/typer'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { JaNei } from '~shared/types/ISvar'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'

export function behandlingErUtfylt(behandling: IDetaljertBehandling): boolean {
  const gyldigUtfylt = !!(behandling.gyldighetsprøving && behandling.virkningstidspunkt)

  if (behandling.sakType == ISaksType.BARNEPENSJON) {
    const kommerBarnetTilgode = !!behandling.kommerBarnetTilgode && behandling.kommerBarnetTilgode?.svar === JaNei.JA

    return gyldigUtfylt && kommerBarnetTilgode
  } else if (behandling.sakType == ISaksType.OMSTILLINGSSTOENAD) {
    return gyldigUtfylt
  } else {
    return false
  }
}

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: string | null): IAdresse[] {
  if (doedsdato == null) {
    return adresser
  }
  return adresser?.filter((adresse) => adresse.aktiv || isAfter(new Date(adresse.gyldigTilOgMed!), new Date(doedsdato)))
}

export const hentGyldigeNavigeringsStatuser = (status: IBehandlingStatus) => {
  const rekkefoelge = [
    IBehandlingStatus.OPPRETTET,
    IBehandlingStatus.VILKAARSVURDERT,
    IBehandlingStatus.BEREGNET,
    IBehandlingStatus.FATTET_VEDTAK,
    IBehandlingStatus.ATTESTERT,
  ]
  const statusEksisterer = rekkefoelge.includes(status)
  const index = statusEksisterer ? rekkefoelge.findIndex((s) => s === status) + 1 : rekkefoelge.length
  return rekkefoelge.slice(0, index)
}

export const hentBehandlesFraStatus = (status: IBehandlingStatus): boolean => {
  return (
    status === IBehandlingStatus.OPPRETTET ||
    status === IBehandlingStatus.VILKAARSVURDERT ||
    status === IBehandlingStatus.BEREGNET ||
    status === IBehandlingStatus.AVKORTET ||
    status === IBehandlingStatus.RETURNERT
  )
}

export const erFerdigBehandlet = (status: IBehandlingStatus): boolean => {
  return (
    status === IBehandlingStatus.ATTESTERT ||
    status === IBehandlingStatus.IVERKSATT ||
    status === IBehandlingStatus.AVBRUTT
  )
}

export const harIngenUavbrutteManuelleOpphoer = (behandlingliste: IBehandlingsammendrag[]): boolean =>
  behandlingliste.every(
    (behandling) =>
      behandling.behandlingType !== IBehandlingsType.MANUELT_OPPHOER || behandling.status === IBehandlingStatus.AVBRUTT
  )

export const kunIverksatteBehandlinger = (behandlingliste: IBehandlingsammendrag[]): IBehandlingsammendrag[] =>
  behandlingliste.filter((behandling) => behandling.status === IBehandlingStatus.IVERKSATT)

export const behandlingSkalSendeBrev = (behandling: IBehandlingReducer): boolean => {
  switch (behandling.behandlingType) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return true
    case IBehandlingsType.MANUELT_OPPHOER:
      return false
    case IBehandlingsType.REVURDERING:
      return !(
        behandling.revurderingsaarsak === Revurderingsaarsak.REGULERING ||
        behandling.revurderingsaarsak === Revurderingsaarsak.DOEDSFALL
      )
  }
}

export const manueltBrevKanRedigeres = (status: IBehandlingStatus): boolean => {
  switch (status) {
    case IBehandlingStatus.OPPRETTET:
    case IBehandlingStatus.VILKAARSVURDERT:
    case IBehandlingStatus.BEREGNET:
    case IBehandlingStatus.AVKORTET:
    case IBehandlingStatus.RETURNERT:
      return true
    default:
      return false
  }
}
