import { HeartIcon } from '@navikt/aksel-icons'
import { Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { Familieforhold, IPdlPerson } from '~shared/types/Person'
import { format } from 'date-fns'
import styled from 'styled-components'
import { IconSize } from '~shared/types/Icon'
import { DatoFormat, formaterDato } from '~utils/formattering'

type Props = {
  familieforhold: Familieforhold
  avdoed: IPdlPerson
}

export const Sivilstand = ({ familieforhold, avdoed }: Props) => {
  const sivilstand = familieforhold.soeker?.opplysning.sivilstand?.filter((ss) => !ss.historisk)

  return (
    <VStack gap="2">
      <HStack gap="2">
        <HeartIcon fontSize={IconSize.DEFAULT} />
        <Heading size="small" level="3">
          Sivilstand (gjenlevende)
        </Heading>
      </HStack>
      <SivilstandTable size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Status</Table.HeaderCell>
            <Table.HeaderCell scope="col">Dato</Table.HeaderCell>
            <Table.HeaderCell scope="col">Partner</Table.HeaderCell>
            <Table.HeaderCell scope="col">Dødsdato</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {sivilstand ? (
            sivilstand.map(
              (ss, index) =>
                ss && (
                  <Table.Row key={index}>
                    <Table.DataCell>{ss.sivilstatus}</Table.DataCell>
                    <Table.DataCell>
                      {ss.gyldigFraOgMed
                        ? format(new Date(ss.gyldigFraOgMed), DatoFormat.DAG_MAANED_AAR)
                        : ' Mangler dato'}
                    </Table.DataCell>
                    <Table.DataCell>
                      {ss.relatertVedSiviltilstand === avdoed.foedselsnummer
                        ? `${avdoed.fornavn} ${avdoed.etternavn}`
                        : 'Annen person'}
                    </Table.DataCell>
                    <Table.DataCell>
                      {ss.relatertVedSiviltilstand === avdoed.foedselsnummer ? formaterDato(avdoed.doedsdato!) : ''}
                    </Table.DataCell>
                  </Table.Row>
                )
            )
          ) : (
            <Table.Row>
              <Table.DataCell aria-colspan={4} colSpan={4} align="center">
                Mangler data om sivilstand
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </SivilstandTable>
    </VStack>
  )
}

const SivilstandTable = styled(Table)`
  width: 67%;
`
