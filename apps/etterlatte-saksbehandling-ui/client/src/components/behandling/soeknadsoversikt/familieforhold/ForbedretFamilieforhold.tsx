import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleLand } from '~shared/api/behandling'
import { useEffect } from 'react'
import { Heading, HStack, VStack } from '@navikt/ds-react'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Personopplysninger } from '~shared/types/grunnlag'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { RedigerFamilieforholdModal } from '~components/behandling/soeknadsoversikt/familieforhold/RedigerFamilieforholdModal'

interface Props {
  behandling: IDetaljertBehandling
  redigerbar: boolean
  personopplysninger: Personopplysninger | null
}

export const ForbedretFamilieforhold = ({ behandling, redigerbar, personopplysninger }: Props) => {
  const [hentAlleLandResult, hentAlleLandFetch] = useApiCall(hentAlleLand)

  useEffect(() => {
    hentAlleLandFetch(undefined)
  }, [])

  return (
    <VStack paddingInline="16" paddingBlock="4" gap="4">
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
    </VStack>
  )
}
