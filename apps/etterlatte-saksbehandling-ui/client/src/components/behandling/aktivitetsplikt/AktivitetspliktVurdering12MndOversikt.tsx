import { BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import React from 'react'
import { AktivitetspliktStatusTagOgGyldig } from '~shared/tags/AktivitetspliktStatusOgGyldig'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'

export const AktivitetspliktVurdering12MndOversikt = () => {
  const { vurdering } = useAktivitetspliktOppgaveVurdering()
  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4" maxWidth="120rem">
        <VStack gap="4">
          <Heading level="1" size="large">
            Oppfølging av aktivitet
          </Heading>
          <Heading size="medium" level="2">
            Du skal nå vurdere brukers aktivitetsgrad
          </Heading>
          <BodyShort>
            Du skal nå vurdere brukes aktivitetsgrad Det stilles ulike krav til aktivitet utifra tid etter dodsfallet.
            Seks måneder etter dødsfallet må gjenlevende være i minst 50 % aktivitet for å ha rett til
            omstillingsstønad. Videre kan det stilles krav til 100 % aktivitet etter 12 måneder. I visse tilfeller kan
            man ha rett på omstillingsstønad selv om aktivtetskravet ikke er oppfylt.
          </BodyShort>
          <AktivitetspliktStatusTagOgGyldig aktivitetspliktVurdering={vurdering} />
        </VStack>
      </Box>
    </>
  )
}
