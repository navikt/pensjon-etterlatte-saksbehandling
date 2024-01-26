import React, { Dispatch, ReactNode, SetStateAction } from 'react'
import { Button, Heading, Modal, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { FlexRow } from '~shared/styled'
import { isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterTittel } from '~shared/api/brev'
import { useForm } from 'react-hook-form'

interface Props {
  isOpen: boolean
  setIsOpen: Dispatch<SetStateAction<boolean>>
  nyTittel: string
  setNyTittel: Dispatch<SetStateAction<string>>
  brevId: number
  sakId: number
}

export const BrevTittelModal = ({ isOpen, setIsOpen, nyTittel, setNyTittel, brevId, sakId }: Props): ReactNode => {
  const [status, apiOppdaterTittel] = useApiCall(oppdaterTittel)

  const {
    register,
    formState: { isDirty, errors },
    handleSubmit,
    reset,
  } = useForm({ defaultValues: { tittel: nyTittel } })

  const lagre = ({ tittel }: { tittel: string | undefined }) => {
    if (!isDirty) {
      setIsOpen(false) // Ikke gjør noe hvis tittel er uendret
      return
    }

    if (tittel) {
      apiOppdaterTittel({ brevId, sakId, tittel }, () => {
        setIsOpen(false)
        setNyTittel(tittel)
      })
    }
  }

  const avbryt = () => {
    reset({ tittel: nyTittel })
    setIsOpen(false)
  }

  return (
    <form onSubmit={handleSubmit((data) => lagre(data))}>
      <TittelModal open={isOpen} onClose={avbryt}>
        <Modal.Body>
          <Heading size="large" spacing>
            Endre tittel
          </Heading>

          <TextField
            {...register('tittel', {
              required: {
                value: true,
                message: 'Du må velge en tittel',
              },
            })}
            label="Ny tittel"
            error={errors.tittel?.message}
          />
          {isFailureHandler({
            apiResult: status,
            errorMessage: 'Kunne ikke oppdatere tittel',
          })}
        </Modal.Body>

        <Modal.Footer>
          <FlexRow justify="right">
            <Button variant="secondary" disabled={isPending(status)} onClick={avbryt}>
              Avbryt
            </Button>
            <Button variant="primary" type="submit" loading={isPending(status)}>
              Lagre
            </Button>
          </FlexRow>
        </Modal.Footer>
      </TittelModal>
    </form>
  )
}

const TittelModal = styled(Modal)`
  width: 40rem;
  padding: 3rem;
`
