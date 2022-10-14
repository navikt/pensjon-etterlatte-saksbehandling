import { RadioGroup, Radio } from '@navikt/ds-react'
import { Underkjenn } from './underkjenn'
import { RadioGroupWrapper } from '../styled'
import { Godkjenn } from './godkjenn'
import { IBeslutning } from '../types'
import { useAppSelector } from '../../../../store/Store'

type Props = {
  beslutning: IBeslutning | undefined
  setBeslutning: (value: IBeslutning) => void
}

export const Beslutningsvalg: React.FC<Props> = ({ beslutning, setBeslutning }) => {
  const behandlingId = useAppSelector((state) => state.behandlingReducer.behandling.id)

  return (
    <>
      <RadioGroupWrapper>
        <RadioGroup
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

      {beslutning === IBeslutning.godkjenn && <Godkjenn behandlingId={behandlingId} />}
      {beslutning === IBeslutning.underkjenn && <Underkjenn behandlingId={behandlingId} />}
    </>
  )
}
