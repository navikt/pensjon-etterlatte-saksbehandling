import { Alert, BodyLong, Box, Heading, VStack } from '@navikt/ds-react'
import React from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingPerioderSkjema } from '~components/tilbakekreving/utbetalinger/TilbakekrevingVurderingPerioderSkjema'
import { TilbakekrevingVurderingPerioderVisning } from '~components/tilbakekreving/utbetalinger/TilbakekrevingVurderingPerioderVisning'
import { SakType } from '~shared/types/sak'

export function TilbakekrevingUtbetalinger({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading level="1" size="large">
          Tilbakekreving
        </Heading>
        {behandling.sak.sakType === SakType.BARNEPENSJON && (
          <Box marginBlock="space-8 space-0" maxWidth="45em">
            <Alert variant="info">
              <VStack gap="space-4">
                <BodyLong>
                  Skatteetaten har ikke utstedt skattekort for de som mottar barnepensjon. Det er derfor ikke mulig å
                  beregne netto tilbakekreving automatisk. Skatt overføres til Skatteetaten annenhver måned.
                </BodyLong>
                <BodyLong>
                  Dersom skattetrekk ikke er overført Skatteetaten vil beregnet feilutbetaling og brutto tilbakekreving
                  derfor være lik netto tilbakekreving.
                </BodyLong>
                <BodyLong>
                  Dersom skattetrekk er registrert på egen linje, så betyr det at skattetrekket ikke er overført
                  Skatteetaten.
                </BodyLong>
                <BodyLong>
                  I oppsummeringen vil du få vist hvilken informasjon som sendes til oppdrag og Nav Innkreving, samt hva
                  som informeres til mottaker i vedtaket.
                </BodyLong>
                <BodyLong>
                  Dersom skatt er overført til Skatteetaten, vil Gjenny forholde seg til kun brutto feilutbetaling. Det
                  vil ikke bli foretatt netto tilbakekreving.
                </BodyLong>
              </VStack>
            </Alert>
          </Box>
        )}
      </Box>
      {redigerbar ? (
        <TilbakekrevingVurderingPerioderSkjema behandling={behandling} redigerbar={redigerbar} />
      ) : (
        <TilbakekrevingVurderingPerioderVisning behandling={behandling} />
      )}
    </>
  )
}
