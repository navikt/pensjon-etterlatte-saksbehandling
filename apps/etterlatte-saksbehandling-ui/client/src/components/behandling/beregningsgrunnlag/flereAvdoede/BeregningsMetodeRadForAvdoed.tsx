import React, { useState } from 'react'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { Button, Table } from '@navikt/ds-react'
import { format, startOfMonth } from 'date-fns'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { BeregningsMetodeForAvdoded } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsMetodeForAvdoded'
import { isPending, Result } from '~shared/api/apiUtils'

interface Props {
  beregningsMetodeForAvdoed: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined
  navn: string
  redigerbar: boolean
  lagreBeregningsgrunnlagResult: Result<void>
  oppdaterBeregningsMetodeForAvdoed: (
    nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>,
    onSuccess: () => void
  ) => void
  slettBeregningsMetodeForAvdoed: (ident: string, onSuccess: () => void) => void
}

export const BeregningsMetodeRadForAvdoed = ({
  beregningsMetodeForAvdoed,
  navn,
  redigerbar,
  oppdaterBeregningsMetodeForAvdoed,
  lagreBeregningsgrunnlagResult,
  slettBeregningsMetodeForAvdoed,
}: Props) => {
  const [redigerModus, setRedigerModus] = useState<boolean>(false)

  const slettFn = (ident) =>
    slettBeregningsMetodeForAvdoed(ident, () => {
      setRedigerModus(false)
    })

  return beregningsMetodeForAvdoed ? (
    <Table.ExpandableRow
      open={redigerModus}
      content={
        redigerModus ? (
          <BeregningsMetodeForAvdoded
            ident={beregningsMetodeForAvdoed.data.avdoed}
            navn={navn}
            paaAvbryt={() => {
              setRedigerModus(false)
            }}
            oppdaterBeregningsMetodeForAvdoed={(data) =>
              oppdaterBeregningsMetodeForAvdoed(data, () => {
                setRedigerModus(false)
              })
            }
            lagreBeregningsgrunnlagResult={lagreBeregningsgrunnlagResult}
          />
        ) : (
          'Hadet'
        )
      }
    >
      <Table.DataCell>{navn}</Table.DataCell>
      <Table.DataCell>{beregningsMetodeForAvdoed.data.beregningsMetode.beregningsMetode}</Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed.fom ? format(startOfMonth(beregningsMetodeForAvdoed.fom), 'yyyy-MM-dd') : ''}
      </Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed.tom ? format(startOfMonth(beregningsMetodeForAvdoed.tom), 'yyyy-MM-dd') : ''}
      </Table.DataCell>
      <Table.DataCell>{redigerbar ? redigerKnapp(() => setRedigerModus(true)) : ''}</Table.DataCell>
      <Table.DataCell>
        {redigerbar
          ? slettKnapp(() => slettFn(beregningsMetodeForAvdoed.data.avdoed), lagreBeregningsgrunnlagResult)
          : ''}
      </Table.DataCell>
      <Table.DataCell>Slett</Table.DataCell>
    </Table.ExpandableRow>
  ) : (
    <Table.ExpandableRow content="">
      <Table.DataCell>{navn}</Table.DataCell>
      <Table.DataCell>Metode er ikke satt</Table.DataCell>
      <Table.DataCell>-</Table.DataCell>
      <Table.DataCell>-</Table.DataCell>
      <Table.DataCell />
      <Table.DataCell />
    </Table.ExpandableRow>
  )

  function redigerKnapp(redigerFn) {
    return (
      <Button
        type="button"
        variant="secondary"
        size="small"
        icon={<PencilIcon aria-hidden />}
        disabled={redigerModus}
        onClick={redigerFn}
      >
        Rediger
      </Button>
    )
  }

  function slettKnapp(slettFn, slettResult) {
    return (
      <Button
        size="small"
        variant="secondary"
        icon={<TrashIcon aria-hidden />}
        loading={isPending(slettResult)}
        onClick={slettFn}
      >
        Slett
      </Button>
    )
  }
}
