import styled from 'styled-components'

export const TableWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  max-width: 600px;

  .table {
    max-width: 500px;
    margin-bottom: 1em;

    .text {
      float: right;
    }

    .tableCell {
      max-width: 100px;
    }
  }
  .icon {
    margin-top: 15px;
  }
`

export const InfoWrapper = styled.div`
  margin-bottom: 30px;
  border: 1px solid #b0b0b0;
  padding: 1.2em 1em 2em 1em;
  width: 80%;
  max-width: 800px;
`

export const DetailWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;  
  padding-top: 0.5em;
  padding-left: 1em;
  justify-content: space-between;
  max-width: 500px;

  .detail {
    width: 200px;
  }
`

export const PersonInfoWrapper = styled.div`
  margin-bottom: 20px;
  border-radius: 4px;
  background-color: #f7f7f7;
  border: 1px solid #b0b0b0;
  padding: 1.2em 1em 2em 1em;
  width: 80%;
  max-width: 800px;

`
export const PersonDetailWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;  
  padding-top: 0.5em;
  padding-left: 1em;
  justify-content: space-between;
  max-width: 500px;

  .bodyShort {
    width: 200px;
  }
  .detail {
    width: 200px;
    color: #707070;
  }

`

export const RadioGroupWrapper = styled.div`
  margin-top: 5em;
  max-width: 400px;

  .link{
    font-weight: bold;
    margin-top: 1em;
    margin-left: 1.5em;
  }
  .radioGroup{
    margin-bottom: 1em;
  }
  .button {
    margin-top: 1.5em;
    padding: 0.5em 3em;
  }
`

export const PersonInfoHeader = styled.div`
  display: inline;
  font-weight: bold;
  padding-left: 0.5em;
`

export const HeadingWrapper = styled.div`
  display: inline-flex;
  
  .details{
    justify-content: center;
    align-item: center;
    padding: 0.2em;
  }
`

export const TextButtonWrapper = styled.div`  
margin-left: auto;
margin-right: 5em;
margin-bottom: 0;

.textButton{
  margin-bottom: 0;
  display: inline-flex;
  justify-content: space-between;
  text-decoration: underline;
  color: #0067c5;
  :hover {
    cursor: pointer;
  }
  .dropdownIcon {
    margin-bottom: 0;
    margin-left: 0.5em;
    margin-top 0.1em;
 
  }
}
`
