import React, { useState } from 'react'
import { Button, ExpansionCard, Heading, Modal, Select, Textarea } from '@navikt/ds-react'
import { useNavigate } from 'react-router'
import { useApiCall } from '~shared/hooks/useApiCall'
import { TrashIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { avbrytKlage } from '~shared/api/klage'
import { useKlage, useKlageRedigerbar } from '~components/klage/useKlage'
import { Controller, useForm } from 'react-hook-form'
import { AarsakTilAvbrytelse, AvbrytKlageRequest, teksterAarsakTilAvbrytelse } from '~shared/types/Klage'

export default function AnnullerKlage() {
  const navigate = useNavigate()
  const [isOpen, setIsOpen] = useState(false)
  const [avbrytKlageStatus, avbrytKlagen] = useApiCall(avbrytKlage)

  const klage = useKlage()
  const klageRedigerbar = useKlageRedigerbar()

  if (!klage || !klageRedigerbar) {
    return null
  }

  const {
    control,
    handleSubmit,
    trigger,
    formState: { errors },
  } = useForm<AvbrytKlageRequest>({
    defaultValues: { klageId: klage.id, aarsakTilAvbrytelse: AarsakTilAvbrytelse.ANNET, kommentar: '' },
  })

  const avbryt = (request: AvbrytKlageRequest) => {
    avbrytKlagen(request, () => {
      setIsOpen(false)
      navigate(`/person/${klage?.sak?.ident}`)
    })
  }

  return (
    <>
      <ExpansionCardSpaced aria-labelledby="card-heading">
        <ExpansionCard.Header>
          <div className="with-icon">
            <div>
              <Button variant="secondary" icon={<TrashIcon />}>
                Annuller klage
              </Button>
            </div>
          </div>
        </ExpansionCard.Header>

        <ExpansionCard.Content>
          <AnnullerForm id="annuller-klage-form" onSubmit={handleSubmit(avbryt)}>
            <FlexRow>
              <Controller
                name="aarsakTilAvbrytelse"
                rules={{
                  required: { value: true, message: 'Du må velge en årsak for omgjøringen.' },
                  minLength: 1,
                }}
                control={control}
                render={({ field }) => {
                  const { value, ...rest } = field
                  return (
                    <>
                      <Select label="Årsak til annullering" value={value ?? ''} {...rest}>
                        {Object.entries(AarsakTilAvbrytelse).map(([key, value]) => (
                          <option key={key} value={key}>
                            {teksterAarsakTilAvbrytelse[value]}
                          </option>
                        ))}
                      </Select>
                    </>
                  )
                }}
              />
            </FlexRow>
            <Controller
              name="kommentar"
              rules={{
                validate: (value, formValues) => {
                  return formValues.aarsakTilAvbrytelse == 'ANNET' && value.length == 0
                    ? 'Du må skrive en kommentar'
                    : true
                },
              }}
              control={control}
              render={({ field }) => {
                const { value, ...rest } = field
                return (
                  <>
                    <Textarea
                      label="Begrunnelse"
                      description="Utdyp hvorfor klagebehandlingen annulleres"
                      value={value ?? ''}
                      {...rest}
                      size="medium"
                      error={errors?.kommentar?.message}
                    />
                  </>
                )
              }}
            />
            <FlexRow>
              <Button
                type="button"
                size="small"
                variant="danger"
                onClick={() => trigger().then((success) => success && setIsOpen(true))}
              >
                Annuller klage
              </Button>
            </FlexRow>
          </AnnullerForm>
        </ExpansionCard.Content>
      </ExpansionCardSpaced>

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-labelledby="modal-heading">
        <Modal.Header>
          <Heading level="1" spacing size="medium" id="modal-heading">
            <TrashIcon />
            Annuller klage
          </Heading>
        </Modal.Header>
        <Modal.Body>Er du sikker på at du vil annullere klagen? Klagen vil få status annullert.</Modal.Body>

        <Modal.Footer>
          <FlexRow justify="center">
            <Button variant="tertiary" onClick={() => setIsOpen(false)} loading={isPending(avbrytKlageStatus)}>
              Avbryt annullering
            </Button>
            <Button type="submit" form="annuller-klage-form" variant="danger" loading={isPending(avbrytKlageStatus)}>
              Annuller klage
            </Button>
          </FlexRow>
          {isFailureHandler({
            apiResult: avbrytKlageStatus,
            errorMessage: 'Det oppsto en feil ved avbryting av klagen.',
          })}
        </Modal.Footer>
      </Modal>
    </>
  )
}

const ExpansionCardSpaced = styled(ExpansionCard)`
  margin: 20px 8px 0 8px;
  border-radius: 3px;

  .title {
    white-space: nowrap;
  }

  .navds-expansioncard__header {
    border-radius: 3px;
  }

  .with-icon {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .icon {
    font-size: 2rem;
    flex-shrink: 0;
    display: grid;
    place-content: center;
  }
`

const AnnullerForm = styled.form`
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 2em;
  margin-bottom: 2em;
`
