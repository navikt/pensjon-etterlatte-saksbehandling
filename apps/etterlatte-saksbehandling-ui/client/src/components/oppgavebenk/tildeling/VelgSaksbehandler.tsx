import React, { ReactNode, useState } from 'react'
import { Button, Dropdown, Select } from '@navikt/ds-react'
import { PersonCrossIcon, PersonPencilIcon, PersonPlusIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'

const saksbehandlere: Array<string> = ['Ikke tildelt', 'Pelle Politi', 'Lars Monsen', 'Jens Monsen']

export const VelgSaksbehandler = (): ReactNode => {
  const [openDropdown, setOpenDropdown] = useState<boolean>(false)

  const [valgtSaksbehandler, setValgtSaksbehandler] = useState<string>()

  const onSaksbehandlerSelect = (saksbehandler: string) => {
    setValgtSaksbehandler(saksbehandler)
    setOpenDropdown(false)
  }

  const onTildelTilMeg = () => {
    // TODO: endre til å være innlogget sb
    setValgtSaksbehandler('Tildelt meg')
    setOpenDropdown(false)
  }

  const onFjernTildeling = () => {
    setValgtSaksbehandler('')
    setOpenDropdown(false)
  }

  return (
    <div>
      <Dropdown open={openDropdown}>
        <Button
          as={Dropdown.Toggle}
          icon={valgtSaksbehandler ? <PersonPencilIcon /> : <PersonPlusIcon />}
          iconPosition="right"
          size="small"
          variant="tertiary"
          onClick={() => setOpenDropdown(true)}
        >
          {valgtSaksbehandler ? valgtSaksbehandler : 'Ikke tildelt'}
        </Button>
        <Dropdown.Menu onClose={() => setOpenDropdown(false)}>
          <MenyWrapper>
            <div>
              <Select
                label="Velg saksbehandler"
                value={valgtSaksbehandler}
                onChange={(e) => onSaksbehandlerSelect(e.target.value)}
              >
                {saksbehandlere.map((saksbehandler) => {
                  return <option value={saksbehandler}>{saksbehandler}</option>
                })}
              </Select>
              <TildelTilMegButton variant="tertiary" size="xsmall" onClick={onTildelTilMeg}>
                Tildel til meg
              </TildelTilMegButton>
            </div>
            <div>
              <Button
                variant="danger"
                size="small"
                onClick={onFjernTildeling}
                icon={<PersonCrossIcon />}
                iconPosition="right"
              >
                Fjern tildeling
              </Button>
            </div>
          </MenyWrapper>
        </Dropdown.Menu>
      </Dropdown>
    </div>
  )
}

const MenyWrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 11rem;
`

const TildelTilMegButton = styled(Button)`
  margin-top: 0.75rem;
`
