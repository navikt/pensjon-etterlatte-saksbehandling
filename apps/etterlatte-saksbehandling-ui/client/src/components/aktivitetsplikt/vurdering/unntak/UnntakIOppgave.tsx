import React from 'react'
import { tekstAktivitetspliktUnntakType } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Box, Detail, ReadMore, Table, VStack } from '@navikt/ds-react'
import { AktivitetspliktUnntakTypeTag } from '~shared/tags/AktivitetspliktUnntakTypeTag'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { VisUnntak } from '~components/aktivitetsplikt/vurdering/unntak/VisUnntak'

export function UnntakIOppgave() {
  const { vurdering } = useAktivitetspliktOppgaveVurdering()
  const unntaker = vurdering.unntak

  return (
    <VStack gap="4">
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
          {!!unntaker?.length ? (
            <>
              {unntaker.map((unntak) => (
                <Table.ExpandableRow key={unntak.id} content={<VisUnntak unntak={unntak} />}>
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

      <Box maxWidth="42.5rem">
        <ReadMore header="Dette menes med unntak">
          I oversikten over unntak ser du hvilke unntak som er satt på den gjenlevende. Det finnes både midlertidige og
          varige unntak
        </ReadMore>
      </Box>
    </VStack>
  )
}
