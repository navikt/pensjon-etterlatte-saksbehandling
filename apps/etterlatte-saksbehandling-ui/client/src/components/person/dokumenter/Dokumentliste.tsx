import { Detail, Heading, Table, Tag } from '@navikt/ds-react'
import { formaterJournalpostStatus, formaterJournalpostType, formaterStringDato } from '~utils/formattering'
import Spinner from '~shared/Spinner'
import { Journalpost, Journalposttype, Journalstatus, Tema } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'
import { mapApiResult, Result } from '~shared/api/apiUtils'
import { Container, FlexRow } from '~shared/styled'
import React, { useEffect, useState } from 'react'
import { hentDokumenter } from '~shared/api/dokument'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Variants } from '~shared/Tags'
import { DokumentFilter } from '~components/person/dokumenter/DokumentFilter'
import { JournalpostInnhold } from '~components/person/journalfoeringsoppgave/journalpost/JournalpostInnhold'
import { OppgaveFraJournalpostModal } from '~components/person/dokumenter/OppgaveFraJournalpostModal'
import DokumentModal from '~components/person/dokumenter/DokumentModal'
import { UtsendingsinfoModal } from '~components/person/dokumenter/UtsendingsinfoModal'
import { SakMedBehandlinger } from '~components/person/typer'

export const Dokumentliste = ({ fnr, sakStatus }: { fnr: string; sakStatus: Result<SakMedBehandlinger> }) => {
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
            <Table.HeaderCell />
            <Table.HeaderCell>ID</Table.HeaderCell>
            <Table.HeaderCell>Tittel</Table.HeaderCell>
            <Table.HeaderCell>Avsender/Mottaker</Table.HeaderCell>
            <Table.HeaderCell>Dato</Table.HeaderCell>
            <Table.HeaderCell>Sak</Table.HeaderCell>
            <Table.HeaderCell>Status</Table.HeaderCell>
            <Table.HeaderCell>Tema</Table.HeaderCell>
            <Table.HeaderCell>Type</Table.HeaderCell>
            <Table.HeaderCell />
          </Table.Row>
        </Table.Header>

        <Table.Body>
          {mapApiResult(
            dokumenter,
            <Table.Row>
              <Table.DataCell colSpan={100}>
                <Spinner margin="0" visible label="Henter dokumenter" />
              </Table.DataCell>
            </Table.Row>,
            () => (
              <Table.Row>
                <Table.DataCell colSpan={100}>
                  <ApiErrorAlert>Det har oppstått en feil ved henting av dokumenter</ApiErrorAlert>
                </Table.DataCell>
              </Table.Row>
            ),
            (dokumentListe) =>
              !dokumentListe.length ? (
                <Table.Row shadeOnHover={false}>
                  <Table.DataCell colSpan={100}>
                    <Detail>
                      <i>Ingen dokumenter funnet</i>
                    </Detail>
                  </Table.DataCell>
                </Table.Row>
              ) : (
                <>
                  {dokumentListe.map((dokument) => (
                    <DokumentRad key={dokument.journalpostId} dokument={dokument} sakStatus={sakStatus} />
                  ))}
                </>
              )
          )}
        </Table.Body>
      </Table>
    </Container>
  )
}

const DokumentRad = ({ dokument, sakStatus }: { dokument: Journalpost; sakStatus: Result<SakMedBehandlinger> }) => {
  const [isOpen, setIsOpen] = useState(false)

  const visUtsendingsinfo =
    dokument.journalposttype === Journalposttype.U &&
    [Journalstatus.FERDIGSTILT, Journalstatus.JOURNALFOERT].includes(dokument.journalstatus)

  const kanRedigeres = [Journalstatus.MOTTATT, Journalstatus.UNDER_ARBEID].includes(dokument.journalstatus)

  return (
    <Table.ExpandableRow shadeOnHover={false} content={<JournalpostInnhold journalpost={dokument} />}>
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
          {visUtsendingsinfo && <UtsendingsinfoModal journalpost={dokument} />}

          {kanRedigeres && (
            <OppgaveFraJournalpostModal
              isOpen={isOpen}
              setIsOpen={setIsOpen}
              journalpost={dokument}
              sakStatus={sakStatus}
            />
          )}

          <DokumentModal journalpost={dokument} />
        </FlexRow>
      </Table.DataCell>
    </Table.ExpandableRow>
  )
}
