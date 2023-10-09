import { Heading, Radio, RadioGroup } from '@navikt/ds-react'
import { Underkjenn } from './underkjenn'
import { RadioGroupWrapper } from '../styled'
import { IBeslutning } from '../types'
import { Godkjenn } from '~components/behandling/attestering/attestering/godkjenn'

type Props = {
  beslutning: IBeslutning | undefined
  setBeslutning: (value: IBeslutning) => void
  disabled: boolean
}

export const Beslutningsvalg = ({ beslutning, setBeslutning, disabled }: Props) => {
  return (
    <>
      <Heading size="xsmall">Beslutning</Heading>

      <RadioGroupWrapper>
        <RadioGroup
          disabled={disabled}
          legend=""
          size="small"
          className="radioGroup"
          onChange={(event) => setBeslutning(IBeslutning[event as IBeslutning])}
        >
          <div className="flex">
            <Radio value={IBeslutning.godkjenn.toString()}>Godkjenn</Radio>
            <Radio value={IBeslutning.underkjenn.toString()}>Underkjenn</Radio>
          </div>
        </RadioGroup>
      </RadioGroupWrapper>
      {beslutning === IBeslutning.godkjenn && <Godkjenn />}
      {beslutning === IBeslutning.underkjenn && <Underkjenn />}
    </>
  )
}
