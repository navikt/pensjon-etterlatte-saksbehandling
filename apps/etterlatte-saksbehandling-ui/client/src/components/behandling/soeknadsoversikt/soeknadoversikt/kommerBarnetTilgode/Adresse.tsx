import { VurderingsResultat, IVilkaarResultat, VilkaarsType } from '../../../../../store/reducers/BehandlingReducer'
import { OversiktElement } from '../OversiktElement'
import { hentBarnUtlandsadresseTekst, hentSammeAdresseSomAvdoedTekst, hentSammeAdresseTekst } from '../../utils'

export const Adresse = ({
  kommerSoekerTilgodeVurdering,
}: {
  kommerSoekerTilgodeVurdering: IVilkaarResultat | undefined
}) => {
  const navn = undefined
  const label = 'Adresse'
  const tekst = settTekst()
  const erOppfylt = kommerSoekerTilgodeVurdering?.resultat === VurderingsResultat.OPPFYLT

  function settTekst(): string {
    const sammeAdresse = kommerSoekerTilgodeVurdering?.vilkaar.find(
      (vilkaar) => vilkaar.navn === VilkaarsType.SAMME_ADRESSE
    )
    const barnIngenUtland = kommerSoekerTilgodeVurdering?.vilkaar.find(
      (vilkaar) => vilkaar.navn === VilkaarsType.BARN_INGEN_OPPGITT_UTLANDSADRESSE
    )
    const sammeAdresseAvdoed = kommerSoekerTilgodeVurdering?.vilkaar.find(
      (vilkaar) => vilkaar.navn === VilkaarsType.BARN_BOR_PAA_AVDOEDES_ADRESSE
    )

    const sammeAdresseSvar = hentSammeAdresseTekst(sammeAdresse?.resultat)
    const barnUtlandsadresseSvar = hentBarnUtlandsadresseTekst(barnIngenUtland?.resultat)
    const sammeAdresseAvdoedSvar = hentSammeAdresseSomAvdoedTekst(sammeAdresseAvdoed?.resultat)

    let svar = sammeAdresseSvar + barnUtlandsadresseSvar
    svar = sammeAdresse?.resultat === VurderingsResultat.IKKE_OPPFYLT ? svar + sammeAdresseAvdoedSvar : svar
    return svar
  }

  return <OversiktElement navn={navn} label={label} tekst={tekst} erOppfylt={erOppfylt} />
}
