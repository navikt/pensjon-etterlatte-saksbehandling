import React, { Dispatch, ReactNode, SetStateAction, useState } from 'react'
import { Button, Heading, HStack, Modal, TextField } from '@navikt/ds-react'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterTittel } from '~shared/api/brev'
import { useForm } from 'react-hook-form'
import { DocPencilIcon } from '@navikt/aksel-icons'

interface Props {
  nyTittel: string
  setNyTittel: Dispatch<SetStateAction<string>>
  brevId: number
  sakId: number
}

export const BrevTittelModal = ({ nyTittel, setNyTittel, brevId, sakId }: Props): ReactNode => {
  const [isOpen, setIsOpen] = useState(false)
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
    <div>
      <Button
        variant="secondary"
        onClick={() => setIsOpen(true)}
        icon={<DocPencilIcon title="Endre tittel" />}
        size="small"
      />

      <Modal open={isOpen} onClose={avbryt} width="medium" aria-label="Endre tittel">
        <form onSubmit={handleSubmit((data) => lagre(data))}>
          <Modal.Body>
            <Heading size="large" spacing>
              Endre tittel
            </Heading>

            <TextField
              {...register('tittel', {
                required: {
                  value: true,
                  message: 'Du må sette en tittel',
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
            <HStack gap="space-4" justify="end">
              <Button variant="secondary" type="button" disabled={isPending(status)} onClick={avbryt}>
                Avbryt
              </Button>
              <Button variant="primary" type="submit" loading={isPending(status)}>
                Lagre
              </Button>
            </HStack>
          </Modal.Footer>
        </form>
      </Modal>
    </div>
  )
}
