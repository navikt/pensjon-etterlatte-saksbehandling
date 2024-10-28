import { HeartIcon } from '@navikt/aksel-icons'
import { Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { Familieforhold, IPdlPerson } from '~shared/types/Person'
import { IconSize } from '~shared/types/Icon'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

type Props = {
  familieforhold: Familieforhold
  avdoed: IPdlPerson
}

export const Sivilstand = ({ familieforhold, avdoed }: Props) => {
  const sivilstand = familieforhold.soeker?.opplysning.sivilstand?.filter((ss) => !ss.historisk)

  return (
    <VStack gap="2" width="67%">
      <HStack gap="2" align="center" marginBlock="0 2">
        <HeartIcon fontSize={IconSize.DEFAULT} />
        <Heading size="small" level="3">
          Sivilstand (gjenlevende)
        </Heading>
      </HStack>

      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Status</Table.HeaderCell>
            <Table.HeaderCell scope="col">Dato</Table.HeaderCell>
            <Table.HeaderCell scope="col">Partner</Table.HeaderCell>
            <Table.HeaderCell scope="col">Dødsdato</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!sivilstand ? (
            sivilstand.map(
              (ss, index) =>
                ss && (
                  <Table.Row key={index}>
                    <Table.DataCell>{ss.sivilstatus}</Table.DataCell>
                    <Table.DataCell>{formaterDatoMedFallback(ss.gyldigFraOgMed, 'Mangler dato')}</Table.DataCell>
                    <Table.DataCell>
                      {ss.relatertVedSiviltilstand === avdoed.foedselsnummer
                        ? `${avdoed.fornavn} ${avdoed.etternavn}`
                        : 'Annen person'}
                    </Table.DataCell>
                    <Table.DataCell>
                      {ss.relatertVedSiviltilstand === avdoed.foedselsnummer
                        ? formaterDatoMedFallback(avdoed.doedsdato, 'Mangler dato')
                        : ''}
                    </Table.DataCell>
                  </Table.Row>
                )
            )
          ) : (
            <Table.Row>
              <Table.DataCell aria-colspan={4} colSpan={4}>
                Mangler data om sivilstand
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
