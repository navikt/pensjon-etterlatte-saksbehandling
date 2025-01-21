import {
  AktivitetspliktOppgaveVurderingType,
  IAktivitetspliktAktivitetsgrad,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import { BodyShort, Box, Detail, ReadMore, Table, VStack } from '@navikt/ds-react'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'
import React from 'react'
import { RedigerbarAktivitetsgradBehandling } from '~components/behandling/aktivitetsplikt/aktivitetsgrad/RedigerbarAktivitetsgradBehandling'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export function AktivitetsgradTabellBehandling({
  aktiviteter,
  behandling,
  typeVurdering,
}: {
  aktiviteter: IAktivitetspliktAktivitetsgrad[]
  behandling: IDetaljertBehandling
  typeVurdering: AktivitetspliktOppgaveVurderingType
}) {
  return (
    <VStack gap="4">
      <Box maxWidth="42.5rem">
        <ReadMore header="Dette menes med aktivitetsgrad">
          I oversikten over aktivitetsgrad kan du se hvilken aktivitetsgrad brukeren har hatt. For å motta
          omstillingsstønad stilles det ingen krav til aktivitet de første seks månedene etter dødsfall. Etter seks
          måneder forventes det at du er i minst 50 % aktivitet, og etter ett år og fremover kan man skjønnsmessig kreve
          opp til 100 % aktivitet. Vær oppmerksom på at det finnes unntak.
        </ReadMore>
      </Box>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Aktivitetsgrad</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Kilde</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {aktiviteter?.length ? (
            <>
              {aktiviteter.map((aktivitet) => (
                <Table.ExpandableRow
                  key={aktivitet.id}
                  content={
                    <RedigerbarAktivitetsgradBehandling
                      aktivitet={aktivitet}
                      behandling={behandling}
                      typeVurdering={typeVurdering}
                    />
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
