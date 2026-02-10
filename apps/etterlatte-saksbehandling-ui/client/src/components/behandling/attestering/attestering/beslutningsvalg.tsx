import { HStack, Radio, RadioGroup } from '@navikt/ds-react'
import { Underkjenn } from './underkjenn'
import { IBeslutning } from '../types'
import { Godkjenn } from '~components/behandling/attestering/attestering/godkjenn'

type Props = {
  beslutning: IBeslutning | undefined
  setBeslutning: (value: IBeslutning) => void
  disabled: boolean
}

export const Beslutningsvalg = ({ beslutning, setBeslutning, disabled }: Props) => (
  <>
    <RadioGroup
      disabled={disabled}
      legend="Beslutning"
      size="small"
      className="radioGroup"
      onChange={(event) => setBeslutning(IBeslutning[event as IBeslutning])}
    >
      <HStack gap="space-4" wrap={false} justify="space-between">
        <Radio value={IBeslutning.godkjenn.toString()}>Godkjenn</Radio>
        <Radio value={IBeslutning.underkjenn.toString()}>Underkjenn</Radio>
      </HStack>
    </RadioGroup>

    {beslutning === IBeslutning.godkjenn && <Godkjenn />}
    {beslutning === IBeslutning.underkjenn && <Underkjenn />}
  </>
)
