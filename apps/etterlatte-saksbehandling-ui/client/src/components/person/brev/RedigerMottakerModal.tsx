import { Alert, Button, Heading, Modal, Select, TextField, ToggleGroup } from '@navikt/ds-react'
import { useState } from 'react'
import { IBrev, Mottaker } from '~shared/types/Brev'
import { DocPencilIcon } from '@navikt/aksel-icons'
import styled, { css } from 'styled-components'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterMottaker } from '~shared/api/brev'
import { FlexRow } from '~shared/styled'

enum MottakerType {
  PRIVATPERSON = 'PRIVATPERSON',
  BEDRIFT = 'BEDRIFT',
}

interface Props {
  brev: IBrev
  oppdater: (mottaker: Mottaker) => void
}

export default function RedigerMottakerModal({ brev, oppdater }: Props) {
  const { id: brevId, sakId, mottaker: initialMottaker } = brev

  const [isOpen, setIsOpen] = useState(false)
  const [mottakerStatus, apiOppdaterMottaker] = useApiCall(oppdaterMottaker)

  const [mottaker, setMottaker] = useState<Mottaker>(initialMottaker)

  const [mottakerType, setMottakerType] = useState(
    brev.mottaker.orgnummer ? MottakerType.BEDRIFT : MottakerType.PRIVATPERSON
  )

  const lagre = () => {
    apiOppdaterMottaker({ brevId, sakId, mottaker }, () => {
      oppdater(mottaker)
      setIsOpen(false)
    })
  }

  const avbryt = () => {
    setMottaker(initialMottaker)
    setIsOpen(false)
  }

  return (
    <>
      <Button
        variant="secondary"
        onClick={() => setIsOpen(true)}
        icon={<DocPencilIcon />}
        style={{ float: 'right' }}
        size="small"
      />

      <MottakerModal open={isOpen} onClose={avbryt}>
        <Modal.Body>
          <Heading size="large" spacing>
            Endre mottaker
          </Heading>

          <SkjemaGruppe>
            <ToggleGroup
              defaultValue={mottakerType}
              onChange={(value) => setMottakerType(value as MottakerType)}
              size="small"
              style={{ marginBottom: '1rem' }}
            >
              <ToggleGroup.Item value={MottakerType.PRIVATPERSON}>Privatperson</ToggleGroup.Item>
              <ToggleGroup.Item value={MottakerType.BEDRIFT}>Bedrift</ToggleGroup.Item>
            </ToggleGroup>

            {mottakerType == MottakerType.BEDRIFT && (
              <TextField
                label="Orgnummer"
                onChange={(e) => setMottaker({ ...mottaker, orgnummer: e.target.value })}
                value={mottaker.orgnummer || ''}
              />
            )}
            {mottakerType == MottakerType.PRIVATPERSON && (
              <TextField
                label="Fødselsnummer"
                onChange={(e) => setMottaker({ ...mottaker, foedselsnummer: { value: e.target.value } })}
                value={mottaker.foedselsnummer?.value || ''}
                pattern="[0-9]+"
              />
            )}
          </SkjemaGruppe>

          <SkjemaGruppe>
            <TextField
              label="Navn"
              onChange={(e) => setMottaker({ ...mottaker, navn: e.target.value })}
              value={mottaker.navn || ''}
            />
          </SkjemaGruppe>

          <SkjemaGruppe>
            <Select
              label="Adressetype"
              onChange={(e) =>
                setMottaker({ ...mottaker, adresse: { ...mottaker.adresse, adresseType: e.target.value } })
              }
              value={mottaker.adresse?.adresseType || ''}
              required
            >
              <option value="">Velg type</option>
              <option value="NORSKPOSTADRESSE">Norsk postadresse</option>
              <option value="UTENLANDSKPOSTADRESSE">Utenlandsk postadresse</option>
            </Select>
          </SkjemaGruppe>

          <SkjemaGruppe>
            <TextField
              label="Adresselinje 1"
              onChange={(e) =>
                setMottaker({
                  ...mottaker,
                  adresse: { ...mottaker.adresse, adresselinje1: e.target.value },
                })
              }
              value={mottaker.adresse?.adresselinje1 || ''}
              required
            />
            <TextField
              label="Adresselinje 2"
              onChange={(e) =>
                setMottaker({
                  ...mottaker,
                  adresse: { ...mottaker.adresse, adresselinje2: e.target.value },
                })
              }
              value={mottaker.adresse?.adresselinje2 || ''}
            />
            <TextField
              label="Adresselinje 3"
              onChange={(e) =>
                setMottaker({
                  ...mottaker,
                  adresse: { ...mottaker.adresse, adresselinje3: e.target.value },
                })
              }
              value={mottaker.adresse?.adresselinje3 || ''}
            />
          </SkjemaGruppe>

          <SkjemaGruppe inline>
            <TextField
              label="Postnummer"
              onChange={(e) =>
                setMottaker({
                  ...mottaker,
                  adresse: { ...mottaker.adresse, postnummer: e.target.value },
                })
              }
              value={mottaker.adresse?.postnummer || ''}
              maxLength={4}
              pattern="[0-9]+"
            />
            <TextField
              label="Poststed"
              onChange={(e) =>
                setMottaker({
                  ...mottaker,
                  adresse: { ...mottaker.adresse, poststed: e.target.value },
                })
              }
              value={mottaker.adresse?.poststed || ''}
            />
          </SkjemaGruppe>

          <SkjemaGruppe inline>
            <TextField
              label="Landkode"
              onChange={(e) =>
                setMottaker({
                  ...mottaker,
                  adresse: { ...mottaker.adresse, landkode: e.target.value },
                })
              }
              value={mottaker.adresse?.landkode || ''}
              pattern="[A-Z]{2}"
              minLength={2}
              maxLength={2}
              required
            />
            <TextField
              label="Land"
              onChange={(e) =>
                setMottaker({
                  ...mottaker,
                  adresse: { ...mottaker.adresse, land: e.target.value },
                })
              }
              value={mottaker.adresse?.land || ''}
              required
            />
          </SkjemaGruppe>

          {isFailure(mottakerStatus) && <Alert variant="error">Kunne ikke oppdatere mottaker...</Alert>}

          <FlexRow justify="right">
            <Button variant="secondary" disabled={isPending(mottakerStatus)} onClick={avbryt}>
              Avbryt
            </Button>
            <Button variant="primary" loading={isPending(mottakerStatus)} onClick={lagre}>
              Lagre
            </Button>
          </FlexRow>
        </Modal.Body>
      </MottakerModal>
    </>
  )
}

const MottakerModal = styled(Modal)`
  width: 40rem;
  padding: 3rem;
`

const SkjemaGruppe = styled.div<{ inline?: boolean }>`
  & > * {
    margin-bottom: 1rem;
  }

  ${(props) => {
    if (props.inline)
      return css`
        display: flex;
        flex-direction: row;
        gap: 1rem;

        & > * {
          flex: 1;
        }
      `
  }};
`
