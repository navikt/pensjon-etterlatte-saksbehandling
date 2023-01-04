import { GyldigFramsattVurdering } from './GyldigFramsattVurdering'
import { Foreldreansvar } from './Foreldreansvar'
import { Innsender } from './Innsender'
import { Verge } from './Verge'
import { InfobokserWrapper, VurderingsWrapper, SoeknadOversiktWrapper } from '../../styled'
import { IGyldighetResultat, IGyldighetproving, GyldigFramsattType } from '~shared/types/IDetaljertBehandling'
import { Heading } from "@navikt/ds-react";

export const OversiktGyldigFramsatt = ({ gyldigFramsatt }: { gyldigFramsatt: IGyldighetResultat | undefined }) => {
  if (gyldigFramsatt == null) {
    return <div style={{ color: 'red' }}>Kunne ikke hente ut data om søkanden er gyldig framsatt</div>
  }

  const innsenderHarForeldreansvar = gyldigFramsatt.vurderinger.find(
    (g: IGyldighetproving) => g.navn === GyldigFramsattType.HAR_FORELDREANSVAR_FOR_BARNET
  )

  const innsenderErForelder = gyldigFramsatt.vurderinger.find(
    (g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER
  )

  const ingenAnnenVergeEnnForelder = gyldigFramsatt.vurderinger.find(
    (g: IGyldighetproving) => g.navn === GyldigFramsattType.INGEN_ANNEN_VERGE_ENN_FORELDER
  )

  return (
    <>
      <Heading size={"medium"} level={"2"}>Gyldig fremsatt</Heading>
      <SoeknadOversiktWrapper>
        <InfobokserWrapper>
          <Innsender innsenderErForelder={innsenderErForelder} />
          <Foreldreansvar innsenderHarForeldreansvar={innsenderHarForeldreansvar} />
          <Verge ingenAnnenVergeEnnForelder={ingenAnnenVergeEnnForelder} />
        </InfobokserWrapper>
        <VurderingsWrapper>
          <GyldigFramsattVurdering
            gyldigFramsatt={gyldigFramsatt}
            innsenderErForelder={innsenderErForelder}
            innsenderHarForeldreansvar={innsenderHarForeldreansvar}
            ingenAnnenVergeEnnForelder={ingenAnnenVergeEnnForelder}
          />
        </VurderingsWrapper>
      </SoeknadOversiktWrapper>
    </>
  )
}
