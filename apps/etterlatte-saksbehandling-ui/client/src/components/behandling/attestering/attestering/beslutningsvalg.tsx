import { RadioGroup, Radio } from '@navikt/ds-react'
import { Underkjenn } from './underkjenn'
import { RadioGroupWrapper } from '../styled'
import { IBeslutning } from '../types'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Godkjenn } from '~components/behandling/attestering/attestering/godkjenn'

type Props = {
  beslutning: IBeslutning | undefined
  setBeslutning: (value: IBeslutning) => void
  behandling: IDetaljertBehandling
  disabled: boolean
}

export const Beslutningsvalg: React.FC<Props> = ({ beslutning, setBeslutning, behandling, disabled }) => {
  return (
    <>
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

      {beslutning === IBeslutning.godkjenn && <Godkjenn behandling={behandling} />}
      {beslutning === IBeslutning.underkjenn && <Underkjenn behandling={behandling} />}
    </>
  )
}
