import { IAktivitetspliktUnntak, IAktivitetspliktVurderingNyDto } from '~shared/types/Aktivitetsplikt'
import { isFailure, isPending, Result } from '~shared/api/apiUtils'
import { BodyShort, Button, HStack, Label, VStack } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import React from 'react'

export const UnntakRedigeringsKnapper = ({
  unntak,
  slettUnntakStatus,
  redigerbar,
  setRedigerer,
  slettUnntak,
}: {
  unntak: IAktivitetspliktUnntak
  slettUnntakStatus: Result<IAktivitetspliktVurderingNyDto>
  redigerbar: boolean
  setRedigerer: (arg: boolean) => void
  slettUnntak: (unntak: IAktivitetspliktUnntak) => void
}) => {
  return (
    <VStack gap="space-6">
      <VStack gap="space-2">
        <Label>Beskrivelse</Label>
        <BodyShort>{unntak.beskrivelse}</BodyShort>
      </VStack>

      {isFailure(slettUnntakStatus) && (
        <ApiErrorAlert>Kunne ikke slette unntaket, på grunn av feil: {slettUnntakStatus.error.detail}</ApiErrorAlert>
      )}

      {redigerbar && (
        <HStack gap="space-4">
          <Button
            size="xsmall"
            variant="secondary"
            icon={<PencilIcon aria-hidden />}
            onClick={() => setRedigerer(true)}
          >
            Rediger
          </Button>
          <Button
            size="xsmall"
            variant="secondary"
            icon={<TrashIcon aria-hidden />}
            onClick={() => slettUnntak(unntak)}
            loading={isPending(slettUnntakStatus)}
          >
            Slett
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
