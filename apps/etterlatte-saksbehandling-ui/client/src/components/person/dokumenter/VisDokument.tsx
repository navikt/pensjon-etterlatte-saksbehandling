import React from 'react'
import { Journalpost, Journalposttype } from '~shared/types/Journalpost'
import { FlexRow } from '~shared/styled'
import { UtsendingsinfoModal } from '~components/person/dokumenter/UtsendingsinfoModal'
import DokumentModal from '~components/person/dokumenter/DokumentModal'

export const VisDokument = ({ dokument }: { dokument: Journalpost }) => {
  return (
    <FlexRow justify="right">
      {dokument.journalposttype === Journalposttype.U && <UtsendingsinfoModal journalpost={dokument} />}
      <DokumentModal journalpost={dokument} />
    </FlexRow>
  )
}
