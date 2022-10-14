import styled from 'styled-components'

export const KolonneContainer = styled.div`
  display: flex;
  width: 100%;
  height: 100%;
  margin-bottom: 1rem;
`

export const VenstreKolonne = styled.div`
  width: calc(70% - 1rem);
  padding-right: 1rem;
`

export const HoeyreKolonne = styled.div`
  width: calc(30% - 1rem);
  padding-left: 1rem;
`

export const HoeyreInnhold = styled.div`
  height: 100%;
  position: sticky;
  top: 20px;
`
