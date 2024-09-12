import React from 'react'
import { Box, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { TagIcon } from '@navikt/aksel-icons'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useBehandling } from '~components/behandling/useBehandling'
import { BeregningsMetodeRadForAvdoed } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsMetodeRadForAvdoed'
import { IPdlPerson } from '~shared/types/Person'
import { AnnenForelderVurdering } from '~shared/types/grunnlag'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

interface Props {
  redigerbar: boolean
  trygdetider: ITrygdetid[]
  tidligsteAvdoede: IPdlPerson
}

export const BeregningsgrunnlagFlereAvdoede = ({ redigerbar, trygdetider, tidligsteAvdoede }: Props) => {
  const behandling = useBehandling()
  const personopplysninger = usePersonopplysninger()
  const kunEnJuridiskForelder =
    personopplysninger?.annenForelder?.vurdering === AnnenForelderVurdering.KUN_EN_REGISTRERT_JURIDISK_FORELDER

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
                erEnesteJuridiskeForelder={kunEnJuridiskForelder && tidligsteAvdoede.foedselsnummer === trygdetid.ident}
              />
            ))}
          </Table.Body>
        </Table>
      </Box>
    </VStack>
  )
}
