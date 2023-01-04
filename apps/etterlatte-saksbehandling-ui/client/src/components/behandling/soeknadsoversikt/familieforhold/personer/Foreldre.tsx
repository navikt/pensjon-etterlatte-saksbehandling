import { Heading } from '@navikt/ds-react'
import { GjenlevendeForelder } from '~components/behandling/soeknadsoversikt/familieforhold/personer/GjenlevendeForelder'
import { AvdoedForelder } from '~components/behandling/soeknadsoversikt/familieforhold/personer/AvdoedForelder'
import { IPdlPerson } from '~shared/types/Person'

interface IProps {
  gjenlevende: IPdlPerson
  innsenderErGjenlevende: boolean
  doedsdato: string
  avdoed: IPdlPerson
}

export const Foreldre = ({ gjenlevende, innsenderErGjenlevende, doedsdato, avdoed }: IProps) => {
  return (
    <>
      <Heading size="small" level="3">
        Foreldre
      </Heading>
      <GjenlevendeForelder
        person={gjenlevende}
        innsenderErGjenlevendeForelder={innsenderErGjenlevende}
        doedsdato={doedsdato}
      />
      <AvdoedForelder person={avdoed} />
    </>
  )
}
