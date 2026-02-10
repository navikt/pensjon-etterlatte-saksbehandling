import React, { useState } from 'react'
import {
  BeregningsGrunnlagDto,
  InstitusjonsoppholdIBeregning,
  LagreBeregningsGrunnlagDto,
  ReduksjonBP,
  ReduksjonOMS,
  toLagreBeregningsGrunnlagDto,
} from '~shared/types/Beregning'
import { BodyShort, Box, Button, HStack, Label, Table } from '@navikt/ds-react'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { InstitusjonsoppholdBeregningsgrunnlagSkjema } from '~components/behandling/beregningsgrunnlag/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagSkjema'
import { SakType } from '~shared/types/sak'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import { isPending } from '~shared/api/apiUtils'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregningsGrunnlag,
} from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

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

interface Props {
  behandling: IBehandlingReducer
  sakType: SakType
  beregningsgrunnlag?: BeregningsGrunnlagDto
  institusjonsopphold: PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>[] | undefined
}

export const InstitusjonsoppholdBeregningsgrunnlagTable = ({
  behandling,
  sakType,
  beregningsgrunnlag,
  institusjonsopphold,
}: Props) => {
  const [periodeRedigeringModus, setPeriodeRedigeringModus] =
    useState<PeriodeRedigeringModus>(defaultPeriodeRedigeringModus)

  const dispatch = useAppDispatch()

  const [lagreBeregningsGrunnlagResult, lagreBeregningsGrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)
  const slettPeriode = (index: number) => {
    if (institusjonsopphold) {
      const perioderKopi = [...institusjonsopphold]
      perioderKopi.splice(index, 1)

      const grunnlag: LagreBeregningsGrunnlagDto = {
        ...toLagreBeregningsGrunnlagDto(beregningsgrunnlag),
        institusjonsopphold: perioderKopi,
      }

      lagreBeregningsGrunnlagRequest(
        {
          behandlingId: behandling.id,
          grunnlag: grunnlag,
        },
        (result) => {
          dispatch(oppdaterBeregningsGrunnlag(result))
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
          <Table.HeaderCell scope="col">Reduksjon</Table.HeaderCell>
          <Table.HeaderCell scope="col">Egen reduksjon</Table.HeaderCell>
          <Table.HeaderCell scope="col" />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {!!institusjonsopphold?.length ? (
          institusjonsopphold.map(
            (opphold: PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>, index: number) => (
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
                    <Box maxWidth="7">
                      <Label>Beskrivelse</Label>
                      <BodyShort>{opphold.data.begrunnelse}</BodyShort>
                    </Box>
                  ) : (
                    <InstitusjonsoppholdBeregningsgrunnlagSkjema
                      sakType={sakType}
                      eksisterendePeriode={opphold}
                      institusjonsopphold={institusjonsopphold}
                      indexTilEksisterendePeriode={index}
                      paaAvbryt={() => setPeriodeRedigeringModus(defaultPeriodeRedigeringModus)}
                      paaLagre={() => setPeriodeRedigeringModus(defaultPeriodeRedigeringModus)}
                    />
                  )
                }
              >
                <Table.DataCell>{formaterDatoMedFallback(opphold.fom, '-')}</Table.DataCell>
                <Table.DataCell>{formaterDatoMedFallback(opphold.tom, '-')}</Table.DataCell>
                <Table.DataCell>
                  {sakType === SakType.OMSTILLINGSSTOENAD
                    ? ReduksjonOMS[opphold.data.reduksjon]
                    : ReduksjonBP[opphold.data.reduksjon]}
                </Table.DataCell>
                <Table.DataCell>{opphold.data.egenReduksjon ?? '-'}</Table.DataCell>
                <Table.DataCell>
                  <HStack gap="space-2" wrap={false} justify="end">
                    <Button
                      type="button"
                      variant="secondary"
                      size="small"
                      icon={<PencilIcon aria-hidden />}
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
                      type="button"
                      variant="secondary"
                      size="small"
                      icon={<TrashIcon aria-hidden />}
                      loading={isPending(lagreBeregningsGrunnlagResult)}
                      onClick={() => slettPeriode(index)}
                    >
                      Slett
                    </Button>
                  </HStack>
                </Table.DataCell>
              </Table.ExpandableRow>
            )
          )
        ) : (
          <Table.Row>
            <Table.DataCell colSpan={6}>Ingen perioder for institusjonsopphold</Table.DataCell>
          </Table.Row>
        )}
      </Table.Body>
    </Table>
  )
}
