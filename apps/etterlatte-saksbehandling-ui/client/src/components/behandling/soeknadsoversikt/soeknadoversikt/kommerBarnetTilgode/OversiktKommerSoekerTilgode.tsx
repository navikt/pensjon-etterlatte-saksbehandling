import { Adresse } from './Adresse'
import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import {
  IKommerSoekerTilgode, ISvar, KriterieOpplysningsType, Kriterietype, VilkaarsType, VurderingsResultat
} from '../../../../../store/reducers/BehandlingReducer'
import { InfobokserWrapper, Header, InfoWrapper, SoeknadOversiktWrapper, VurderingsWrapper } from '../../styled'
import { hentKriterierMedOpplysning } from "../../../felles/utils";
import { hentKommerBarnetTilgodeTekst, hentKommerBarnetTilgodeVurderingsTekst } from "../../utils";

export const OversiktKommerSoekerTilgode = ({
  kommerSoekerTilgode,
}: {
  kommerSoekerTilgode: IKommerSoekerTilgode | undefined
}) => {
  const vilkaar = kommerSoekerTilgode?.kommerSoekerTilgodeVurdering?.vilkaar

  const sammeAdresse = vilkaar?.find((vilkaar) => vilkaar.navn === VilkaarsType.SAMME_ADRESSE)
  const barnIngenUtland = vilkaar?.find((vilkaar) => vilkaar.navn === VilkaarsType.BARN_INGEN_OPPGITT_UTLANDSADRESSE)
  const sammeAdresseAvdoed = vilkaar?.find((vilkaar) => vilkaar.navn === VilkaarsType.BARN_BOR_PAA_AVDOEDES_ADRESSE)
  const saksbehandlerVurdering = vilkaar?.find((vilkaar) => vilkaar.navn === VilkaarsType.SAKSBEHANDLER_RESULTAT)
  const saksbehandlerOpplysning = hentKriterierMedOpplysning(saksbehandlerVurdering,
    Kriterietype.SAKSBEHANDLER_RESULTAT,
    KriterieOpplysningsType.SAKSBEHANDLER_RESULTAT
  )
  const saksbehandlerResultat = saksbehandlerOpplysning?.opplysning?.svar === ISvar.JA ? VurderingsResultat.OPPFYLT :
    VurderingsResultat.IKKE_OPPFYLT

  const kommerBarnetTilgodeTekst = hentKommerBarnetTilgodeTekst(sammeAdresse?.resultat,
    barnIngenUtland?.resultat,
    sammeAdresseAvdoed?.resultat,
    saksbehandlerResultat
  )

  const kommerBarnetTilgodeVurderingsTekst = hentKommerBarnetTilgodeVurderingsTekst(sammeAdresse?.resultat,
    barnIngenUtland?.resultat,
    sammeAdresseAvdoed?.resultat,
  )

  return (
    <>
      <Header>Vurdering om pensjonen kommer barnet til gode</Header>
      {kommerSoekerTilgode ? (
        <SoeknadOversiktWrapper>
          <InfobokserWrapper>
            <InfoWrapper>
              <Adresse
                resultat={kommerSoekerTilgode.kommerSoekerTilgodeVurdering.resultat}
                tekst={kommerBarnetTilgodeTekst}/>
            </InfoWrapper>
          </InfobokserWrapper>
          <VurderingsWrapper>
            <KommerBarnetTilGodeVurdering
              kommerSoekerTilgodeVurdering={kommerSoekerTilgode.kommerSoekerTilgodeVurdering}
              automatiskTekst={kommerBarnetTilgodeVurderingsTekst}
            />
          </VurderingsWrapper>
        </SoeknadOversiktWrapper>
      ) : (
        <div style={{color: 'red'}}>Kunne ikke hente ut data om pensjon kommer barnet tilgode</div>
      )}
    </>
  )
}
