import { useState } from 'react'
import { useMatch } from 'react-router-dom'
import { Button, Link } from '@navikt/ds-react'
import { useBehandlingRoutes } from './BehandlingRoutes'
import { avbrytBehandling } from '../../shared/api/behandling'
import { useNavigate } from 'react-router-dom'
import { Modal } from '../../shared/modal/modal'
import styled from 'styled-components'
import { IApiResponse } from '../../shared/api/types'
import { WarningText } from '../../shared/styled'

export const BehandlingHandlingKnapper = () => {
  const { next, back, lastPage, firstPage } = useBehandlingRoutes()
  const navigate = useNavigate()
  const match = useMatch('/behandling/:behandlingId/*')

  const [isOpen, setIsOpen] = useState(false)
  const [error, setError] = useState(false)

  const avbryt = () => {
    //TODO!
    if (match?.params.behandlingId) {
      avbrytBehandling(match.params.behandlingId).then((response: IApiResponse<any>) => {
        if (response.status !== 500) {
          navigate('/')
        } else {
          console.log(response)
          setError(true)
        }
      })
    }
  }
  //TODO: andre tekster for knapper og innhold i modal
  //TODO: Ferdigknapp på siste side?

  return (
    <VilkaarsKnapperWrapper>
      <div>
        <Button variant="secondary" size="medium" className="button" onClick={back} disabled={firstPage}>
          Tilbake
        </Button>
        <Button variant="primary" size="medium" className="button" onClick={next} disabled={lastPage}>
          Neste
        </Button>
      </div>
      <Link className="textButton" onClick={() => setIsOpen(true)}>
        Avbryt behandling
      </Link>

      {isOpen && (
        <Modal
          onClose={() => {
            setIsOpen(false)
            setError(false)
          }}
        >
          <h2>Er du sikker på at du vil avbryte behandlingen?</h2>
          <Button
            variant="primary"
            size="medium"
            className="button"
            onClick={() => {
              setIsOpen(false)
              setError(false)
            }}
          >
            Nei, fortsett behandling
          </Button>
          <Button variant="secondary" size="medium" className="button" onClick={avbryt}>
            Ja, avbryt behandling
          </Button>
          {error && <WarningText>Det oppsto enn feil ved avbryting av behandlingen.</WarningText>}
        </Modal>
      )}
    </VilkaarsKnapperWrapper>
  )
}

export const VilkaarsKnapperWrapper = styled.div`
  margin: 3em 0em 2em 0em;
  text-align: center;

  .button {
    width: 200px;
    margin: 0em 1em 0em 1em;
  }
  .textButton {
    margin-top: 2em;
    text-decoration: none;
    cursor: pointer;
    font-weight: bold;
    &:hover {
      text-decoration: underline;
    }
  }
`
