import { isAfter } from 'date-fns'
import { IAdresse } from '~shared/types/IAdresse'
import { IBehandlingStatus, IDetaljertBehandling, IVilkaarsproving } from '~shared/types/IDetaljertBehandling'
import { IKriterie, IKriterieOpplysning, KriterieOpplysningsType, Kriterietype } from '~shared/types/Kriterie'

export function behandlingErUtfylt(behandling: IDetaljertBehandling): boolean {
  return Boolean(behandling.gyldighetsprøving && behandling.kommerBarnetTilgode && behandling.virkningstidspunkt)
}

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: string | null): IAdresse[] {
  if (doedsdato == null) {
    return adresser
  }
  return adresser?.filter((adresse) => adresse.aktiv || isAfter(new Date(adresse.gyldigTilOgMed!), new Date(doedsdato)))
}

export const kanGaaTilStatus = (status: IBehandlingStatus) => {
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

export const hentKriterierMedOpplysning = (
  vilkaar: IVilkaarsproving | undefined,
  kriterieType: Kriterietype,
  kriterieOpplysningsType: KriterieOpplysningsType
): IKriterieOpplysning | undefined => {
  try {
    return vilkaar?.kriterier
      ?.find((krit: IKriterie) => krit.navn === kriterieType)
      ?.basertPaaOpplysninger.find(
        (opplysning: IKriterieOpplysning) => opplysning.kriterieOpplysningsType === kriterieOpplysningsType
      )
  } catch (e: any) {
    console.error(e)
  }
}
