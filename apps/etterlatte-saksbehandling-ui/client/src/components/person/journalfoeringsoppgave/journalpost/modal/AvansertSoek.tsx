import React, { Dispatch, SetStateAction, useEffect } from 'react'
import { Button, Heading, Modal, Select, TextField } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAlleLand } from '~shared/api/trygdetid'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { soekPerson } from '~shared/api/pdltjenester'

interface ModalProps {
  open: boolean
  setOpen: Dispatch<SetStateAction<boolean>>
}

export interface SoekPerson {
  fornavn?: string
  etternavn?: string
  statsborgerskap?: string
  foedselsdato?: Date
}

export default function AvansertSoek({ open, setOpen }: ModalProps) {
  const [landListeResult, landListeFetch] = useApiCall(hentAlleLand)
  const [, soekperson] = useApiCall(soekPerson)

  useEffect(() => {
    landListeFetch(null)
  }, [])

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
    getValues,
    register,
    control,
    formState: { errors },
    handleSubmit,
    watch,
  } = useForm<SoekPerson>({ defaultValues: { statsborgerskap: undefined }, mode: 'all' })

  console.log('formstate: ', getValues())
  console.log('errors: ', errors)

  watch()
  return (
    <Modal open={open} aria-labelledby="modal-heading" onClose={() => setOpen(false)} width="medium">
      <Modal.Body>
        <Heading size="medium" id="modal-heading" spacing>
          Avansert Søk
        </Heading>
        <TextField
          {...register('fornavn', {
            validate: (fornavn) => navnErGyldig(fornavn) || 'Fornavn kan kun bestå av bokstaver',
          })}
          label="Fornavn"
          error={errors?.fornavn?.message}
        />
        <TextField
          {...register('etternavn', {
            validate: (etternavn) => navnErGyldig(etternavn) || 'Etternavn kan kun bestå av bokstaver',
          })}
          label="Etternavn"
          error={errors?.etternavn?.message}
        />
        {mapResult(landListeResult, {
          pending: <Spinner label="Hent landliste" visible />,
          error: (error) => (
            <ApiErrorAlert>
              {error.detail || 'Feil ved henting av landliste. Kan ikke søke med statsborgerskap.'}
            </ApiErrorAlert>
          ),
          success: (landListe) => (
            <Select label="Velg statsborgerskap" {...register('statsborgerskap', {})}>
              <option disabled>Velg land</option>
              {landListe.map((land) => (
                <option key={land.isoLandkode} value={land.isoLandkode}>
                  {land.beskrivelse.tekst}
                </option>
              ))}
            </Select>
          ),
        })}
        <ControlledDatoVelger name="foedselsdato" label="Fødselsdato" control={control} />
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={() => setOpen(false)}>
          Avbryt søk
        </Button>
        <Button
          variant="primary"
          onClick={() => {
            console.log('onclick')
            soekperson(getValues())
            handleSubmit(
              (person) => {
                soekperson(person)
                console.log('person: ', person)
              },
              (errors) => {
                console.log('errors:', errors)
              }
            )
          }}
        >
          Søk
        </Button>
      </Modal.Footer>
    </Modal>
  )
}
