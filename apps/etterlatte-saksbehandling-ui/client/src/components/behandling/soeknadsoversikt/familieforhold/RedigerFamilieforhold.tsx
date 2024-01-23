import { Alert, Button, Heading, Modal, Panel, TextField } from '@navikt/ds-react'
import { InputList, InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import React, { useState } from 'react'
import { PencilIcon, PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerFamilieforhold } from '~shared/api/behandling'
import { isFailure, isPending, isSuccess } from '~shared/api/apiUtils'
import { Personopplysninger } from '~shared/types/grunnlag'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useFieldArray, useForm } from 'react-hook-form'
import { fnrErGyldig } from '~utils/fnr'
import { FlexRow } from '~shared/styled'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'

type Props = {
  behandling: IDetaljertBehandling
  personopplysninger: Personopplysninger
}

// RHF krever at en array består av objekter
interface RHFRedigerbarFamilie {
  gjenlevende: Array<{ fnr: string }>
  avdoede: Array<{ fnr: string }>
}

export const RedigerFamilieforhold = ({ behandling, personopplysninger }: Props) => {
  const [isOpen, setIsOpen] = useState(false)
  const [feilmelding, setFeilmelding] = useState<string | null>(null)

  const [status, redigerFamilieforholdRequest] = useApiCall(redigerFamilieforhold)

  const {
    control,
    formState: { errors },
    handleSubmit,
    getValues,
    register,
    reset,
  } = useForm<RHFRedigerbarFamilie>({
    defaultValues: {
      gjenlevende: personopplysninger.gjenlevende.map((gjenlevende) => ({
        fnr: gjenlevende.opplysning.foedselsnummer,
      })),
      avdoede: personopplysninger.avdoede.map((avdoede) => ({ fnr: avdoede.opplysning.foedselsnummer })),
    },
  })
  const gjenlevendeListe = useFieldArray({ name: 'gjenlevende', control })
  const avdoedListe = useFieldArray({ name: 'avdoede', control })

  const lagre = () => {
    setFeilmelding(null)
    const familieforhold = getValues()
    const foreldre = [...familieforhold.avdoede, ...familieforhold.gjenlevende]

    if (foreldre.length != 2) {
      setFeilmelding('Mangler en eller flere forelder')
    } else {
      redigerFamilieforholdRequest({
        behandlingId: behandling.id,
        redigert: {
          gjenlevende: familieforhold.gjenlevende.map((v) => v.fnr),
          avdoede: familieforhold.avdoede.map((v) => v.fnr),
        },
      })
    }
  }

  const avbryt = () => {
    reset()
    setIsOpen(false)
  }

  const kanLeggeTil = () => {
    const familie = getValues()
    return familie.gjenlevende.length + familie.avdoede.length < 2
  }

  return (
    <>
      <Button
        variant="tertiary"
        size="small"
        onClick={() => setIsOpen(true)}
        icon={<PencilIcon />}
        style={{ float: 'right' }}
      >
        Rediger
      </Button>

      <Modal open={isOpen} onClose={avbryt}>
        <Modal.Header>
          <Heading size="medium" spacing>
            Rediger familieforhold
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <FormWrapper column={true}>
            <Panel border>
              <InputList>
                {avdoedListe.fields?.map((field, index) => (
                  <InputRow key={field.id}>
                    <TextField
                      {...register(`avdoede.${index}.fnr`, {
                        validate: {
                          fnrErGyldig: (value) => fnrErGyldig(value) || 'Ugyldig fødselsnummer',
                        },
                      })}
                      label="Avdød forelder"
                      description="Oppgi fødselsnummer"
                      error={errors?.avdoede?.[index]?.fnr?.message}
                    />
                    <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => avdoedListe.remove(index)} />
                  </InputRow>
                ))}
                <Button
                  icon={<PlusIcon />}
                  onClick={() => avdoedListe.append({ fnr: '' })}
                  disabled={!kanLeggeTil() || isPending(status)}
                >
                  Legg til avdøde
                </Button>
              </InputList>
            </Panel>

            <Panel border>
              <InputList>
                {gjenlevendeListe.fields?.map((field, index) => (
                  <InputRow key={field.id}>
                    <TextField
                      {...register(`gjenlevende.${index}.fnr`, {
                        validate: {
                          fnrErGyldig: (value) => fnrErGyldig(value) || 'Ugyldig fødselsnummer',
                        },
                      })}
                      label="Gjenlevende forelder"
                      description="Oppgi fødselsnummer"
                      error={errors?.gjenlevende?.[index]?.fnr?.message}
                    />
                    <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => gjenlevendeListe.remove(index)} />
                  </InputRow>
                ))}

                <Button
                  icon={<PlusIcon />}
                  onClick={() => gjenlevendeListe.append({ fnr: '' })}
                  disabled={!kanLeggeTil() || isPending(status)}
                >
                  Legg til gjenlevende
                </Button>
              </InputList>
            </Panel>
          </FormWrapper>

          <br />

          {isSuccess(status) && <Alert variant="success">Lagret redigert familieforhold</Alert>}
          {isFailure(status) && <Alert variant="error">Noe gikk galt!</Alert>}
          {feilmelding && <Alert variant="error">{feilmelding}</Alert>}
        </Modal.Body>

        <Modal.Footer>
          <FlexRow justify="right">
            <Button variant="secondary" onClick={avbryt} disabled={isPending(status)}>
              Avbryt
            </Button>
            <Button onClick={handleSubmit(lagre)} loading={isPending(status)}>
              Lagre
            </Button>
          </FlexRow>
        </Modal.Footer>
      </Modal>
    </>
  )
}
