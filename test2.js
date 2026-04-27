const item1 = { id: 1, onRelocate: () => console.log('a') };
const item2 = { id: 1, onRelocate: () => console.log('b') };
console.log(item1.onRelocate === item2.onRelocate);
