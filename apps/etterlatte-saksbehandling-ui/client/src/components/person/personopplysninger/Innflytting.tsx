import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { AirplaneIcon } from '@navikt/aksel-icons'
import { Heading, ReadMore, Table, VStack } from '@navikt/ds-react'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import { ILand } from '~utils/kodeverk'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { InnflyttingDTO } from '~shared/types/Person'

export const Innflytting = ({
  innflytting,
  landListe,
}: {
  innflytting?: InnflyttingDTO[]
  landListe: ILand[]
}): ReactNode => {
  return (
    <Personopplysning heading="Innflytting" icon={<AirplaneIcon />}>
      <VStack gap="4">
        <ReadMore header="Gyldighetsdato">
          Gyldighetsdato kommer fra folkeregisteret og har ikke nødvendigvis sammenheng med når innflytting faktisk
          skjedde.
          <br />
          <br />
          Dersom man skal finne ut om sen preson regnes som innflyttet i hendhold til folkeregisterlover, så kan man se
          på om personen har en norsk bostedsadresse med angitt flyttedato.
        </ReadMore>
        <ReadMore header="Ajourholdsdato">
          Datoen opplysningen ble opprettet i Folkeregisteret. Feltet mangler på en del opplysninger migrert fra gammelt
          registert.
        </ReadMore>
      </VStack>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Innflyttet fra</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Dato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Gyldighetsdato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Ajourholdsdato</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!innflytting?.length ? (
            <>
              {innflytting.map((flytting: InnflyttingDTO, index) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    {!!flytting.fraflyttingsland && finnLandSomTekst(flytting.fraflyttingsland, landListe)}
                  </Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(flytting.dato)}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(flytting.gyldighetstidspunkt)}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(flytting.ajourholdstidspunkt)}</Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={2}>
                <Heading size="small">Ingen innflyttinger</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
