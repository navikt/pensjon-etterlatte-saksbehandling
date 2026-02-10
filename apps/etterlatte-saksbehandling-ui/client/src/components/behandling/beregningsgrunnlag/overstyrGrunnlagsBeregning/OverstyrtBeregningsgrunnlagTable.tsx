import React, { useState } from 'react'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { Button, HStack, Table } from '@navikt/ds-react'
import { OverstyrBeregningsperiode, OverstyrtAarsak } from '~shared/types/Beregning'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { OverstyrBeregningsgrunnlagExpandableRowContent } from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/OverstyrBeregningsgrunnlagExpandableRowContent'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreOverstyrBeregningGrunnlag } from '~shared/api/beregning'
import { oppdaterBehandlingsstatus, oppdaterOverstyrBeregningsGrunnlag } from '~store/reducers/BehandlingReducer'
import { isPending } from '~shared/api/apiUtils'
import { OverstyrBeregningsgrunnlagPeriodeSkjema } from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/OverstyrBeregningsgrunnlagPeriodeSkjema'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

interface PeriodeRedigeringModus {
  redigerPeriode: boolean
  erAapen: boolean
  periodeIndex: number | undefined
}

const defaultPeriodeRedigeringModus: PeriodeRedigeringModus = {
  redigerPeriode: false,
  erAapen: false,
  periodeIndex: undefined,
}

export const OverstyrtBeregningsgrunnlagTable = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const [periodeRedigeringModus, setPeriodeRedigeringModus] =
    useState<PeriodeRedigeringModus>(defaultPeriodeRedigeringModus)

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
          behandlingId: behandling.id,
          grunnlag: {
            perioder: perioderKopi,
          },
        },
        (result) => {
          dispatch(oppdaterOverstyrBeregningsGrunnlag(result))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
        }
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
              overstyrBeregningsgrunnlagPeriode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>,
              index: number
            ) => {
              return (
                <Table.ExpandableRow
                  key={index}
                  open={periodeRedigeringModus.erAapen && index === periodeRedigeringModus.periodeIndex}
                  onOpenChange={(open) => {
                    setPeriodeRedigeringModus({
                      redigerPeriode: open && periodeRedigeringModus.redigerPeriode,
                      erAapen: open,
                      periodeIndex: index,
                    })
                  }}
                  content={
                    !periodeRedigeringModus.redigerPeriode ? (
                      <OverstyrBeregningsgrunnlagExpandableRowContent
                        overtyrBeregningsgrunnlagPeriode={overstyrBeregningsgrunnlagPeriode}
                        sakType={behandling.sakType}
                      />
                    ) : (
                      <OverstyrBeregningsgrunnlagPeriodeSkjema
                        behandling={behandling}
                        eksisterendePeriode={overstyrBeregningsgrunnlagPeriode}
                        indexTilEksisterendePeriode={index}
                        paaAvbryt={() => setPeriodeRedigeringModus(defaultPeriodeRedigeringModus)}
                        paaLagre={() => setPeriodeRedigeringModus(defaultPeriodeRedigeringModus)}
                      />
                    )
                  }
                >
                  <Table.DataCell>{formaterDatoMedFallback(overstyrBeregningsgrunnlagPeriode.fom, '-')}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(overstyrBeregningsgrunnlagPeriode.tom, '-')}</Table.DataCell>
                  <Table.DataCell>{overstyrBeregningsgrunnlagPeriode.data.utbetaltBeloep}</Table.DataCell>
                  <Table.DataCell>
                    {overstyrBeregningsgrunnlagPeriode.data.aarsak &&
                      overstyrBeregningsgrunnlagPeriode.data.aarsak !== 'VELG_AARSAK' &&
                      OverstyrtAarsak[overstyrBeregningsgrunnlagPeriode.data.aarsak]}
                  </Table.DataCell>
                  <Table.DataCell>
                    <HStack gap="space-2" align="center" justify="end" wrap={false}>
                      <Button
                        size="small"
                        variant="secondary"
                        icon={<PencilIcon aria-hidden />}
                        disabled={
                          periodeRedigeringModus.redigerPeriode && index === periodeRedigeringModus.periodeIndex
                        }
                        onClick={() => {
                          setPeriodeRedigeringModus({
                            redigerPeriode: true,
                            erAapen: true,
                            periodeIndex: index,
                          })
                        }}
                      >
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
