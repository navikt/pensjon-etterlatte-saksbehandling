import { IGyldighetResultat, IGyldighetproving } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { hentGyldighetsTekst } from '../../../utils'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'

export const GyldigFramsattVurdering = ({
  gyldigFramsatt,
  innsenderHarForeldreansvar,
  innsenderErForelder,
  ingenAnnenVergeEnnForelder,
}: {
  gyldigFramsatt: IGyldighetResultat
  innsenderHarForeldreansvar: IGyldighetproving | undefined
  innsenderErForelder: IGyldighetproving | undefined
  ingenAnnenVergeEnnForelder: IGyldighetproving | undefined
}) => {
  const hentTekst = (): string | undefined => {
    if (gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT) {
      return undefined
    } else {
      return hentGyldighetsTekst(
        innsenderErForelder?.resultat,
        innsenderHarForeldreansvar?.resultat,
        ingenAnnenVergeEnnForelder?.resultat
      )
    }
  }

  const tittel =
    gyldigFramsatt.resultat !== VurderingsResultat.OPPFYLT ? 'Søknad ikke gyldig fremsatt' : 'Søknad gyldig fremsatt'
  const vurderingstekst = hentTekst()

  return (
    <VurderingsboksWrapper
      tittel={tittel}
      redigerbar={false}
      automatiskVurdertDato={new Date(gyldigFramsatt.vurdertDato)}
      kommentar={vurderingstekst}
    />
  )
}
