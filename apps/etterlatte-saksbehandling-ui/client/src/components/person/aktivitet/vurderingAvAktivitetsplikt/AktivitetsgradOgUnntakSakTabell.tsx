import React from 'react'
import { IAktivitetspliktAktivitetsgrad, IAktivitetspliktUnntak } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Box, Label } from '@navikt/ds-react'
import { AktivitetsgradOgUnntakTabell } from '~components/aktivitetsplikt/AktivitetsgradOgUnntakTabell'

export const AktivitetsgradOgUnntakSakTabell = ({
  aktiviteter,
  unntak,
}: {
  aktiviteter: IAktivitetspliktAktivitetsgrad[]
  unntak: IAktivitetspliktUnntak[]
}) => {
  return (
    <AktivitetsgradOgUnntakTabell
      aktiviteter={aktiviteter}
      unntak={unntak}
      utvidetVisning={(aktivitetEllerUnntak) => (
        <Box maxWidth="42.5rem">
          <Label>Beskrivelse</Label>
          <BodyShort>{aktivitetEllerUnntak.beskrivelse}</BodyShort>
        </Box>
      )}
    />
  )
}
