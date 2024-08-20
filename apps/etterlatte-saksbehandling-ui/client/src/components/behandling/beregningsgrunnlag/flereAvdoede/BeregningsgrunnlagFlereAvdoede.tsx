import React from 'react'
import { Box, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { TagIcon } from '@navikt/aksel-icons'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useBehandling } from '~components/behandling/useBehandling'
import { BeregningsMetodeRadForAvdoed } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsMetodeRadForAvdoed'

interface Props {
  redigerbar: boolean
  trygdetider: ITrygdetid[]
}

export const BeregningsgrunnlagFlereAvdoede = ({ redigerbar, trygdetider }: Props) => {
  const behandling = useBehandling()

  return (
    <VStack gap="4">
      <HStack gap="2">
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
              />
            ))}
          </Table.Body>
        </Table>
      </Box>
    </VStack>
  )
}
