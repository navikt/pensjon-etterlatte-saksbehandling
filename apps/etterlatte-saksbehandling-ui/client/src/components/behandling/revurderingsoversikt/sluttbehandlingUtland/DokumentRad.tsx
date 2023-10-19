import { Button, HStack, TextField } from '@navikt/ds-react'
import { DatoVelger, formatDateToLocaleDateOrEmptyString } from '~shared/DatoVelger'
import { MottattDokument } from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLand'
import React from 'react'

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
        label="Kommentar"
        value={dokument.kommentar}
        onChange={(e) => oppdaterDokument('kommentar', e.target.value)}
      />
      <Button variant="tertiary" onClick={() => fjernDokument()}>
        Fjern dokument
      </Button>
    </HStack>
  )
}
