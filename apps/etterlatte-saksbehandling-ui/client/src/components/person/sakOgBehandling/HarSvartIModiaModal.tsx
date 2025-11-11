import { Button } from '@navikt/ds-react'
import { PersonChatIcon } from '@navikt/aksel-icons'
import React from 'react'

export const HarSvartIModiaModal = () => {
  return (
    <>
      <Button variant="secondary" icon={<PersonChatIcon />} iconPosition="right">
        Har svart i Modia
      </Button>
    </>
  )
}
