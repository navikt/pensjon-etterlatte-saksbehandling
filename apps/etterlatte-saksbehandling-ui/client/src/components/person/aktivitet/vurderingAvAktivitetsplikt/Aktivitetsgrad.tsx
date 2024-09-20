import React from 'react'
import { IAktivitetspliktAktivitetsgrad, tekstAktivitetspliktVurderingType } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Box, Detail, Heading, HStack, Label, Table, VStack } from '@navikt/ds-react'
import { ClockDashedIcon } from '@navikt/aksel-icons'
import { AktivitetsgradReadMore } from '~components/person/aktivitet/vurderingAvAktivitetsplikt/components/AktivitetsgradReadMore'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'

export const Aktivitetsgrad = ({ aktiviteter }: { aktiviteter: IAktivitetspliktAktivitetsgrad[] | undefined }) => {
  return (
    <VStack gap="4">
      <HStack gap="4" align="center">
        <ClockDashedIcon fontSize="1.5rem" aria-hidden />
        <Heading size="medium">Aktivitetsgrad</Heading>
      </HStack>

      <AktivitetsgradReadMore />

      <Table size="small">
        <Table.Header>
          <Table.HeaderCell />
          <Table.HeaderCell scope="col">Aktivitetsgrad</Table.HeaderCell>
          <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
          <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
          <Table.HeaderCell scope="col">Kilde</Table.HeaderCell>
        </Table.Header>
        <Table.Body>
          {!!aktiviteter?.length ? (
            <>
              {aktiviteter.map((aktivitet) => (
                <Table.ExpandableRow
                  key={aktivitet.id}
                  content={
                    <Box maxWidth="42.5rem">
                      <Label>Beskrivelse</Label>
                      <BodyShort>{aktivitet.beskrivelse}</BodyShort>
                    </Box>
                  }
                >
                  <Table.DataCell>{tekstAktivitetspliktVurderingType[aktivitet.aktivitetsgrad]}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(aktivitet.fom, '-')}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(aktivitet.tom, '-')}</Table.DataCell>
                  <Table.DataCell>
                    <BodyShort>{aktivitet.endret.ident}</BodyShort>
                    <Detail>Saksbehandler: {formaterDato(aktivitet.endret.tidspunkt)}</Detail>
                  </Table.DataCell>
                </Table.ExpandableRow>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={5}>Ingen aktivitetsgrad</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
