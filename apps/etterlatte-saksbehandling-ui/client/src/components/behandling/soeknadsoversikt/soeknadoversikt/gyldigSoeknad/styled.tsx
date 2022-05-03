import styled from 'styled-components'

export const Wrapper = styled.div`
  display: flex;
  border-left: 4px solid #e5e5e5;
  max-height: fit-content;
  width: 350px;
  margin-bottom: 2em;
`
export const Endre = styled.div`
  display: inline-flex;
  height: 25px;
  display: inline-flex;
  cursor: pointer;
  color: #0056b4;

  .text {
    padding-bottom: 10px;
  }
`

export const Title = styled.div`
  display: flex;
  font-size: 18px;
  font-weight: bold;
`
export const Undertekst = styled.div<{ gray: boolean }>`
  display: flex;
  margin-bottom: ${(props) => (props.gray ? '1em;' : '0em')};
  margin-top: 0.3em;
  max-width: 250px;
  color: ${(props) => (props.gray ? '#707070' : '#000000')};
`
