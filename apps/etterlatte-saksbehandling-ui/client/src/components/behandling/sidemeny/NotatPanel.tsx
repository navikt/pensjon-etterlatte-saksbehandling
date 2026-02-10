import { useApiCall } from '~shared/hooks/useApiCall'
import { hentNotaterForSak } from '~shared/api/notat'
import { useEffect } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import { Alert, BodyShort, Detail, Heading, HStack, VStack } from '@navikt/ds-react'
import { SidebarPanel } from '~shared/components/Sidebar'
import styled from 'styled-components'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'

export const NotatPanel = ({ sakId, fnr }: { sakId: number; fnr: string }) => {
  const [notatResult, hentNotater] = useApiCall(hentNotaterForSak)

  useEffect(() => {
    hentNotater(sakId)
  }, [sakId])

  return mapResult(notatResult, {
    pending: <Spinner label="Henter notater..." />,
    error: (err) => (
      <Alert variant="error" size="small">
        {err.detail}
      </Alert>
    ),
    success: (notater) => (
      <SidebarPanel $border>
        <Heading size="small" spacing>
          Notater
        </Heading>

        <VStack gap="space-2">
          {!!notater?.length ? (
            notater.map((notat, index) => (
              <TextOverflow key={index} size="small">
                {notat.tittel}
              </TextOverflow>
            ))
          ) : (
            <Detail spacing>Ingen notater funnet</Detail>
          )}
        </VStack>

        <hr />

        <HStack justify="space-between">
          <PersonButtonLink
            fnr={fnr}
            fane={PersonOversiktFane.NOTATER}
            variant="tertiary"
            size="small"
            target="_blank"
            rel="noreferrer noopener"
            icon={<ExternalLinkIcon aria-hidden />}
          >
            Åpne notatoversikten
          </PersonButtonLink>
        </HStack>
      </SidebarPanel>
    ),
  })
}

const TextOverflow = styled(BodyShort)`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
`
