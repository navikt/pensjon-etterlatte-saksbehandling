import React, { useState } from 'react'
import { Button, Heading, HStack, Modal, Table, TextField, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { soekPerson } from '~shared/api/pdltjenester'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Adressevisning } from '~components/behandling/felles/Adressevisning'
import { formaterKanskjeNavn } from '~components/behandling/soeknadsoversikt/familieforhold/SamsvarPersongalleri'
import { BrukerIdType, SoekPersonValg } from '~shared/types/Journalpost'
import { MagnifyingGlassIcon } from '@navikt/aksel-icons'
import { IPdlPersonSoekResponse } from '~shared/types/Person'

export interface PersonSoekCriteria {
  navn?: string
  foedselsdato?: Date
}

export const PersonSoekModal = ({ velgPerson }: { velgPerson: (bruker: SoekPersonValg) => void }) => {
  const [isOpen, setIsOpen] = useState(false)

  const [personSoekResult, soekperson] = useApiCall(soekPerson)

  const navnErGyldig = (navn: string | undefined): boolean => {
    if (navn) {
      const utenMellomrom = navn.trim()
      const spesialtegn = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]+/
      return utenMellomrom.match(/\d+/) == null && utenMellomrom.match(spesialtegn) == null
    } else {
      return false
    }
  }

  const personValgt = (person: IPdlPersonSoekResponse) => {
    setIsOpen(false)
    velgPerson({
      id: person.foedselsnummer,
      type: BrukerIdType.FNR,
      navn: formaterKanskjeNavn(person),
    })
  }

  const {
    register,
    control,
    formState: { errors },
    handleSubmit,
  } = useForm<PersonSoekCriteria>()

  return (
    <div>
      <Button
        icon={<MagnifyingGlassIcon aria-hidden />}
        iconPosition="right"
        variant="secondary"
        onClick={() => setIsOpen(true)}
        size="small"
      >
        Søk etter person
      </Button>

      <Modal open={isOpen} aria-labelledby="modal-heading" onClose={() => setIsOpen(false)} width="medium">
        <Modal.Header>
          <Heading size="medium" id="modal-heading" spacing>
            Søk etter person
          </Heading>
        </Modal.Header>

        <Modal.Body>
          <VStack gap="space-8">
            <TextField
              {...register('navn', {
                required: { value: true, message: 'Navn er påkrevd i søket' },
                validate: (fornavn) => navnErGyldig(fornavn) || 'Fornavn kan kun bestå av bokstaver',
              })}
              label="Navn"
              error={errors?.navn?.message}
            />
            <ControlledDatoVelger name="foedselsdato" label="Fødselsdato" control={control} />

            {mapResult(personSoekResult, {
              pending: <Spinner label="Søker etter personer" />,
              success: (personer) => {
                return (
                  <Table className="table" zebraStripes>
                    <Table.Header>
                      <Table.Row>
                        <Table.HeaderCell>Navn</Table.HeaderCell>
                        <Table.HeaderCell>Personidentifikator</Table.HeaderCell>
                        <Table.HeaderCell>Adresse</Table.HeaderCell>
                        <Table.HeaderCell></Table.HeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {personer.map((person, i) => (
                        <Table.Row key={i + person.foedselsnummer}>
                          <Table.HeaderCell>{formaterKanskjeNavn(person)}</Table.HeaderCell>
                          <Table.DataCell>{person.foedselsnummer}</Table.DataCell>
                          <Table.DataCell>
                            {person.bostedsadresse ? (
                              <Adressevisning adresser={person.bostedsadresse} />
                            ) : (
                              <p>Ingen adresser</p>
                            )}
                          </Table.DataCell>
                          <Table.DataCell>
                            <Button size="small" onClick={() => personValgt(person)}>
                              Velg
                            </Button>
                          </Table.DataCell>
                        </Table.Row>
                      ))}
                    </Table.Body>
                  </Table>
                )
              },
              error: (error) => {
                return <ApiErrorAlert>{error.detail || 'Fant ingen personer'}</ApiErrorAlert>
              },
            })}
          </VStack>
        </Modal.Body>

        <Modal.Footer>
          <HStack gap="space-4">
            <Button variant="tertiary" onClick={() => setIsOpen(false)}>
              Avbryt
            </Button>

            <Button type="submit" variant="primary" onClick={handleSubmit((person) => soekperson(person))}>
              Søk
            </Button>
          </HStack>
        </Modal.Footer>
      </Modal>
    </div>
  )
}
