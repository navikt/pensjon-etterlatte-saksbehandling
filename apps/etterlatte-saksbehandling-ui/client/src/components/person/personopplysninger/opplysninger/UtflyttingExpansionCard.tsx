import { UtflyttingDTO } from '~shared/types/Person'
import { ILand } from '~utils/kodeverk'
import { ExpansionCard, Heading, HStack, Table } from '@navikt/ds-react'
import { AirplaneIcon } from '@navikt/aksel-icons'
import React from 'react'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

interface Props {
  utflytting?: UtflyttingDTO[]
  alleLand: ILand[]
  erAvdoedesUtflytting?: boolean
}

export const UtflyttingExpansionCard = ({ utflytting, alleLand, erAvdoedesUtflytting = false }: Props) => {
  return (
    <ExpansionCard aria-labelledby="Utflytting" size="small" defaultOpen>
      <ExpansionCard.Header>
        <HStack gap="space-4" align="center">
          <AirplaneIcon aria-hidden fontSize="1.5rem" />
          <ExpansionCard.Title size="small">
            {erAvdoedesUtflytting ? 'Avdødes utflytting' : 'Utflytting'}
          </ExpansionCard.Title>
        </HStack>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <Table size="small">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Utflyttet fra</Table.HeaderCell>
              <Table.HeaderCell scope="col">Dato</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {!!utflytting?.length ? (
              utflytting.map((flytting, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    {!!flytting.tilflyttingsland && finnLandSomTekst(flytting.tilflyttingsland, alleLand)}
                  </Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(flytting.dato)}</Table.DataCell>
                </Table.Row>
              ))
            ) : (
              <Table.Row>
                <Table.DataCell colSpan={2}>
                  <Heading size="small">Ingen utflyttninger</Heading>
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>
      </ExpansionCard.Content>
    </ExpansionCard>
  )
}
