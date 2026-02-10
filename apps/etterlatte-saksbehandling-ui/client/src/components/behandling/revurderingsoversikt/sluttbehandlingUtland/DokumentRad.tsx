import { Button, HStack, TextField } from '@navikt/ds-react'
import { DatoVelger } from '~shared/components/datoVelger/DatoVelger'
import { formatDateToLocaleDateOrEmptyString } from '~shared/components/datoVelger/datoVelgerUtils'
import React from 'react'
import { XMarkIcon } from '@navikt/aksel-icons'
import { MottattDokument } from '~shared/types/RevurderingInfo'

export default function DokumentRad({
  lesevisning,
  dokument,
  oppdaterDokument,
  fjernDokument,
  label = undefined,
}: {
  lesevisning: boolean
  dokument: MottattDokument
  oppdaterDokument: (field: string, value: string) => void
  fjernDokument: () => void
  label: string | undefined
}) {
  return (
    <HStack gap="space-4">
      <TextField
        readOnly={lesevisning}
        label="Dokument fra Rina"
        value={dokument.dokumenttype}
        onChange={(e) => oppdaterDokument('dokumenttype', e.target.value)}
      />
      <DatoVelger
        disabled={lesevisning}
        label={label ? label : 'Datovelger'}
        value={dokument.dato ? new Date(dokument.dato) : undefined}
        onChange={(date) => oppdaterDokument('dato', formatDateToLocaleDateOrEmptyString(date))}
      />
      <TextField
        readOnly={lesevisning}
        label="Kommentar"
        value={dokument.kommentar}
        onChange={(e) => oppdaterDokument('kommentar', e.target.value)}
      />
      {!lesevisning && (
        <Button
          variant="tertiary"
          icon={<XMarkIcon aria-hidden />}
          onClick={() => fjernDokument()}
          style={{ marginTop: '1.9rem', maxHeight: '3rem' }}
        >
          Slett dokument
        </Button>
      )}
    </HStack>
  )
}
