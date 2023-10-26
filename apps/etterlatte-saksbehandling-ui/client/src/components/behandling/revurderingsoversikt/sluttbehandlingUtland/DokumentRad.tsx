import { Button, HStack, TextField } from '@navikt/ds-react'
import { DatoVelger, formatDateToLocaleDateOrEmptyString } from '~shared/DatoVelger'
import React from 'react'
import { XMarkIcon } from '@navikt/aksel-icons'
import { MottattDokument } from '~shared/types/RevurderingInfo'

export default function DokumentRad({
  dokument,
  oppdaterDokument,
  fjernDokument,
}: {
  dokument: MottattDokument
  oppdaterDokument: (field: string, value: string) => void
  fjernDokument: () => void
}) {
  return (
    <HStack gap="4">
      <TextField
        label="Dokument fra Rina"
        value={dokument.dokumenttype}
        onChange={(e) => oppdaterDokument('dokumenttype', e.target.value)}
      />
      <DatoVelger
        label="Datovelger"
        value={dokument.dato ? new Date(dokument.dato) : undefined}
        onChange={(date) => oppdaterDokument('dato', formatDateToLocaleDateOrEmptyString(date))}
      />
      <TextField
        size="small"
        label="Kommentar"
        value={dokument.kommentar}
        onChange={(e) => oppdaterDokument('kommentar', e.target.value)}
      />
      <Button
        variant="tertiary"
        icon={<XMarkIcon />}
        onClick={() => fjernDokument()}
        style={{ marginTop: '1.9rem', maxHeight: '3rem' }}
      >
        Slett
      </Button>
    </HStack>
  )
}
