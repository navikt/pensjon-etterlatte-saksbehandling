import { Button, BodyShort, Detail, Heading, Link } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import Spinner from '~shared/Spinner'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { Journalposttype, Journalstatus } from '~shared/types/Journalpost'

import { mapApiResult } from '~shared/api/apiUtils'
import { SidebarPanel } from '~shared/components/Sidebar'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentDokumenter } from '~shared/api/dokument'
import { useEffect } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FlexRow } from '~shared/styled'

export const DokumentlisteLiten = ({ fnr }: { fnr: string }) => {
  const [status, hentDokumenterForBruker] = useApiCall(hentDokumenter)

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
    <SidebarPanel border>
      <Heading size="small" spacing>
        Dokumenter
      </Heading>

      {mapApiResult(
        status,
        <Spinner label="Henter dokumenter" visible />,
        (error) => (
          <ApiErrorAlert>{error.detail || 'Det har oppstått en feil ved henting av dokumenter'}</ApiErrorAlert>
        ),
        (dokumenter) => (
          <>
            {dokumenter.map((dokument) => (
              <div key={dokument.journalpostId}>
                {dokument.dokumenter.map((dokumentInfo) => (
                  <BodyShort key={dokumentInfo.dokumentInfoId} as="div" size="small" spacing>
                    <Link
                      href={`/api/dokumenter/${dokument.journalpostId}/${dokumentInfo.dokumentInfoId}`}
                      target="_blank"
                      rel="noreferrer noopener"
                    >
                      {dokumentInfo.tittel}
                      <ExternalLinkIcon title={dokument.tittel} />
                    </Link>
                    <Detail>
                      {
                        {
                          [Journalposttype.I]: 'Avsender: ',
                          [Journalposttype.U]: 'Mottaker: ',
                          [Journalposttype.N]: 'Notat',
                        }[dokument.journalposttype]
                      }
                      {dokument.avsenderMottaker.navn || 'Ukjent'} ({formaterStringDato(dokument.datoOpprettet)})
                    </Detail>
                  </BodyShort>
                ))}
              </div>
            )) || (
              <Detail style={{ textAlign: 'center' }}>
                <i>Ingen dokumenter ble funnet</i>
              </Detail>
            )}

            <hr />

            <FlexRow justify="right">
              <Button
                variant="tertiary"
                size="small"
                as={Link}
                href={`/person/${fnr}?fane=DOKUMENTER`}
                target="_blank"
                icon={<ExternalLinkIcon />}
              >
                Gå til dokumentoversikten
              </Button>
            </FlexRow>
          </>
        )
      )}
    </SidebarPanel>
  )
}
