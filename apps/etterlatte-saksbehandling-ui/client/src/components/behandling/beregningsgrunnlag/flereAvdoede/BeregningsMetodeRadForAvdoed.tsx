import React, { useState } from 'react'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { BeregningsGrunnlagPostDto, BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { Button, Table } from '@navikt/ds-react'
import { format, startOfMonth } from 'date-fns'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { BeregningsMetodeForAvdoded } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsMetodeForAvdoded'
import { isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import { oppdaterBeregingsGrunnlag } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'

interface Props {
  behandlingId: string
  ident: string
  navn: string
  beregningsMetodeForAvdoed: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined
  redigerbar: boolean
  patchGrunnlagOppdaterMetode: (
    nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>
  ) => BeregningsGrunnlagPostDto
  patchGrunnlagSlettMetode: (ident: string) => BeregningsGrunnlagPostDto
}

export const BeregningsMetodeRadForAvdoed = ({
  behandlingId,
  ident,
  beregningsMetodeForAvdoed,
  navn,
  redigerbar,
  patchGrunnlagOppdaterMetode,
  patchGrunnlagSlettMetode,
}: Props) => {
  const dispatch = useAppDispatch()
  const [redigerModus, setRedigerModus] = useState<boolean>(false)
  const [lagreBeregningsgrunnlagResult, lagreBeregningsgrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)

  return (
    <Table.ExpandableRow
      open={redigerModus}
      onOpenChange={(open) => {
        setRedigerModus(open)
      }}
      key={ident}
      content={
        redigerModus ? (
          <BeregningsMetodeForAvdoded
            ident={ident}
            navn={navn}
            eksisterendeMetode={beregningsMetodeForAvdoed}
            paaAvbryt={() => {
              setRedigerModus(false)
            }}
            oppdaterBeregningsMetodeForAvdoed={oppdaterBeregninggsMetodeForAvdoed}
            lagreBeregningsgrunnlagResult={lagreBeregningsgrunnlagResult}
          />
        ) : (
          'Hadet'
        )
      }
    >
      <Table.DataCell>{navn}</Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed
          ? beregningsMetodeForAvdoed.data.beregningsMetode.beregningsMetode
          : 'Metode er ikke satt'}
      </Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed?.fom ? format(startOfMonth(beregningsMetodeForAvdoed.fom), 'yyyy-MM-dd') : '-'}
      </Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed?.tom ? format(startOfMonth(beregningsMetodeForAvdoed.tom), 'yyyy-MM-dd') : '-'}
      </Table.DataCell>
      {redigerbar &&
        (beregningsMetodeForAvdoed ? (
          <>
            <Table.DataCell>{redigerKnapp(() => setRedigerModus(true))}</Table.DataCell>
            <Table.DataCell>{slettKnapp(slettBeregningsMetodeForAvdoed, lagreBeregningsgrunnlagResult)}</Table.DataCell>
          </>
        ) : (
          <Table.DataCell>{leggTilKnapp(() => setRedigerModus(true))}</Table.DataCell>
        ))}
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

  function leggTilKnapp(redigerFn) {
    return (
      <Button
        type="button"
        variant="secondary"
        size="small"
        icon={<PencilIcon aria-hidden />}
        disabled={redigerModus}
        onClick={redigerFn}
      >
        Legg til
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

  function oppdaterBeregninggsMetodeForAvdoed(nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>) {
    const grunnlag = patchGrunnlagOppdaterMetode(nyMetode)

    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandlingId,
        grunnlag,
      },
      () => {
        dispatch(oppdaterBeregingsGrunnlag(grunnlag))
        setRedigerModus(false)
      }
    )
  }

  function slettBeregningsMetodeForAvdoed() {
    const grunnlag = patchGrunnlagSlettMetode(ident)

    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandlingId,
        grunnlag,
      },
      () => {
        dispatch(oppdaterBeregingsGrunnlag(grunnlag))
        setRedigerModus(false)
      }
    )
  }
}
