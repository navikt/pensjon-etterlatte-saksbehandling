export const combineReducers = (reducers: any) => {
  return (state: any, action: any) => {
    const tempState = { ...state };
    Object.keys(reducers).forEach((key) => {
      tempState[key] = reducers[key](tempState[key], action);
    });
    return tempState;
  };
};