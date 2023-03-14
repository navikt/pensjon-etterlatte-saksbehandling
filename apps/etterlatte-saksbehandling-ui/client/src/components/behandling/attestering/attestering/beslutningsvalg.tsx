import { RadioGroup, Radio } from '@navikt/ds-react'
import { Underkjenn } from './underkjenn'
import { RadioGroupWrapper } from '../styled'
import { IBeslutning } from '../types'
import { AttesterVedtak } from '~components/behandling/attestering/handinger/attesterVedtak'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

type Props = {
  beslutning: IBeslutning | undefined
  setBeslutning: (value: IBeslutning) => void
  behandling: IDetaljertBehandling
}

export const Beslutningsvalg: React.FC<Props> = ({ beslutning, setBeslutning, behandling }) => {
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

      {beslutning === IBeslutning.godkjenn && <AttesterVedtak behandling={behandling} />}
      {beslutning === IBeslutning.underkjenn && <Underkjenn behandlingId={behandling.id} />}
    </>
  )
}
