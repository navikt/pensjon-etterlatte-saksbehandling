import React from 'react'
import { Klage } from '~shared/types/Klage'
import { Alert, BodyLong, Heading } from '@navikt/ds-react'
import { ButtonNavigerTilBrev } from '~components/klage/vurdering/KlageVurderingFelles'

export const BeOmInfoFraKlager = ({ klage }: { klage: Klage }) => {
  return (
    <Alert variant="info">
      <Heading level="2" size="small">
        Hent informasjon fra klager
      </Heading>
      <BodyLong spacing>
        Du må innhente mer informasjon fra klager for å avgjøre om formkravene kan oppfylles. Sett klagebehandlingen på
        vent og opprett et nytt brev til klager.
      </BodyLong>
      <ButtonNavigerTilBrev klage={klage} />
    </Alert>
  )
}
