import { FlexHeader } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { BodyShort, Button, Heading, TextField } from '@navikt/ds-react'
import styled from 'styled-components'
import { ITrygdetid } from '~shared/api/trygdetid'
import { useEffect, useState } from 'react'
import { FormKnapper } from './styled'
import { SakType } from '~shared/types/sak'

interface Props {
  redigerbar: boolean
  sakType: SakType
  trygdetid: ITrygdetid
  virkningstidspunktEtterNyRegelDato: Boolean
  overstyrTrygdetidPoengaar: (trygdetid: ITrygdetid) => void
}

export const OverstyrtTrygdetid = ({
  redigerbar,
  sakType,
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
          {
            {
              [SakType.BARNEPENSJON]: (
                <BodyShort>
                  Eksportvurdering - pensjonen er innvilget etter unntaksregelen om at avdød har mindre enn 20 års
                  botid,
                  {opptjentTekst()}:
                </BodyShort>
              ),
              [SakType.OMSTILLINGSSTOENAD]: (
                <BodyShort>
                  Ved eksport skal trygdetid være lik avdødes antall poengår fordi samlet botid til avdøde eller
                  gjenlevende er mindre enn 20 år:
                </BodyShort>
              ),
            }[sakType]
          }
          <AntallAar>{trygdetid.overstyrtNorskPoengaar} år</AntallAar>
        </>
      )}
      {redigerbar && (
        <>
          {
            {
              [SakType.BARNEPENSJON]: (
                <BodyShort>
                  Fyll ut ved eksportvurdering når pensjonen er innvilget etter unntaksregelen om at avdød har mindre
                  enn 20 års botid, {opptjentTekst()}. Trygdetid skal da være lik antall poengår.
                </BodyShort>
              ),
              [SakType.OMSTILLINGSSTOENAD]: (
                <BodyShort>
                  Fyll ut ved eksportvurderdering når stønaden er innvilget etter unntaksregelen om å få trygdetid lik
                  avdødes antall poengår hvis samlet botid til avdøde eller gjenlevende er mindre enn 20 år.
                </BodyShort>
              ),
            }[sakType]
          }
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
