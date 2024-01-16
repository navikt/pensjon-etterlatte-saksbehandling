import { isAfter } from 'date-fns'
import { IAdresse } from '~shared/types/IAdresse'
import { IBehandlingStatus, IBehandlingsType, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { JaNei } from '~shared/types/ISvar'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { IHendelse, IHendelseType } from '~shared/types/IHendelse'

export function soeknadsoversiktErFerdigUtfylt(behandling: IDetaljertBehandling): boolean {
  const gyldigUtfylt = !!(
    behandling.gyldighetsprøving &&
    behandling.virkningstidspunkt &&
    behandling.boddEllerArbeidetUtlandet
  )

  if (behandling.sakType == SakType.BARNEPENSJON) {
    const kommerBarnetTilgode = !!behandling.kommerBarnetTilgode && behandling.kommerBarnetTilgode?.svar === JaNei.JA

    return gyldigUtfylt && kommerBarnetTilgode
  } else if (behandling.sakType == SakType.OMSTILLINGSSTOENAD) {
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
    IBehandlingStatus.TRYGDETID_OPPDATERT,
    IBehandlingStatus.BEREGNET,
    IBehandlingStatus.AVKORTET,
    IBehandlingStatus.FATTET_VEDTAK,
    IBehandlingStatus.ATTESTERT,
  ]
  const statusEksisterer = rekkefoelge.includes(status)
  const index = statusEksisterer ? rekkefoelge.findIndex((s) => s === status) + 1 : rekkefoelge.length
  return rekkefoelge.slice(0, index)
}

export const behandlingErRedigerbar = (status: IBehandlingStatus): boolean => {
  return (
    status === IBehandlingStatus.OPPRETTET ||
    status === IBehandlingStatus.VILKAARSVURDERT ||
    status === IBehandlingStatus.TRYGDETID_OPPDATERT ||
    status === IBehandlingStatus.BEREGNET ||
    status === IBehandlingStatus.AVKORTET ||
    status === IBehandlingStatus.RETURNERT
  )
}

export const erFerdigBehandlet = (status: IBehandlingStatus): boolean => {
  return (
    status === IBehandlingStatus.ATTESTERT ||
    status === IBehandlingStatus.TIL_SAMORDNING ||
    status === IBehandlingStatus.SAMORDNET ||
    status === IBehandlingStatus.IVERKSATT ||
    status === IBehandlingStatus.AVBRUTT
  )
}
export const behandlingErIverksattEllerSamordnet = (behandlingStatus: IBehandlingStatus): boolean =>
  behandlingStatus === IBehandlingStatus.IVERKSATT ||
  behandlingStatus === IBehandlingStatus.SAMORDNET ||
  behandlingStatus === IBehandlingStatus.TIL_SAMORDNING

export const behandlingSkalSendeBrev = (
  behandlingType: IBehandlingsType,
  revurderingsaarsak: Revurderingaarsak | null
): boolean => {
  switch (behandlingType) {
    case IBehandlingsType.FØRSTEGANGSBEHANDLING:
      return true
    case IBehandlingsType.MANUELT_OPPHOER:
      return false
    case IBehandlingsType.REVURDERING:
      return !(
        revurderingsaarsak === Revurderingaarsak.REGULERING ||
        revurderingsaarsak === Revurderingaarsak.DOEDSFALL ||
        revurderingsaarsak === Revurderingaarsak.OPPHOER_UTEN_BREV ||
        revurderingsaarsak === Revurderingaarsak.ALDERSOVERGANG
      )
  }
}

export function requireNotNull<T>(value: T | null, message: string): T {
  if (!!value) return value
  else throw Error(message)
}

export const sisteBehandlingHendelse = (hendelser: IHendelse[]): IHendelse => {
  const hendelserSortert = hendelser
    .filter((hendelse) =>
      [
        IHendelseType.BEHANDLING_OPPRETTET,
        IHendelseType.BEHANDLING_VILKAARSVURDERT,
        IHendelseType.BEHANDLING_TRYGDETID_OPPDATERT,
        IHendelseType.BEHANDLING_BEREGNET,
        IHendelseType.BEHANDLING_AVKORTET,
      ].includes(hendelse.hendelse)
    )
    .sort((a, b) => (a.opprettet > b.opprettet ? 1 : -1))

  return hendelserSortert[hendelserSortert.length - 1]
}
