import React, { Dispatch, ReactNode, SetStateAction, useState } from 'react'
import { Alert, BodyShort, Button, Heading, Modal, Select } from '@navikt/ds-react'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { FlexRow } from '~shared/styled'
import { isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterSpraak, tilbakestillManuellPayload } from '~shared/api/brev'
import { useForm } from 'react-hook-form'
import { DocPencilIcon } from '@navikt/aksel-icons'
import { Brevtype, Spraak } from '~shared/types/Brev'
import { formaterSpraak } from '~utils/formattering'

interface Props {
  nyttSpraak: Spraak
  settNyttSpraak: Dispatch<SetStateAction<Spraak>>
  brevId: number
  sakId: number
  behandlingId?: string
  brevtype: Brevtype
}

export const BrevSpraakModal = ({
  nyttSpraak,
  settNyttSpraak,
  brevId,
  sakId,
  behandlingId,
  brevtype,
}: Props): ReactNode => {
  const [isOpen, setIsOpen] = useState(false)
  const [oppdaterSpraakStatus, apiOppdaterSpraak] = useApiCall(oppdaterSpraak)
  const [tilbakestillBrevStatus, apiTilbakestillBrev] = useApiCall(tilbakestillManuellPayload)

  const {
    register,
    formState: { isDirty, errors },
    handleSubmit,
    reset,
  } = useForm({ defaultValues: { spraak: nyttSpraak } })

  const lagre = ({ spraak }: { spraak: Spraak }) => {
    if (!isDirty) {
      setIsOpen(false) // Ikke gjør noe hvis spraak er uendret
      return
    }

    if (spraak) {
      apiOppdaterSpraak({ brevId, sakId, spraak }, () => {
        if (!!behandlingId) {
          apiTilbakestillBrev({ brevId, sakId, behandlingId, brevtype }, () => {
            window.location.reload()
          })
        } else {
          setIsOpen(false)
          settNyttSpraak(spraak)
        }
      })
    }
  }

  const avbryt = () => {
    reset({ spraak: nyttSpraak })
    setIsOpen(false)
  }

  return (
    <div>
      <Button variant="secondary" onClick={() => setIsOpen(true)} icon={<DocPencilIcon aria-hidden />} size="small" />

      <Modal open={isOpen} onClose={avbryt} width="medium">
        <form onSubmit={handleSubmit(lagre)}>
          <Modal.Body>
            <Heading size="large" spacing>
              Endre tittel
            </Heading>

            <Select
              {...register('spraak', {
                required: {
                  value: true,
                  message: 'Du må velge ',
                },
              })}
              label="Endre språk/målform"
              error={errors.spraak?.message}
              defaultValue={nyttSpraak}
            >
              {Object.values(Spraak).map((spraak) => (
                <option key={spraak} value={spraak}>
                  {formaterSpraak(spraak)}
                </option>
              ))}
            </Select>

            <br />

            {!!behandlingId && (
              <BodyShort as="div" spacing>
                <Alert variant="warning" inline>
                  Endring av språk på vedtaksbrev vil medføre at brevet tilbakestilles og informasjon hentes på nytt.
                  Dette vil slette tidligere endringer, så husk å kopiere tekst du vil beholde før du fortsetter.
                </Alert>
              </BodyShort>
            )}

            {isFailureHandler({
              apiResult: oppdaterSpraakStatus,
              errorMessage: 'Kunne ikke oppdatere tittel',
            })}
            {isFailureHandler({
              apiResult: tilbakestillBrevStatus,
              errorMessage:
                'Kunne ikke tilbakestille brev med nytt språk automatisk. Forsøk å tilbakestille brevet manuelt',
            })}
          </Modal.Body>

          <Modal.Footer>
            <FlexRow justify="right">
              <Button
                variant="secondary"
                type="button"
                disabled={isPending(oppdaterSpraakStatus) || isPending(tilbakestillBrevStatus)}
                onClick={avbryt}
              >
                Avbryt
              </Button>
              <Button
                variant="primary"
                type="submit"
                loading={isPending(oppdaterSpraakStatus) || isPending(tilbakestillBrevStatus)}
                disabled={!isDirty}
              >
                Lagre
              </Button>
            </FlexRow>
          </Modal.Footer>
        </form>
      </Modal>
    </div>
  )
}
