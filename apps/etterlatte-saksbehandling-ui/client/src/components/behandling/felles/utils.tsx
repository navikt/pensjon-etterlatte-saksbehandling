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

export const hentBehandlesFraStatus = (status: IBehandlingStatus): boolean => {
  return (
    status === IBehandlingStatus.OPPRETTET ||
    status === IBehandlingStatus.VILKAARSVURDERT ||
    status === IBehandlingStatus.BEREGNET ||
    status === IBehandlingStatus.RETURNERT
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
