import React, { useState } from 'react'
import { InstitusjonsoppholdIBeregning, ReduksjonOMS } from '~shared/types/Beregning'
import { BodyShort, Box, Button, HStack, Label, Table } from '@navikt/ds-react'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { InstitusjonsoppholdBeregningsgrunnlagSkjema } from '~components/behandling/beregningsgrunnlag/felles/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagSkjema'
import { SakType } from '~shared/types/sak'

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
  behandling: IDetaljertBehandling
  sakType: SakType
  institusjonsopphold: PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>[] | undefined
}

export const InstitusjonsoppholdBeregningsgrunnlagTable = ({ behandling, sakType, institusjonsopphold }: Props) => {
  const [periodeRedigeringModus, setPeriodeRedigeringModus] =
    useState<PeriodeRedigeringModus>(defaultPeriodeRedigeringModus)

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
                      behandling={behandling}
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
                <Table.DataCell>{ReduksjonOMS[opphold.data.reduksjon]}</Table.DataCell>
                <Table.DataCell>{opphold.data.egenReduksjon ?? '-'}</Table.DataCell>
                <Table.DataCell>
                  <HStack gap="2" wrap={false} justify="end">
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
                      //TODO: LEGGE INN STØTTE FOR SLETTING
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
