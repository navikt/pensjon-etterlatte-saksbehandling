import React from 'react'
import { Box, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { TagIcon } from '@navikt/aksel-icons'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useBehandling } from '~components/behandling/useBehandling'
import { BeregningsMetodeRadForAvdoed } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsMetodeRadForAvdoed'
import { IPdlPerson } from '~shared/types/Person'

interface Props {
  redigerbar: boolean
  trygdetider: ITrygdetid[]
  tidligsteAvdoede: IPdlPerson
}

export const BeregningsmetoderFlereAvdoede = ({ redigerbar, trygdetider, tidligsteAvdoede }: Props) => {
  const behandling = useBehandling()

  return (
    <VStack gap="space-4">
      <HStack gap="space-2">
        <TagIcon aria-hidden fontSize="1.5rem" />
        <Heading size="small" level="3">
          Trygdetid-metode brukt for flere avdøde
        </Heading>
      </HStack>
      <Box maxWidth="fit-content">
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell />
              <Table.HeaderCell scope="col">Forelder</Table.HeaderCell>
              <Table.HeaderCell scope="col">Trygdetid brukt i beregningen</Table.HeaderCell>
              <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
              <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
              <Table.HeaderCell />
              <Table.HeaderCell />
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {trygdetider.map((trygdetid: ITrygdetid) => (
              <BeregningsMetodeRadForAvdoed
                key={trygdetid.ident}
                behandling={behandling!!}
                redigerbar={redigerbar}
                trygdetid={trygdetid}
                erTidligsteAvdoede={tidligsteAvdoede.foedselsnummer === trygdetid.ident}
              />
            ))}
          </Table.Body>
        </Table>
      </Box>
    </VStack>
  )
}
