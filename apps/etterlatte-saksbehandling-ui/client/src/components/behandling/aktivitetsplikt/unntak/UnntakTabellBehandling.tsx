import {
  AktivitetspliktOppgaveVurderingType,
  IAktivitetspliktUnntak,
  tekstAktivitetspliktUnntakType,
} from '~shared/types/Aktivitetsplikt'
import { BodyShort, Box, Detail, Heading, HStack, ReadMore, Table, VStack } from '@navikt/ds-react'
import { AktivitetspliktUnntakTypeTag } from '~shared/tags/AktivitetspliktUnntakTypeTag'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'
import React from 'react'
import { HandShakeHeartIcon } from '@navikt/aksel-icons'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { RedigerbarUnntakBehandling } from '~components/behandling/aktivitetsplikt/unntak/RedigerbarUnntakBehandling'

export const UnntakTabellBehandling = ({
  unntak,
  behandling,
  typeVurdering,
}: {
  unntak: IAktivitetspliktUnntak[]
  behandling: IDetaljertBehandling
  typeVurdering: AktivitetspliktOppgaveVurderingType
}) => {
  return (
    <VStack gap="space-4">
      <HStack gap="space-4" align="center">
        <HandShakeHeartIcon fontSize="1.5rem" aria-hidden />
        <Heading size="small">Unntak</Heading>
      </HStack>

      <Box maxWidth="42.5rem">
        <ReadMore header="Dette menes med unntak">
          I oversikten over unntak ser du hvilke unntak som er satt på den gjenlevende. Det finnes både midlertidige og
          varige unntak
        </ReadMore>
      </Box>

      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Unntak</Table.HeaderCell>
            <Table.HeaderCell scope="col">Type</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Kilde</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {unntak?.length ? (
            <>
              {unntak.map((unntak) => (
                <Table.ExpandableRow
                  key={unntak.id}
                  content={
                    <RedigerbarUnntakBehandling behandling={behandling} unntak={unntak} typeVurdering={typeVurdering} />
                  }
                >
                  <Table.DataCell>{tekstAktivitetspliktUnntakType[unntak.unntak]}</Table.DataCell>
                  <Table.DataCell>
                    <AktivitetspliktUnntakTypeTag unntak={unntak.unntak} />
                  </Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(unntak.fom, '-')}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(unntak.tom, '-')}</Table.DataCell>
                  <Table.DataCell>
                    <BodyShort>{unntak.endret.ident}</BodyShort>
                    <Detail>Saksbehandler: {formaterDato(unntak.endret.tidspunkt)}</Detail>
                  </Table.DataCell>
                </Table.ExpandableRow>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={6}>Ingen unntak</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
