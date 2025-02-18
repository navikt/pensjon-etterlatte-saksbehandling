import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleLand } from '~shared/api/behandling'
import { useEffect } from 'react'
import { Box, Heading, HStack, VStack } from '@navikt/ds-react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Personopplysninger } from '~shared/types/grunnlag'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { RedigerFamilieforholdModal } from '~components/behandling/soeknadsoversikt/familieforhold/RedigerFamilieforholdModal'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { TabellOverAvdoede } from '~components/behandling/soeknadsoversikt/familieforhold/TabellOverAvdoede'
import { TabellOverGjenlevende } from '~components/behandling/soeknadsoversikt/familieforhold/TabellOverGjenlevende'
import { SakType } from '~shared/types/sak'
import { TabellOverAvdoedesBarn } from '~components/behandling/soeknadsoversikt/familieforhold/TabellOverAvdoedesBarn'
import { hentLevendeSoeskenFraAvdoedeForSoekerGrunnlag } from '~shared/types/Person'
import { AnnenForelderSkjema } from '~components/behandling/soeknadsoversikt/familieforhold/AnnenForelderSkjema'
import { SamsvarPersongalleri } from '~components/behandling/soeknadsoversikt/familieforhold/SamsvarPersongalleri'

interface Props {
  behandling: IDetaljertBehandling
  redigerbar: boolean
  personopplysninger: Personopplysninger | null
}

export const Familieforhold = ({ behandling, redigerbar, personopplysninger }: Props) => {
  const [hentAlleLandResult, hentAlleLandFetch] = useApiCall(hentAlleLand)

  const skaViseAnnenForelderSkjema =
    behandling.sakType === SakType.BARNEPENSJON &&
    personopplysninger?.avdoede.length === 1 &&
    personopplysninger.gjenlevende.length === 0

  useEffect(() => {
    hentAlleLandFetch(undefined)
  }, [])

  return (
    <VStack paddingInline="16" paddingBlock="4" gap="8">
      <HStack gap="4" justify="start" align="center" wrap={false}>
        <Heading size="medium" level="2">
          Familieforhold
        </Heading>
        <div>
          <PersonButtonLink
            variant="secondary"
            size="small"
            icon={<ExternalLinkIcon aria-hidden />}
            fnr={personopplysninger?.soeker?.opplysning.foedselsnummer ?? ''}
            fane={PersonOversiktFane.PERSONOPPLYSNINGER}
            target="_blank"
          >
            Åpne personopplysninger i eget vindu
          </PersonButtonLink>
        </div>

        {redigerbar && personopplysninger && (
          <RedigerFamilieforholdModal behandling={behandling} personopplysninger={personopplysninger} />
        )}
      </HStack>
      <Box paddingInline="4 0">
        {mapResult(hentAlleLandResult, {
          pending: <Spinner label="Henter alle land..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente alle land'}</ApiErrorAlert>,
          success: (alleLand) => (
            <VStack gap="8">
              <SamsvarPersongalleri landListe={alleLand} />
              {skaViseAnnenForelderSkjema && (
                <AnnenForelderSkjema behandlingId={behandling.id} personopplysninger={personopplysninger} />
              )}
              <TabellOverAvdoede avdoede={personopplysninger?.avdoede} alleLand={alleLand} />
              {behandling.sakType === SakType.OMSTILLINGSSTOENAD ? (
                <>
                  <TabellOverGjenlevende
                    gjenlevende={!!personopplysninger?.soeker ? [personopplysninger?.soeker] : []}
                    alleLand={alleLand}
                  />
                  <TabellOverAvdoedesBarn
                    avdoedesBarn={personopplysninger?.avdoede.flatMap((it) => it.opplysning.avdoedesBarn ?? [])}
                    soeker={personopplysninger?.soeker}
                    sakType={behandling.sakType}
                  />
                </>
              ) : (
                <>
                  <TabellOverGjenlevende gjenlevende={personopplysninger?.gjenlevende} alleLand={alleLand} />
                  <TabellOverAvdoedesBarn
                    avdoedesBarn={hentLevendeSoeskenFraAvdoedeForSoekerGrunnlag(
                      personopplysninger?.avdoede ?? [],
                      personopplysninger?.soeker?.opplysning.foedselsnummer ?? ''
                    )}
                    soeker={personopplysninger?.soeker}
                    sakType={behandling.sakType}
                  />
                </>
              )}
            </VStack>
          ),
        })}
      </Box>
    </VStack>
  )
}
