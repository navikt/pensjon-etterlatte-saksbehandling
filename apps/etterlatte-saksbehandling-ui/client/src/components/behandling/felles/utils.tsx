import { isAfter } from 'date-fns'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'
import { IAdresse } from '~shared/types/IAdresse'
import { IBehandlingStatus, IDetaljertBehandling, IVilkaarsproving } from '~shared/types/IDetaljertBehandling'
import { Kriterietype, IKriterie, KriterieOpplysningsType, IKriterieOpplysning } from '~shared/types/Kriterie'

export function behandlingErUtfylt(behandling: IDetaljertBehandling): boolean {
  return Boolean(behandling.gyldighetsprøving && behandling.kommerBarnetTilgode && behandling.virkningstidspunkt)
}

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: string | null): IAdresse[] {
  if (doedsdato == null) {
    return adresser
  }
  return adresser?.filter((adresse) => adresse.aktiv || isAfter(new Date(adresse.gyldigTilOgMed!), new Date(doedsdato)))
}

export function behandlingErAvslag(behandling: IDetaljertBehandling) {
  return behandling.vilkårsprøving?.resultat?.utfall === VilkaarsvurderingResultat.IKKE_OPPFYLT
}

export const hentBehandlesFraStatus = (status: IBehandlingStatus): boolean => {
  return (
    status === IBehandlingStatus.OPPRETTET ||
    status === IBehandlingStatus.VILKAARSVURDERT ||
    status === IBehandlingStatus.BEREGNET ||
    status === IBehandlingStatus.RETURNERT
  )
}

export function hentUtenlandskAdresseEtterDoedsdato(adresser: IAdresse[], doedsdato: string | null): IAdresse[] {
  const etterDoedsdato = hentAdresserEtterDoedsdato(adresser, doedsdato)
  return etterDoedsdato?.filter((ad) => ad.type === 'UTENLANDSKADRESSE' || ad.type === 'UTENLANDSKADRESSEFRITTFORMAT')
}

export function hentUtlandskeAdresser(adresser: IAdresse[] | undefined): IAdresse[] {
  const utlandsadresser = adresser?.filter(
    (ad) => ad.type === 'UTENLANDSKADRESSE' || ad.type === 'UTENLANDSKADRESSEFRITTFORMAT'
  )
  return utlandsadresser ? utlandsadresser : []
}

export function hentNorskeAdresser(adresser: IAdresse[] | undefined): IAdresse[] {
  const norske = adresser?.filter((ad) => ad.type !== 'UTENLANDSKADRESSE' && ad.type !== 'UTENLANDSKADRESSEFRITTFORMAT')
  return norske ? norske : []
}

export const hentKriterie = (
  vilkaar: IVilkaarsproving | undefined,
  kriterieType: Kriterietype
): IKriterie | undefined => {
  return vilkaar?.kriterier?.find((krit: IKriterie) => krit.navn === kriterieType)
}

export const hentKriterieOpplysning = (
  krit: IKriterie | undefined,
  kriterieOpplysningsType: KriterieOpplysningsType
): IKriterieOpplysning | undefined => {
  return krit?.basertPaaOpplysninger.find(
    (opplysning: IKriterieOpplysning) => opplysning.kriterieOpplysningsType === kriterieOpplysningsType
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
