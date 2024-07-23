import React from 'react'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { Button, HStack, Table } from '@navikt/ds-react'
import { OverstyrBeregningsperiode, OverstyrtAarsak } from '~shared/types/Beregning'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { OverstyrBeregningsgrunnlagExpandableRowContent } from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/OverstyrBeregningsgrunnlagExpandableRowContent'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreOverstyrBeregningGrunnlag } from '~shared/api/beregning'
import { oppdaterOverstyrBeregningsGrunnlag } from '~store/reducers/BehandlingReducer'
import { isPending } from '~shared/api/apiUtils'

export const OverstyrtBeregningsgrunnlagTable = ({ behandlingId }: { behandlingId: string }) => {
  const overstyrtBeregningPerioder = useAppSelector(
    (state) => state.behandlingReducer.behandling?.overstyrBeregning?.perioder
  )

  const dispatch = useAppDispatch()

  const [lagreOverstyrBeregningGrunnlagResult, lagreOverstyrBeregningGrunnlagRequest] =
    useApiCall(lagreOverstyrBeregningGrunnlag)

  const slettPeriode = (index: number) => {
    if (overstyrtBeregningPerioder) {
      const perioderKopi = [...overstyrtBeregningPerioder]
      perioderKopi.splice(index, 1)

      lagreOverstyrBeregningGrunnlagRequest(
        {
          behandlingId,
          grunnlag: {
            perioder: perioderKopi,
          },
        },
        (result) => dispatch(oppdaterOverstyrBeregningsGrunnlag(result))
      )
    }
  }

  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell />
          <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
          <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
          <Table.HeaderCell scope="col">Beløp utbetalt</Table.HeaderCell>
          <Table.HeaderCell scope="col">Årsak</Table.HeaderCell>
          <Table.HeaderCell scope="col" />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {!!overstyrtBeregningPerioder?.length ? (
          overstyrtBeregningPerioder.map(
            (
              overtyrBeregningsgrunnlagPeriode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>,
              index: number
            ) => {
              return (
                <Table.ExpandableRow
                  key={index}
                  content={
                    <OverstyrBeregningsgrunnlagExpandableRowContent
                      overtyrBeregningsgrunnlagPeriode={overtyrBeregningsgrunnlagPeriode}
                    />
                  }
                >
                  <Table.DataCell>{formaterDatoMedFallback(overtyrBeregningsgrunnlagPeriode.fom, '-')}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(overtyrBeregningsgrunnlagPeriode.tom, '-')}</Table.DataCell>
                  <Table.DataCell>{overtyrBeregningsgrunnlagPeriode.data.utbetaltBeloep}</Table.DataCell>
                  <Table.DataCell>
                    {overtyrBeregningsgrunnlagPeriode.data.aarsak &&
                      overtyrBeregningsgrunnlagPeriode.data.aarsak !== 'VELG_AARSAK' &&
                      OverstyrtAarsak[overtyrBeregningsgrunnlagPeriode.data.aarsak]}
                  </Table.DataCell>
                  <Table.DataCell>
                    <HStack gap="2" align="center" justify="end" wrap={false}>
                      <Button size="small" variant="secondary" icon={<PencilIcon aria-hidden />}>
                        Rediger
                      </Button>
                      <Button
                        size="small"
                        variant="secondary"
                        icon={<TrashIcon aria-hidden />}
                        onClick={() => slettPeriode(index)}
                        loading={isPending(lagreOverstyrBeregningGrunnlagResult)}
                      >
                        Slett
                      </Button>
                    </HStack>
                  </Table.DataCell>
                </Table.ExpandableRow>
              )
            }
          )
        ) : (
          <Table.Row>
            <Table.DataCell colSpan={6}>Ingen perioder for overstyrt beregningsgrunnlag</Table.DataCell>
          </Table.Row>
        )}
      </Table.Body>
    </Table>
  )
}
