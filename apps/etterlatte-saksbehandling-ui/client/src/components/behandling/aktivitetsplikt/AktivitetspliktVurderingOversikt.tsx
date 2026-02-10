import { BodyShort, Box, Heading, VStack } from '@navikt/ds-react'
import React from 'react'
import { AktivitetspliktStatusTagOgGyldig } from '~shared/tags/AktivitetspliktStatusOgGyldig'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { AktivitetspliktOppgaveVurderingType } from '~shared/types/Aktivitetsplikt'

export const AktivitetspliktVurderingOversikt = () => {
  const { vurdering, vurderingType } = useAktivitetspliktOppgaveVurdering()
  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4" maxWidth="120rem">
        <VStack gap="space-4">
          <Heading level="1" size="large">
            Infobrev om aktivitetsplikt{' '}
            {vurderingType === AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER ? 'ved 12 måneder' : 'ved 6 måneder'}
          </Heading>
          <Heading size="medium" level="2">
            Du skal nå vurdere brukers aktivitetsgrad
          </Heading>
          <BodyShort>
            Det stilles ulike krav til aktivitet utifra tid etter dødsfallet. Seks måneder etter dødsfallet må
            gjenlevende være i minst 50 % aktivitet for å ha rett til omstillingsstønad. Videre kan det stilles krav til
            100 % aktivitet etter 12 måneder. I visse tilfeller kan man ha rett på omstillingsstønad selv om
            aktivtetskravet ikke er oppfylt.
          </BodyShort>
          <AktivitetspliktStatusTagOgGyldig aktivitetspliktVurdering={vurdering} />
        </VStack>
      </Box>
    </>
  )
}
