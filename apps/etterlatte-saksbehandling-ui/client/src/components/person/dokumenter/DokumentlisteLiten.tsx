import { Alert, BodyShort, Button, Detail, Heading, HStack, Link } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { Journalstatus } from '~shared/types/Journalpost'
import { mapApiResult } from '~shared/api/apiUtils'
import { SidebarPanel } from '~shared/components/Sidebar'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentDokumenter } from '~shared/api/dokument'
import { useEffect } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { DokumentInfoDetail } from '~components/person/dokumenter/DokumentInfoDetail'
import { useAppSelector } from '~store/Store'

export const DokumentlisteLiten = ({ fnr }: { fnr: string }) => {
  const [status, hentDokumenterForBruker] = useApiCall(hentDokumenter)
  const saksId = useAppSelector((state) => state.behandlingReducer.behandling?.sakId)

  useEffect(
    () =>
      void hentDokumenterForBruker({
        fnr,
        statuser: [Journalstatus.FERDIGSTILT, Journalstatus.JOURNALFOERT, Journalstatus.EKSPEDERT],
        foerste: 10,
      }),
    [fnr]
  )

  return (
    <SidebarPanel $border>
      <Heading size="small" spacing>
        Dokumenter
      </Heading>

      {mapApiResult(
        status,
        <Spinner label="Henter dokumenter" visible />,
        (error) => (
          <ApiErrorAlert>{error.detail || 'Det har oppstått en feil ved henting av dokumenter'}</ApiErrorAlert>
        ),
        (journalposter) => (
          <>
            {journalposter.map((journalpost) => (
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

            <hr />

            <HStack justify="end">
              <Button
                variant="tertiary"
                size="small"
                as={Link}
                href={`/sak/${saksId}?fane=DOKUMENTER`}
                target="_blank"
                icon={<ExternalLinkIcon />}
              >
                Gå til dokumentoversikten
              </Button>
            </HStack>
          </>
        )
      )}
    </SidebarPanel>
  )
}
