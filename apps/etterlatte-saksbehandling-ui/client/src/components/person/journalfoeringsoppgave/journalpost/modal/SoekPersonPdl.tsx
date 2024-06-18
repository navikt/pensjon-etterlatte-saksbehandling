import React, { Dispatch, SetStateAction } from 'react'
import { Button, Heading, Modal, Table, TextField } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { soekPerson } from '~shared/api/pdltjenester'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Adressevisning } from '~components/behandling/felles/Adressevisning'
import { formaterKanskjeNavn } from '~components/behandling/soeknadsoversikt/familieforhold/SamsvarPersongalleri'
import { BrukerIdType, SoekPersonVelg } from '~shared/types/Journalpost'

interface SoekPersonProps {
  open: boolean
  setOpen: Dispatch<SetStateAction<boolean>>
  oppdaterBruker: (person: SoekPersonVelg) => void
}

export interface SoekPerson {
  navn?: string
  foedselsdato?: Date
}

export default function SoekPersonPdl({ open, setOpen, oppdaterBruker }: SoekPersonProps) {
  const [personSoekResult, soekperson] = useApiCall(soekPerson)

  const navnErGyldig = (navn: string | undefined): boolean => {
    if (navn) {
      const utenMellomrom = navn.trim()
      const spesicalCharacters = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]+/
      return utenMellomrom.match(/\d+/) == null && utenMellomrom.match(spesicalCharacters) == null
    } else {
      return false
    }
  }

  const {
    register,
    control,
    formState: { errors },
    handleSubmit,
    watch,
  } = useForm<SoekPerson>({ defaultValues: {}, mode: 'all' })

  watch()

  return (
    <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)} width="medium">
      <Modal.Body>
        <Heading size="medium" id="modal-heading" spacing>
          Avansert Søk
        </Heading>
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
          pending: <Spinner visible={true} label="Søker etter personer" />,
          success: (personer) => {
            return (
              <Table className="table" zebraStripes>
                <Table.Header>
                  <Table.Row>
                    <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Personidentifikator</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Adresse</Table.HeaderCell>
                    <Table.HeaderCell scope="col"></Table.HeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {personer.map((person, i) => {
                    return (
                      <Table.Row key={i + person.foedselsnummer}>
                        <Table.HeaderCell scope="row">{formaterKanskjeNavn(person)}</Table.HeaderCell>
                        <Table.DataCell scope="row">{person.foedselsnummer}</Table.DataCell>
                        <Table.DataCell scope="row">
                          {person.bostedsadresse ? (
                            <Adressevisning adresser={person.bostedsadresse} />
                          ) : (
                            <p>Ingen adresser</p>
                          )}
                        </Table.DataCell>
                        <Table.DataCell scope="row">
                          <Button
                            onClick={() => {
                              setOpen(!open)
                              oppdaterBruker({
                                id: person.foedselsnummer,
                                type: BrukerIdType.FNR,
                                navn: formaterKanskjeNavn(person),
                              })
                            }}
                          >
                            Velg
                          </Button>
                        </Table.DataCell>
                      </Table.Row>
                    )
                  })}
                </Table.Body>
              </Table>
            )
          },
          error: (error) => {
            return <ApiErrorAlert>{error.detail || 'Fant ingen personer'}</ApiErrorAlert>
          },
        })}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={() => setOpen(false)}>
          Avbryt søk
        </Button>
        <Button type="submit" variant="primary" onClick={handleSubmit((person) => soekperson(person))}>
          Søk
        </Button>
      </Modal.Footer>
    </Modal>
  )
}
