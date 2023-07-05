import { Alert, Link } from '@navikt/ds-react'
import styled from 'styled-components'
import { Journalpost } from '../behandling/types'
import { formaterStringDato } from '~utils/formattering'
import Spinner from '~shared/Spinner'
import { ExternalLinkIcon } from '@navikt/aksel-icons'

export const DokumentlisteLiten = ({
  dokumenter,
  dokumenterHentet,
  error,
}: {
  dokumenter: Journalpost[]
  dokumenterHentet: boolean
  error: boolean
}) => {
  return (
    <>
      {dokumenter.length > 0 &&
        !error &&
        dokumenter.map((brev) => (
          <div key={`${brev.journalpostId}/${brev.dokumenter[0].dokumentInfoId}`}>
            <Dokumentnavn>
              <Link
                href={`/api/dokumenter/${brev.journalpostId}/${brev.dokumenter[0].dokumentInfoId}`}
                target="_blank"
                rel="noreferrer noopener"
              >
                {brev.tittel}
                <ExternalLinkIcon title={brev.tittel} />
              </Link>
            </Dokumentnavn>
            <Avsender>
              {brev.journalposttype === 'I' ? 'Avsender' : 'Mottaker'}: {brev.avsenderMottaker.navn || 'Ukjent'} (
              {formaterStringDato(brev.datoOpprettet)})
            </Avsender>
          </div>
        ))}
      {dokumenter.length === 0 && !error && (
        <IngenDokumenterRad>
          {dokumenterHentet ? (
            'Ingen dokumenter ble funnet'
          ) : (
            <Spinner margin={'0'} visible={!dokumenterHentet} label="Henter dokumenter" />
          )}
        </IngenDokumenterRad>
      )}
      {error && (
        <Alert variant={'error'} style={{ marginTop: '10px' }}>
          Det har oppstått en feil ved henting av dokumenter.
        </Alert>
      )}
    </>
  )
}

const IngenDokumenterRad = styled.div`
  text-align: center;
  padding-top: 16px;
  font-size: 14px;
  font-style: italic;
`
const Dokumentnavn = styled.div`
  font-weight: 600;
  font-size: 14px;
  padding-top: 20px;
  margin-top: 20px;
  border-top: 1px solid;
`

const Avsender = styled.div`
  font-style: normal;
  font-weight: 400;
  font-size: 14px;
  line-height: 20px;

  color: #3e3832;
`
