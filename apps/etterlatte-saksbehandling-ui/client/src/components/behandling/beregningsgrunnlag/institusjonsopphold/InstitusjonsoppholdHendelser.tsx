import React, { useEffect } from 'react'
import { Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { HospitalIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGrunnlagsendringshendelserInstitusjonsoppholdForSak } from '~shared/api/behandling'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Grunnlagsendringshendelse, InstitusjonsoppholdSamsvar } from '~components/person/typer'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'
import { institusjonstype } from '~shared/types/Institusjonsopphold'

export const InstitusjonsoppholdHendelser = ({ sakId }: { sakId: number }) => {
  const [institusjonsHendelserResult, institusjonsHendelserRequest] = useApiCall(
    hentGrunnlagsendringshendelserInstitusjonsoppholdForSak
  )

  useEffect(() => {
    institusjonsHendelserRequest(sakId)
  }, [])

  return (
    <VStack gap="space-4">
      <HStack gap="space-2" align="center">
        <HospitalIcon aria-hidden fontSize="1.5rem" />
        <Heading size="small" level="3">
          Hendelser registrert i inst2
        </Heading>
      </HStack>
      <VStack gap="space-2">
        {mapResult(institusjonsHendelserResult, {
          pending: <Spinner label="Henter hendelser for institusjonsopphold..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente hendelser'}</ApiErrorAlert>,
          success: (hendelser) => (
            <Table size="small">
              <Table.Header>
                <Table.Row>
                  <Table.HeaderCell scope="col">Inn</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Ut</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Varighet</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Type</Table.HeaderCell>
                  <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {!!hendelser?.length ? (
                  hendelser.map((hendelse: Grunnlagsendringshendelse) => {
                    const inst = hendelse.samsvarMellomKildeOgGrunnlag as InstitusjonsoppholdSamsvar
                    return (
                      <Table.Row key={hendelse.id}>
                        <Table.DataCell>
                          {inst.oppholdBeriket.startdato && formaterDatoMedFallback(inst.oppholdBeriket.startdato, '-')}
                        </Table.DataCell>
                        <Table.DataCell>
                          {inst.oppholdBeriket.faktiskSluttdato &&
                            formaterDatoMedFallback(inst.oppholdBeriket.faktiskSluttdato, '-')}
                        </Table.DataCell>
                        <Table.DataCell>
                          {!!inst.oppholdBeriket.forventetSluttdato
                            ? formaterDato(inst.oppholdBeriket.forventetSluttdato)
                            : 'Ingen forventet sluttdato'}
                        </Table.DataCell>
                        <Table.DataCell>
                          {inst.oppholdBeriket.institusjonsType
                            ? institusjonstype[inst.oppholdBeriket.institusjonsType]
                            : `Ukjent type ${inst.oppholdBeriket.institusjonsType}`}
                        </Table.DataCell>
                        <Table.DataCell>
                          {inst.oppholdBeriket.institusjonsnavn ? inst.oppholdBeriket.institusjonsnavn : '-'}
                        </Table.DataCell>
                      </Table.Row>
                    )
                  })
                ) : (
                  <Table.Row>
                    <Table.DataCell colSpan={5}>Ingen hendelser fra inst2</Table.DataCell>
                  </Table.Row>
                )}
              </Table.Body>
            </Table>
          ),
        })}
      </VStack>
    </VStack>
  )
}
