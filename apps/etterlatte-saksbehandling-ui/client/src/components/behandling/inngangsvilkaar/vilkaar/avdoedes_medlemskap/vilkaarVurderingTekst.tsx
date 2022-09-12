import { IVilkaarsproving, Kriterietype, VurderingsResultat } from '../../../../../store/reducers/BehandlingReducer'
import { hentKriterie } from '../../../felles/utils'
import { VilkaarVurderingEnkeltElement } from '../VilkaarVurderingsliste'

export function lagVilkaarVisningUtland(vilkaar: IVilkaarsproving) {
  //todo: legg til MEDL her når det er klart
  const utlandKriterierResultater: VurderingsResultat[] = [
    hentKriterie(vilkaar, Kriterietype.AVDOED_IKKE_OPPHOLD_UTLAND_FRA_SOEKNAD),
    hentKriterie(vilkaar, Kriterietype.AVDOED_NORSK_STATSBORGER),
    hentKriterie(vilkaar, Kriterietype.AVDOED_INGEN_INN_ELLER_UTVANDRING),
    hentKriterie(vilkaar, Kriterietype.AVDOED_KUN_NORSKE_BOSTEDSADRESSER),
  ]
    .map((kriterie) => kriterie?.resultat)
    .filter((kriterie) => kriterie !== undefined) as VurderingsResultat[]

  const tittel = 'Utlandsopphold'
  let svar
  if (utlandKriterierResultater.includes(VurderingsResultat.IKKE_OPPFYLT)) {
    svar = 'Avdøde har indikasjoner på utlandsopphold. Må behandles i psys. '
  } else if (utlandKriterierResultater.includes(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING)) {
    svar = 'Mangler opplysninger om utlandsopphold. Må avklares. '
  } else {
    svar = 'Avdøde har ingen indikasjoner på utlandsopphold. '
  }
  return <VilkaarVurderingEnkeltElement tittel={tittel} svar={svar} />
}

export function lagVilkaarVisningAvklaring(vilkaar: IVilkaarsproving) {
  const avklaringResultater: VurderingsResultat[] = [
    hentKriterie(vilkaar, Kriterietype.AVDOED_KUN_NORSKE_KONTAKTADRESSER),
    hentKriterie(vilkaar, Kriterietype.AVDOED_KUN_NORSKE_OPPHOLDSSADRESSER),
    hentKriterie(vilkaar, Kriterietype.AVDOED_SAMMENHENGENDE_BOSTEDSADRESSE_NORGE_SISTE_FEM_AAR),
  ]
    .map((kriterie) => kriterie?.resultat)
    .filter((kriterie) => kriterie !== undefined) as VurderingsResultat[]

  const tittel = 'Adresser'
  let svar
  if (avklaringResultater.includes(VurderingsResultat.IKKE_OPPFYLT)) {
    svar = 'Norske adresser har hull i perioden, eller er utenlandske. Må avklares og legges inn. '
  } else if (avklaringResultater.includes(VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING)) {
    svar = 'Mangler opplysninger om adresser. Må avklares. '
  } else {
    svar = 'Alle adresser har blitt vurdert som opphold i Norge. '
  }
  return <VilkaarVurderingEnkeltElement tittel={tittel} svar={svar} />
}
