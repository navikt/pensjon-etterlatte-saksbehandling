import { Detail, Heading, Table, Tag } from '@navikt/ds-react'
import { formaterJournalpostStatus, formaterJournalpostType, formaterStringDato } from '~utils/formattering'
import Spinner from '~shared/Spinner'
import { Tema } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'
import { mapApiResult } from '~shared/api/apiUtils'
import { Container, FlexRow } from '~shared/styled'
import { useEffect, useState } from 'react'
import { hentDokumenter } from '~shared/api/dokument'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Variants } from '~shared/Tags'
import { DokumentFilter } from '~components/person/dokumenter/DokumentFilter'
import { VisDokument } from '~components/person/dokumenter/VisDokument'

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
                          <VisDokument dokument={dokument} />
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
