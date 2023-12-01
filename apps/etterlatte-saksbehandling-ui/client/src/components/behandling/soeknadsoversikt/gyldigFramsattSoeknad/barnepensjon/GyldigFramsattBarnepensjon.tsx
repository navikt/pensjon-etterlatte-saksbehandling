import { GyldigFramsattType, IDetaljertBehandling, IGyldighetResultat } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/LovtekstMedLenke'
import {
  Beskrivelse,
  InfobokserWrapper,
  VurderingsContainerWrapper,
} from '~components/behandling/soeknadsoversikt/styled'
import { Innsender } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/barnepensjon/Innsender'
import { Foreldreansvar } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/barnepensjon/Foreldreansvar'
import { Verger } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/barnepensjon/Verger'
import {
  finnVurdering,
  GyldigFramsattVurdering,
} from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/barnepensjon/GyldigFramsattVurdering'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { StatusIconProps } from '~shared/icons/statusIcon'
import { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getPersongalleriFraSoeknad } from '~shared/api/grunnlag'
import { Familieforhold } from '~shared/types/Person'

import { isSuccess } from '~shared/api/apiUtils'

export const GyldigFramsattBarnepensjon = ({
  behandling,
  familieforhold,
  gyldigFramsatt,
  gyldigFremsattTilStatusIcon,
}: {
  behandling: IDetaljertBehandling
  familieforhold: Familieforhold
  gyldigFramsatt: IGyldighetResultat | undefined
  gyldigFremsattTilStatusIcon: StatusIconProps
}) => {
  if (gyldigFramsatt == null) {
    return <div style={{ color: 'red' }}>Kunne ikke hente ut data om søknaden er gyldig framsatt</div>
  }

  const redigerbar = behandlingErRedigerbar(behandling.status)
  const [personGalleriSoeknad, getPersonGalleriSoeknad] = useApiCall(getPersongalleriFraSoeknad)
  useEffect(() => {
    getPersonGalleriSoeknad({ sakId: behandling.sakId, behandlingId: behandling.id })
  }, [behandling.sakId, behandling.id])
  const manuellVurdering = finnVurdering(gyldigFramsatt, GyldigFramsattType.MANUELL_VURDERING)?.basertPaaOpplysninger
  const harKildePesys = manuellVurdering?.kilde?.ident == 'PESYS'

  return (
    <>
      {isSuccess(personGalleriSoeknad) && (
        <LovtekstMedLenke
          tittel="Vurdering - søknad gyldig fremsatt"
          hjemler={[
            {
              lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-13',
              tittel: 'Folketrygdloven § 22-13 første ledd og tilhørende rundskriv',
            },
            { lenke: 'https://lovdata.no/lov/2010-03-26-9/§9', tittel: 'Vergemålsloven § 9, § 16 og § 19' },
            {
              lenke: 'https://lovdata.no/pro/rundskriv/a45-01-02/ARTIKKEL_45',
              tittel: 'Forordning 987/2009 artikkel 45 nr. 4',
            },
          ]}
          status={gyldigFremsattTilStatusIcon}
        >
          <div>
            <Beskrivelse>
              Den som har rett til ytelsen må sette frem krav (forelder/verge hvis under 18 år). Om annet må fullmakt
              ligge i saken. Søknaden må være signert og vise hva det søkes om, og den må settes fram i bostedslandet
              eller i det landet vedkommende sist var medlem.
            </Beskrivelse>
            <InfobokserWrapper>
              <Innsender
                harKildePesys={harKildePesys}
                persongalleriGrunnlag={personGalleriSoeknad.data}
                gjenlevendeGrunnlag={familieforhold?.gjenlevende?.find((po) => po)}
              />
              <Foreldreansvar
                harKildePesys={harKildePesys}
                persongalleriGrunnlag={personGalleriSoeknad.data}
                gjenlevendeGrunnlag={familieforhold?.gjenlevende?.find((po) => po)}
              />
              <Verger behandlingId={behandling.id} sakId={behandling.sakId} />
            </InfobokserWrapper>
          </div>
          <VurderingsContainerWrapper>
            <GyldigFramsattVurdering
              behandlingId={behandling.id}
              gyldigFramsatt={behandling.gyldighetsprøving}
              redigerbar={redigerbar}
            />
          </VurderingsContainerWrapper>
        </LovtekstMedLenke>
      )}
    </>
  )
}
