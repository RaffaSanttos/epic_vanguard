// Gera o template de GameTest: plataforma de pedra 9x9 com 4 blocos de ar em cima.
// Uso: node tools/gametest/make_platform.js
const fs = require('fs'), path = require('path'), zlib = require('zlib');
const OUT = path.resolve(__dirname, '../../src/gametest/resources/data/epicvanguard/structures/gametest/platform.nbt');

// Escritor NBT mínimo (só os tipos usados aqui)
const T = { END: 0, INT: 3, STRING: 8, LIST: 9, COMPOUND: 10 };
const parts = [];
const u8 = v => parts.push(Buffer.from([v]));
const i16 = v => { const b = Buffer.alloc(2); b.writeInt16BE(v); parts.push(b); };
const i32 = v => { const b = Buffer.alloc(4); b.writeInt32BE(v); parts.push(b); };
const str = s => { const b = Buffer.from(s, 'utf8'); i16(b.length); parts.push(b); };
function payload(type, v) {
  if (type === T.INT) i32(v);
  else if (type === T.STRING) str(v);
  else if (type === T.LIST) { u8(v.type); i32(v.items.length); v.items.forEach(x => payload(v.type, x)); }
  else if (type === T.COMPOUND) { for (const [k, [t, x]] of Object.entries(v)) { u8(t); str(k); payload(t, x); } u8(T.END); }
}
const SIZE_X = 9, SIZE_Y = 5, SIZE_Z = 9;
const blocks = [];
for (let x = 0; x < SIZE_X; x++) for (let z = 0; z < SIZE_Z; z++)
  blocks.push({ pos: [T.LIST, { type: T.INT, items: [x, 0, z] }], state: [T.INT, 0] });
const root = {
  DataVersion: [T.INT, 3465], // 1.20.1
  size: [T.LIST, { type: T.INT, items: [SIZE_X, SIZE_Y, SIZE_Z] }],
  palette: [T.LIST, { type: T.COMPOUND, items: [{ Name: [T.STRING, 'minecraft:stone'] }] }],
  blocks: [T.LIST, { type: T.COMPOUND, items: blocks }],
  entities: [T.LIST, { type: T.END, items: [] }],
};
u8(T.COMPOUND); str(''); payload(T.COMPOUND, root);
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, zlib.gzipSync(Buffer.concat(parts)));
console.log('ok', path.relative(process.cwd(), OUT));
