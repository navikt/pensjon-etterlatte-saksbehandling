import { GyldigFramsattVurdering } from './GyldigFramsattVurdering'
import { Foreldreansvar } from './Foreldreansvar'
import { Innsender } from './Innsender'
import { Verge } from './Verge'
import { InfobokserWrapper, Beskrivelse, VurderingsContainerWrapper } from '../../styled'
import { IGyldighetResultat, IGyldighetproving, GyldigFramsattType } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { Soeknadsvurdering } from '../SoeknadsVurdering'

const gyldigFremsattTilStatusIcon = (gyldigFramsatt: IGyldighetResultat | undefined) => {
  if (gyldigFramsatt == undefined || gyldigFramsatt.resultat == undefined) {
    return 'warning'
  }

  switch (gyldigFramsatt.resultat) {
    case VurderingsResultat.OPPFYLT:
      return 'success'
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'error'
    default:
      return 'warning'
  }
}

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
    <Soeknadsvurdering
      tittel="Gyldig fremsatt"
      hjemler={[
        {
          lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-13',
          tittel: 'Folketrygdloven § 22-13 første ledd og tilhørende rundskriv',
        },
        { lenke: 'https://lovdata.no/lov/2010-03-26-9/§9', tittel: 'Vergemålsloven § 9, § 16 og § 19' },
      ]}
      status={gyldigFremsattTilStatusIcon(gyldigFramsatt)}
    >
      <div>
        <Beskrivelse>Den som har rett til ytelsen må sette frem krav (forelder/verge hvis under 18 år).</Beskrivelse>
        <InfobokserWrapper>
          <Innsender innsenderErForelder={innsenderErForelder} />
          <Foreldreansvar innsenderHarForeldreansvar={innsenderHarForeldreansvar} />
          <Verge ingenAnnenVergeEnnForelder={ingenAnnenVergeEnnForelder} />
        </InfobokserWrapper>
      </div>
      <VurderingsContainerWrapper>
        <GyldigFramsattVurdering
          gyldigFramsatt={gyldigFramsatt}
          innsenderErForelder={innsenderErForelder}
          innsenderHarForeldreansvar={innsenderHarForeldreansvar}
          ingenAnnenVergeEnnForelder={ingenAnnenVergeEnnForelder}
        />
      </VurderingsContainerWrapper>
    </Soeknadsvurdering>
  )
}
