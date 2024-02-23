import { Alert, BodyShort, Detail, Heading, Link } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import Spinner from '~shared/Spinner'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { Journalpost, Journalposttype } from '~shared/types/Journalpost'

import { isPending, isSuccess, Result } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { FlexRow } from '~shared/styled'

export const DokumentlisteLiten = ({ dokumenter }: { dokumenter: Result<Journalpost[]> }) => (
  <>
    <Heading size="small" spacing>
      Dokumenter
    </Heading>

    {isPending(dokumenter) && <Spinner label="Henter dokumenter" visible />}

    {isSuccess(dokumenter) &&
      (dokumenter.data?.map((dokument) => (
        <div key={dokument.journalpostId}>
          {dokument.dokumenter.map((dokumentInfo) => (
            <BodyShort key={dokumentInfo.dokumentInfoId} as="div" size="small" spacing>
              {!dokumentInfo.dokumentvarianter[0].saksbehandlerHarTilgang ? (
                <>
                  <Link
                    href={`/api/dokumenter/${dokument.journalpostId}/${dokumentInfo.dokumentInfoId}`}
                    target="_blank"
                    rel="noreferrer noopener"
                  >
                    {dokumentInfo.tittel}
                    <ExternalLinkIcon aria-hidden title={dokument.tittel} />
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
                </>
              ) : (
                <FlexRow justify="space-between">
                  <div>
                    {dokumentInfo.tittel}
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
                  </div>
                  <Alert variant="warning" size="small">
                    Ingen tilgang
                  </Alert>
                </FlexRow>
              )}
            </BodyShort>
          ))}
        </div>
      )) || (
        <Detail style={{ textAlign: 'center' }}>
          <i>Ingen dokumenter ble funnet</i>
        </Detail>
      ))}
    {isFailureHandler({
      apiResult: dokumenter,
      errorMessage: 'Det har oppstått en feil ved henting av dokumenter.',
    })}
  </>
)
