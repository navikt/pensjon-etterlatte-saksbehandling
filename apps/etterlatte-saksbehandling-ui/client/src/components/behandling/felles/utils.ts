import { differenceInYears, isAfter, parse } from 'date-fns'
import { IAdresse } from '~shared/types/IAdresse'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { JaNei } from '~shared/types/ISvar'
import { IHendelse, IHendelseType } from '~shared/types/IHendelse'
import { DatoFormat } from '~utils/formatering/dato'

export const hentAlderForDato = (dato: Date) =>
  differenceInYears(new Date(), parse(String(dato), DatoFormat.AAR_MAANED_DAG, new Date()))

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

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: Date | undefined): IAdresse[] {
  if (doedsdato == null) {
    return adresser
  }
  return adresser?.filter((adresse) => adresse.aktiv || isAfter(new Date(adresse.gyldigTilOgMed!), doedsdato))
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

export const enhetErSkrivbar = (enhetId: string, enheter: Array<string>): boolean => {
  return enheter.includes(enhetId)
}

export const statusErRedigerbar = (status: IBehandlingStatus) => {
  return (
    status === IBehandlingStatus.OPPRETTET ||
    status === IBehandlingStatus.VILKAARSVURDERT ||
    status === IBehandlingStatus.TRYGDETID_OPPDATERT ||
    status === IBehandlingStatus.BEREGNET ||
    status === IBehandlingStatus.AVKORTET ||
    status === IBehandlingStatus.RETURNERT
  )
}

export const behandlingErRedigerbar = (status: IBehandlingStatus, enhetId: string, enheter: Array<string>): boolean => {
  return enhetErSkrivbar(enhetId, enheter) && statusErRedigerbar(status)
}

export const erFerdigBehandlet = (status: IBehandlingStatus): boolean => {
  return (
    status === IBehandlingStatus.ATTESTERT ||
    status === IBehandlingStatus.TIL_SAMORDNING ||
    status === IBehandlingStatus.SAMORDNET ||
    status === IBehandlingStatus.IVERKSATT ||
    status === IBehandlingStatus.AVSLAG ||
    status === IBehandlingStatus.AVBRUTT
  )
}
export const behandlingErIverksatt = (behandlingStatus: IBehandlingStatus): boolean =>
  behandlingStatus === IBehandlingStatus.IVERKSATT

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

export function hasValue<T>(value?: T | null): value is T {
  return value !== undefined && value !== null
}
