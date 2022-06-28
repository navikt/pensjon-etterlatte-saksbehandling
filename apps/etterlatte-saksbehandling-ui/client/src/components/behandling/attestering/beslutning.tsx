import { RadioGroup, Radio } from '@navikt/ds-react'
import { useContext } from 'react'
import { Underkjenn } from './underkjenn'
import { RadioGroupWrapper } from './styled'
import { Godkjenn } from './godkjenn'
import { AppContext } from '../../../store/AppContext'
import { IBeslutning } from './types'

export type Props = {
  beslutning: IBeslutning | undefined
  setBeslutning: any
}

export const Beslutning: React.FC<Props> = ({ beslutning, setBeslutning }) => {
  const behandlingId = useContext(AppContext).state.behandlingReducer.id

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
