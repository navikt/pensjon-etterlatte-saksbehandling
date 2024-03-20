import { Journalpost, Journalposttype, Journalstatus } from '~shared/types/Journalpost'
import { Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { useState } from 'react'
import { Table, Tag } from '@navikt/ds-react'
import { JournalpostInnhold } from '~components/person/journalfoeringsoppgave/journalpost/JournalpostInnhold'
import { formaterJournalpostStatus, formaterJournalpostType, formaterStringDato } from '~utils/formattering'
import { Variants } from '~shared/Tags'
import { FlexRow } from '~shared/styled'
import { UtsendingsinfoModal } from '~components/person/dokumenter/UtsendingsinfoModal'
import { OppgaveFraJournalpostModal } from '~components/person/dokumenter/OppgaveFraJournalpostModal'
import DokumentModal from '~components/person/dokumenter/DokumentModal'
import { HaandterAvvikModal } from './avvik/HaandterAvvikModal'

export const DokumentRad = ({
  dokument,
  sakStatus,
}: {
  dokument: Journalpost
  sakStatus: Result<SakMedBehandlinger>
}) => {
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

          <HaandterAvvikModal journalpost={dokument} sakStatus={sakStatus} />

          <DokumentModal journalpost={dokument} />
        </FlexRow>
      </Table.DataCell>
    </Table.ExpandableRow>
  )
}
