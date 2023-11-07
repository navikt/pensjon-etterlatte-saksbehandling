import {
  GyldigFramsattType,
  IDetaljertBehandling,
  IGyldighetproving,
  IGyldighetResultat,
} from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LovtekstMedLenke'
import {
  Beskrivelse,
  InfobokserWrapper,
  VurderingsContainerWrapper,
} from '~components/behandling/soeknadsoversikt/styled'
import { Innsender } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/barnepensjon/Innsender'
import { Foreldreansvar } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/barnepensjon/Foreldreansvar'
import { Verger } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/barnepensjon/Verger'
import { GyldigFramsattVurdering } from '~components/behandling/soeknadsoversikt/soeknadoversikt/gyldigFramsattSoeknad/barnepensjon/GyldigFramsattVurdering'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'

export const GyldigFramsattBarnepensjon = ({
  behandling,
  gyldigFramsatt,
  gyldigFremsattTilStatusIcon,
}: {
  behandling: IDetaljertBehandling
  gyldigFramsatt: IGyldighetResultat | undefined
  gyldigFremsattTilStatusIcon: 'success' | 'error' | 'warning'
}) => {
  if (gyldigFramsatt == null) {
    return <div style={{ color: 'red' }}>Kunne ikke hente ut data om søknaden er gyldig framsatt</div>
  }

  const behandles = hentBehandlesFraStatus(behandling.status)
  const innsenderHarForeldreansvar = gyldigFramsatt.vurderinger.find(
    (g: IGyldighetproving) => g.navn === GyldigFramsattType.HAR_FORELDREANSVAR_FOR_BARNET
  )
  const innsenderErForelder = gyldigFramsatt.vurderinger.find(
    (g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER
  )

  return (
    <LovtekstMedLenke
      tittel="Vurdering - søknad gyldig fremsatt"
      hjemler={[
        {
          lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-13',
          tittel: 'Folketrygdloven § 22-13 første ledd og tilhørende rundskriv',
        },
        { lenke: 'https://lovdata.no/lov/2010-03-26-9/§9', tittel: 'Vergemålsloven § 9, § 16 og § 19' },
      ]}
      status={gyldigFremsattTilStatusIcon}
    >
      <div>
        <Beskrivelse>
          Den som har rett til ytelsen må sette frem krav (forelder/verge hvis under 18 år). Om annet må fullmakt ligge
          i saken. Søknaden må være signert og vise hva det søkes om, og den må være fremsatt i riktig land. Søknaden må
          være fremstilt i riktig land, dette skal være landet bruker er bosatt i (unntak hvis ikke avtaleland).
        </Beskrivelse>
        <InfobokserWrapper>
          <Innsender innsenderErForelder={innsenderErForelder} />
          <Foreldreansvar innsenderHarForeldreansvar={innsenderHarForeldreansvar} />
          <Verger behandlingId={behandling.id} sakId={behandling.sakId} />
        </InfobokserWrapper>
      </div>
      <VurderingsContainerWrapper>
        <GyldigFramsattVurdering
          behandlingId={behandling.id}
          gyldigFramsatt={behandling.gyldighetsprøving}
          redigerbar={behandles}
        />
      </VurderingsContainerWrapper>
    </LovtekstMedLenke>
  )
}
