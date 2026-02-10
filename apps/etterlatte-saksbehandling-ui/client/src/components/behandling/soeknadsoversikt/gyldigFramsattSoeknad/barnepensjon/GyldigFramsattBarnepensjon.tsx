import { GyldigFramsattType, IDetaljertBehandling, IGyldighetResultat } from '~shared/types/IDetaljertBehandling'
import { SoeknadVurdering } from '~components/behandling/soeknadsoversikt/SoeknadVurdering'
import { Innsender } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/barnepensjon/Innsender'
import { Foreldreansvar } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/barnepensjon/Foreldreansvar'
import { Verger } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/Verger'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { StatusIconProps } from '~shared/icons/statusIcon'
import { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getPersongalleriFraSoeknad } from '~shared/api/grunnlag'
import { Familieforhold } from '~shared/types/Person'

import { isSuccess } from '~shared/api/apiUtils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Box, HStack } from '@navikt/ds-react'
import {
  finnVurdering,
  GyldigFramsattVurdering,
} from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/GyldigFramsattVurdering'

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
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const [personGalleriSoeknad, getPersonGalleriSoeknad] = useApiCall(getPersongalleriFraSoeknad)
  useEffect(() => {
    getPersonGalleriSoeknad({ behandlingId: behandling.id })
  }, [behandling.sakId, behandling.id])

  const manuellVurdering = finnVurdering(gyldigFramsatt, GyldigFramsattType.MANUELL_VURDERING)?.basertPaaOpplysninger
  const harKildePesys = manuellVurdering?.kilde?.ident == 'PESYS'

  return (
    <>
      {isSuccess(personGalleriSoeknad) && (
        <SoeknadVurdering
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
            <Box marginBlock="space-2" marginInline="space-0" maxWidth="41rem">
              Den som har rett til ytelsen må sette frem krav (forelder/verge hvis under 18 år). Om annet må fullmakt
              ligge i saken. Søknaden må være signert og vise hva det søkes om, og den må settes fram i bostedslandet
              eller i det landet vedkommende sist var medlem.
            </Box>
            <HStack gap="space-4">
              <Innsender harKildePesys={harKildePesys} />
              <Foreldreansvar
                harKildePesys={harKildePesys}
                soekerGrunnlag={familieforhold?.soeker}
                innsender={personGalleriSoeknad.data.opplysning.innsender}
                avdoed={personGalleriSoeknad.data.opplysning.avdoed}
                gjenlevendeGrunnlag={familieforhold?.gjenlevende?.find((po) => po)}
              />
              <Verger behandlingId={behandling.id} sakId={behandling.sakId} />
            </HStack>
          </div>
          <Box
            paddingInline="space-2 space-0"
            minWidth="18.75rem"
            width="10rem"
            borderWidth="0 0 0 2"
            borderColor="neutral-subtle"
          >
            <GyldigFramsattVurdering
              behandlingId={behandling.id}
              gyldigFramsatt={behandling.gyldighetsprøving}
              redigerbar={redigerbar}
            />
          </Box>
        </SoeknadVurdering>
      )}
    </>
  )
}
