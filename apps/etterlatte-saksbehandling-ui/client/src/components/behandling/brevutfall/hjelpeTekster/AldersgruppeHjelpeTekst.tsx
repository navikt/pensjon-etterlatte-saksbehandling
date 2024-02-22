import React, { ReactNode } from 'react'
import { HjelpeTekst } from '~shared/components/hjelpeTekst/HjelpeTekst'
import { HelpText } from '@navikt/ds-react'

export const AldersgruppeHjelpeTekst = (): ReactNode => {
  return (
    <HjelpeTekst>
      Gjelder brevet under eller over 18 år?
      <HelpText strategy="fixed">
        Velg her gjeldende alternativ for barnet, slik at riktig informasjon kommer med i vedlegg 2. For barn under 18
        år skal det stå &quot;Informasjon til deg som handler på vegne av barnet&quot;, mens for barn over 18 år skal
        det stå &quot;Informasjon til deg som mottar barnepensjon&quot;.
      </HelpText>
    </HjelpeTekst>
  )
}
