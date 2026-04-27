let count = 0;
function useItem() {
  const id = `item_${count++}`;
  return id;
}
console.log(useItem());
console.log(useItem());
