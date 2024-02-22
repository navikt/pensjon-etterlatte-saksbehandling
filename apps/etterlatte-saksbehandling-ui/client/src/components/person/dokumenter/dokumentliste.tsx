import { Button, Detail, Heading, Modal, Table, Tag } from '@navikt/ds-react'
import { formaterJournalpostStatus, formaterJournalpostType, formaterStringDato } from '~utils/formattering'
import DokumentModal from './dokumentModal'
import Spinner from '~shared/Spinner'
import { Journalpost, Journalposttype, Tema } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'

import { mapApiResult } from '~shared/api/apiUtils'
import { InformationSquareIcon } from '@navikt/aksel-icons'
import { Container, FlexRow } from '~shared/styled'
import { useEffect, useState } from 'react'
import { hentDokumenter, hentUtsendingsinfo } from '~shared/api/dokument'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { Variants } from '~shared/Tags'
import { DokumentFilter } from '~components/person/dokumenter/DokumentFilter'

const colonner = ['ID', 'Tittel', 'Avsender/Mottaker', 'Dato', 'Sak', 'Status', 'Tema', 'Type', '']

export const Dokumentliste = ({ fnr }: { fnr: string }) => {
  const [filter, setFilter] = useState<DokumentFilter>({
    tema: [Tema.EYO, Tema.EYB],
    type: [],
    status: [],
  })
  const [dokumenter, hentDokumenterForBruker] = useApiCall(hentDokumenter)

  useEffect(
    () => void hentDokumenterForBruker({ fnr, temaer: filter.tema, typer: filter.type, statuser: filter.status }),
    [fnr, filter]
  )

  return (
    <Container>
      <Heading size="medium" spacing>
        Dokumenter
      </Heading>

      <DokumentFilter filter={filter} setFilter={setFilter} />

      <Table zebraStripes>
        <Table.Header>
          <Table.Row>
            {colonner.map((col) => (
              <Table.HeaderCell key={`header${col}`}>{col}</Table.HeaderCell>
            ))}
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {mapApiResult(
            dokumenter,
            <Table.Row>
              <Table.DataCell colSpan={colonner.length}>
                <Spinner margin="0" visible label="Henter dokumenter" />
              </Table.DataCell>
            </Table.Row>,
            () => (
              <Table.Row>
                <Table.DataCell colSpan={colonner.length}>
                  <ApiErrorAlert>Det har oppstått en feil ved henting av dokumenter</ApiErrorAlert>
                </Table.DataCell>
              </Table.Row>
            ),
            (dokumentListe) =>
              !dokumentListe.length ? (
                <Table.Row shadeOnHover={false}>
                  <Table.DataCell colSpan={colonner.length}>
                    <Detail>
                      <i>Ingen dokumenter funnet</i>
                    </Detail>
                  </Table.DataCell>
                </Table.Row>
              ) : (
                <>
                  {dokumentListe.map((dokument, i) => (
                    <Table.Row key={i} shadeOnHover={false}>
                      <Table.DataCell>{dokument.journalpostId}</Table.DataCell>
                      <Table.DataCell>{dokument.tittel}</Table.DataCell>
                      <Table.DataCell>{dokument.avsenderMottaker.navn || 'Ukjent'}</Table.DataCell>
                      <Table.DataCell>{formaterStringDato(dokument.datoOpprettet)}</Table.DataCell>
                      <Table.DataCell>
                        {dokument?.sak ? `${dokument.sak.fagsaksystem}: ${dokument.sak.fagsakId || '-'}` : '-'}
                      </Table.DataCell>
                      <Table.DataCell>{formaterJournalpostStatus(dokument.journalstatus)}</Table.DataCell>
                      <Table.DataCell title={`Tema ${dokument.tema}`}>
                        {
                          {
                            ['EYO']: <Tag variant={Variants.ALT2}>Omstillingsstønad</Tag>,
                            ['EYB']: <Tag variant={Variants.INFO}>Barnepensjon</Tag>,
                            ['PEN']: <Tag variant={Variants.ALT1}>Pensjon</Tag>,
                          }[dokument.tema]
                        }
                      </Table.DataCell>
                      <Table.DataCell>{formaterJournalpostType(dokument.journalposttype)}</Table.DataCell>
                      <Table.DataCell>
                        <FlexRow justify="right">
                          {dokument.journalposttype === Journalposttype.U && (
                            <UtsendingsinfoModal journalpost={dokument} />
                          )}

                          <DokumentModal journalpost={dokument} />
                        </FlexRow>
                      </Table.DataCell>
                    </Table.Row>
                  ))}
                </>
              )
          )}
        </Table.Body>
      </Table>
    </Container>
  )
}

const UtsendingsinfoModal = ({ journalpost }: { journalpost: Journalpost }) => {
  const [isOpen, setIsOpen] = useState(false)

  const [status, apiHentUtsendingsinfo] = useApiCall(hentUtsendingsinfo)

  const open = () => {
    setIsOpen(true)
    apiHentUtsendingsinfo({ journalpostId: journalpost.journalpostId })
  }

  return (
    <>
      <Button variant="tertiary" title="Utsendingsinfo" size="small" icon={<InformationSquareIcon />} onClick={open} />

      <Modal open={isOpen} onClose={() => setIsOpen(false)}>
        <Modal.Header>
          <Heading size="medium">Utsendingsinfo</Heading>
        </Modal.Header>

        <Modal.Body>
          {mapApiResult(
            status,
            <Spinner visible label="Henter utsendingsinfo" />,
            (error) => (
              <ApiErrorAlert>{error.detail || 'Feil ved henting av utsendingsinfo'}</ApiErrorAlert>
            ),
            (result) => (
              <>
                {!result.utsendingsinfo && <i>Ingen utsendingsinformasjon på journalposten</i>}

                {result.utsendingsinfo?.fysiskpostSendt?.adressetekstKonvolutt && (
                  <Info
                    label="Adressetekst konvolutt"
                    tekst={
                      <div style={{ whiteSpace: 'pre-wrap' }}>
                        {result.utsendingsinfo?.fysiskpostSendt?.adressetekstKonvolutt}
                      </div>
                    }
                  />
                )}

                {result.utsendingsinfo?.digitalpostSendt?.adresse && (
                  <Info
                    label="Adressetekst konvolutt"
                    tekst={
                      <div style={{ whiteSpace: 'pre-wrap' }}>{result.utsendingsinfo?.digitalpostSendt?.adresse}</div>
                    }
                  />
                )}
              </>
            )
          )}
        </Modal.Body>
      </Modal>
    </>
  )
}
