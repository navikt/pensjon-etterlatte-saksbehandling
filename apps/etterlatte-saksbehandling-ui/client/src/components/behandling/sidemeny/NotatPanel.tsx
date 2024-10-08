import { NotatRedigeringModal } from '~components/person/notat/NotatRedigeringModal'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentNotaterForReferanse, NotatMal, opprettNotatForSak } from '~shared/api/notat'
import { useEffect } from 'react'
import { isInitial, isPending, mapResult } from '~shared/api/apiUtils'
import { Alert, BodyShort, Button, Detail, Heading, HStack, VStack } from '@navikt/ds-react'
import { SidebarPanel } from '~shared/components/Sidebar'
import styled from 'styled-components'
import { NotatVisningModal } from '~components/person/notat/NotatVisningModal'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'
import { ExternalLinkIcon } from '@navikt/aksel-icons'

export const NotatPanel = ({ sakId, behandlingId, fnr }: { sakId: number; behandlingId: string; fnr: string }) => {
  const [notatResult, hentNotater] = useApiCall(hentNotaterForReferanse)
  const [nyttNotatResult, opprettNotat] = useApiCall(opprettNotatForSak)

  useEffect(() => {
    if (isInitial(notatResult)) {
      hentNotater(behandlingId)
    }
  }, [behandlingId])

  const opprettNyttNotat = () => {
    opprettNotat({ sakId, referanse: behandlingId, mal: NotatMal.TOM_MAL }, () => {
      hentNotater(behandlingId)
    })
  }

  return mapResult(notatResult, {
    success: (notater) => (
      <SidebarPanel $border>
        <Heading size="small" spacing>
          Notater
        </Heading>

        <VStack gap="2">
          {notater.map(
            (notat) =>
              !notat.journalpostId && (
                <HStack gap="2" justify="space-between" wrap={false} key={`notat-${notat.id}`}>
                  <TextOverflow size="small">{notat.tittel}</TextOverflow>

                  {!notat.journalpostId ? (
                    <NotatRedigeringModal notat={notat} minifyRedigerKnapp />
                  ) : (
                    <NotatVisningModal notat={notat} />
                  )}
                </HStack>
              )
          )}

          {!notater.length && <Detail spacing>Ingen notater funnet</Detail>}
        </VStack>

        <hr />

        <HStack justify="space-between">
          <Button onClick={opprettNyttNotat} loading={isPending(nyttNotatResult)} size="small" variant="secondary">
            Opprett nytt notat
          </Button>

          <PersonButtonLink
            fnr={fnr}
            fane={PersonOversiktFane.NOTATER}
            variant="tertiary"
            size="small"
            target="_blank"
            rel="noreferrer noopener"
            icon={<ExternalLinkIcon />}
          >
            Gå til notatoversikten
          </PersonButtonLink>
        </HStack>
      </SidebarPanel>
    ),
    error: (err) => (
      <Alert variant="error" size="small">
        {err.detail}
      </Alert>
    ),
  })
}

const TextOverflow = styled(BodyShort)`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
`
