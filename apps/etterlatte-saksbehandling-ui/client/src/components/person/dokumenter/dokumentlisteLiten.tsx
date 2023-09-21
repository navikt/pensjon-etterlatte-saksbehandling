import { Alert, BodyShort, Detail, Heading, Link } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import Spinner from '~shared/Spinner'
import { ExternalLinkIcon } from '@navikt/aksel-icons'
import { isFailure, isPending, isSuccess, Result } from '~shared/hooks/useApiCall'
import { Journalpost } from '~shared/types/Journalpost'

export const DokumentlisteLiten = ({ dokumenter }: { dokumenter: Result<Journalpost[]> }) => (
  <>
    <Heading size="small" spacing>
      Dokumenter
    </Heading>

    {isPending(dokumenter) && <Spinner label="Henter dokumenter" visible />}

    {isSuccess(dokumenter) &&
      (dokumenter.data.length ? (
        dokumenter.data.map((dokument) => (
          <div key={`${dokument.journalpostId}/${dokument.dokumenter[0].dokumentInfoId}`}>
            <BodyShort size="small" spacing>
              <Link
                href={`/api/dokumenter/${dokument.journalpostId}/${dokument.dokumenter[0].dokumentInfoId}`}
                target="_blank"
                rel="noreferrer noopener"
              >
                {dokument.tittel}
                <ExternalLinkIcon title={dokument.tittel} />
              </Link>
              <Detail>
                {dokument.journalposttype === 'I' ? 'Avsender' : 'Mottaker'}:{' '}
                {dokument.avsenderMottaker.navn || 'Ukjent'} ({formaterStringDato(dokument.datoOpprettet)})
              </Detail>
            </BodyShort>
          </div>
        ))
      ) : (
        <Detail style={{ textAlign: 'center' }}>
          <i>Ingen dokumenter ble funnet</i>
        </Detail>
      ))}

    {isFailure(dokumenter) && (
      <Alert variant="error" style={{ marginTop: '10px' }}>
        Det har oppstått en feil ved henting av dokumenter.
      </Alert>
    )}
  </>
)
