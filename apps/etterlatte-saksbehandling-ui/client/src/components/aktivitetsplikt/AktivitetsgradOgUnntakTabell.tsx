import {
  AktivitetspliktUnntakType,
  IAktivitetspliktAktivitetsgrad,
  IAktivitetspliktUnntak,
  tekstAktivitetspliktUnntakType,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import { BodyShort, Box, Detail, Heading, HStack, ReadMore, Table, Tag, VStack } from '@navikt/ds-react'
import React from 'react'
import { BriefcaseIcon } from '@navikt/aksel-icons'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'

export function erAktivitetsgrad(
  unntakEllerAktivitetsgrad: IAktivitetspliktUnntak | IAktivitetspliktAktivitetsgrad
): unntakEllerAktivitetsgrad is IAktivitetspliktAktivitetsgrad {
  return !!(unntakEllerAktivitetsgrad as IAktivitetspliktAktivitetsgrad).aktivitetsgrad
}

function TagForAktivitetEllerUnntak(props: {
  aktivitetEllerUnntak: IAktivitetspliktUnntak | IAktivitetspliktAktivitetsgrad
}) {
  const { aktivitetEllerUnntak } = props
  if (erAktivitetsgrad(aktivitetEllerUnntak)) {
    return <Tag variant="alt3">Aktivitetsgrad</Tag>
  }
  if (aktivitetEllerUnntak.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT) {
    return <Tag variant="alt1">Varig unntak</Tag>
  }
  return <Tag variant="alt2">Unntak</Tag>
}

export function AktivitetsgradOgUnntakTabell({
  aktiviteter,
  unntak,
  utvidetVisning,
}: {
  aktiviteter: IAktivitetspliktAktivitetsgrad[]
  unntak: IAktivitetspliktUnntak[]
  utvidetVisning: (aktivitetEllerUnntak: IAktivitetspliktAktivitetsgrad | IAktivitetspliktUnntak) => React.ReactNode
}) {
  const aktiviteterOgUnntak = [...aktiviteter, ...unntak]
  aktiviteterOgUnntak.sort((a, b) => new Date(a.fom).getUTCMilliseconds() - new Date(b.fom).getUTCMilliseconds())

  return (
    <VStack gap="space-4">
      <HStack gap="space-4" align="center">
        <BriefcaseIcon fontSize="1.5rem" aria-hidden />
        <Heading size="small">Aktivitetsgrad og unntak</Heading>
      </HStack>
      <VStack>
        <Box maxWidth="42.5rem">
          <ReadMore header="Dette menes med aktivitetsgrad og unntak">
            I tabellen kan du se hvilken aktivitetsgrad brukeren har hatt. For å motta omstillingsstønad stilles det
            ingen krav til aktivitet de første seks månedene etter dødsfall. Etter seks måneder forventes det at du er i
            minst 50 % aktivitet, og etter ett år og fremover kan man skjønnsmessig kreve opp til 100 % aktivitet. Vær
            oppmerksom på at det finnes unntak. I tabellen ser du hvilke unntak som er satt på den gjenlevende, hvis det
            er satt noen.
          </ReadMore>
        </Box>
      </VStack>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Type</Table.HeaderCell>
            <Table.HeaderCell scope="col">Detalj</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Kilde</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {aktiviteterOgUnntak.length ? (
            <>
              {aktiviteterOgUnntak.map((aktivitetEllerUnntak) => {
                return (
                  <Table.ExpandableRow key={aktivitetEllerUnntak.id} content={utvidetVisning(aktivitetEllerUnntak)}>
                    <Table.DataCell>
                      <TagForAktivitetEllerUnntak aktivitetEllerUnntak={aktivitetEllerUnntak} />
                    </Table.DataCell>
                    <Table.DataCell>
                      {erAktivitetsgrad(aktivitetEllerUnntak)
                        ? tekstAktivitetspliktVurderingType[aktivitetEllerUnntak.aktivitetsgrad]
                        : tekstAktivitetspliktUnntakType[aktivitetEllerUnntak.unntak]}
                    </Table.DataCell>
                    <Table.DataCell>{formaterDatoMedFallback(aktivitetEllerUnntak.fom, '-')}</Table.DataCell>
                    <Table.DataCell>{formaterDatoMedFallback(aktivitetEllerUnntak.tom, '-')}</Table.DataCell>
                    <Table.DataCell>
                      <BodyShort>{aktivitetEllerUnntak.endret.ident}</BodyShort>
                      <Detail>Saksbehandler: {formaterDato(aktivitetEllerUnntak.endret.tidspunkt)}</Detail>
                    </Table.DataCell>
                  </Table.ExpandableRow>
                )
              })}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={6}>Ingen aktivitetsgrad eller unntak</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
