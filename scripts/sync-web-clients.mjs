// Author: Soidraw. The server HTML is the source for the bundled app clients.
import { readFileSync, writeFileSync } from 'node:fs';

const root = new URL('../', import.meta.url);
const source = readFileSync(new URL('src/main/resources/web/index.html', root));
const targets = ['mobile-app/www/index.html', 'harmonyos-app/entry/src/main/resources/rawfile/index.html'];
for (const target of targets) {
    if (process.argv.includes('--check')) {
        if (!source.equals(readFileSync(new URL(target, root)))) throw new Error(`Web client differs: ${target}`);
    } else {
        writeFileSync(new URL(target, root), source);
    }
}
console.log(process.argv.includes('--check') ? 'Web clients are in sync.' : 'Web clients synchronized.');
