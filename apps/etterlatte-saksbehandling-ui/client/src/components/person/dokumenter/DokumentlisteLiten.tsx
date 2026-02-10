import { Alert, BodyShort, Box, Button, Detail, Heading, HStack, Link } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import { ArrowDownIcon, ExternalLinkIcon } from '@navikt/aksel-icons'
import { Journalpost, Journalstatus, SideInfo } from '~shared/types/Journalpost'
import { mapResult } from '~shared/api/apiUtils'
import { SidebarPanel } from '~shared/components/Sidebar'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentDokumenter } from '~shared/api/dokument'
import React, { useEffect, useState } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { filter } from 'lodash'
import { DokumentInfoDetail } from '~components/person/dokumenter/DokumentInfoDetail'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'

export const DokumentlisteLiten = ({ fnr }: { fnr: string }) => {
  const [journalposter, setJournalposter] = useState<Journalpost[]>([])
  const [sideInfo, setSideInfo] = useState<SideInfo>()

  const [status, hentDokumenterForBruker] = useApiCall(hentDokumenter)

  useEffect(() => void hent(), [fnr, filter])

  const hent = (etter?: string) =>
    void hentDokumenterForBruker(
      {
        fnr,
        statuser: [Journalstatus.FERDIGSTILT, Journalstatus.JOURNALFOERT, Journalstatus.EKSPEDERT],
        foerste: 10,
        etter,
      },
      (response) => {
        if (etter) setJournalposter([...journalposter, ...response.journalposter])
        else setJournalposter(response.journalposter)

        setSideInfo(response.sideInfo)
      }
    )

  return (
    <SidebarPanel $border>
      <Heading size="small" spacing>
        Dokumenter
      </Heading>

      {journalposter?.map((journalpost) => (
        <div key={journalpost.journalpostId}>
          {journalpost.dokumenter.map((dokumentInfo) => (
            <BodyShort key={dokumentInfo.dokumentInfoId} as="div" size="small" spacing>
              {dokumentInfo.dokumentvarianter[0]?.saksbehandlerHarTilgang ? (
                <Link
                  href={`/api/dokumenter/${journalpost.journalpostId}/${dokumentInfo.dokumentInfoId}`}
                  target="_blank"
                  rel="noreferrer noopener"
                >
                  {dokumentInfo.tittel}
                  <ExternalLinkIcon aria-hidden title={journalpost.tittel} />
                </Link>
              ) : (
                <>
                  {dokumentInfo.tittel}
                  <Alert variant="warning" size="small" inline>
                    Mangler tilgang
                  </Alert>
                </>
              )}

              <DokumentInfoDetail dokument={journalpost} />
            </BodyShort>
          ))}
        </div>
      )) || (
        <Detail style={{ textAlign: 'center' }}>
          <i>Ingen dokumenter ble funnet</i>
        </Detail>
      )}

      {sideInfo?.finnesNesteSide && (
        <Button
          variant="secondary"
          size="small"
          onClick={() => hent(sideInfo?.sluttpeker)}
          icon={<ArrowDownIcon aria-hidden />}
        >
          Last flere
        </Button>
      )}

      {mapResult(status, {
        pending: <Spinner label="Henter dokumenter" />,
        error: (error) => (
          <ApiErrorAlert>{error.detail || 'Det har oppstått en feil ved henting av dokumenter'}</ApiErrorAlert>
        ),
      })}

      <Box borderWidth="1 0 0 0" paddingBlock="space-4 space-0" marginBlock="space-4 space-0">
        <HStack justify="end">
          <PersonButtonLink
            fnr={fnr}
            fane={PersonOversiktFane.DOKUMENTER}
            variant="tertiary"
            size="small"
            target="_blank"
            rel="noreferrer noopener"
            icon={<ExternalLinkIcon aria-hidden />}
          >
            Gå til dokumentoversikten
          </PersonButtonLink>
        </HStack>
      </Box>
    </SidebarPanel>
  )
}
