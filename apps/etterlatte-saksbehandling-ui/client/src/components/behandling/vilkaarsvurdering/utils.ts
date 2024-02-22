import {
  IVilkaarsvurdering,
  Lovreferanse,
  VilkaarsvurderingResultat,
  VurderingsResultat,
} from '~shared/api/vilkaarsvurdering'
import { ISvar } from '~shared/types/ISvar'
import { KildeType } from '~shared/types/kilde'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'

export const svarTilTotalResultat = (svar: ISvar) => {
  switch (svar) {
    case ISvar.JA:
      return VilkaarsvurderingResultat.OPPFYLT
    case ISvar.NEI:
      return VilkaarsvurderingResultat.IKKE_OPPFYLT
    case ISvar.IKKE_VURDERT:
      throw Error('IKKE_VURDERT er ikke et gyldig valg for VilkaarsvurderingResultat')
  }
}

export function hentKildenavn(type?: KildeType): string {
  switch (type) {
    case KildeType.pdl:
      return 'PDL'
    case KildeType.privatperson:
      return 'Søknad'
    case KildeType.a_ordningen:
      return 'A-ordningen'
    case KildeType.aa_registeret:
      return 'AA-registeret'
    case KildeType.vilkaarskomponenten:
      return 'Vilkårskomponenten'
    case KildeType.saksbehandler:
      return 'Saksbehandler'
    default:
      return 'Ukjent kilde'
  }
}

export function formaterVurderingsResultat(vurderingsResultat?: VurderingsResultat | null): string {
  switch (vurderingsResultat) {
    case VurderingsResultat.OPPFYLT:
      return 'Ja'
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'Nei'
    case VurderingsResultat.IKKE_VURDERT:
      return 'Ikke aktuelt'
    default:
      return 'Ukjent'
  }
}

// Siden vi ikke har noen versjonering av vilkårsvurdering p.t. så sjekker vi om det finnes vilkår
// som kun eksisterer for nytt regelverk på barnepensjon.
export function vilkaarsvurderingErPaaNyttRegelverk(vilkaarsvurdering: IVilkaarsvurdering): boolean {
  return vilkaarsvurdering.vilkaar.some(
    (vilkaar) => vilkaar.hovedvilkaar.type.startsWith('BP') && vilkaar.hovedvilkaar.type.endsWith('2024')
  )
}

export function behandlingGjelderBarnepensjonPaaNyttRegelverk(behandling: IDetaljertBehandling): boolean {
  return (
    behandling.sakType === SakType.BARNEPENSJON &&
    new Date(behandling.virkningstidspunkt!!.dato) >= new Date('2024-01-01')
  )
}

export const formatertLovreferanse = (lovreferanse: Lovreferanse) => {
  let formatertStr = lovreferanse.paragraf

  if (lovreferanse.ledd) {
    const leddSomTekst = ['første', 'andre', 'tredje', 'fjerde', 'femte', 'sjette']
    formatertStr += ` ${leddSomTekst[lovreferanse.ledd - 1]} ledd`
  }

  if (lovreferanse.bokstav) {
    // TODO vi har en bug i vilkårsvurdering hvor paragraf har blitt lagret som bokstav - må fikses før dette kan vises
    // formatertStr += ` bokstav ${lovreferanse.bokstav}`
  }
  return formatertStr
}
