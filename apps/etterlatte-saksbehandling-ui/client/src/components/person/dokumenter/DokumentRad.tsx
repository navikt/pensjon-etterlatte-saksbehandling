import { Journalpost, Journalposttype, Journalstatus } from '~shared/types/Journalpost'
import { Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { useState } from 'react'
import { HStack, Table } from '@navikt/ds-react'
import { JournalpostInnhold } from '~components/person/journalfoeringsoppgave/journalpost/JournalpostInnhold'
import { formaterJournalpostStatus, formaterJournalpostType } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'
import { UtsendingsinfoModal } from '~components/person/dokumenter/UtsendingsinfoModal'
import { OppgaveFraJournalpostModal } from '~components/person/dokumenter/OppgaveFraJournalpostModal'
import DokumentModal from '~components/person/dokumenter/DokumentModal'
import { HaandterAvvikModal } from './avvik/HaandterAvvikModal'
import { GosysTemaTag } from '~shared/tags/GosysTemaTag'
import { GosysTema } from '~shared/types/Gosys'

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
      <Table.DataCell>{formaterDato(dokument.datoOpprettet)}</Table.DataCell>
      <Table.DataCell>
        {dokument?.sak ? `${dokument.sak.fagsaksystem}: ${dokument.sak.fagsakId || '-'}` : '-'}
      </Table.DataCell>
      <Table.DataCell>{formaterJournalpostStatus(dokument.journalstatus)}</Table.DataCell>
      <Table.DataCell title={`Tema ${dokument.tema}`}>
        <GosysTemaTag tema={dokument.tema as GosysTema} />
      </Table.DataCell>
      <Table.DataCell>{formaterJournalpostType(dokument.journalposttype)}</Table.DataCell>
      <Table.DataCell>
        <HStack gap="4" justify="end">
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
        </HStack>
      </Table.DataCell>
    </Table.ExpandableRow>
  )
}
