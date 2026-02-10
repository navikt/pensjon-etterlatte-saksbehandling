import { PdlStatsborgerskap } from '~shared/types/familieOpplysninger'
import { ILand } from '~utils/kodeverk'
import { ExpansionCard, Heading, HStack, Table } from '@navikt/ds-react'
import { PassportIcon } from '@navikt/aksel-icons'
import React from 'react'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

interface Props {
  statsborgerskap?: string[]
  pdlStatsborgerskap?: PdlStatsborgerskap[]
  alleLand: ILand[]
  erAvdoedesStatsborgerskap?: boolean
}

export const StatsborgerskapExpansionCard = ({
  statsborgerskap,
  pdlStatsborgerskap,
  alleLand,
  erAvdoedesStatsborgerskap = false,
}: Props) => {
  return (
    <ExpansionCard aria-labelledby="Statsborgerskap" size="small" defaultOpen>
      <ExpansionCard.Header>
        <HStack gap="space-4" align="center">
          <PassportIcon aria-hidden fontSize="1.5rem" />
          <ExpansionCard.Title size="small">
            {erAvdoedesStatsborgerskap ? 'Avdødes statsborgerskap' : 'Statsborgerskap'}
          </ExpansionCard.Title>
        </HStack>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <Table size="small">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Land</Table.HeaderCell>
              <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
              <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {!!pdlStatsborgerskap?.length ? (
              pdlStatsborgerskap.map((borgerskap, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>{finnLandSomTekst(borgerskap.land, alleLand)}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(borgerskap.gyldigFraOgMed)}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(borgerskap.gyldigTilOgMed)}</Table.DataCell>
                </Table.Row>
              ))
            ) : !!statsborgerskap?.length ? (
              statsborgerskap.map((borgerskap, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>{finnLandSomTekst(borgerskap, alleLand)}</Table.DataCell>
                  <Table.DataCell>-</Table.DataCell>
                  <Table.DataCell>-</Table.DataCell>
                </Table.Row>
              ))
            ) : (
              <Table.Row>
                <Table.DataCell colSpan={3}>
                  <Heading size="small">Ingen statsborgerskap</Heading>
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>
      </ExpansionCard.Content>
    </ExpansionCard>
  )
}
