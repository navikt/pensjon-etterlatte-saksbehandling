import { FlexHeader } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { BodyShort, Heading, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useEffect, useState } from 'react'

interface Props {
  redigerbar: boolean
  trygdetid: ITrygdetid
  overstyrTrygdetidPoengaar: (trygdetid: ITrygdetid) => void
}

export const OverstyrtTrygdetid = ({ redigerbar, trygdetid, overstyrTrygdetidPoengaar }: Props) => {
  const [overstyrtNorskPoengaar, setOverstyrtNorskPoengaar] = useState<number | undefined>(undefined)

  useEffect(() => {
    setOverstyrtNorskPoengaar(trygdetid.overstyrtNorskPoengaar)
  }, [])

  return (
    <Overstyrt>
      <FlexHeader>
        <Heading size="small" level="4">
          Norsk poengår
        </Heading>
      </FlexHeader>
      {!redigerbar && (
        <>
          <BodyShort>
            Den avdøde har mindre enn 20 års botid, men har opptjent rett til tilleggspensjon - norsk poengår:
          </BodyShort>
          <AntallAar>{trygdetid.overstyrtNorskPoengaar} 12</AntallAar>
        </>
      )}
      {redigerbar && (
        <>
          <BodyShort>
            Hvis den avdøde har mindre enn 20 års botid, men har opptjent rett til tilleggspensjon - så må antall
            poengår i Norge spesifiseres
          </BodyShort>
          <AntallAarFelt
            value={overstyrtNorskPoengaar}
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            label="Norsk poengår"
            onChange={(e) => setOverstyrtNorskPoengaar(e.target.value === '' ? undefined : Number(e.target.value))}
            onBlur={() => overstyrTrygdetidPoengaar({ ...trygdetid, overstyrtNorskPoengaar: overstyrtNorskPoengaar })}
          />
        </>
      )}
    </Overstyrt>
  )
}

const Overstyrt = styled.div`
  padding: 2em 0 0 0;
`

const AntallAar = styled(BodyShort)`
  padding: 1em 0 0 0;
`

const AntallAarFelt = styled(TextField)`
  padding: 1em 0 0 0;
  width: 10em;
`
