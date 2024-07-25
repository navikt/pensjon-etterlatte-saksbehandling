import React from 'react'
import { InstitusjonsoppholdGrunnlagData, InstitusjonsoppholdIBeregning, ReduksjonOMS } from '~shared/types/Beregning'
import { BodyShort, Box, Button, HStack, Label, Table } from '@navikt/ds-react'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'

interface Props {
  institusjonsopphold: InstitusjonsoppholdGrunnlagData
}

export const InstitusjonsoppholdBeregningsgrunnlagTable = ({ institusjonsopphold }: Props) => {
  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell />
          <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
          <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
          <Table.HeaderCell scope="col">Reduksjon</Table.HeaderCell>
          <Table.HeaderCell scope="col" />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {!!institusjonsopphold?.length ? (
          institusjonsopphold.map(
            (opphold: PeriodisertBeregningsgrunnlag<InstitusjonsoppholdIBeregning>, index: number) => (
              <Table.ExpandableRow
                key={index}
                content={
                  <HStack gap="8">
                    <div>
                      <Label>Egen reduksjon</Label>
                      <BodyShort>{opphold.data.egenReduksjon ?? '-'}</BodyShort>
                    </div>
                    <Box maxWidth="7">
                      <Label>Beskrivelse</Label>
                      <BodyShort>{opphold.data.begrunnelse}</BodyShort>
                    </Box>
                  </HStack>
                }
              >
                <Table.DataCell>{formaterDatoMedFallback(opphold.fom, '-')}</Table.DataCell>
                <Table.DataCell>{formaterDatoMedFallback(opphold.tom, '-')}</Table.DataCell>
                <Table.DataCell>{ReduksjonOMS[opphold.data.reduksjon]}</Table.DataCell>
                <Table.DataCell>
                  <HStack gap="2" wrap={false} justify="end">
                    <Button type="button" variant="secondary" size="small" icon={<PencilIcon aria-hidden />}>
                      Rediger
                    </Button>
                    <Button type="button" variant="secondary" size="small" icon={<TrashIcon aria-hidden />}>
                      Slett
                    </Button>
                  </HStack>
                </Table.DataCell>
              </Table.ExpandableRow>
            )
          )
        ) : (
          <Table.Row>
            <Table.DataCell colSpan={5}>Ingen perioder for institusjonsopphold</Table.DataCell>
          </Table.Row>
        )}
      </Table.Body>
    </Table>
  )
}
