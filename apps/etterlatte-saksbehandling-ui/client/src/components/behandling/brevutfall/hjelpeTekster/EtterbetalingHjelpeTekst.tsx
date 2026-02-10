import React, { ReactNode } from 'react'
import { BodyLong, HelpText, HStack, VStack } from '@navikt/ds-react'

export const EtterbetalingHjelpeTekst = (): ReactNode => {
  return (
    <HStack gap="space-2">
      Skal det etterbetales?
      <HelpText placement="top">
        <VStack gap="space-4">
          <BodyLong>
            Velg &quot;ja&quot; hvis ytelsen er innvilget tilbake i tid, og normal utbetalingsdato for gjeldende periode
            er forbi. Dette gjelder dersom det vises perioder med &quot;Etterbetaling&quot; under &quot;Simulere
            utbetaling&quot;.
          </BodyLong>
          <BodyLong>
            &quot;Fra og med&quot;-dato settes lik virkningstidspunktet i kravet, og &quot;Til og med&quot;-dato settes
            lik siste passerte normale utbetalingsdato. Det vil si at inneværende måned skal være med som etterbetaling.
            Simuleringen ovenfor viser inneværende måned som etterbetaling dersom normal utbetalingsdato er passert.
          </BodyLong>
        </VStack>
      </HelpText>
    </HStack>
  )
}
