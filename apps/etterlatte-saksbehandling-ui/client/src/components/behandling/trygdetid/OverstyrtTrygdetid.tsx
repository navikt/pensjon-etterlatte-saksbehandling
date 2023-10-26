import { FlexHeader } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { BodyShort, Button, Heading, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useEffect, useState } from 'react'
import { FormKnapper } from './styled'

interface Props {
  redigerbar: boolean
  trygdetid: ITrygdetid
  virkningstidspunktEtterNyRegelDato: Boolean
  overstyrTrygdetidPoengaar: (trygdetid: ITrygdetid) => void
}

export const OverstyrtTrygdetid = ({
  redigerbar,
  trygdetid,
  virkningstidspunktEtterNyRegelDato,
  overstyrTrygdetidPoengaar,
}: Props) => {
  const [overstyrtNorskPoengaar, setOverstyrtNorskPoengaar] = useState<number | undefined>(undefined)

  useEffect(() => {
    setOverstyrtNorskPoengaar(trygdetid.overstyrtNorskPoengaar ?? undefined)
  }, [])

  const opptjentTekst = () => {
    return virkningstidspunktEtterNyRegelDato
      ? 'men har minimum tre poengår'
      : 'men har opptjent rett til tilleggspensjon'
  }

  return (
    <Overstyrt>
      {(redigerbar || overstyrtNorskPoengaar !== undefined) && (
        <FlexHeader>
          <Heading size="small" level="4">
            Poengår i Norge
          </Heading>
        </FlexHeader>
      )}
      {!redigerbar && overstyrtNorskPoengaar !== undefined && (
        <>
          <BodyShort>
            Eksportvurdering - pensjonen er innvilget etter unntaksregelen om at avdød har mindre enn 20 års botid,
            {opptjentTekst()}:
          </BodyShort>
          <AntallAar>{trygdetid.overstyrtNorskPoengaar}</AntallAar>
        </>
      )}
      {redigerbar && (
        <>
          <BodyShort>
            Fyll ut ved eksportvurdering når pensjonen er innvilget etter unntaksregelen om at avdød har mindre enn 20
            års botid, {opptjentTekst()}. Trygdetid skal da være lik antall poengår.
          </BodyShort>
          <AntallAarFelt
            value={overstyrtNorskPoengaar ?? ''}
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            label="Antall år"
            onChange={(e) => setOverstyrtNorskPoengaar(e.target.value === '' ? undefined : Number(e.target.value))}
            onBlur={() => overstyrTrygdetidPoengaar({ ...trygdetid, overstyrtNorskPoengaar: overstyrtNorskPoengaar })}
          />
          <FormKnapper>
            <Button
              size="small"
              onClick={(event) => {
                event.preventDefault()
                setOverstyrtNorskPoengaar(undefined)
                overstyrTrygdetidPoengaar({ ...trygdetid, overstyrtNorskPoengaar: undefined })
              }}
              disabled={overstyrtNorskPoengaar === undefined}
            >
              Slett
            </Button>
          </FormKnapper>
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
