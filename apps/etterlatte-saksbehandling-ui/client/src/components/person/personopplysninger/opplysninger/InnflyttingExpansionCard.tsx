import { InnflyttingDTO } from '~shared/types/Person'
import { ILand } from '~utils/kodeverk'
import { ExpansionCard, Heading, HStack, Table } from '@navikt/ds-react'
import { AirplaneIcon } from '@navikt/aksel-icons'
import React from 'react'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

interface Props {
  innflytting?: InnflyttingDTO[]
  alleLand: ILand[]
  erAvdoedesInnflytting?: boolean
}

export const InnflyttingExpansionCard = ({ innflytting, alleLand, erAvdoedesInnflytting = false }: Props) => {
  return (
    <ExpansionCard aria-labelledby="Innflytting" size="small" defaultOpen>
      <ExpansionCard.Header>
        <HStack gap="space-4" align="center">
          <AirplaneIcon aria-hidden fontSize="1.5rem" />
          <ExpansionCard.Title size="small">
            {erAvdoedesInnflytting ? 'Avdødes innflytting' : 'Innflytting'}
          </ExpansionCard.Title>
        </HStack>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <Table size="small">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Innflyttet fra</Table.HeaderCell>
              <Table.HeaderCell scope="col">Dato</Table.HeaderCell>
              <Table.HeaderCell scope="col">Gyldighetsdato</Table.HeaderCell>
              <Table.HeaderCell scope="col">Ajourholdsdato</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {!!innflytting?.length ? (
              innflytting.map((flytting, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    {!!flytting.fraflyttingsland ? finnLandSomTekst(flytting.fraflyttingsland, alleLand) : ''}
                  </Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(flytting.dato)}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(flytting.gyldighetsdato)}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(flytting.ajourholdsdato)}</Table.DataCell>
                </Table.Row>
              ))
            ) : (
              <Table.Row>
                <Table.DataCell colSpan={4}>
                  <Heading size="small">Ingen innflyttinger</Heading>
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>
      </ExpansionCard.Content>
    </ExpansionCard>
  )
}
