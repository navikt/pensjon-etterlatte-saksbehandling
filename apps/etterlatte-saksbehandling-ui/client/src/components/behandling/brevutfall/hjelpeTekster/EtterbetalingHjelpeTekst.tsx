import React, { ReactNode } from 'react'
import { HelpText, HStack } from '@navikt/ds-react'

export const EtterbetalingHjelpeTekst = (): ReactNode => {
  return (
    <HStack gap="2">
      Skal det etterbetales?
      <HelpText placement="top">
        Velg &#34;ja&#34; hvis ytelsen er innvilget tilbake i tid, og normal utbetalingsdato for gjeldende periode er
        forbi. Dette gjelder dersom det vises perioder med &#34;Etterbetaling&#34; under &#34;Simulere utbetaling&#34;.
        <br />
        <br />
        &#34;Fra og med&#34;-dato settes lik virkningstidspunktet i kravet, og &#34;Til og med&#34;-dato settes lik
        siste passerte normale utbetalingsdato. Det vil si at inneværende måned skal være med som etterbetaling.
        Simuleringen ovenfor viser inneværende måned som etterbetaling dersom normal utbetalingsdato er passert.
      </HelpText>
    </HStack>
  )
}
